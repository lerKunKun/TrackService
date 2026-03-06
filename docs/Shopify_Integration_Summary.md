# Shopify OAuth 与 Webhook 集成逻辑总结文档

本文档总结了本项目中关于 Shopify OAuth 授权流程以及 Webhook 订阅与处理的核心代码逻辑与业务流程。

## 1. 核心类说明

*   **OAuth 相关**:
    *   `ShopifyOAuthController.java`: 处理应用授权的进入和回调。
    *   `ShopifyOAuthService.java`: 提供组装授权 URL、Token 换取、HMAC 签名校验、店铺基础信息获取（优先 GraphQL）等底层支持。
*   **Webhook 相关**:
    *   `ShopifyWebhookController.java`: 接收并处理 Shopify 各种事件的回调（如订单、应用卸载、争议等）。
    *   `ShopifyWebhookService.java`: 负责在店铺授权后通过 API 自动注册所需的 Webhook 主题，并提供回调请求的 HMAC 签名校验方案。

---

## 2. Shopify OAuth 授权流程 (OAuth Flow)

授权流程采用了 Shopify 标准的授权码模式 (Authorization Code Grant)，并请求了离线访问令牌 (Offline Access Token) 以便后台持久化调用。

### 2.1 触发授权 (`/oauth/shopify/authorize`)
此接口为开始 OAuth 流程的入口，主要接收前端传入的 `shopDomain`：
1.  **域名校验检查**：确保传入的是标准的 `.myshopify.com` 格式。
2.  **生成授权链接**：调用 Service 层拼接 `https://{shop}/admin/oauth/authorize` 并带上 Client ID、回调地址、授权范围。
    *   *注：参数中硬编码了 `grant_options[]=offline` 以获取永久型的 Token。*
    *   *Scope 包含：`read_orders, write_orders, read_products, write_products`*。
3.  **State 防伪造 (CSRF)**：生成随机 `state` 字符串并存入 Redis 中防篡改（有效期设置5分钟）。
4.  **跳转**：向前端返回 302 重定向到 Shopify 的授权安装界面。

### 2.2 授权回调处理 (`/oauth/shopify/callback`)
当用户在 Shopify 后台确认安装/授权后，Shopify 会重定向回该接口，携带 `code`, `shop`, `hmac`, `state` 字段：
1.  **安全阻断检测**：
    *   比对请求中的 `state` 与 Redis 中存储的值是否一致，防范 CSRF。
    *   提取完整的 Query String，通过应用 Secret 重新计算 HMAC 进行比对，确保回调数据未被篡改。
2.  **换取 Token**：使用 `code` 通过请求 `oauth/access_token` 端点，换取该店铺的访问令牌 (`access_token`)。
3.  **拉取店铺资料**：通过 Token 请求 Shopify 获取店铺详情。这里设计了**主备方案**：
    *   主要方案：调用 GraphQL API (`getShopInfoGraphQL`) 拉取，成功率和字段扩展性强。
    *   备用方案：遇到网络或语法异常，自动回退到 REST API (`getShopInfoREST`) 查询基础信息 (`shop.json`)。
4.  **数据落库与绑定**：构建 `Shop` 实体对象：
    *   存储 Token、状态、套餐、时区等关键数据。
    *   若库中不存在，将新店铺与当前操作用户 (`UserContextHolder.getCurrentUserId()`) 关联；若无前台上下文，会 Fallback 将所有权赋予默认的 admin 管理员账号。
    *   调用 `ShopService.saveOrUpdateShop` 统一落库。
5.  **自动注册 Webhooks**：**异步/分离** 调用 `webhookService.registerAllWebhooks()` 自动打通回调事件链路（详见下面第三部分），不阻塞主授权流程。
6.  **完结跳出**：最后重定向回系统前端页面（如配置的 `frontendRedirect`），传递 `oauth=success` 或错误原因以便前端给予提示。

---

## 3. Shopify Webhook 连接与处理机制

本项目采用服务端统一订阅和鉴权机制来实现业务的数据推拉结合。

### 3.1 自动订阅 (Webhook Registration)
在 OAuth 授权成功的回调流程中，会默认触发 `registerAllWebhooks`，为该店铺注册 6 个核心 Webhook Topic：
*   `shop/update`
*   `app/uninstalled`
*   `orders/create`
*   `orders/updated`
*   `disputes/create`
*   `disputes/update`

**注册逻辑如下**：
程序会先请求 Shopify 查询已生效的订阅，进行对比比对。如果 Topic 一样但回调地址（Address）变了，则自动先删除旧的，再通过向 `/admin/api/{version}/webhooks.json` 发送 POST 请求将上述回调主题与服务端的 API 端点 (`/webhooks/shopify/*`) 一一绑定。

### 3.2 Webhook 接收端处理
对于所有的 Webhook 回调接收，首先强制校验请求 Header 中自带的 `X-Shopify-Hmac-SHA256` 签名。校验通过后才分配至以下各个子业务流程：

#### 1) 店铺资料更新 (`/shop-update`)
*   **动作**：提取请求体内的 name, email, timezone, domain 等字段。
*   **影响**：动态同步保持数据库的 `Shop` 表中字段为最新状态。

#### 2) 应用卸载事件 (`/app-uninstalled`)
*   **动作**：捕获商家卸载 App 的行为。
*   **影响**：
    *   直接将表中的 `connectionStatus` 置为 "invalid"，`isActive` 设置为 false。
    *   调用钉钉消息通知模块 (`DingtalkNotificationService`) 发送【应用被卸载】告警到运维/业务群。

#### 3) 订单新建 (`/orders-create`)
*   **动作**：解析出订单的全量 json 数据。
*   **影响**：委托外层 `OrderService.saveOrderFromWebhook()`，由其执行具体订单入库的映射。

#### 4) 订单更新及物流捕获 (`/orders-updated`)
*   **动作**：订单发生变动同步入账的基础上，有一层关键附加逻辑：物流追踪。
*   **影响**：
    *   同样会调用 `saveOrderFromWebhook()` 同步订单自身变化（如付款状态、发货状态）。
    *   遍历订单内 `fulfillments`（履约记录）列表。若检测到 `tracking_number` (追踪单号) 及 `tracking_company` 字段存在，则会自动构造追踪请求 (`TrackingRequest`) ，调用 `trackingService.create()` 注册物流查询任务，实现 Shopify 物流单号被自动捕获至此自建物流追踪平台分析。

#### 5) 支付争议告警 (`/disputes-create` & `/disputes-update`)
*   **动作**：针对买家发起的信用卡拒付等争议场景的专项捕获。
*   **影响**：解析金额 (amount)、币种 (currency)、争议原因 (reason)、状态 (status)及举证截止期 (evidence_due_by)。系统不进行复杂业务落库，而是第一时间拼装内容，通过钉钉发送【支付争议告警/更新】，提醒商家在举证截止日期内去处理该笔冻结款项。
