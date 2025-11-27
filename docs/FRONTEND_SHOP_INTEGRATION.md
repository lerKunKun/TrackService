# 前端店铺管理集成指南

## 📋 API 接口总览

### 店铺管理 API

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 获取店铺列表 | GET | `/api/v1/shops` | 分页查询店铺列表 |
| 获取店铺详情 | GET | `/api/v1/shops/{id}` | 查询单个店铺详情 |
| 创建店铺 | POST | `/api/v1/shops` | 手动创建店铺 |
| 更新店铺 | PUT | `/api/v1/shops/{id}` | 更新店铺信息 |
| 删除店铺 | DELETE | `/api/v1/shops/{id}` | 删除店铺 |
| **验证连接** | POST | `/api/v1/shops/{id}/validate` | ✨ 验证单个店铺连接状态 |
| **批量验证** | POST | `/api/v1/shops/validate-all` | ✨ 验证所有店铺连接状态 |

### Shopify OAuth API

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 发起授权 | GET | `/api/v1/oauth/shopify/authorize` | 跳转到 Shopify 授权页面 |
| 授权回调 | GET | `/api/v1/oauth/shopify/callback` | Shopify 回调（自动处理） |

---

## 🎨 UI 设计建议

### 1. 店铺列表页面

```
┌─────────────────────────────────────────────────────────────────┐
│  店铺管理                                  [+ 添加店铺] [🔄 批量验证] │
├─────────────────────────────────────────────────────────────────┤
│  筛选: [平台▼] [状态▼]                           [搜索店铺...]   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ 🟢 My Shopify Store                    [🔄验证] [✏️编辑]  │  │
│  │ shopify  •  jaxdevstore.myshopify.com                     │  │
│  │ Token: Offline (永久)  •  最后验证: 2分钟前                │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ 🔴 Another Store                    [⚠️重新授权] [✏️编辑]  │  │
│  │ shopify  •  another.myshopify.com                         │  │
│  │ Token: Offline  •  ⚠️ 连接失效，请重新授权                 │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ 🟡 TikTok Shop                         [🔄验证] [✏️编辑]   │  │
│  │ tiktokshop  •  myshop.tiktok.com                          │  │
│  │ 不支持自动验证                                             │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  第 1 页 / 共 3 页                              [上一页] [下一页] │
└─────────────────────────────────────────────────────────────────┘
```

**状态指示器**:
- 🟢 绿色 - 连接正常（active）
- 🔴 红色 - 连接失效（invalid）
- 🟡 黄色 - 待授权（pending）

---

### 2. 店铺详情页面

```
┌─────────────────────────────────────────────────────────────────┐
│  ← 返回                           My Shopify Store              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─ 基本信息 ─────────────────────────────────────────────────┐ │
│  │  店铺名称: My Shopify Store                                │ │
│  │  平台类型: Shopify                                         │ │
│  │  店铺域名: jaxdevstore.myshopify.com                       │ │
│  │  店铺URL:  https://jaxdevstore.myshopify.com               │ │
│  │  时区:     America/New_York (EST)                          │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌─ 连接状态 ─────────────────────────────────────────────────┐ │
│  │  状态:     🟢 连接正常                     [🔄 立即验证]    │ │
│  │  Token类型: Offline (永久有效)                             │ │
│  │  授权范围: read_orders, write_orders, read_products        │ │
│  │  最后验证: 2025-11-26 14:30:00 (2分钟前)                   │ │
│  │  创建时间: 2025-11-20 10:00:00                             │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌─ 数据统计 ─────────────────────────────────────────────────┐ │
│  │  关联订单: 156 个                                          │ │
│  │  同步次数: 342 次                                          │ │
│  │  最后同步: 2025-11-26 14:25:00                             │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  [编辑店铺] [删除店铺]                                           │
└─────────────────────────────────────────────────────────────────┘
```

---

## 💻 前端代码实现（React + TypeScript）

### 1. 类型定义

