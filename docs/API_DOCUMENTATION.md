# 后端 API 文档

## 基础信息

- **Base URL**: `http://localhost:8080/api/v1`
- **认证方式**: JWT Token (Bearer Token)
- **数据格式**: JSON
- **字符编码**: UTF-8

## 通用响应格式

### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    // 具体数据
  },
  "timestamp": 1672531200000
}
```

### 分页响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [],
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "totalPages": 5
  },
  "timestamp": 1672531200000
}
```

### 错误响应
```json
{
  "code": 400,
  "message": "请求参数错误",
  "error": "tracking_number is required",
  "timestamp": 1672531200000
}
```

## HTTP状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 503 | 服务不可用 |

---

# API 接口

## 1. 店铺管理 (Shops)

### 1.1 创建店铺

**接口**: `POST /shops`

**描述**: 添加新的店铺

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**:
```json
{
  "shopName": "My Shopify Store",
  "platform": "shopify",
  "storeUrl": "https://mystore.myshopify.com",
  "apiKey": "xxxxx",
  "apiSecret": "xxxxx",
  "accessToken": "xxxxx",
  "timezone": "America/New_York"
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| shopName | string | 是 | 店铺名称 |
| platform | string | 是 | 平台类型: shopify/shopline/tiktokshop |
| storeUrl | string | 否 | 店铺URL |
| apiKey | string | 是 | API Key |
| apiSecret | string | 是 | API Secret |
| accessToken | string | 是 | Access Token |
| timezone | string | 否 | 时区，默认UTC |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "shopName": "My Shopify Store",
    "platform": "shopify",
    "storeUrl": "https://mystore.myshopify.com",
    "timezone": "America/New_York",
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

---

### 1.2 获取店铺列表

**接口**: `GET /shops`

**描述**: 获取所有店铺列表

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页数量，默认20 |
| platform | string | 否 | 平台筛选 |

**请求示例**:
```
GET /shops?page=1&pageSize=20&platform=shopify
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "shopName": "My Shopify Store",
        "platform": "shopify",
        "storeUrl": "https://mystore.myshopify.com",
        "timezone": "America/New_York",
        "orderCount": 150,
        "createdAt": "2024-01-01T00:00:00Z"
      }
    ],
    "total": 1,
    "page": 1,
    "pageSize": 20,
    "totalPages": 1
  }
}
```

---

### 1.3 获取店铺详情

**接口**: `GET /shops/{id}`

**描述**: 获取单个店铺的详细信息

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 店铺ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "shopName": "My Shopify Store",
    "platform": "shopify",
    "storeUrl": "https://mystore.myshopify.com",
    "apiKey": "xxxxx",
    "timezone": "America/New_York",
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

---

### 1.4 更新店铺

**接口**: `PUT /shops/{id}`

**描述**: 更新店铺信息

**请求体**:
```json
{
  "shopName": "Updated Store Name",
  "storeUrl": "https://newstore.myshopify.com",
  "apiKey": "new_api_key",
  "apiSecret": "new_api_secret",
  "accessToken": "new_access_token",
  "timezone": "Asia/Shanghai"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "shopName": "Updated Store Name",
    "updatedAt": "2024-01-02T00:00:00Z"
  }
}
```

---

### 1.5 删除店铺

**接口**: `DELETE /shops/{id}`

**描述**: 删除店铺（级联删除相关订单和运单）

**响应示例**:
```json
{
  "code": 200,
  "message": "店铺删除成功"
}
```

---

## 2. 运单管理 (Tracking Numbers)

### 2.1 批量导入运单 (CSV)

**接口**: `POST /tracking/import/csv`

**描述**: 通过CSV文件批量导入运单

**请求头**:
```
Content-Type: multipart/form-data
```

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | file | 是 | CSV文件 |
| shopId | long | 否 | 关联店铺ID |

**CSV格式**:
```csv
tracking_number,carrier_code,order_id,remarks
1Z999AA10123456784,ups,#1001,
LZ123456789CN,4px,#1002,Priority
```

**响应示例**:
```json
{
  "code": 200,
  "message": "导入完成",
  "data": {
    "total": 100,
    "success": 95,
    "failed": 5,
    "failedRecords": [
      {
        "row": 10,
        "trackingNumber": "INVALID123",
        "reason": "无效的运单号格式"
      }
    ]
  }
}
```

---

### 2.2 手动添加运单

**接口**: `POST /tracking`

**描述**: 手动添加单个运单

**请求体**:
```json
{
  "trackingNumber": "1Z999AA10123456784",
  "carrierCode": "ups",
  "orderId": 123,
  "source": "manual",
  "remarks": "客户加急"
}
```

**字段说明**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| trackingNumber | string | 是 | 运单号 |
| carrierCode | string | 是 | 承运商代码 |
| orderId | long | 否 | 关联订单ID |
| source | string | 否 | 来源: manual/shopify/api |
| remarks | string | 否 | 备注 |

**响应示例**:
```json
{
  "code": 200,
  "message": "运单添加成功",
  "data": {
    "id": 1001,
    "trackingNumber": "1Z999AA10123456784",
    "carrierCode": "ups",
    "carrierName": "UPS",
    "trackStatus": "InfoReceived",
    "createdAt": "2024-01-01T10:00:00Z"
  }
}
```

---

### 2.3 获取运单列表

**接口**: `GET /tracking`

**描述**: 分页获取运单列表，支持筛选和搜索

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页数量，默认20，最大100 |
| keyword | string | 否 | 搜索关键词（运单号/订单号） |
| shopId | long | 否 | 店铺ID筛选 |
| status | string | 否 | 状态筛选，多个用逗号分隔 |
| carrierCode | string | 否 | 承运商筛选 |
| startDate | string | 否 | 开始时间 (ISO 8601) |
| endDate | string | 否 | 结束时间 (ISO 8601) |
| sortBy | string | 否 | 排序字段: createdAt/updatedAt |
| sortOrder | string | 否 | 排序方向: asc/desc，默认desc |

**请求示例**:
```
GET /tracking?page=1&pageSize=20&status=InTransit,Delivered&carrierCode=ups
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1001,
        "trackingNumber": "1Z999AA10123456784",
        "carrierCode": "ups",
        "carrierName": "UPS",
        "trackStatus": "InTransit",
        "orderId": 123,
        "orderNumber": "#1001",
        "lastSyncAt": "2024-01-01T12:00:00Z",
        "createdAt": "2024-01-01T10:00:00Z",
        "updatedAt": "2024-01-01T12:00:00Z"
      }
    ],
    "total": 500,
    "page": 1,
    "pageSize": 20,
    "totalPages": 25
  }
}
```

---

### 2.4 获取运单详情

**接口**: `GET /tracking/{id}`

**描述**: 获取运单详细信息，包括物流轨迹

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 运单ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1001,
    "trackingNumber": "1Z999AA10123456784",
    "carrierCode": "ups",
    "carrierName": "UPS",
    "trackStatus": "InTransit",
    "source": "manual",
    "lastSyncAt": "2024-01-01T12:00:00Z",
    "nextSyncAt": "2024-01-01T14:00:00Z",
    "createdAt": "2024-01-01T10:00:00Z",
    "updatedAt": "2024-01-01T12:00:00Z",
    "order": {
      "id": 123,
      "orderNumber": "#1001",
      "customerName": "张三",
      "customerEmail": "zhang@example.com"
    },
    "events": [
      {
        "id": 5001,
        "status": "Out for Delivery",
        "stage": "InTransit",
        "location": "New York, NY",
        "description": "包裹正在派送中",
        "eventTime": "2024-01-01T09:00:00Z"
      },
      {
        "id": 5000,
        "status": "Arrived at Facility",
        "stage": "InTransit",
        "location": "Newark, NJ",
        "description": "包裹已到达分拣中心",
        "eventTime": "2024-01-01T06:00:00Z"
      }
    ]
  }
}
```

