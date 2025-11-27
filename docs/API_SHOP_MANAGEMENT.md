# 店铺管理 API 接口文档

## 基础信息

**Base URL**: `/api/v1`

**认证方式**: JWT Token（在 Header 中添加 `Authorization: Bearer {token}`）

---

## 📋 接口列表

### 1. 获取店铺列表

```http
GET /shops
```

**Query Parameters**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| platform | string | 否 | 平台筛选：shopify, shopline, tiktokshop |
| page | number | 否 | 页码，默认 1 |
| pageSize | number | 否 | 每页数量，默认 20 |

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "userId": 1,
        "shopName": "My Shopify Store",
        "platform": "shopify",
        "storeUrl": "https://mystore.myshopify.com",
        "shopDomain": "mystore.myshopify.com",
        "timezone": "America/New_York",
        "tokenType": "offline",
        "connectionStatus": "active",
        "lastValidatedAt": "2025-11-26T14:30:00",
        "oauthScope": "read_orders,write_orders",
        "isActive": true,
        "createdAt": "2025-11-20T10:00:00",
        "updatedAt": "2025-11-26T14:30:00",
        "orderCount": 156
      }
    ],
    "total": 10,
    "page": 1,
    "pageSize": 20
  }
}
```

---

### 2. 获取店铺详情

```http
GET /shops/{id}
```

**Path Parameters**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | number | 是 | 店铺 ID |

**Response**: 同上，data 为单个店铺对象

---

### 3. ✨ 验证店铺连接状态

```http
POST /shops/{id}/validate
```

**说明**: 验证指定店铺的 access token 是否仍然有效

**Path Parameters**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | number | 是 | 店铺 ID |

**Response**:
```json
{
  "code": 200,
  "message": "连接验证完成",
  "data": {
    "shopId": 1,
    "shopName": "My Shopify Store",
    "platform": "shopify",
    "status": "active",
    "message": "连接正常",
    "tokenType": "offline",
    "lastValidatedAt": "2025-11-26T15:00:00"
  }
}
```

**状态说明**:
- `active` - 连接正常
- `invalid` - Token 已失效，需要重新授权
- `unsupported` - 该平台不支持验证
- `error` - 验证过程出错

---

### 4. ✨ 批量验证所有店铺

```http
POST /shops/validate-all
```

**说明**: 批量验证所有店铺的连接状态

**Response**:
```json
{
  "code": 200,
  "message": "批量验证完成",
  "data": [
    {
      "shopId": 1,
      "shopName": "Store 1",
      "platform": "shopify",
      "status": "active",
      "message": "连接正常"
    },
    {
      "shopId": 2,
      "shopName": "Store 2",
      "platform": "shopify",
      "status": "invalid",
      "message": "访问令牌已失效，请重新授权"
    }
  ]
}
```

---

### 5. 创建店铺（手动）

```http
POST /shops
```

**Request Body**:
```json
{
  "shopName": "My Store",
  "platform": "shopify",
  "storeUrl": "https://mystore.myshopify.com",
  "apiKey": "your-api-key",
  "apiSecret": "your-api-secret"
}
```

**Response**: 同获取店铺详情

---

### 6. 更新店铺

```http
PUT /shops/{id}
```

**Request Body**: 同创建店铺，所有字段可选

**Response**: 同获取店铺详情

---

### 7. 删除店铺

```http
DELETE /shops/{id}
```

**Response**:
```json
{
  "code": 200,
  "message": "店铺删除成功",
  "data": null
}
```

---

### 8. 发起 Shopify OAuth 授权

```http
GET /oauth/shopify/authorize?shopDomain={domain}
```

**说明**:
- 这是一个重定向接口，会跳转到 Shopify 授权页面
- 用户授权后会回调到后端 callback 接口
- 后端处理完成后会重定向到前端回调 URL

**Query Parameters**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| shopDomain | string | 是 | Shopify 店铺域名，如：mystore.myshopify.com |

**流程**:
```
1. 前端跳转: GET /oauth/shopify/authorize?shopDomain=mystore.myshopify.com
2. 后端重定向到 Shopify 授权页面
3. 用户在 Shopify 确认授权
4. Shopify 回调后端: GET /oauth/shopify/callback?code=xxx&state=xxx
5. 后端处理授权，保存店铺信息
6. 后端重定向前端: http://localhost:3000/shops?oauth=success
```

**前端处理回调**:
```typescript
// 在 /shops 页面检查 URL 参数
const searchParams = new URLSearchParams(window.location.search);
const oauthStatus = searchParams.get('oauth');

if (oauthStatus === 'success') {
  message.success('店铺授权成功！');
} else if (oauthStatus === 'error') {
  const reason = searchParams.get('reason');
  message.error(`授权失败: ${reason}`);
}
```

---

## 🎯 快速集成示例

### TypeScript 类型定义

```typescript
export interface Shop {
  id: number;
  userId: number;
  shopName: string;
  platform: 'shopify' | 'shopline' | 'tiktokshop';
  storeUrl: string;
  shopDomain?: string;
  timezone?: string;
  tokenType?: 'offline' | 'online';
  connectionStatus: 'active' | 'invalid' | 'pending';
  lastValidatedAt?: string;
  oauthScope?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  orderCount?: number;
}