```typescript
// src/types/shop.ts

export interface Shop {
  id: number;
  userId: number;
  shopName: string;
  platform: 'shopify' | 'shopline' | 'tiktokshop';
  storeUrl: string;
  shopDomain?: string;
  timezone?: string;
  accessToken?: string;      // 前端不应该获取完整token
  tokenType?: 'offline' | 'online';
  connectionStatus: 'active' | 'invalid' | 'pending';
  lastValidatedAt?: string;
  oauthScope?: string;
  tokenExpiresAt?: string;
  isActive: boolean;
  lastSyncTime?: string;
  createdAt: string;
  updatedAt: string;
  orderCount?: number;       // 关联订单数
}

export interface ShopListResponse {
  code: number;
  message: string;
  data: {
    items: Shop[];
    total: number;
    page: number;
    pageSize: number;
  };
}

export interface ConnectionValidationResult {
  shopId: number;
  shopName: string;
  platform: string;
  status: 'active' | 'invalid' | 'unsupported' | 'error';
  message: string;
  tokenType?: string;
  lastValidatedAt?: string;
}
```

---

### 2. API Service

```typescript
// src/services/shopApi.ts

import axios from 'axios';
import type { Shop, ShopListResponse, ConnectionValidationResult } from '@/types/shop';

const API_BASE = '/api/v1';

export const shopApi = {
  /**
   * 获取店铺列表
   */
  getList: async (params: {
    platform?: string;
    page?: number;
    pageSize?: number;
  }): Promise<ShopListResponse> => {
    const response = await axios.get(`${API_BASE}/shops`, { params });
    return response.data;
  },

  /**
   * 获取店铺详情
   */
  getById: async (id: number): Promise<Shop> => {
    const response = await axios.get(`${API_BASE}/shops/${id}`);
    return response.data.data;
  },

  /**
   * 创建店铺（手动）
   */
  create: async (data: Partial<Shop>): Promise<Shop> => {
    const response = await axios.post(`${API_BASE}/shops`, data);
    return response.data.data;
  },

  /**
   * 更新店铺
   */
  update: async (id: number, data: Partial<Shop>): Promise<Shop> => {
    const response = await axios.put(`${API_BASE}/shops/${id}`, data);
    return response.data.data;
  },

  /**
   * 删除店铺
   */
  delete: async (id: number): Promise<void> => {
    await axios.delete(`${API_BASE}/shops/${id}`);
  },

  /**
   * ✨ 验证单个店铺连接状态
   */
  validateConnection: async (id: number): Promise<ConnectionValidationResult> => {
    const response = await axios.post(`${API_BASE}/shops/${id}/validate`);
    return response.data.data;
  },

  /**
   * ✨ 批量验证所有店铺
   */
  validateAllConnections: async (): Promise<ConnectionValidationResult[]> => {
    const response = await axios.post(`${API_BASE}/shops/validate-all`);
    return response.data.data;
  },

  /**
   * 发起 Shopify OAuth 授权
   */
  authorizeShopify: (shopDomain: string) => {
    window.location.href = `${API_BASE}/oauth/shopify/authorize?shopDomain=${shopDomain}`;
  },
};
```

---

### 3. 店铺列表组件

