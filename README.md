# Track17 跨境电商管理系统

基于 Spring Boot 的跨境电商综合管理系统，深度集成 Shopify 平台、17Track 物流 API 和钉钉企业应用，涵盖店铺管理、商品管理、订单管理、主题迁移、RBAC 权限控制等核心功能。

## 项目简介

Track17 是一个面向跨境电商企业的综合管理系统，提供多店铺管理、商品全生命周期管理、Shopify 主题迁移、订单物流追踪、数据统计分析等功能，并通过钉钉企业集成实现组织架构同步和统一身份认证。

## 核心功能

### 用户认证与授权
- **JWT 认证**：基于 JWT 的用户登录和权限验证
- **钉钉扫码登录**：OAuth 2.0 授权，支持企业内部应用 QR 码扫码
- **多登录方式**：账号密码 + 钉钉扫码双模式
- **Token 黑名单**：通过 Redis 实现安全退出登录
- **登录 IP 记录**：记录用户最后登录 IP

### RBAC 权限管理
- **角色管理**：创建、编辑、删除角色，支持细粒度权限分配
- **权限管理**：37+ 系统权限，支持菜单/按钮/数据三种类型
- **菜单管理**：动态菜单配置，基于角色的菜单可见性控制
- **审计日志**：基于 @AuditLog 注解的用户操作审计追踪

### 钉钉企业集成
- **扫码登录**：动态二维码扫码登录，QR 码自动刷新
- **企业白名单**：限制可登录的企业 CorpId
- **用户同步**：从钉钉通讯录同步用户数据
- **部门同步**：同步组织架构和部门信息
- **角色映射**：基于部门自动分配系统角色
- **同步日志**：记录同步操作历史和详情

### 店铺管理
- **多平台支持**：支持 Shopify、Shopline、TikTok Shop 等平台
- **CRUD 操作**：店铺的创建、查询、更新、删除
- **连接健康检查**：验证单个或批量店铺的连接状态
- **店铺信息同步**：自动获取并同步店铺名称、时区、货币、套餐等信息

### Shopify 深度集成
- **OAuth 授权**：完整的 Shopify OAuth 2.0 授权流程
- **Offline Access Token**：获取永久有效的访问令牌
- **HMAC 签名验证**：确保回调请求的安全性
- **Redis State 管理**：防止 CSRF 攻击的状态验证
- **Webhook 管理**：注册、查看、删除 Webhook，接收订单履行事件

### 商品管理
- **CSV 导入/导出**：支持 Shopify 格式的商品批量导入和导出
- **商品 CRUD**：创建、查询、更新、删除商品
- **变体管理**：商品变体与 SKU 追踪
- **价格管理**：更新售价和对比价
- **采购管理**：追踪 SKU、采购链接、采购价、供应商信息
- **标签管理**：商品分类标签
- **批量操作**：批量更新、批量删除、批量关联店铺
- **多店铺关联**：商品与多个店铺的关联管理

### 商品媒体管理
- **文件上传**：支持图片、视频等媒体文件上传
- **URL 批量下载**：从外部 URL 批量下载媒体文件
- **Shopify 图片同步**：自动同步 Shopify 商品图片
- **文件分类**：按主图、详情图等类型组织文件
- **文件排序**：自定义媒体文件排列顺序
- **参考链接**：关联竞品/参考商品链接
- **MinIO 集成**：支持 MinIO 对象存储，同时支持本地存储回退

### 商品发布
- **发布/下架**：推送商品到店铺或从店铺下架
- **CSV 导出**：导出 Shopify 格式的商品 CSV
- **批量发布**：支持多店铺商品批量发布

### 商品模板
- **模板管理**：创建、更新商品模板
- **Liquid Schema**：支持 Shopify Liquid 模板 Schema
- **模板后缀**：自定义模板后缀配置

### Shopify 主题迁移
- **主题版本管理**：追踪和管理主题版本
- **差异分析**：深度分析主题版本间的变更
- **迁移规则**：配置和应用迁移规则
- **主题导出/导入**：下载迁移后的主题 ZIP 包
- **一键迁移**：快速主题迁移功能
- **Git 集成**：基于 JGit 实现版本控制