export interface ValidationResult {
  shopId: number;
  shopName: string;
  platform: string;
  status: 'active' | 'invalid' | 'unsupported' | 'error';
  message: string;
  tokenType?: string;
  lastValidatedAt?: string;
}
```

### Axios 调用示例

```typescript
import axios from 'axios';

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`,
  },
});

// 获取店铺列表
const getShops = async (page = 1) => {
  const response = await api.get('/shops', { params: { page } });
  return response.data.data;
};

// 验证店铺连接
const validateShop = async (shopId: number) => {
  const response = await api.post(`/shops/${shopId}/validate`);
  return response.data.data;
};

// 批量验证
const validateAll = async () => {
  const response = await api.post('/shops/validate-all');
  return response.data.data;
};

// 发起 Shopify 授权
const authorizeShopify = (shopDomain: string) => {
  window.location.href = `/api/v1/oauth/shopify/authorize?shopDomain=${shopDomain}`;
};
```

### Fetch 调用示例

```javascript
// 验证店铺连接
async function validateShop(shopId) {
  const response = await fetch(`/api/v1/shops/${shopId}/validate`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
      'Content-Type': 'application/json',
    },
  });

  const result = await response.json();

  if (result.code === 200 && result.data.status === 'active') {
    console.log('连接正常');
  } else if (result.data.status === 'invalid') {
    console.error('Token 已失效，需要重新授权');
  }

  return result.data;
}

// 批量验证
async function validateAllShops() {
  const response = await fetch('/api/v1/shops/validate-all', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
    },
  });

  const result = await response.json();
  const invalidShops = result.data.filter(shop => shop.status === 'invalid');

  console.log(`共验证 ${result.data.length} 个店铺`);
  console.log(`失效店铺: ${invalidShops.length} 个`);

  return result.data;
}
```

---

## 🔄 OAuth 授权完整流程

### 1. 前端发起授权

```typescript
// 用户点击"添加 Shopify 店铺"按钮
const handleAddShopify = () => {
  const shopDomain = prompt('请输入店铺域名（如：mystore.myshopify.com）');

  if (shopDomain) {
    // 跳转到后端授权接口
    window.location.href = `/api/v1/oauth/shopify/authorize?shopDomain=${shopDomain}`;
  }
};
```

### 2. 后端处理授权

后端会：
1. 生成 OAuth state（存储到 Redis）
2. 重定向到 Shopify 授权页面
3. 用户授权后，Shopify 回调后端
4. 后端验证 state 和 HMAC
5. 使用授权码换取 **offline access token**（永久有效）
6. 获取店铺详细信息
7. 保存到数据库
8. 重定向回前端

### 3. 前端处理回调

```typescript
// 在 /shops 页面（或专门的回调页面）
useEffect(() => {
  const params = new URLSearchParams(window.location.search);
  const oauthStatus = params.get('oauth');

  if (oauthStatus === 'success') {
    message.success('店铺授权成功！');
    // 刷新店铺列表
    loadShops();
    // 清理 URL 参数
    window.history.replaceState({}, '', '/shops');
  } else if (oauthStatus === 'error') {
    const reason = params.get('reason');
    message.error(`授权失败: ${reason}`);
    window.history.replaceState({}, '', '/shops');
  }
}, []);
```

---

## ⚠️ 错误处理

### 通用错误响应格式

```json
{
  "code": 400,
  "message": "错误信息",
  "data": null
}
```

### 常见错误码

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 400 | 请求参数错误 | 检查请求参数 |
| 401 | 未授权 | 重新登录 |
| 403 | 无权限 | 检查用户权限 |
| 404 | 资源不存在 | 检查 ID 是否正确 |
| 500 | 服务器错误 | 联系技术支持 |

### 前端错误处理示例

```typescript
axios.interceptors.response.use(
  response => response,
  error => {
    const { status, data } = error.response || {};

    switch (status) {
      case 401:
        message.error('登录已过期，请重新登录');
        // 跳转到登录页
        window.location.href = '/login';
        break;
      case 403:
        message.error('没有权限执行此操作');
        break;
      case 404:
        message.error('请求的资源不存在');
        break;
      case 500:
        message.error(data?.message || '服务器错误，请稍后重试');
        break;
      default:
        message.error(data?.message || '请求失败');
    }

    return Promise.reject(error);
  }
);
```

---

## 📱 移动端建议

对于移动端 H5 页面，建议：

1. **使用响应式设计**
   - 表格使用卡片列表替代
   - 简化操作按钮

2. **优化交互**
   - 使用下拉刷新
   - 滑动操作（验证、删除）

3. **减少网络请求**
   - 合理使用缓存
   - 懒加载长列表

---

## 🎯 最佳实践

1. **定时刷新**
   ```typescript
   // 每 5 分钟自动刷新店铺列表
   useEffect(() => {
     const interval = setInterval(() => {
       loadShops();
     }, 5 * 60 * 1000);
     return () => clearInterval(interval);
   }, []);
   ```

2. **请求去重**
   ```typescript
   // 避免重复点击触发多次验证
   const [validating, setValidating] = useState(false);

   const handleValidate = async () => {
     if (validating) return;
     setValidating(true);
     try {
       await validateShop(shopId);
     } finally {
       setValidating(false);
     }
   };
   ```

3. **缓存策略**
   ```typescript
   // 使用 React Query 管理缓存
   import { useQuery } from 'react-query';

   const { data, isLoading } = useQuery(
     ['shops', page],
     () => getShops(page),
     {
       staleTime: 5 * 60 * 1000, // 5分钟内不重新请求
       cacheTime: 10 * 60 * 1000,
     }
   );
   ```

---

## 📞 技术支持

如有问题，请参考：
- [完整前端集成指南](./FRONTEND_SHOP_INTEGRATION.md)
- [Shopify 集成总结](./SHOPIFY_SHOP_INTEGRATION_SUMMARY.md)
- [API 后端文档](./API_DOCUMENTATION.md)
