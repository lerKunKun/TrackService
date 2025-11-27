# Track17 物流追踪系统

基于 Spring Boot 和 17Track API 的物流追踪管理系统，深度集成 Shopify 平台，专为跨境电商设计。

## 🚀 项目简介

Track17 是一个功能完整的物流追踪系统，集成了 17Track API 和 Shopify 电商平台，帮助电商企业集中管理和监控订单物流状态，实现自动化的物流跟踪更新。

## ✨ 核心功能

### 已实现功能

#### 用户认证与权限
- ✅ **JWT 认证**：基于 JWT 的用户登录和权限验证
- ✅ **Token 验证**：请求拦截和 Token 有效性验证

#### 店铺管理
- ✅ **多平台支持**：支持 Shopify、Shopline、TikTok Shop 等平台
- ✅ **CRUD 操作**：店铺的创建、查询、更新、删除
- ✅ **分页查询**：支持按平台筛选的分页列表
- ✅ **连接健康检查**：验证单个或批量店铺的连接状态

#### Shopify 深度集成
- ✅ **OAuth 授权**：完整的 Shopify OAuth 2.0 授权流程
- ✅ **Offline Access Token**：获取永久有效的访问令牌
- ✅ **店铺信息同步**：自动获取店铺名称、时区等信息
- ✅ **HMAC 签名验证**：确保回调请求的安全性
- ✅ **Redis State 管理**：防止 CSRF 攻击的状态验证

#### Webhook 管理
- ✅ **Webhook 注册**：自动注册订单履行和跟踪更新 webhook
- ✅ **Webhook 查看**：查询已注册的 webhook 列表
- ✅ **Webhook 删除**：批量删除店铺的 webhook
- ✅ **订单履行事件**：接收 `fulfillments/create` 和 `fulfillments/update`
- ✅ **实时更新**：自动接收 Shopify 的物流更新通知

#### 运单管理
- ✅ **手动添加运单**：支持单个运单录入
- ✅ **运单查询**：分页查询运单列表，支持多条件筛选
- ✅ **运单详情**：查看完整的运单信息和物流轨迹
- ✅ **手动同步**：主动同步单个运单的最新状态
- ✅ **批量删除**：支持批量删除运单

#### 17Track 集成
- ✅ **自动注册**：新运单自动注册到 17Track
- ✅ **物流查询**：实时查询运单的物流状态
- ✅ **事件追踪**：记录完整的物流轨迹事件
- ✅ **承运商识别**：自动识别并记录承运商信息

#### 系统优化
- ✅ **Redis 缓存**：OAuth state、Token 等关键数据缓存
- ✅ **连接池管理**：数据库和 Redis 连接池优化
- ✅ **异常处理**：统一的异常处理和错误响应
- ✅ **日志记录**：完整的操作日志和调试信息

### 计划功能（后续版本）

- 📦 CSV 批量导入运单
- 🔄 自动同步 Shopify 订单（基于 Webhook 自动创建运单）
- 📊 物流轨迹可视化展示（时间线组件）
- 🔔 异常运单提醒（超时、退回等）
- 📈 数据统计看板（运单状态分布、时效分析）
- 🌐 更多平台集成（Shopline、TikTok Shop）
- 🔐 多用户权限管理
- 📱 移动端适配

## 🛠 技术栈

### 后端
- **Java 8**
- **Spring Boot 2.7.18**
- **MyBatis** - ORM 框架
- **MySQL 8.0** - 数据持久化
- **Redis 6.0+** - 缓存和会话管理
- **RestTemplate** - HTTP 客户端
- **JWT** - 认证令牌
- **Lombok** - 简化代码

### 前端（两个版本可选）

**Vue3 版本（推荐）**
- Vue 3 (Composition API)
- Vite 5
- Ant Design Vue 4
- Vue Router 4
- Pinia
- Axios

**原生版本（简易）**
- 原生 HTML/CSS/JavaScript
- Fetch API

### 外部服务
- **17Track API** - 物流追踪服务
- **Shopify API** - 电商平台集成
- **Shopify Webhooks** - 实时事件推送

## 📋 环境要求