### 订单管理
- **订单列表**：分页查询订单，支持多条件筛选
- **订单详情**：查看完整订单信息
- **订单商品**：Shopify 订单行项目详情
- **履行状态**：追踪订单履行进度
- **收货地址**：存储和管理配送地址

### 数据统计
- **统计看板**：首页关键指标概览
- **订单统计**：总数、日新增、状态分布
- **店铺统计**：店铺总数统计
- **用户统计**：用户总数统计
- **承运商分析**：按承运商分组统计（Top 10）
- **趋势分析**：最近 7 天趋势图

### 系统功能
- **邮件监控**：邮件配置与通知
- **邀请系统**：用户邀请管理
- **通知管理**：通知接收人管理
- **审计日志**：全量用户操作追踪

## 技术栈

### 后端
- **Java 17**
- **Spring Boot 2.7.18**
- **MyBatis Plus 3.5.3** - ORM 框架
- **MySQL 8.0+** - 数据持久化
- **Redis 6.0+** - 缓存和会话管理
- **JWT 0.9.1** - 认证令牌
- **OkHttp 4.10.0** - HTTP 客户端
- **MinIO 8.5.7** - 对象存储
- **JGit 6.8.0** - Git 操作（主题版本控制）
- **DingTalk SDK 2.0.0** - 钉钉企业集成
- **FastJSON 1.2.83** - JSON 处理
- **Spring Security Crypto** - BCrypt 密码加密
- **Spring AOP** - 面向切面编程（审计日志等）
- **Apache Commons** (CSV, Compress, IO, Lang3) - 通用工具
- **Lombok** - 简化代码

### 前端
- **Vue 3** - Composition API
- **Vite 5** - 构建工具
- **Ant Design Vue 4** - UI 组件库
- **Vue Router 4** - 路由管理
- **Pinia** - 状态管理
- **Axios** - HTTP 客户端

### 外部服务
- **17Track API v2.4** - 物流追踪服务
- **Shopify API** - 电商平台集成
- **Shopify Webhooks** - 实时事件推送
- **钉钉开放平台** - 企业身份认证与组织同步

## 环境要求

### 后端
- **JDK 17** 或更高版本
- **Maven 3.6+**
- **MySQL 8.0+**
- **Redis 6.0+**（必需）

### 前端
- **Node.js 16+**
- **npm** 或 **yarn** 或 **pnpm**

## 快速开始

### 1. 使用 Docker Compose（推荐）

项目提供了 `docker-compose.yml`，可一键启动 MySQL、Redis 等依赖服务：

```bash
docker-compose up -d
```

### 2. 手动搭建环境

#### 创建数据库

项目使用 Flyway 数据库迁移，迁移脚本位于 `src/main/resources/db/migration/` 目录，应用启动时自动执行。

也可手动执行：

```bash
mysql -u root -p < docs/database.sql
```

#### 启动 Redis

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
    url: jdbc:mysql://localhost:3306/logistics_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password

  redis:
    host: localhost
    port: 6379
    password:

# JWT 配置
jwt:
  secret: track17-logistics-secret-key-change-in-production  # 生产环境请修改
  expiration: 86400000  # Token 过期时间（24小时）

# 17Track API 配置
track17:
  api:
    token: YOUR_17TRACK_API_TOKEN

# Shopify OAuth 配置
shopify:
  api:
    key: YOUR_SHOPIFY_API_KEY
    secret: YOUR_SHOPIFY_API_SECRET
  oauth:
    redirect-uri: http://localhost:8080/api/v1/oauth/shopify/callback
    frontend-redirect: http://localhost:3000/shops
  webhook:
    base-url: http://localhost:8080/api/v1

# 钉钉配置
dingtalk:
  corp-id: YOUR_CORP_ID
  app-key: YOUR_APP_KEY
  app-secret: YOUR_APP_SECRET

