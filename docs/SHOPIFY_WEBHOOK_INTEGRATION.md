# Shopify Webhook 集成指南

## 📚 概述

本文档说明如何使用Shopify Webhook功能来自动同步店铺信息、订单数据和处理App卸载事件。

### 什么是Webhook?

Webhook是Shopify推送到你的服务器的HTTP POST请求,当特定事件发生时(如订单创建、店铺信息更新等),Shopify会自动调用你预先注册的URL。

**优势**:
- ✅ **实时性**: 事件发生时立即收到通知,无需轮询
- ✅ **自动化**: 店铺信息变更自动同步,无需手动更新
- ✅ **资源节省**: 相比定时轮询,大幅减少API调用次数
- ✅ **可靠性**: Shopify会重试失败的webhook,保证消息送达

---

## 🎯 已实现的Webhook类型

| Topic | URL | 用途 | 优先级 |
|-------|-----|------|--------|
| `shop/update` | `/api/v1/webhooks/shopify/shop-update` | 店铺信息更新时自动同步 | 🔥 高 |
| `app/uninstalled` | `/api/v1/webhooks/shopify/app-uninstalled` | App被卸载时标记店铺失效 | 🔥 高 |
| `orders/create` | `/api/v1/webhooks/shopify/orders-create` | 新订单创建通知 | 🔶 中 |
| `orders/updated` | `/api/v1/webhooks/shopify/orders-updated` | 订单更新(含物流信息) | 🔶 中 |

---

## 🔄 完整的集成流程

### 步骤1: OAuth授权 (自动注册Webhooks)

当店铺完成OAuth授权后,系统会**自动注册**所有webhooks:

```
流程:
1. 用户访问: /api/v1/oauth/shopify/authorize?shopDomain=xxx.myshopify.com
2. 用户在Shopify页面授权
3. Shopify回调: /api/v1/oauth/shopify/callback
4. 系统保存access_token
5. 🔑 系统自动调用 registerAllWebhooks() 注册4个webhooks
6. 完成
```

**实现代码位置**: `ShopifyOAuthController.java:198-206`

```java
// 6. 注册Webhooks (异步执行,不阻塞OAuth流程)
try {
    Map<String, Object> webhookResult = webhookService.registerAllWebhooks(shop, accessToken);
    log.info("Webhook registration result for {}: {} success, {} failed",
            shop, webhookResult.get("totalSuccess"), webhookResult.get("totalFailed"));
} catch (Exception webhookError) {
    log.error("Failed to register webhooks for shop: {}, but OAuth succeeded", shop, webhookError);
    // Webhook注册失败不影响OAuth流程
}
```

---

### 步骤2: Webhook接收与验证

当Shopify推送webhook时:

```
Shopify → POST /api/v1/webhooks/shopify/{topic}
Headers:
  - X-Shopify-Shop-Domain: xxx.myshopify.com
  - X-Shopify-Hmac-SHA256: [签名]
  - X-Shopify-Topic: shop/update
Body: JSON格式的事件数据
```

**安全验证**:
系统使用`X-Shopify-Hmac-SHA256`头验证请求来自Shopify:

```java
// ShopifyWebhookService.java:231-259
public boolean verifyWebhookSignature(String requestBody, String hmacHeader) {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKeySpec = new SecretKeySpec(
        shopifyApiSecret.getBytes(StandardCharsets.UTF_8),
        "HmacSHA256"
    );
    mac.init(secretKeySpec);

    byte[] hash = mac.doFinal(requestBody.getBytes(StandardCharsets.UTF_8));
    String calculatedHmac = Base64.getEncoder().encodeToString(hash);

    return calculatedHmac.equals(hmacHeader);
}
```

❌ **如果签名验证失败,返回401 Unauthorized,Shopify会重试**

---

## 📡 Webhook处理详解

### 1. Shop/Update - 店铺信息更新

**触发时机**:
- 店铺名称变更
- 店铺邮箱变更
- 时区设置变更
- 域名变更