```tsx
// src/pages/ShopList.tsx

import React, { useState, useEffect } from 'react';
import { shopApi } from '@/services/shopApi';
import type { Shop, ConnectionValidationResult } from '@/types/shop';
import {
  Table, Button, Tag, Space, message, Modal, Input,
  Tooltip, Spin
} from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  WarningOutlined,
  SyncOutlined,
  PlusOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

export const ShopList: React.FC = () => {
  const [shops, setShops] = useState<Shop[]>([]);
  const [loading, setLoading] = useState(false);
  const [validating, setValidating] = useState<number | null>(null);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  // 加载店铺列表
  const loadShops = async () => {
    setLoading(true);
    try {
      const response = await shopApi.getList({ page, pageSize });
      setShops(response.data.items);
      setTotal(response.data.total);
    } catch (error) {
      message.error('加载店铺列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadShops();
  }, [page, pageSize]);

  // 验证单个店铺连接
  const handleValidate = async (shopId: number) => {
    setValidating(shopId);
    try {
      const result = await shopApi.validateConnection(shopId);

      if (result.status === 'active') {
        message.success('连接验证成功');
      } else if (result.status === 'invalid') {
        message.error(`连接已失效: ${result.message}`);
      } else {
        message.warning(result.message);
      }

      // 刷新列表
      await loadShops();
    } catch (error) {
      message.error('验证失败');
    } finally {
      setValidating(null);
    }
  };

  // 批量验证所有店铺
  const handleValidateAll = async () => {
    Modal.confirm({
      title: '批量验证确认',
      content: '确定要验证所有店铺的连接状态吗？这可能需要一些时间。',
      onOk: async () => {
        setLoading(true);
        try {
          const results = await shopApi.validateAllConnections();

          const invalidCount = results.filter(r => r.status === 'invalid').length;
          const activeCount = results.filter(r => r.status === 'active').length;

          message.success(
            `批量验证完成！正常: ${activeCount} 个，失效: ${invalidCount} 个`
          );

          // 刷新列表
          await loadShops();
        } catch (error) {
          message.error('批量验证失败');
        } finally {
          setLoading(false);
        }
      },
    });
  };

  // 重新授权
  const handleReauthorize = (shop: Shop) => {
    if (shop.platform === 'shopify' && shop.shopDomain) {
      shopApi.authorizeShopify(shop.shopDomain);
    } else {
      message.warning('该平台暂不支持自动授权');
    }
  };

  // 添加 Shopify 店铺
  const handleAddShopify = () => {
    Modal.confirm({
      title: '添加 Shopify 店铺',
      content: (
        <div>
          <p>请输入您的 Shopify 店铺域名：</p>
          <Input
            id="shopify-domain-input"
            placeholder="例如: mystore.myshopify.com"
            onKeyPress={(e) => {
              if (e.key === 'Enter') {
                const input = document.getElementById('shopify-domain-input') as HTMLInputElement;
                if (input && input.value) {
                  shopApi.authorizeShopify(input.value);
                }
              }
            }}
          />
        </div>
      ),
      onOk: () => {
        const input = document.getElementById('shopify-domain-input') as HTMLInputElement;
        if (input && input.value) {
          shopApi.authorizeShopify(input.value);
        }
      },
    });
  };

  // 渲染连接状态
  const renderConnectionStatus = (shop: Shop) => {
    const status = shop.connectionStatus;

    if (status === 'active') {
      return (
        <Tag icon={<CheckCircleOutlined />} color="success">
          连接正常
        </Tag>
      );
    } else if (status === 'invalid') {
      return (
        <Tag icon={<CloseCircleOutlined />} color="error">
          连接失效
        </Tag>
      );
    } else {
      return (
        <Tag icon={<WarningOutlined />} color="warning">
          待授权
        </Tag>
      );
    }
  };

  // 渲染最后验证时间
  const renderLastValidated = (shop: Shop) => {
    if (!shop.lastValidatedAt) {
      return <span style={{ color: '#999' }}>从未验证</span>;
    }

    const fromNow = dayjs(shop.lastValidatedAt).fromNow();
    const fullTime = dayjs(shop.lastValidatedAt).format('YYYY-MM-DD HH:mm:ss');

    return (
      <Tooltip title={fullTime}>
        <span>{fromNow}</span>
      </Tooltip>
    );
  };

  // 表格列定义
  const columns = [
    {
      title: '店铺名称',
      dataIndex: 'shopName',
      key: 'shopName',
      width: 200,
    },
    {
      title: '平台',
      dataIndex: 'platform',
      key: 'platform',
      width: 100,
      render: (platform: string) => (
        <Tag color={platform === 'shopify' ? 'green' : 'blue'}>
          {platform}
        </Tag>
      ),
    },
    {
      title: '店铺域名',
      dataIndex: 'shopDomain',
      key: 'shopDomain',
      width: 250,
      render: (domain: string) => domain || '-',
    },
    {
      title: '连接状态',
      key: 'connectionStatus',
      width: 120,
      render: (_, shop: Shop) => renderConnectionStatus(shop),
    },
    {
      title: 'Token 类型',
      dataIndex: 'tokenType',
      key: 'tokenType',
      width: 120,
      render: (tokenType: string) => {
        if (tokenType === 'offline') {
          return (
            <Tooltip title="永久有效">
              <Tag color="blue">Offline</Tag>
            </Tooltip>
          );
        } else if (tokenType === 'online') {
          return (
            <Tooltip title="24小时有效">
              <Tag color="orange">Online</Tag>
            </Tooltip>
          );
        }
        return '-';
      },
    },
    {
      title: '最后验证',
      key: 'lastValidatedAt',
      width: 150,
      render: (_, shop: Shop) => renderLastValidated(shop),
    },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right' as const,
      width: 200,
      render: (_, shop: Shop) => (
        <Space size="small">
          {shop.connectionStatus === 'invalid' ? (
            <Button
              type="primary"
              danger
              size="small"
              onClick={() => handleReauthorize(shop)}
            >
              重新授权
            </Button>
          ) : (
            <Button
              type="link"
              size="small"
              icon={<SyncOutlined spin={validating === shop.id} />}
              onClick={() => handleValidate(shop.id)}
              loading={validating === shop.id}
            >
              验证连接
            </Button>
          )}
          <Button
            type="link"
            size="small"
            onClick={() => window.location.href = `/shops/${shop.id}`}
          >
            详情
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className="shop-list-container">
      <div className="page-header">
        <h1>店铺管理</h1>
        <Space>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleAddShopify}
          >
            添加 Shopify 店铺
          </Button>
          <Button
            icon={<ReloadOutlined />}
            onClick={handleValidateAll}
            loading={loading}
          >
            批量验证
          </Button>
        </Space>
      </div>

      <Table
        columns={columns}
        dataSource={shops}
        loading={loading}
        rowKey="id"
        scroll={{ x: 1200 }}
        pagination={{
          current: page,
          pageSize: pageSize,
          total: total,
          onChange: (newPage, newPageSize) => {
            setPage(newPage);
            setPageSize(newPageSize || 20);
          },
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 个店铺`,
        }}
      />
    </div>
  );
};
```

---

### 4. 店铺详情组件

```tsx
// src/pages/ShopDetail.tsx