# 存储配置（local 或 minio）
storage:
  type: local
  local:
    path: ./storage/theme-archives
  minio:
    endpoint: http://localhost:9000
    access-key: YOUR_ACCESS_KEY
    secret-key: YOUR_SECRET_KEY
```

建议使用环境变量管理敏感配置，参见下方「环境变量」章节。

### 4. Shopify App 配置

1. 访问 [Shopify Partners](https://partners.shopify.com/) 创建 Custom App
2. 配置 **Allowed redirection URL(s)**：
   ```
   http://localhost:8080/api/v1/oauth/shopify/callback
   ```
3. 配置 **Scopes**：
   ```
   read_orders, write_orders, read_products, write_products
   ```
4. 复制 **API Key** 和 **API Secret Key** 到 `application.yml`

### 5. 编译和启动

```bash
# 编译
mvn clean package

# 方式1: 使用 Maven
mvn spring-boot:run

# 方式2: 使用 jar 包
java -jar target/track-17-server-1.0.0.jar
```

启动成功后，后端服务运行在 `http://localhost:8080`

### 6. 启动前端

```bash
cd frontend-vue
npm install
npm run dev
# 访问 http://localhost:3000
```

**默认登录账号：**
- 用户名：`admin`
- 密码：`admin123`

## 项目结构

```
track-17-server/
├── src/
│   ├── main/
│   │   ├── java/com/logistics/track17/
│   │   │   ├── annotation/        # 自定义注解（@AuditLog 等）
│   │   │   ├── aspect/            # AOP 切面（审计日志、权限校验）
│   │   │   ├── config/            # 配置类
│   │   │   │   ├── CorsConfig.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── RedisConfig.java
│   │   │   │   └── ...
│   │   │   ├── controller/        # REST 控制器（28 个）
│   │   │   │   ├── AuthController.java          # 认证
│   │   │   │   ├── UserController.java          # 用户管理
│   │   │   │   ├── RoleController.java          # 角色管理
│   │   │   │   ├── MenuController.java          # 菜单管理
│   │   │   │   ├── ShopController.java          # 店铺管理
│   │   │   │   ├── ShopifyOAuthController.java  # Shopify OAuth
│   │   │   │   ├── ShopifyWebhookController.java
│   │   │   │   ├── OrderController.java         # 订单管理
│   │   │   │   ├── ProductController.java       # 商品管理
│   │   │   │   ├── ProductMediaController.java  # 商品媒体
│   │   │   │   ├── ProductPublishController.java # 商品发布
│   │   │   │   ├── ProductListingController.java # 商品上架
│   │   │   │   ├── ProductTemplateController.java # 商品模板
│   │   │   │   ├── ThemeMigrationController.java # 主题迁移
│   │   │   │   ├── ThemeVersionController.java   # 主题版本
│   │   │   │   ├── DingtalkSyncController.java   # 钉钉同步
│   │   │   │   ├── StatsController.java          # 数据统计
│   │   │   │   ├── CarrierController.java        # 承运商
│   │   │   │   └── ...
│   │   │   ├── dto/               # 数据传输对象
│   │   │   ├── entity/            # 数据库实体（~40 个）
│   │   │   ├── enums/             # 枚举类型
│   │   │   ├── exception/         # 异常处理
│   │   │   ├── interceptor/       # 拦截器
│   │   │   ├── mapper/            # MyBatis Mapper 接口
│   │   │   ├── service/           # 业务逻辑（40+ 个）
│   │   │   │   ├── impl/         # 服务实现
│   │   │   │   └── storage/      # 存储服务
│   │   │   └── util/              # 工具类
│   │   └── resources/
│   │       ├── db/migration/      # Flyway 数据库迁移脚本
│   │       ├── mapper/            # MyBatis XML 映射文件
│   │       └── application.yml    # 应用配置
│   └── test/
├── Dockerfile                     # Docker 镜像构建
├── docker-compose.yml             # Docker Compose 编排
└── pom.xml                        # Maven 配置文件
```