**处理逻辑**:
```java
// ShopifyWebhookController.java:44-94
@PostMapping("/shop-update")
public ResponseEntity<Void> handleShopUpdate(
    @RequestBody String payload,
    @RequestHeader("X-Shopify-Shop-Domain") String shopDomain,
    @RequestHeader("X-Shopify-Hmac-SHA256") String hmac) {

    // 1. 验证签名
    if (!webhookService.verifyWebhookSignature(payload, hmac)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // 2. 解析JSON
    JsonNode shopData = objectMapper.readTree(payload);

    // 3. 查找店铺
    Shop shop = shopService.getByShopDomain(shopDomain);

    // 4. 更新字段
    if (shopData.has("name")) {
        shop.setShopName(shopData.get("name").asText());
    }
    if (shopData.has("iana_timezone")) {
        shop.setTimezone(shopData.get("iana_timezone").asText());
    }

    // 5. 保存
    shop.setLastSyncTime(LocalDateTime.now());
    shopService.update(shop);

    return ResponseEntity.ok().build();
}
```

**Webhook Payload示例**:
```json
{
  "id": 548380009,
  "name": "My Shopify Store",
  "email": "admin@example.com",
  "domain": "shop.example.com",
  "myshopify_domain": "my-store.myshopify.com",
  "iana_timezone": "America/New_York",
  "currency": "USD",
  "plan_name": "shopify_plus"
}
```

---

### 2. App/Uninstalled - App卸载通知

**触发时机**:
- 商家在Shopify后台卸载你的App

**处理逻辑**:
```java
// ShopifyWebhookController.java:96-141
@PostMapping("/app-uninstalled")
public ResponseEntity<Void> handleAppUninstalled(
    @RequestBody String payload,
    @RequestHeader("X-Shopify-Shop-Domain") String shopDomain,
    @RequestHeader("X-Shopify-Hmac-SHA256") String hmac) {

    // 验证签名
    if (!webhookService.verifyWebhookSignature(payload, hmac)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // 查找店铺
    Shop shop = shopService.getByShopDomain(shopDomain);

    // 标记为失效
    shop.setConnectionStatus("invalid");
    shop.setIsActive(false);
    shop.setLastSyncTime(LocalDateTime.now());
    shopService.update(shop);

    log.info("Marked shop as uninstalled: {}", shopDomain);

    // TODO: 发送通知给管理员

    return ResponseEntity.ok().build();
}
```

**重要性**: 🔥
- 防止使用失效的access_token继续调用API
- 及时通知管理员店铺已断开连接

---

### 3. Orders/Create - 订单创建通知

**触发时机**:
- 店铺收到新订单

**处理逻辑**:
```java
// ShopifyWebhookController.java:143-200
@PostMapping("/orders-create")
public ResponseEntity<Void> handleOrdersCreate(
    @RequestBody String payload,
    @RequestHeader("X-Shopify-Shop-Domain") String shopDomain,
    @RequestHeader("X-Shopify-Hmac-SHA256") String hmac) {

    // 解析订单数据
    JsonNode orderData = objectMapper.readTree(payload);

    String orderNumber = orderData.get("order_number").asText();
    Long orderId = orderData.get("id").asLong();

    log.info("New order created: {} (ID: {}) for shop: {}",
             orderNumber, orderId, shopDomain);

    // TODO: 实现订单同步逻辑
    // 1. 检查订单是否已存在
    // 2. 创建或更新订单记录
    // 3. 同步物流信息(如果订单已发货)

    return ResponseEntity.ok().build();
}
```

**Webhook Payload示例**:
```json
{
  "id": 820982911946154500,
  "order_number": 1001,
  "email": "customer@example.com",
  "created_at": "2024-11-26T10:00:00-05:00",
  "total_price": "199.99",
  "currency": "USD",
  "fulfillment_status": null,
  "line_items": [
    {
      "id": 466157049,
      "title": "Product Name",
      "quantity": 2,
      "price": "99.99"
    }
  ]
}
```