import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { shopApi } from '@/services/shopApi';
import type { Shop } from '@/types/shop';
import {
  Card, Descriptions, Button, Space, message, Tag,
  Spin, Modal, Statistic, Row, Col
} from 'antd';
import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined,
  EditOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';

export const ShopDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [shop, setShop] = useState<Shop | null>(null);
  const [loading, setLoading] = useState(false);
  const [validating, setValidating] = useState(false);

  const loadShop = async () => {
    if (!id) return;

    setLoading(true);
    try {
      const data = await shopApi.getById(Number(id));
      setShop(data);
    } catch (error) {
      message.error('加载店铺信息失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadShop();
  }, [id]);

  const handleValidate = async () => {
    if (!shop) return;

    setValidating(true);
    try {
      const result = await shopApi.validateConnection(shop.id);

      if (result.status === 'active') {
        message.success('连接验证成功');
      } else if (result.status === 'invalid') {
        message.error(`连接已失效: ${result.message}`);
      } else {
        message.warning(result.message);
      }

      await loadShop();
    } catch (error) {
      message.error('验证失败');
    } finally {
      setValidating(false);
    }
  };

  const handleDelete = () => {
    if (!shop) return;

    Modal.confirm({
      title: '确认删除',
      content: `确定要删除店铺"${shop.shopName}"吗？此操作不可恢复！`,
      okText: '确定删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await shopApi.delete(shop.id);
          message.success('店铺已删除');
          navigate('/shops');
        } catch (error: any) {
          message.error(error.response?.data?.message || '删除失败');
        }
      },
    });
  };

  if (loading || !shop) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" />
      </div>
    );
  }

  const statusIcon = shop.connectionStatus === 'active'
    ? <CheckCircleOutlined style={{ color: '#52c41a' }} />
    : <CloseCircleOutlined style={{ color: '#ff4d4f' }} />;

  return (
    <div className="shop-detail-container">
      <div className="page-header">
        <Button
          type="link"
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate('/shops')}
        >
          返回列表
        </Button>
        <h1>{shop.shopName}</h1>
      </div>

      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        {/* 统计卡片 */}
        <Row gutter={16}>
          <Col span={8}>
            <Card>
              <Statistic
                title="关联订单"
                value={shop.orderCount || 0}
                suffix="个"
              />
            </Card>
          </Col>
          <Col span={8}>
            <Card>
              <Statistic
                title="连接状态"
                value={shop.connectionStatus === 'active' ? '正常' : '失效'}
                valueStyle={{
                  color: shop.connectionStatus === 'active' ? '#3f8600' : '#cf1322'
                }}
                prefix={statusIcon}
              />
            </Card>
          </Col>
          <Col span={8}>
            <Card>
              <Statistic
                title="Token 类型"
                value={shop.tokenType === 'offline' ? '永久有效' : '24小时'}
              />
            </Card>
          </Col>
        </Row>

        {/* 基本信息 */}
        <Card title="基本信息" bordered={false}>
          <Descriptions column={2}>
            <Descriptions.Item label="店铺名称">
              {shop.shopName}
            </Descriptions.Item>
            <Descriptions.Item label="平台类型">
              <Tag color={shop.platform === 'shopify' ? 'green' : 'blue'}>
                {shop.platform}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="店铺域名">
              {shop.shopDomain || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="店铺 URL">
              <a href={shop.storeUrl} target="_blank" rel="noopener noreferrer">
                {shop.storeUrl}
              </a>
            </Descriptions.Item>
            <Descriptions.Item label="时区">
              {shop.timezone || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {dayjs(shop.createdAt).format('YYYY-MM-DD HH:mm:ss')}
            </Descriptions.Item>
          </Descriptions>
        </Card>

        {/* 连接状态 */}
        <Card
          title="连接状态"
          bordered={false}
          extra={
            <Button
              type="primary"
              icon={<SyncOutlined spin={validating} />}
              onClick={handleValidate}
              loading={validating}
            >
              立即验证
            </Button>
          }
        >
          <Descriptions column={2}>
            <Descriptions.Item label="连接状态">
              {shop.connectionStatus === 'active' ? (
                <Tag icon={<CheckCircleOutlined />} color="success">
                  连接正常
                </Tag>
              ) : (
                <Tag icon={<CloseCircleOutlined />} color="error">
                  连接失效
                </Tag>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="Token 类型">
              {shop.tokenType === 'offline' ? (
                <Tag color="blue">Offline (永久有效)</Tag>
              ) : shop.tokenType === 'online' ? (
                <Tag color="orange">Online (24小时)</Tag>
              ) : (
                '-'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="授权范围">
              {shop.oauthScope || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="Token 过期时间">
              {shop.tokenExpiresAt
                ? dayjs(shop.tokenExpiresAt).format('YYYY-MM-DD HH:mm:ss')
                : '永不过期'
              }
            </Descriptions.Item>
            <Descriptions.Item label="最后验证时间">
              {shop.lastValidatedAt
                ? dayjs(shop.lastValidatedAt).format('YYYY-MM-DD HH:mm:ss')
                : '从未验证'
              }
            </Descriptions.Item>
            <Descriptions.Item label="最后同步时间">
              {shop.lastSyncTime
                ? dayjs(shop.lastSyncTime).format('YYYY-MM-DD HH:mm:ss')
                : '-'
              }
            </Descriptions.Item>
          </Descriptions>
        </Card>

        {/* 操作按钮 */}
        <Card bordered={false}>
          <Space>
            <Button
              type="primary"
              icon={<EditOutlined />}
              onClick={() => navigate(`/shops/${shop.id}/edit`)}
            >
              编辑店铺
            </Button>
            <Button
              danger
              icon={<DeleteOutlined />}
              onClick={handleDelete}
            >
              删除店铺
            </Button>
          </Space>
        </Card>
      </Space>
    </div>
  );
};
```

---

### 5. 添加样式

```css
/* src/pages/ShopList.css */

.shop-list-container {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

/* src/pages/ShopDetail.css */

.shop-detail-container {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}
```

---

## 🔄 OAuth 回调处理

### 前端路由配置

```tsx
// src/App.tsx or routes config

import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { message } from 'antd';

// OAuth 回调页面
export const OAuthCallback: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    const status = searchParams.get('oauth');

    if (status === 'success') {
      message.success('店铺授权成功！');
      // 延迟跳转，让用户看到成功消息
      setTimeout(() => {
        navigate('/shops');
      }, 1500);
    } else if (status === 'error') {
      const reason = searchParams.get('reason');
      message.error(`授权失败: ${reason || '未知错误'}`);
      setTimeout(() => {
        navigate('/shops');
      }, 2000);
    }
  }, [searchParams, navigate]);

  return (
    <div style={{ textAlign: 'center', padding: '100px 0' }}>
      <Spin size="large" />
      <p style={{ marginTop: 16 }}>正在处理授权结果...</p>
    </div>
  );
};

// 路由配置
const routes = [
  {
    path: '/shops',
    element: <ShopList />,
  },
  {
    path: '/shops/:id',
    element: <ShopDetail />,
  },
  {
    path: '/shops/oauth-callback',  // 前端回调路由
    element: <OAuthCallback />,
  },
];
```

---

## 📱 移动端适配建议

```tsx
// 使用 Ant Design Mobile 或响应式设计

import { List, SwipeAction, Toast } from 'antd-mobile';

export const ShopListMobile: React.FC = () => {
  const [shops, setShops] = useState<Shop[]>([]);

  return (
    <List>
      {shops.map(shop => (
        <SwipeAction
          key={shop.id}
          rightActions={[
            {
              key: 'validate',
              text: '验证',
              color: 'primary',
              onClick: () => handleValidate(shop.id),
            },
            {
              key: 'delete',
              text: '删除',
              color: 'danger',
              onClick: () => handleDelete(shop.id),
            },
          ]}
        >
          <List.Item
            onClick={() => navigate(`/shops/${shop.id}`)}
            description={
              <>
                <div>{shop.shopDomain}</div>
                <div>
                  {shop.connectionStatus === 'active' ? (
                    <span style={{ color: '#00b578' }}>● 连接正常</span>
                  ) : (
                    <span style={{ color: '#ff3141' }}>● 连接失效</span>
                  )}
                </div>
              </>
            }
          >
            {shop.shopName}
          </List.Item>
        </SwipeAction>
      ))}
    </List>
  );
};
```

---

## 🎯 关键功能实现要点

### 1. 实时状态更新

```typescript
// 使用轮询或 WebSocket 实时更新状态
useEffect(() => {
  const interval = setInterval(() => {
    // 每 5 分钟自动刷新列表
    loadShops();
  }, 5 * 60 * 1000);

  return () => clearInterval(interval);
}, []);
```

### 2. 错误处理

```typescript
// 统一错误处理
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      message.error('登录已过期，请重新登录');
      // 跳转到登录页
    } else if (error.response?.status === 403) {
      message.error('没有权限');
    } else {
      message.error(error.response?.data?.message || '请求失败');
    }
    return Promise.reject(error);
  }
);
```

### 3. 加载状态优化

```typescript
// 使用 React Query 优化数据加载
import { useQuery, useMutation, useQueryClient } from 'react-query';