## API 接口

所有接口前缀：`/api/v1`

### 认证
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | 用户登录 |
| POST | `/auth/logout` | 用户退出 |
| GET | `/auth/validate` | 验证 Token |
| GET | `/auth/current` | 获取当前用户信息 |
| GET | `/auth/dingtalk/login-url` | 获取钉钉登录 URL |
| POST | `/auth/dingtalk/callback` | 钉钉 OAuth 回调 |

### 用户管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/users` | 用户列表（分页） |
| GET | `/users/all` | 全部用户（不分页） |
| GET | `/users/{id}` | 用户详情 |
| POST | `/users` | 创建用户 |
| PUT | `/users/{id}` | 更新用户 |
| DELETE | `/users/{id}` | 删除用户 |
| POST | `/users/{id}/password` | 修改密码 |
| PUT | `/users/{id}/status` | 更新用户状态 |
| GET | `/users/{id}/roles` | 获取用户角色 |
| PUT | `/users/{id}/roles` | 分配用户角色 |

### 角色与权限
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/roles` | 角色列表 |
| POST | `/roles` | 创建角色 |
| PUT | `/roles/{id}` | 更新角色 |
| DELETE | `/roles/{id}` | 删除角色 |
| GET | `/roles/{id}/permissions` | 获取角色权限 |
| PUT | `/roles/{id}/permissions` | 分配角色权限 |
| GET | `/permissions` | 权限列表 |
| GET | `/permissions/search` | 搜索权限 |

### 菜单管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/menus/tree` | 获取菜单树 |
| GET | `/menus/user` | 获取当前用户菜单树 |
| GET | `/menus` | 菜单列表 |
| POST | `/menus` | 创建菜单 |
| PUT | `/menus/{id}` | 更新菜单 |
| DELETE | `/menus/{id}` | 删除菜单 |

### 店铺管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/shops` | 店铺列表（分页） |
| GET | `/shops/{id}` | 店铺详情 |
| DELETE | `/shops/{id}` | 删除店铺 |
| POST | `/shops/{id}/validate` | 验证连接 |
| POST | `/shops/validate-all` | 批量验证连接 |
| GET | `/shops/{id}/webhooks` | 获取 Webhooks |
| POST | `/shops/{id}/webhooks/register` | 注册 Webhooks |
| DELETE | `/shops/{id}/webhooks` | 删除 Webhooks |
| GET | `/shops/{id}/info` | 获取店铺信息（实时） |
| POST | `/shops/{id}/refresh-info` | 刷新店铺信息 |

### Shopify OAuth
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/oauth/shopify/authorize` | 开始 OAuth 授权 |
| GET | `/oauth/shopify/callback` | OAuth 回调处理 |

### 订单管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/orders` | 订单列表（分页） |
| GET | `/orders/{id}` | 订单详情 |

### 商品管理
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/product/import` | CSV 导入商品 |
| GET | `/product/list` | 商品列表 |
| GET | `/product/{id}` | 商品详情 |
| PUT | `/product/{id}` | 更新商品 |
| DELETE | `/product/{id}` | 删除商品 |
| POST | `/product/batch/delete` | 批量删除 |
| POST | `/product/batch/update` | 批量更新 |
| POST | `/product/batch/shops` | 批量关联店铺 |
| GET | `/product/{productId}/variants` | 获取变体 |
| PUT | `/product/variants/{variantId}/price` | 更新变体价格 |
| PUT | `/product/{productId}/price` | 更新全部变体价格 |
| PUT | `/product/variants/{variantId}/procurement` | 更新采购信息 |
| DELETE | `/product/variants/{variantId}` | 删除变体 |
| GET | `/product/export/csv` | 导出 CSV |
| GET | `/product/procurement-list` | 采购列表 |
| GET | `/product/procurement/stats` | 采购统计 |
| GET | `/product/tags` | 获取所有标签 |

### 商品媒体
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/product-media/list` | 带媒体的商品列表 |
| GET | `/product-media/{productId}/files` | 文件列表 |
| POST | `/product-media/{productId}/upload` | 上传文件 |
| POST | `/product-media/{productId}/download-urls` | 从 URL 下载 |
| POST | `/product-media/{productId}/sync-images` | 同步 Shopify 图片 |
| DELETE | `/product-media/files/{fileId}` | 删除文件 |
| DELETE | `/product-media/{productId}/files/batch` | 批量删除文件 |
| PUT | `/product-media/{productId}/files/sort` | 更新排序 |
| PUT | `/product-media/files/{fileId}/category` | 移动分类 |
| GET | `/product-media/{productId}/reference-link` | 获取参考链接 |
| PUT | `/product-media/{productId}/reference-link` | 更新参考链接 |