---

### 2.5 通过运单号查询

**接口**: `GET /tracking/by-number/{trackingNumber}`

**描述**: 通过运单号直接查询

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| trackingNumber | string | 运单号 |

**响应示例**: 同2.4

---

### 2.6 手动同步运单

**接口**: `POST /tracking/{id}/sync`

**描述**: 手动触发运单状态同步

**响应示例**:
```json
{
  "code": 200,
  "message": "同步成功",
  "data": {
    "trackingNumber": "1Z999AA10123456784",
    "trackStatus": "InTransit",
    "lastSyncAt": "2024-01-01T13:00:00Z",
    "newEventsCount": 2
  }
}
```

---

### 2.7 删除运单

**接口**: `DELETE /tracking/{id}`

**描述**: 删除单个运单

**响应示例**:
```json
{
  "code": 200,
  "message": "运单删除成功"
}
```

---

### 2.8 批量删除运单

**接口**: `POST /tracking/batch-delete`

**描述**: 批量删除运单

**请求体**:
```json
{
  "ids": [1001, 1002, 1003]
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "批量删除成功",
  "data": {
    "total": 3,
    "success": 3,
    "failed": 0
  }
}
```

---

### 2.9 导出运单

**接口**: `GET /tracking/export`

**描述**: 导出运单列表为CSV文件