export const useShops = (page: number, pageSize: number) => {
  return useQuery(
    ['shops', page, pageSize],
    () => shopApi.getList({ page, pageSize }),
    {
      staleTime: 5 * 60 * 1000, // 5分钟内不重新请求
      cacheTime: 10 * 60 * 1000,
    }
  );
};

export const useValidateShop = () => {
  const queryClient = useQueryClient();

  return useMutation(
    (shopId: number) => shopApi.validateConnection(shopId),
    {
      onSuccess: () => {
        // 验证成功后自动刷新列表
        queryClient.invalidateQueries('shops');
      },
    }
  );
};
```

---

## 📝 集成检查清单

- [ ] API Service 已创建（shopApi.ts）
- [ ] 类型定义已添加（shop.ts）
- [ ] 店铺列表页面已实现
- [ ] 店铺详情页面已实现
- [ ] OAuth 回调页面已实现
- [ ] 验证连接功能已测试
- [ ] 批量验证功能已测试
- [ ] 添加店铺功能已测试
- [ ] 重新授权流程已测试
- [ ] 错误处理已完善
- [ ] 移动端适配已完成
- [ ] 样式已优化

---

## 🚀 下一步优化建议

1. **WebSocket 实时通知**
   - 当 token 失效时实时通知用户
   - 批量验证进度实时显示

2. **定时健康检查**
   - 前端定时触发验证（可选）
   - 后台定时任务 + 前端轮询结果

3. **数据可视化**
   - 店铺连接状态统计图表
   - 历史验证记录趋势

4. **批量操作增强**
   - 批量重新授权
   - 批量删除店铺
   - 导出店铺列表

---

希望这个完整的前端集成指南对你有帮助！所有代码都是即用型的，可以直接集成到你的前端项目中。🎉