### 商品发布
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/product-publish/publish` | 发布商品到店铺 |
| POST | `/product-publish/unpublish` | 下架商品 |
| POST | `/api/v1/listing/export/csv` | 导出 Shopify CSV |

### 主题迁移
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/theme/migration/start` | 开始迁移 |
| POST | `/theme/migration/execute` | 执行迁移 |
| GET | `/theme/migration/session/{sessionId}` | 获取迁移会话 |
| GET | `/theme/migration/download/{historyId}` | 下载迁移主题 |
| POST | `/theme/quick-migration/execute` | 一键迁移 |

### 主题版本
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/theme/versions` | 版本列表 |
| GET | `/theme/versions/{id}` | 版本详情 |
| POST | `/theme/versions` | 创建版本 |
| DELETE | `/theme/versions/{id}` | 删除版本 |

### 钉钉同步
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/dingtalk/sync/full` | 全量同步 |
| POST | `/dingtalk/sync/departments` | 同步部门 |
| POST | `/dingtalk/sync/users` | 同步用户 |
| POST | `/dingtalk/sync/roles` | 应用角色映射 |
| GET | `/dingtalk/sync/logs` | 同步日志列表 |
| GET | `/dingtalk/sync/logs/{id}` | 同步日志详情 |

### 其他
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/carriers` | 活跃承运商列表 |
| GET | `/carriers/all` | 全部承运商 |
| GET | `/stats/dashboard` | 统计看板 |

## 环境变量

生产环境建议通过环境变量管理敏感配置：

| 变量 | 说明 |
|------|------|
| `DB_URL` | 数据库连接 URL |
| `DB_USERNAME` | 数据库用户名 |
| `DB_PASSWORD` | 数据库密码 |
| `REDIS_PASSWORD` | Redis 密码 |
| `JWT_SECRET` | JWT 签名密钥 |
| `JWT_EXPIRATION` | JWT 过期时间（毫秒） |
| `TRACK17_TOKEN` | 17Track API Token |
| `SHOPIFY_API_KEY` | Shopify API Key |
| `SHOPIFY_API_SECRET` | Shopify API Secret |
| `DINGTALK_CORP_ID` | 钉钉企业 ID |
| `DINGTALK_APP_KEY` | 钉钉应用 Key |
| `DINGTALK_APP_SECRET` | 钉钉应用 Secret |
| `DINGTALK_AGENT_ID` | 钉钉 Agent ID |
| `MINIO_ENDPOINT` | MinIO 服务地址 |
| `MINIO_ACCESS_KEY` | MinIO Access Key |
| `MINIO_SECRET_KEY` | MinIO Secret Key |

## 数据库

项目使用 Flyway 管理数据库迁移，迁移脚本位于 `src/main/resources/db/migration/`。

主要数据表：

| 分类 | 表名 | 说明 |
|------|------|------|
| 用户权限 | `users` | 用户表 |
| | `roles` | 角色表 |
| | `permissions` | 权限表 |
| | `menus` | 菜单表 |
| | `audit_logs` | 审计日志 |
| 组织 | `companies` | 企业/组织 |
| | `company_members` | 企业成员 |
| 店铺 | `shops` | 店铺信息 |
| | `user_shop_roles` | 用户-店铺-角色关联 |
| 订单 | `orders` | 订单表 |
| | `order_items` | 订单行项目 |
| | `fulfillments` | 履行记录 |
| 商品 | `products` | 商品表 |
| | `product_variants` | 商品变体 |
| | `product_images` | 商品图片 |
| | `product_shops` | 商品-店铺关联 |
| | `product_imports` | 导入记录 |
| | `product_media_files` | 媒体文件 |
| | `product_templates` | 商品模板 |
| 主题 | `theme_versions` | 主题版本 |
| | `theme_migration_history` | 迁移历史 |
| | `theme_migration_rules` | 迁移规则 |
| | `liquid_schema_cache` | Liquid Schema 缓存 |
| 物流 | `tracking_numbers` | 物流追踪号 |
| | `tracking_events` | 物流事件 |
| | `carriers` | 承运商 |
| 钉钉 | `dingtalk_dept_mappings` | 部门映射 |
| | `dingtalk_sync_logs` | 同步日志 |
| | `allowed_corp_ids` | 企业白名单 |
| 系统 | `notification_recipients` | 通知接收人 |
| | `notification_logs` | 通知日志 |
| | `email_monitor_configs` | 邮件监控配置 |
| | `invitations` | 邀请记录 |

## 配置说明

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
    base-url: http://localhost:8080/api/v1  # 需要 ngrok 暴露到公网
```