---

### 4. Orders/Updated - 订单更新通知

**触发时机**:
- 订单状态变更
- 订单发货(含物流跟踪号)
- 订单取消/退款

**处理逻辑**:
```java
// ShopifyWebhookController.java:202-263
@PostMapping("/orders-updated")
public ResponseEntity<Void> handleOrdersUpdated(
    @RequestBody String payload,
    @RequestHeader("X-Shopify-Shop-Domain") String shopDomain,
    @RequestHeader("X-Shopify-Hmac-SHA256") String hmac) {

    JsonNode orderData = objectMapper.readTree(payload);

    String fulfillmentStatus = orderData.has("fulfillment_status") ?
        orderData.get("fulfillment_status").asText() : null;

    // 检查是否有物流信息
    if (orderData.has("fulfillments") && orderData.get("fulfillments").isArray()) {
        JsonNode fulfillments = orderData.get("fulfillments");
        for (JsonNode fulfillment : fulfillments) {
            if (fulfillment.has("tracking_number")) {
                String trackingNumber = fulfillment.get("tracking_number").asText();
                String trackingCompany = fulfillment.has("tracking_company") ?
                    fulfillment.get("tracking_company").asText() : null;

                log.info("Tracking info found - Number: {}, Company: {}",
                        trackingNumber, trackingCompany);

                // TODO: 调用TrackingService注册跟踪号
            }
        }
    }

    return ResponseEntity.ok().build();
}
```

**带物流信息的Payload示例**:
```json
{
  "id": 820982911946154500,
  "order_number": 1001,
  "fulfillment_status": "fulfilled",
  "fulfillments": [
    {
      "id": 255858046,
      "status": "success",
      "tracking_company": "USPS",
      "tracking_number": "1234567890",
      "tracking_url": "https://tools.usps.com/go/TrackConfirmAction_input?qtc_tLabels1=1234567890"
    }
  ]
}
```

---

## 🛠️ API接口说明

### 1. 获取店铺的已注册Webhooks

```http
GET /api/v1/shops/{id}/webhooks
Authorization: Bearer {token}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "获取webhooks成功",
  "data": [
    {
      "id": 901431826,
      "topic": "shop/update",
      "address": "http://localhost:8080/api/v1/webhooks/shopify/shop-update",
      "format": "json"
    },
    {
      "id": 901431827,
      "topic": "app/uninstalled",
      "address": "http://localhost:8080/api/v1/webhooks/shopify/app-uninstalled",
      "format": "json"
    }
  ]
}
```

---

### 2. 手动注册所有Webhooks

```http
POST /api/v1/shops/{id}/webhooks/register
Authorization: Bearer {token}
```

**用途**: 如果OAuth时自动注册失败,可以手动触发注册

**响应示例**:
```json
{
  "code": 200,
  "message": "Webhooks注册完成",
  "data": {
    "shop": "my-store.myshopify.com",
    "success": ["shop/update", "app/uninstalled", "orders/create", "orders/updated"],
    "failed": [],
    "totalSuccess": 4,
    "totalFailed": 0
  }
}
```

---

### 3. 删除店铺的所有Webhooks

```http
DELETE /api/v1/shops/{id}/webhooks
Authorization: Bearer {token}
```

**用途**: 清理webhook注册(如测试、重新配置等)

**响应示例**:
```json
{
  "code": 200,
  "message": "Webhooks删除成功",
  "data": {
    "shopId": 1,
    "shopDomain": "my-store.myshopify.com",
    "deletedCount": 4
  }
}
```

---

## ⚙️ 配置说明

### application.yml配置

```yaml
shopify:
  api:
    key: your-api-key
    secret: your-api-secret  # 用于验证webhook签名
  webhook:
    base-url: http://localhost:8080/api/v1  # ⚠️ 生产环境必须改为公网HTTPS地址
```