### 后端
- **JDK 1.8** 或更高版本
- **Maven 3.6+**
- **MySQL 8.0+**
- **Redis 6.0+**（必需）

### 前端（Vue3 版本）
- **Node.js 16+** 或更高版本
- **npm** 或 **yarn** 或 **pnpm**

## 🚀 快速开始

### 1. 创建数据库

执行以下 SQL 创建数据库和表结构：

```bash
mysql -u root -p < docs/database.sql
```

或者手动执行数据库建表脚本（参考项目文档中的建表语句）。

### 2. 启动 Redis

```bash
# macOS (使用 Homebrew)
brew services start redis

# Linux
sudo systemctl start redis

# Docker
docker run -d -p 6379:6379 redis:latest
```

### 3. 配置应用

编辑 `src/main/resources/application.yml`，修改以下配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/logistics_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC
    username: root        # 修改为你的数据库用户名
    password: 123456      # 修改为你的数据库密码

  redis:
    host: localhost
    port: 6379
    password:             # 如有密码请填写

# JWT 配置
jwt:
  secret: track17-logistics-secret-key-change-in-production  # 生产环境请修改
  expiration: 86400000  # Token 过期时间（24小时）

# 17Track API 配置
track17:
  api:
    token: YOUR_17TRACK_API_TOKEN  # 替换为你的 17Track API Token

# Shopify OAuth 配置
shopify:
  api:
    key: YOUR_SHOPIFY_API_KEY          # Shopify App 的 API Key
    secret: YOUR_SHOPIFY_API_SECRET    # Shopify App 的 API Secret
  oauth:
    redirect-uri: http://localhost:8080/api/v1/oauth/shopify/callback  # OAuth 回调地址
    frontend-redirect: http://localhost:3000/shops  # 前端重定向地址
  webhook:
    base-url: http://localhost:8080/api/v1  # Webhook 接收地址（生产环境需要公网地址）
```

### 4. Shopify App 配置

#### 4.1 创建 Shopify App

1. 访问 [Shopify Partners](https://partners.shopify.com/)
2. 创建一个新的 App（Custom App）
3. 在 App 设置中配置：

**Allowed redirection URL(s)**（重要！必须完全匹配）
```
http://localhost:8080/api/v1/oauth/shopify/callback
```

**Scopes（权限范围）**
```
read_orders, write_orders, read_products, write_products
```

4. 复制 **API Key** 和 **API Secret Key** 到 `application.yml`

#### 4.2 生产环境配置

生产环境部署时，需要：
1. 使用 HTTPS 域名
2. 修改 `redirect-uri` 为：`https://yourdomain.com/api/v1/oauth/shopify/callback`
3. 在 Shopify App 设置中添加生产环境的回调 URL
4. 修改 `webhook.base-url` 为公网可访问的地址

### 5. 编译项目

```bash
mvn clean package
```

### 6. 启动应用

```bash
# 方式1: 使用 Maven
mvn spring-boot:run

# 方式2: 使用 jar 包
java -jar target/track-17-server-1.0.0.jar
```

启动成功后，后端服务运行在 `http://localhost:8080`

### 7. 访问前端界面

#### 方式一：Vue3 版本（推荐）

```bash
# 进入 Vue3 前端目录
cd frontend-vue

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 访问 http://localhost:3000
```

#### 方式二：原生版本（简易）

使用浏览器直接打开前端页面：

```
frontend/login.html
```

**默认登录账号：**
- 用户名：`admin`
- 密码：`admin123`

## 📁 项目结构