**请求参数**: 同2.3（支持相同的筛选条件）

**响应**: 返回CSV文件流

**响应头**:
```
Content-Type: text/csv
Content-Disposition: attachment; filename="tracking_export_20240101.csv"
```

---

### 2.10 下载CSV模板

**接口**: `GET /tracking/template`

**描述**: 下载运单导入CSV模板

**响应**: 返回CSV文件流

---

## 3. 订单管理 (Orders)

### 3.1 获取订单列表

**接口**: `GET /orders`

**描述**: 分页获取订单列表

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| pageSize | int | 否 | 每页数量 |
| shopId | long | 否 | 店铺ID |
| keyword | string | 否 | 搜索订单号 |
| fulfillmentStatus | string | 否 | 配送状态 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 123,
        "shopId": 1,
        "orderId": "#1001",
        "externalOrderId": "4567890",
        "customerName": "张三",
        "customerEmail": "zhang@example.com",
        "totalPrice": 99.99,
        "currency": "USD",
        "fulfillmentStatus": "fulfilled",
        "trackingCount": 2,
        "createdAt": "2024-01-01T00:00:00Z"
      }
    ],
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "totalPages": 5
  }
}
```

---

### 3.2 获取订单详情

**接口**: `GET /orders/{id}`

**描述**: 获取订单详细信息

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 123,
    "shopId": 1,
    "shopName": "My Shopify Store",
    "orderId": "#1001",
    "externalOrderId": "4567890",
    "customerName": "张三",
    "customerEmail": "zhang@example.com",
    "customerPhone": "+86 138****1234",
    "totalPrice": 99.99,
    "currency": "USD",
    "fulfillmentStatus": "fulfilled",
    "financialStatus": "paid",
    "parcels": [
      {
        "id": 501,
        "parcelNo": "PKG001",
        "carrierCode": "ups",
        "carrierName": "UPS",
        "status": "in_transit",
        "trackingNumbers": [
          {
            "id": 1001,
            "trackingNumber": "1Z999AA10123456784",
            "trackStatus": "InTransit"
          }
        ]
      }
    ],
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T10:00:00Z"
  }
}
```

---

### 3.3 同步Shopify订单

**接口**: `POST /orders/sync/{shopId}`

**描述**: 从Shopify同步订单数据

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| shopId | long | 店铺ID |

**请求体**:
```json
{
  "startDate": "2024-01-01T00:00:00Z",
  "endDate": "2024-01-31T23:59:59Z",
  "limit": 250
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "同步已启动",
  "data": {
    "jobId": "sync-job-001",
    "status": "processing"
  }
}
```

---

## 4. 承运商管理 (Carriers)

### 4.1 获取承运商列表

**接口**: `GET /carriers`

**描述**: 获取所有承运商信息

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| country | string | 否 | 国家筛选 |
| keyword | string | 否 | 搜索承运商名称 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "carrierCode": "ups",
      "carrierName": "UPS",
      "carrierCountry": "US",
      "servicePhone": "1-800-742-5877",
      "website": "https://www.ups.com"
    }
  ]
}
```

---

### 4.2 添加承运商

**接口**: `POST /carriers`

**描述**: 添加新的承运商

**请求体**:
```json
{
  "carrierCode": "dhl",
  "carrierName": "DHL Express",
  "carrierCountry": "DE",
  "servicePhone": "+49 228 4333112",
  "website": "https://www.dhl.com"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "承运商添加成功",
  "data": {
    "id": 10,
    "carrierCode": "dhl",
    "carrierName": "DHL Express"
  }
}
```

---

## 5. Webhook接口 (17Track)

### 5.1 接收17Track Webhook

**接口**: `POST /webhook/17track`

**描述**: 接收17Track推送的运单状态更新

**请求头**:
```
17Token: {17track_webhook_token}
Content-Type: application/json
```

**请求体示例**:
```json
{
  "event": "TRACKING_UPDATE",
  "data": {
    "number": "1Z999AA10123456784",
    "carrier": 1001,
    "track_info": {
      "tracking": {
        "providers": [
          {
            "provider": {
              "key": "ups",
              "name": "UPS"
            },
            "events": [
              {
                "time_iso": "2024-01-01T09:00:00Z",
                "description": "Out for Delivery",
                "location": "New York, NY",
                "stage": "InTransit"
              }
            ],
            "latest_status": {
              "status": "InTransit",
              "sub_status": "InfoReceived"
            }
          }
        ]
      }
    }
  }
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "Webhook received"
}
```

**说明**:
- 必须在500ms内返回200响应
- 验证17Token签名
- 数据异步处理（通过消息队列）
- 记录到tracking_webhook_logs表

---

## 6. 统计接口 (Statistics)

### 6.1 获取运单统计

**接口**: `GET /statistics/tracking`

**描述**: 获取运单统计数据

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| shopId | long | 否 | 店铺ID筛选 |
| startDate | string | 否 | 开始日期 |
| endDate | string | 否 | 结束日期 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalCount": 5000,
    "statusBreakdown": {
      "InfoReceived": 500,
      "InTransit": 2000,
      "Delivered": 2300,
      "Exception": 200
    },
    "carrierBreakdown": {
      "ups": 2000,
      "fedex": 1500,
      "usps": 1000,
      "dhl": 500
    },
    "dailyTrend": [
      {
        "date": "2024-01-01",
        "count": 150
      },
      {
        "date": "2024-01-02",
        "count": 200
      }
    ]
  }
}
```