**生产环境配置示例**:
```yaml
shopify:
  webhook:
    base-url: https://api.yourdomain.com/api/v1  # 必须是HTTPS
```

---

## 🚀 部署要求

### 1. 公网可访问性

**重要**: Shopify无法访问`localhost`,webhook URL必须是公网可访问的HTTPS地址

**开发环境解决方案**:

#### 方案A: 使用ngrok (推荐)
```bash
# 安装ngrok
brew install ngrok

# 启动隧道
ngrok http 8080

# 输出:
# Forwarding https://abc123.ngrok.io -> http://localhost:8080
```

修改`application.yml`:
```yaml
shopify:
  webhook:
    base-url: https://abc123.ngrok.io/api/v1
```

#### 方案B: 使用Cloudflare Tunnel
```bash
cloudflared tunnel --url http://localhost:8080
```

#### 方案C: 使用serveo.net
```bash
ssh -R 80:localhost:8080 serveo.net
```

---

### 2. HTTPS要求

Shopify **强制要求** webhook URL使用HTTPS (开发环境除外)

**生产环境部署检查清单**:
- ✅ 域名配置SSL证书 (Let's Encrypt/商业证书)
- ✅ Nginx/Apache配置HTTPS
- ✅ 防火墙开放443端口
- ✅ webhook URL配置为 `https://...`

---

## 🔍 调试与监控

### 1. 日志监控

所有webhook请求都会记录日志:

```bash
# 查看webhook日志
tail -f logs/track-17-server.log | grep "webhook"

# 输出示例:
2025-11-26 10:30:15 [http-nio-8080-exec-1] INFO  ShopifyWebhookController - Received webhook: shop/update from: my-store.myshopify.com
2025-11-26 10:30:15 [http-nio-8080-exec-1] INFO  ShopifyWebhookController - Successfully updated shop info for: my-store.myshopify.com
```

---

### 2. Shopify Webhook调试工具

在Shopify后台可以查看webhook发送记录:

```
Settings → Notifications → Webhooks → View Details
```

可以看到:
- ✅ 发送时间
- ✅ 响应状态码
- ✅ 响应时间
- ✅ 重试次数
- ❌ 失败原因

---

### 3. 测试Webhook

#### 手动触发测试 (开发环境)

使用curl模拟Shopify发送webhook:

```bash
# 1. 生成HMAC签名
PAYLOAD='{"id":123,"name":"Test Shop"}'
SECRET="your-shopify-api-secret"
HMAC=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "$SECRET" -binary | base64)

# 2. 发送webhook
curl -X POST http://localhost:8080/api/v1/webhooks/shopify/shop-update \
  -H "Content-Type: application/json" \
  -H "X-Shopify-Shop-Domain: test.myshopify.com" \
  -H "X-Shopify-Hmac-SHA256: $HMAC" \
  -H "X-Shopify-Topic: shop/update" \
  -d "$PAYLOAD"
```

#### 使用Shopify CLI测试

```bash
# 安装Shopify CLI
npm install -g @shopify/cli

# 触发测试webhook
shopify webhook trigger --topic=shop/update --address=https://your-domain.com/api/v1/webhooks/shopify/shop-update
```

---

## 🔐 安全最佳实践

### 1. 始终验证HMAC签名

```java
// ✅ 正确做法
if (!webhookService.verifyWebhookSignature(payload, hmac)) {
    log.warn("Invalid webhook signature");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
}
```

```java
// ❌ 错误做法 - 跳过验证
// 绝对不要这样做!任何人都可以伪造webhook请求
```

---

### 2. 防止重放攻击

建议记录已处理的webhook ID:

```java
// 可选: 使用Redis存储最近处理的webhook ID
if (redisTemplate.hasKey("webhook:processed:" + webhookId)) {
    log.warn("Webhook already processed: {}", webhookId);
    return ResponseEntity.ok().build(); // 返回200避免重试
}

// 处理webhook...

// 标记为已处理 (24小时过期)
redisTemplate.opsForValue().set("webhook:processed:" + webhookId, "1", 24, TimeUnit.HOURS);
```

---

### 3. 限制请求来源 (可选)

如果需要额外安全层,可以限制只接受Shopify IP:

```java
// 参考: https://shopify.dev/docs/apps/build/webhooks/subscribe/https#ip-addresses
String[] SHOPIFY_IPS = {
    "23.227.38.0/24",
    "35.160.0.0/13",
    // ... 完整列表见Shopify文档
};
```

---

## 🐛 常见问题排查

### 问题1: Webhook注册失败

**症状**: OAuth成功,但webhook注册日志显示失败

**可能原因**:
1. ❌ webhook URL不可访问 (localhost、防火墙)
2. ❌ URL不是HTTPS (生产环境)
3. ❌ OAuth scope不包含webhook权限

**解决方案**:
```bash
# 检查URL可访问性
curl -I https://your-domain.com/api/v1/webhooks/shopify/health

# 应该返回 200 OK

# 检查Shopify可以访问
curl -I https://your-domain.com/api/v1/webhooks/shopify/health -H "User-Agent: Shopify"
```

---

### 问题2: Webhook验证失败

**症状**: 日志显示 "Invalid webhook signature"

**可能原因**:
1. ❌ `shopify.api.secret` 配置错误
2. ❌ 请求body被修改 (中间件、过滤器)

**解决方案**:
```java
// 确保webhook controller接收原始body
@PostMapping("/shop-update")
public ResponseEntity<Void> handleShopUpdate(
    @RequestBody String payload,  // ✅ 使用String,不要用对象
    ...
) {
    // payload必须是原始字符串用于HMAC验证
}
```

---

### 问题3: Shopify不断重试

**症状**: 相同webhook被发送多次

**可能原因**:
1. ❌ 返回了非2xx状态码
2. ❌ 响应超时 (>5秒)
3. ❌ 连接失败

**解决方案**:
```java
// 即使处理失败,也返回200避免重试
try {
    // 处理webhook
} catch (Exception e) {
    log.error("Webhook processing failed, but returning 200", e);
    // TODO: 存储到失败队列,异步重试
    return ResponseEntity.ok().build();  // ✅ 返回200
}
```

---

## 📊 监控指标

建议监控以下指标:

| 指标 | 说明 | 告警阈值 |
|------|------|----------|
| Webhook接收总数 | 每小时接收的webhook数量 | - |
| 验证失败率 | HMAC验证失败的百分比 | > 1% |
| 处理失败率 | 返回非2xx状态码的百分比 | > 5% |
| 平均响应时间 | webhook处理耗时 | > 3秒 |
| 店铺失效数 | app/uninstalled触发次数 | - |

---

## 🎓 延伸阅读

- [Shopify Webhooks官方文档](https://shopify.dev/docs/apps/build/webhooks)
- [Webhook安全验证](https://shopify.dev/docs/apps/build/webhooks/subscribe/https#step-5-verify-the-webhook)
- [可用的Webhook Topics完整列表](https://shopify.dev/docs/api/admin-rest/2024-10/resources/webhook#event-topics)
- [Webhook最佳实践](https://shopify.dev/docs/apps/build/webhooks/best-practices)

---

## ✅ 总结

通过集成Shopify Webhook,系统实现了:

1. ✅ **自动信息同步** - 店铺信息变更自动更新,无需手动维护
2. ✅ **实时订单通知** - 新订单创建、发货立即收到通知
3. ✅ **App卸载感知** - 自动标记失效店铺,避免无效API调用
4. ✅ **安全验证** - HMAC签名验证,防止伪造请求
5. ✅ **易于管理** - 提供API接口查看、注册、删除webhooks

**下一步**:
- 完善订单同步逻辑 (orders/create, orders/updated)
- 实现物流跟踪号自动注册
- 添加webhook失败重试队列
- 配置生产环境HTTPS域名