```
track-17-server/
├── src/
│   ├── main/
│   │   ├── java/com/logistics/track17/
│   │   │   ├── config/          # 配置类
│   │   │   │   ├── CorsConfig.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── RedisConfig.java          # Redis 配置
│   │   │   ├── controller/      # 控制器
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── ShopController.java
│   │   │   │   ├── TrackingController.java
│   │   │   │   ├── ShopifyOAuthController.java     # Shopify OAuth
│   │   │   │   └── ShopifyWebhookController.java   # Shopify Webhook
│   │   │   ├── service/         # 业务逻辑
│   │   │   │   ├── ShopService.java
│   │   │   │   ├── TrackingService.java
│   │   │   │   ├── CarrierService.java
│   │   │   │   ├── ShopifyOAuthService.java        # Shopify OAuth 服务
│   │   │   │   └── ShopifyWebhookService.java      # Shopify Webhook 服务
│   │   │   ├── mapper/          # MyBatis Mapper 接口
│   │   │   ├── entity/          # 实体类
│   │   │   ├── dto/             # 数据传输对象
│   │   │   ├── exception/       # 异常处理
│   │   │   └── util/            # 工具类
│   │   └── resources/
│   │       ├── mapper/          # MyBatis XML 映射文件
│   │       └── application.yml  # 应用配置
│   └── test/
├── frontend-vue/                # Vue3 前端（推荐）
│   ├── src/
│   │   ├── api/                # API 接口
│   │   ├── assets/             # 静态资源
│   │   ├── components/         # 组件
│   │   ├── router/             # 路由
│   │   ├── stores/             # 状态管理
│   │   ├── utils/              # 工具函数
│   │   └── views/              # 页面
│   ├── index.html
│   ├── vite.config.js
│   └── package.json
├── frontend/                    # 原生前端（简易版）
│   ├── login.html              # 登录页面
│   ├── shops.html              # 店铺管理页面
│   └── tracking.html           # 运单管理页面
├── docs/                        # 文档目录
│   ├── API_SHOP_MANAGEMENT.md         # 店铺管理 API
│   ├── FRONTEND_SHOP_INTEGRATION.md   # 前端集成指南
│   ├── SHOPIFY_SHOP_INTEGRATION_SUMMARY.md  # Shopify 集成总结
│   ├── SHOPIFY_WEBHOOK_INTEGRATION.md       # Shopify Webhook 文档
│   ├── REDIS_IMPLEMENTATION_SUMMARY.md      # Redis 实现总结
│   └── REDIS_OPTIMIZATION_REPORT.md         # Redis 优化报告
└── pom.xml                      # Maven 配置文件
```

## 📡 API 接口

### 认证相关
- `POST /api/v1/auth/login` - 用户登录
- `GET /api/v1/auth/validate` - 验证 Token
- `GET /api/v1/auth/current` - 获取当前用户

### 店铺管理
- `POST /api/v1/shops` - 创建店铺
- `GET /api/v1/shops` - 获取店铺列表（支持分页和平台筛选）
- `GET /api/v1/shops/{id}` - 获取店铺详情
- `PUT /api/v1/shops/{id}` - 更新店铺
- `DELETE /api/v1/shops/{id}` - 删除店铺
- `POST /api/v1/shops/{id}/validate` - 验证店铺连接状态
- `POST /api/v1/shops/validate-all` - 批量验证所有店铺连接

### Shopify OAuth
- `GET /api/v1/oauth/shopify/authorize?shopDomain={domain}` - 开始 OAuth 授权
- `GET /api/v1/oauth/shopify/callback` - OAuth 回调处理

### Shopify Webhook 管理
- `GET /api/v1/shops/{id}/webhooks` - 获取店铺的已注册 webhooks
- `POST /api/v1/shops/{id}/webhooks/register` - 为店铺注册所有 webhooks
- `DELETE /api/v1/shops/{id}/webhooks` - 删除店铺的所有 webhooks

### Webhook 接收（由 Shopify 调用）
- `POST /api/v1/webhooks/shopify/fulfillments/create` - 订单履行创建事件
- `POST /api/v1/webhooks/shopify/fulfillments/update` - 订单履行更新事件

### 运单管理
- `POST /api/v1/tracking` - 添加运单
- `GET /api/v1/tracking` - 获取运单列表
- `GET /api/v1/tracking/{id}` - 获取运单详情
- `POST /api/v1/tracking/{id}/sync` - 手动同步运单
- `DELETE /api/v1/tracking/{id}` - 删除运单
- `POST /api/v1/tracking/batch-delete` - 批量删除运单

详细 API 文档请查看：
- [API 文档](docs/API_DOCUMENTATION.md)
- [店铺管理 API](docs/API_SHOP_MANAGEMENT.md)

## 🔧 配置说明

### JWT 配置