---

## 7. 系统管理

### 7.1 获取同步任务列表

**接口**: `GET /sync-jobs`

**描述**: 获取同步任务记录

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| pageSize | int | 否 | 每页数量 |
| status | string | 否 | 状态: success/failed |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "trackingId": 1001,
        "trackingNumber": "1Z999AA10123456784",
        "status": "success",
        "errorMessage": null,
        "startedAt": "2024-01-01T12:00:00Z",
        "finishedAt": "2024-01-01T12:00:05Z"
      }
    ],
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "totalPages": 5
  }
}
```

---

### 7.2 重试失败任务

**接口**: `POST /sync-jobs/{id}/retry`

**描述**: 重试失败的同步任务

**响应示例**:
```json
{
  "code": 200,
  "message": "重试任务已添加到队列"
}
```

---

## 8. 17Track API集成

### 8.1 注册运单到17Track

**内部接口**: 由系统自动调用

**17Track API**: `POST https://api.17track.net/track/v2.4/register`

**请求头**:
```
17token: {your_17track_api_key}
Content-Type: application/json
```

**请求体**:
```json
[
  {
    "number": "1Z999AA10123456784",
    "carrier": 1001,
    "param": {
      "org": "US",
      "dst": "CN"
    }
  }
]
```

---

### 8.2 查询运单信息

**内部接口**: 由系统定时任务调用

**17Track API**: `POST https://api.17track.net/track/v2.4/gettrackinfo`

**请求头**:
```
17token: {your_17track_api_key}
Content-Type: application/json
```

**请求体**:
```json
[
  {
    "number": "1Z999AA10123456784",
    "carrier": 1001
  }
]
```

---

## 错误码说明

| 错误码 | 说明 | 解决方案 |
|--------|------|----------|
| 1001 | 参数验证失败 | 检查请求参数 |
| 1002 | 资源不存在 | 确认资源ID是否正确 |
| 1003 | 重复数据 | 运单号已存在 |
| 2001 | 17Track API调用失败 | 检查API配置和网络 |
| 2002 | Shopify API调用失败 | 检查店铺配置 |
| 3001 | 数据库操作失败 | 联系管理员 |
| 3002 | Redis连接失败 | 检查Redis服务 |
| 4001 | 文件格式错误 | 检查CSV格式 |
| 4002 | 文件大小超限 | 文件不能超过10MB |
| 5001 | 认证失败 | 检查Token |
| 5002 | 权限不足 | 联系管理员 |

---

## 接口限流

| 接口类型 | 限制 |
|----------|------|
| 查询接口 | 100次/分钟 |
| 写入接口 | 50次/分钟 |
| 批量导入 | 10次/分钟 |
| Webhook | 1000次/分钟 |

---

## 开发指南

### Postman测试

1. 导入Postman Collection（待提供）
2. 配置环境变量：
   - `base_url`: http://localhost:8080/api/v1
   - `token`: {your_jwt_token}

### 认证流程

1. 获取JWT Token（待实现用户登录接口）
2. 在请求头添加: `Authorization: Bearer {token}`
3. Token有效期：24小时

### WebSocket实时通知（可选，第二期）

**连接地址**: `ws://localhost:8080/ws/tracking`

**消息格式**:
```json
{
  "type": "TRACKING_UPDATE",
  "data": {
    "trackingId": 1001,
    "trackingNumber": "1Z999AA10123456784",
    "status": "Delivered"
  }
}
```

---

## 变更日志

### v1.0.0 (2024-01-01)
- 初始版本
- 店铺管理API
- 运单管理API
- 订单管理API
- Webhook接收
- 统计接口

### 待实现
- 用户认证和授权API
- WebSocket实时通知
- 更多电商平台支持
- 高级报表接口