#### 生产环境

```yaml
shopify:
  oauth:
    redirect-uri: https://yourdomain.com/api/v1/oauth/shopify/callback
    frontend-redirect: https://yourdomain.com/shops
  webhook:
    base-url: https://yourdomain.com/api/v1  # 必须 HTTPS
```

#### 本地 Webhook 测试

```bash
# 使用 ngrok 暴露本地服务到公网
ngrok http 8080
# 复制 ngrok 提供的公网地址，更新 application.yml 中的 webhook.base-url
```

### 存储配置

支持本地存储和 MinIO 对象存储两种模式：

```yaml
# 本地存储
storage:
  type: local
  local:
    path: ./storage/theme-archives

# MinIO 对象存储
storage:
  type: minio
  minio:
    endpoint: http://localhost:9000
    access-key: YOUR_ACCESS_KEY
    secret-key: YOUR_SECRET_KEY
    buckets:
      themes: shopify-themes
      media: product-media
```

## 故障排除

### 数据库连接失败
- 检查 MySQL 服务是否启动
- 确认数据库用户名和密码正确
- 确认数据库 `logistics_system` 已创建

### Redis 连接失败
- 检查 Redis 服务是否启动：`redis-cli ping`
- 确认 Redis 端口（默认 6379）

### Shopify OAuth 失败

**`invalid_request: The redirect_uri is not whitelisted`**
- 检查 Shopify App 设置中的 Allowed redirection URL(s) 与 `application.yml` 中的 `redirect-uri` 完全一致

**State 验证失败**
- 检查 Redis 是否正常运行
- OAuth state 有效期默认 5 分钟

**HMAC 验证失败**
- 检查 Shopify API Secret 配置是否正确

### Webhook 接收失败
- 确认 `webhook.base-url` 配置为公网可访问地址
- 本地开发需使用 ngrok 等工具

## 注意事项

### 安全性
1. **生产环境必须修改**：JWT Secret、数据库密码、Shopify API Secret、17Track API Token
2. **HTTPS**：生产环境必须使用 HTTPS，Shopify OAuth 和 Webhook 强制要求
3. **敏感信息**：使用环境变量管理，不要将 `application.yml` 中的密钥提交到版本控制

### API 限制
- **17Track API**：有调用频率限制，建议实现请求队列
- **Shopify API**：REST API 限制 2 次/秒，GraphQL API 积分制限制

### 本地开发
- **Webhook 测试**：需使用 ngrok 暴露本地服务到公网
- **跨域配置**：前后端分离已配置 CORS
- **日志级别**：开发环境建议使用 DEBUG 级别

---

**版本**: v2.0.0
**最后更新**: 2026-03-12