```yaml
jwt:
  secret: track17-logistics-secret-key-change-in-production  # 生产环境请修改
  expiration: 86400000  # Token 过期时间（毫秒），默认 24 小时
```

### 17Track API 配置

1. 注册 17Track 账号：https://www.17track.net/
2. 申请 API Key
3. 在 `application.yml` 中配置 Token

```yaml
track17:
  api:
    url: https://api.17track.net/track/v2.4
    token: YOUR_17TRACK_API_TOKEN
    register-endpoint: /register
    query-endpoint: /gettrackinfo
    timeout: 10000
```

### Shopify 配置

#### 开发环境

```yaml
shopify:
  api:
    key: YOUR_SHOPIFY_API_KEY
    secret: YOUR_SHOPIFY_API_SECRET
  oauth:
    redirect-uri: http://localhost:8080/api/v1/oauth/shopify/callback
    frontend-redirect: http://localhost:3000/shops
  webhook:
    base-url: http://localhost:8080/api/v1  # 需要使用 ngrok 等工具暴露到公网
```

#### 生产环境

```yaml
shopify:
  api:
    key: YOUR_SHOPIFY_API_KEY
    secret: YOUR_SHOPIFY_API_SECRET
  oauth:
    redirect-uri: https://yourdomain.com/api/v1/oauth/shopify/callback
    frontend-redirect: https://yourdomain.com/shops
  webhook:
    base-url: https://yourdomain.com/api/v1  # 必须是 HTTPS 公网地址
```

#### 本地测试 Webhook

由于 Shopify Webhook 需要公网地址，本地开发时可以使用 ngrok：

```bash
# 安装 ngrok
brew install ngrok  # macOS

# 启动 ngrok
ngrok http 8080

# 复制 ngrok 提供的公网地址（如 https://abc123.ngrok.io）
# 更新 application.yml 中的 webhook.base-url
```

### Redis 配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password:             # 如有密码请填写
    database: 0
    timeout: 5000
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1ms
```

Redis 用途：
- OAuth state 存储（防 CSRF）
- 缓存优化（待实现）
- 会话管理（待实现）

## 🗄️ 数据库表结构

主要数据表：

- `users` - 用户表
- `shops` - 店铺信息表（包含 Shopify OAuth 相关字段）
  - `oauth_state` - OAuth state nonce
  - `oauth_scope` - OAuth 授权范围
  - `token_type` - Token 类型（offline/online）
  - `connection_status` - 连接状态
  - `token_expires_at` - Token 过期时间
  - `shop_domain` - Shopify 店铺域名
  - `timezone` - 店铺时区
- `orders` - 订单表
- `parcels` - 包裹表
- `tracking_numbers` - 运单表
- `tracking_events` - 物流事件表
- `carriers` - 承运商表

详细建表语句见项目根目录下的 SQL 文件。

## 📖 使用流程

### 1. Shopify 店铺接入

1. **登录系统**
   - 使用 `admin/admin123` 登录

2. **开始 Shopify OAuth 授权**
   - 访问：`http://localhost:8080/api/v1/oauth/shopify/authorize?shopDomain=yourstore.myshopify.com`
   - 或在前端点击"连接 Shopify 店铺"按钮

3. **完成授权**
   - 在 Shopify 授权页面点击"安装应用"
   - 授权完成后自动跳转回系统
   - 系统自动保存店铺信息和 access token

4. **注册 Webhook**
   - 系统会自动为新接入的店铺注册 webhook
   - 也可以在店铺管理页面手动注册

### 2. 运单追踪

1. **自动创建运单**
   - Shopify 订单履行后，系统通过 Webhook 自动接收通知
   - 系统自动创建运单并注册到 17Track

2. **手动添加运单**
   - 在运单管理页面手动添加运单号
   - 系统自动识别承运商并注册到 17Track

3. **查看物流信息**
   - 在运单列表查看所有运单状态
   - 点击运单查看详细物流轨迹

4. **手动同步**
   - 点击"同步"按钮手动更新运单状态

## 🐛 故障排除

### 数据库连接失败
- 检查 MySQL 服务是否启动
- 确认数据库用户名和密码正确
- 检查数据库 URL 中的端口号
- 确认数据库 `logistics_system` 已创建

