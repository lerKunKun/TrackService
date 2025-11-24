# Track17 前端项目 (Vue3 + Vite)

基于 Vue3 + Vite + Ant Design Vue 的现代化物流追踪系统前端。

## 🎨 技术栈

- **框架**: Vue 3 (Composition API)
- **构建工具**: Vite 5
- **UI 组件库**: Ant Design Vue 4
- **路由**: Vue Router 4
- **状态管理**: Pinia
- **HTTP 客户端**: Axios
- **日期处理**: Day.js

## 🚀 快速开始

### 1. 安装依赖

```bash
cd frontend-vue
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 3. 构建生产版本

```bash
npm run build
```

### 4. 预览生产构建

```bash
npm run preview
```

## 📁 项目结构

```
frontend-vue/
├── public/              # 静态资源
├── src/
│   ├── api/            # API 接口
│   │   ├── auth.js     # 认证相关
│   │   ├── shop.js     # 店铺管理
│   │   └── tracking.js # 运单管理
│   ├── assets/         # 资源文件
│   │   └── styles/     # 样式文件
│   │       └── main.css
│   ├── components/     # 公共组件
│   ├── router/         # 路由配置
│   │   └── index.js
│   ├── stores/         # Pinia 状态管理
│   │   └── user.js     # 用户状态
│   ├── utils/          # 工具函数
│   │   └── request.js  # Axios 封装
│   ├── views/          # 页面组件
│   │   ├── Login.vue   # 登录页
│   │   ├── Layout.vue  # 布局组件
│   │   ├── Shops.vue   # 店铺管理
│   │   └── Tracking.vue # 运单管理
│   ├── App.vue         # 根组件
│   └── main.js         # 入口文件
├── index.html          # HTML 模板
├── vite.config.js      # Vite 配置
└── package.json        # 项目配置
```

## 🎨 设计规范

### 配色方案

遵循阿里巴巴设计规范，采用蓝白配色主题：

- **主色**: `#1890ff` (Ant Design Blue)
- **悬浮色**: `#40a9ff`
- **激活色**: `#096dd9`
- **成功色**: `#52c41a`
- **警告色**: `#faad14`
- **错误色**: `#ff4d4f`
- **背景色**: `#f0f2f5`
- **白色**: `#ffffff`

### 组件规范

- 使用 Ant Design Vue 组件库
- 遵循阿里巴巴前端设计规范
- 保持统一的视觉风格和交互体验

## 🔌 API 配置

项目使用 Vite 代理配置，自动将 `/api` 请求代理到后端服务：

```javascript
// vite.config.js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

后端 API 地址: `http://localhost:8080/api/v1`

## 📝 功能模块

### 1. 用户认证

- 登录页面 (`/login`)
- JWT Token 认证
- 自动路由守卫
- Token 过期处理

**默认账号**:
- 用户名: `admin`
- 密码: `admin123`

### 2. 店铺管理

- 店铺列表展示（分页）
- 添加/编辑/删除店铺
- 支持 Shopify、Shopline、TikTok Shop
- API 配置管理

### 3. 运单管理

- 运单列表展示（分页）
- 搜索和筛选功能
- 添加运单
- 查看运单详情和物流轨迹
- 同步运单状态
- 批量删除运单

## 🛠 开发指南

### 添加新页面

1. 在 `src/views/` 创建 Vue 组件
2. 在 `src/router/index.js` 添加路由配置
3. 在侧边菜单添加入口（如需要）

### 添加 API 接口

1. 在 `src/api/` 创建或编辑 API 文件
2. 使用统一的 `request` 实例发送请求
3. 接口自动处理认证和错误

示例：

```javascript
import request from '@/utils/request'

export const exampleApi = {
  getList(params) {
    return request.get('/example', { params })
  },

  create(data) {
    return request.post('/example', data)
  }
}
```

### 状态管理

使用 Pinia 进行状态管理：

```javascript
import { defineStore } from 'pinia'

export const useExampleStore = defineStore('example', () => {
  const state = ref(initialState)

  const action = () => {
    // 业务逻辑
  }

  return { state, action }
})
```

## 🔒 安全性

- JWT Token 存储在 localStorage
- 自动在请求头添加 Authorization
- 401 状态码自动跳转登录
- 路由守卫保护需要认证的页面

## 🌐 浏览器支持

- Chrome (推荐)
- Firefox
- Safari
- Edge

建议使用最新版本的现代浏览器。

## 📦 依赖说明

### 主要依赖

- `vue`: ^3.4.21 - Vue 3 框架
- `vue-router`: ^4.3.0 - 路由管理
- `pinia`: ^2.1.7 - 状态管理
- `ant-design-vue`: ^4.1.2 - UI 组件库
- `axios`: ^1.6.7 - HTTP 客户端
- `dayjs`: ^1.11.10 - 日期处理

### 开发依赖

- `vite`: ^5.1.6 - 构建工具
- `@vitejs/plugin-vue`: ^5.0.4 - Vue 插件

## 🐛 常见问题

### 问题 1: 端口被占用

修改 `vite.config.js` 中的端口：

```javascript
server: {
  port: 3001  // 修改为其他端口
}
```

### 问题 2: API 请求失败

1. 确认后端服务已启动（http://localhost:8080）
2. 检查代理配置
3. 查看浏览器控制台错误信息

### 问题 3: 依赖安装失败

```bash
# 清除缓存
rm -rf node_modules package-lock.json

# 重新安装
npm install
```

## 📝 开发规范

### 代码风格

- 使用 Vue 3 Composition API
- 使用 `<script setup>` 语法糖
- 组件名使用 PascalCase
- 事件处理函数使用 `handle` 前缀

### Git 提交规范

- `feat`: 新功能
- `fix`: 修复 Bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建/工具相关

## 🔄 更新日志

### v1.0.0 (2025-11-23)

- ✅ 初始版本发布
- ✅ 用户登录功能
- ✅ 店铺管理功能
- ✅ 运单管理功能
- ✅ 蓝白主题配色
- ✅ Ant Design Vue 集成

## 📞 技术支持

如有问题，请提交 Issue 或联系开发团队。

---

**Powered by Vue 3 + Vite + Ant Design Vue**
