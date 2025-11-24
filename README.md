# Track17 物流追踪系统

基于 Spring Boot 和 17Track API 的物流追踪管理系统，专为跨境电商设计。

## 🚀 项目简介

Track17 是一个功能完整的物流追踪系统，集成了 17Track API 和主流电商平台（Shopify/Shopline/TikTok Shop），帮助电商企业集中管理和监控订单物流状态。

## ✨ 核心功能

### MVP 版本（当前实现）

- ✅ **用户认证**：简易的 JWT 登录系统
- ✅ **店铺管理**：支持添加、编辑、删除 Shopify/Shopline/TikTok Shop 店铺
- ✅ **运单管理**：手动添加运单、查看运单列表、删除运单
- ✅ **17Track 集成**：自动注册运单到 17Track、查询物流信息
- ✅ **前端界面**：提供简洁易用的管理界面

### 计划功能（后续版本）

- 📦 CSV 批量导入运单
- 🔄 自动同步 Shopify 订单
- 📊 物流轨迹可视化展示
- 🔔 异常运单提醒
- 📈 数据统计看板
- 🌐 Webhook 实时接收物流更新

## 🛠 技术栈

### 后端
- Java 8+
- Spring Boot 2.7.18
- MyBatis
- MySQL 8.0
- Redis
- OkHttp (HTTP 客户端)
- JWT (认证)

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

## 📋 环境要求

### 后端
- JDK 1.8 或更高版本
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+ (可选)

### 前端（Vue3 版本）
- Node.js 16+ 或更高版本
- npm 或 yarn 或 pnpm

## 🚀 快速开始

### 1. 创建数据库

执行以下 SQL 创建数据库和表结构：

```bash
mysql -u root -p < docs/database.sql
```

或者手动执行数据库建表脚本（参考项目文档中的建表语句）。

### 2. 配置应用

编辑 `src/main/resources/application.yml`，修改以下配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/logistics_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC
    username: root        # 修改为你的数据库用户名
    password: root        # 修改为你的数据库密码

  redis:
    host: localhost
    port: 6379
    password:             # 如有密码请填写

# 17Track API配置
track17:
  api:
    token: YOUR_17TRACK_API_TOKEN  # 替换为你的 17Track API Token
```

### 3. 编译项目

```bash
mvn clean package
```

### 4. 启动应用

```bash
# 方式1: 使用 Maven
mvn spring-boot:run

# 方式2: 使用 jar 包
java -jar target/track-17-server-1.0.0.jar
```

启动成功后，后端服务运行在 `http://localhost:8080`

### 5. 访问前端界面

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
│   │   │   ├── controller/      # 控制器
│   │   │   ├── service/         # 业务逻辑
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
│   ├── PRD.md                  # 产品需求文档
│   ├── USER_STORIES.md         # 用户故事
│   └── API_DOCUMENTATION.md    # API 文档
└── pom.xml                      # Maven 配置文件
```

## 📡 API 接口

### 认证相关
- `POST /api/v1/auth/login` - 用户登录
- `GET /api/v1/auth/validate` - 验证 Token
- `GET /api/v1/auth/current` - 获取当前用户

### 店铺管理
- `POST /api/v1/shops` - 创建店铺
- `GET /api/v1/shops` - 获取店铺列表
- `GET /api/v1/shops/{id}` - 获取店铺详情
- `PUT /api/v1/shops/{id}` - 更新店铺
- `DELETE /api/v1/shops/{id}` - 删除店铺

### 运单管理
- `POST /api/v1/tracking` - 添加运单
- `GET /api/v1/tracking` - 获取运单列表
- `GET /api/v1/tracking/{id}` - 获取运单详情
- `POST /api/v1/tracking/{id}/sync` - 手动同步运单
- `DELETE /api/v1/tracking/{id}` - 删除运单
- `POST /api/v1/tracking/batch-delete` - 批量删除运单

详细 API 文档请查看：[docs/API_DOCUMENTATION.md](docs/API_DOCUMENTATION.md)

## 🔧 配置说明

### JWT 配置

```yaml
jwt:
  secret: track17-logistics-secret-key-change-in-production  # 生产环境请修改
  expiration: 86400000  # Token 过期时间（毫秒），默认 24 小时
```

### 17Track API 配置

1. 注册 17Track 账号：https://www.17track.net/
2. 获取 API Token
3. 在 `application.yml` 中配置 Token

```yaml
track17:
  api:
    url: https://api.17track.net/track/v2.4
    token: YOUR_17TRACK_API_TOKEN
```

## 🗄️ 数据库表结构

主要数据表：

- `shops` - 店铺信息表
- `orders` - 订单表
- `parcels` - 包裹表
- `tracking_numbers` - 运单表
- `tracking_events` - 物流事件表
- `carriers` - 承运商表

详细建表语句见项目根目录下的 SQL 文件。

## 🐛 故障排除

### 数据库连接失败
- 检查 MySQL 服务是否启动
- 确认数据库用户名和密码正确
- 检查数据库 URL 中的端口号

### JWT Token 无效
- 检查 Token 是否过期
- 确认请求头格式：`Authorization: Bearer {token}`

### 17Track API 调用失败
- 确认 API Token 配置正确
- 检查网络连接
- 查看 17Track API 文档确认请求格式

## 📝 开发计划

查看详细的开发计划和用户故事：[docs/USER_STORIES.md](docs/USER_STORIES.md)

### Sprint 规划

- **Sprint 1 (当前)**: MVP 核心功能 ✅
  - 店铺管理
  - 手动添加运单
  - 运单列表查看

- **Sprint 2**: 导入和详情功能
  - CSV 批量导入
  - 运单详情页
  - 物流轨迹展示

- **Sprint 3**: 17Track 集成
  - Webhook 接收
  - 自动注册运单
  - 失败重试机制

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

## 📧 联系方式

如有问题，请通过 Issue 反馈。

---

**注意事项：**

1. 本项目为 MVP 版本，部分功能仍在开发中
2. 生产环境部署前请修改 JWT Secret 和数据库密码
3. 建议配置 Redis 以提升系统性能
4. 17Track API 有调用频率限制，请合理使用