### Redis 连接失败
- 检查 Redis 服务是否启动：`redis-cli ping`
- 确认 Redis 端口（默认 6379）
- 检查防火墙设置

### JWT Token 无效
- 检查 Token 是否过期
- 确认请求头格式：`Authorization: Bearer {token}`
- 检查 JWT secret 配置

### 17Track API 调用失败
- 确认 API Token 配置正确
- 检查网络连接
- 查看 17Track API 文档确认请求格式
- 检查 API 调用配额

### Shopify OAuth 失败

**错误：`invalid_request: The redirect_uri is not whitelisted`**
- 检查 Shopify App 设置中的 "Allowed redirection URL(s)"
- 确保配置的 URL 与 `application.yml` 中的 `redirect-uri` **完全一致**
- URL 必须包含协议（http/https）、端口、路径

**错误：State 验证失败**
- 检查 Redis 是否正常运行
- 确认 OAuth state 未过期（默认 5 分钟）
- 清除 Redis 缓存重试：`redis-cli FLUSHDB`

**错误：HMAC 验证失败**
- 检查 Shopify API Secret 配置是否正确
- 确认没有代理或中间件修改了请求参数

### Webhook 接收失败

**Webhook 无法接收**
- 确认 `webhook.base-url` 配置的是公网可访问地址
- 本地开发需要使用 ngrok 等工具
- 检查防火墙和安全组设置

**Webhook 验证失败**
- Shopify Webhook 需要 HMAC 签名验证
- 检查 API Secret 配置
- 查看日志确认签名计算是否正确

## 📝 开发计划

查看详细的开发计划和用户故事：[docs/USER_STORIES.md](docs/USER_STORIES.md)

### 已完成 ✅

- **用户认证**：JWT 登录和权限验证
- **店铺管理**：CRUD 操作、分页查询、连接验证
- **Shopify OAuth**：完整的授权流程、Token 管理
- **Shopify Webhook**：订单履行事件接收、自动注册
- **运单管理**：手动添加、查询、详情、同步、删除
- **17Track 集成**：自动注册、物流查询、事件追踪
- **Redis 缓存**：OAuth state 管理、连接池优化

### 进行中 🚧

- **前端店铺集成**：Shopify OAuth 流程前端页面
- **Webhook 自动创建运单**：基于订单履行事件自动创建运单

### 计划中 📋

- **批量导入**：CSV 批量导入运单
- **可视化展示**：物流轨迹时间线组件
- **异常监控**：超时运单、退回包裹提醒
- **数据统计**：运单状态分布、时效分析
- **多平台支持**：Shopline、TikTok Shop 集成
- **权限管理**：多用户、角色权限
- **移动端**：响应式设计、移动端适配

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

## 📧 联系方式

如有问题，请通过 Issue 反馈。

---

## ⚠️ 注意事项

### 安全性
1. **生产环境部署前必须修改**：
   - JWT Secret（`jwt.secret`）
   - 数据库密码
   - Shopify API Secret
   - 17Track API Token

2. **HTTPS 要求**：
   - 生产环境必须使用 HTTPS
   - Shopify OAuth 和 Webhook 在生产环境要求 HTTPS

3. **敏感信息保护**：
   - 不要将 `application.yml` 提交到版本控制
   - 使用环境变量管理敏感配置
   - Access Token 加密存储

### 性能优化
1. **Redis 是必需的**：OAuth 流程依赖 Redis
2. **数据库索引**：确保运单表和事件表有合适的索引
3. **连接池配置**：根据并发量调整数据库和 Redis 连接池

### API 限制
1. **17Track API**：
   - 有调用频率限制
   - 建议实现请求队列和重试机制

2. **Shopify API**：
   - REST API 有 2 次/秒的限制
   - GraphQL API 有积分制限制
   - 注意避免超过限制

### 本地开发
1. **Webhook 测试**：需要使用 ngrok 暴露本地服务到公网
2. **跨域配置**：前后端分离开发已配置 CORS
3. **日志级别**：开发环境建议使用 DEBUG 级别

---

**版本**: v1.0.0
**最后更新**: 2025-11-27
