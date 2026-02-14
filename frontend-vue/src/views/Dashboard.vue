<template>
  <div class="dashboard-container">
    <!-- 顶部统计卡片 -->
    <a-row :gutter="16" class="stats-row">
      <a-col :xs="24" :sm="12" :md="6">
        <div class="stat-card" @click="goToPage('/shops')">
          <div class="stat-icon icon-shops">
            <ShopOutlined />
          </div>
          <div class="stat-content">
            <div class="stat-value">
              <a-spin v-if="loading" size="small" />
              <span v-else>{{ stats.activeShops || 0 }}<small>/{{ stats.totalShops || 0 }}</small></span>
            </div>
            <div class="stat-label">活跃店铺 / 总计</div>
          </div>
        </div>
      </a-col>

      <a-col :xs="24" :sm="12" :md="6">
        <div class="stat-card" @click="goToPage('/orders')">
          <div class="stat-icon icon-orders">
            <ShoppingCartOutlined />
          </div>
          <div class="stat-content">
            <div class="stat-value">
              <a-spin v-if="loading" size="small" />
              <span v-else>{{ formatNumber(stats.totalOrders) }}</span>
            </div>
            <div class="stat-label">总订单数</div>
          </div>
          <div class="stat-footer">
            <span class="stat-today">
              <RiseOutlined class="trend-up" /> 今日 +{{ stats.todayOrders || 0 }}
            </span>
          </div>
        </div>
      </a-col>

      <a-col :xs="24" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-icon icon-revenue">
            <DollarCircleOutlined />
          </div>
          <div class="stat-content">
            <div class="stat-value">
              <a-spin v-if="loading" size="small" />
              <span v-else>{{ formatCurrency(stats.totalRevenue) }}</span>
            </div>
            <div class="stat-label">总销售额</div>
          </div>
          <div class="stat-footer">
            <span class="stat-today">
              <RiseOutlined class="trend-up" /> 今日 {{ formatCurrency(stats.todayRevenue) }}
            </span>
          </div>
        </div>
      </a-col>

      <a-col :xs="24" :sm="12" :md="6">
        <div class="stat-card stat-card-alert" @click="goToPage('/system/alert-settings')">
          <div class="stat-icon icon-alerts">
            <BellOutlined />
          </div>
          <div class="stat-content">
            <div class="stat-value">
              <a-spin v-if="loading" size="small" />
              <span v-else>{{ stats.pendingAlerts || 0 }}</span>
            </div>
            <div class="stat-label">待处理告警</div>
          </div>
          <div class="stat-footer">
            <span class="stat-today">最近20条通知</span>
            <RightOutlined class="stat-arrow" />
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 店铺概览 + 告警面板 -->
    <a-row :gutter="16" class="main-row">
      <!-- 店铺列表 -->
      <a-col :xs="24" :lg="14" style="margin-bottom: 16px">
        <div class="content-card">
          <div class="card-header">
            <h3><ShopOutlined /> 店铺概览</h3>
            <a-button type="link" size="small" @click="goToPage('/shops')">
              查看全部 <RightOutlined />
            </a-button>
          </div>
          <div class="shop-list" v-if="stats.shops && stats.shops.length > 0">
            <div
              class="shop-item"
              v-for="shop in stats.shops"
              :key="shop.id"
            >
              <div class="shop-header">
                <div class="shop-name-row">
                  <span class="shop-name">{{ shop.shopName || shop.shopDomain || '-' }}</span>
                  <a-tag :color="shop.isActive ? 'green' : 'default'" size="small">
                    {{ shop.isActive ? '活跃' : '未激活' }}
                  </a-tag>
                  <a-tag v-if="shop.connectionStatus" :color="getConnectionColor(shop.connectionStatus)" size="small">
                    {{ shop.connectionStatus }}
                  </a-tag>
                </div>
                <div class="shop-meta">
                  <span v-if="shop.platform" class="meta-item">
                    <GlobalOutlined /> {{ shop.platform }}
                  </span>
                  <span v-if="shop.planDisplayName" class="meta-item">
                    <CrownOutlined /> {{ shop.planDisplayName }}
                  </span>
                  <span v-if="shop.shopDomain" class="meta-item domain-text">
                    {{ shop.shopDomain }}
                  </span>
                </div>
              </div>

              <div class="shop-stats">
                <div class="shop-stat">
                  <div class="shop-stat-value">{{ formatNumber(shop.orderCount) }}</div>
                  <div class="shop-stat-label">订单</div>
                </div>
                <div class="shop-stat-divider"></div>
                <div class="shop-stat">
                  <div class="shop-stat-value">{{ formatCurrency(shop.revenue, shop.currency) }}</div>
                  <div class="shop-stat-label">销售额</div>
                </div>
                <div class="shop-stat-divider"></div>
                <div class="shop-stat">
                  <div class="shop-stat-value sync-time">{{ shop.lastSyncTime || '未同步' }}</div>
                  <div class="shop-stat-label">最近同步</div>
                </div>
              </div>
            </div>
          </div>
          <a-empty v-else description="暂无店铺数据" />
        </div>
      </a-col>

      <!-- 告警列表 -->
      <a-col :xs="24" :lg="10" style="margin-bottom: 16px">
        <div class="content-card alert-card">
          <div class="card-header">
            <h3><BellOutlined /> 最近告警</h3>
            <a-tag v-if="stats.pendingAlerts > 0" color="red">
              {{ stats.pendingAlerts }} 待处理
            </a-tag>
          </div>
          <div class="alert-list" v-if="stats.recentAlerts && stats.recentAlerts.length > 0">
            <div
              class="alert-item"
              v-for="alert in stats.recentAlerts"
              :key="alert.id"
              :class="'alert-severity-' + (alert.severity || 'INFO').toLowerCase()"
            >
              <div class="alert-indicator"></div>
              <div class="alert-body">
                <div class="alert-title-row">
                  <span class="alert-title">{{ alert.title }}</span>
                  <a-tag :color="getAlertTypeColor(alert.alertType)" size="small">
                    {{ getAlertTypeLabel(alert.alertType) }}
                  </a-tag>
                </div>
                <div class="alert-meta">
                  <span v-if="alert.shopName" class="meta-item">
                    <ShopOutlined /> {{ alert.shopName }}
                  </span>
                  <span class="meta-item">
                    <ClockCircleOutlined /> {{ alert.createdTime }}
                  </span>
                </div>
                <div class="alert-status">
                  <a-tag :color="alert.sendStatus === 'SENT' ? 'green' : alert.sendStatus === 'FAILED' ? 'red' : 'default'" size="small">
                    {{ getStatusLabel(alert.sendStatus) }}
                  </a-tag>
                </div>
              </div>
            </div>
          </div>
          <a-empty v-else description="暂无告警记录，系统运行正常 🎉" />
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  ShopOutlined,
  ShoppingCartOutlined,
  DollarCircleOutlined,
  BellOutlined,
  RightOutlined,
  RiseOutlined,
  GlobalOutlined,
  CrownOutlined,
  ClockCircleOutlined
} from '@ant-design/icons-vue'
import { statsApi } from '@/api/stats'

const router = useRouter()
const loading = ref(true)
const stats = ref({})
let refreshTimer = null

// 获取统计数据
const fetchStats = async () => {
  loading.value = true
  try {
    stats.value = await statsApi.getDashboardStats()
  } catch (error) {
    console.error('获取统计数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 格式化数字
const formatNumber = (num) => {
  if (!num) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
  return num.toLocaleString()
}

// 格式化金额
const formatCurrency = (amount, currency = 'USD') => {
  if (!amount || amount === 0) return '$0.00'
  const num = parseFloat(amount)
  if (num >= 10000) {
    return '$' + (num / 1000).toFixed(1) + 'K'
  }
  return '$' + num.toFixed(2)
}

// 连接状态颜色
const getConnectionColor = (status) => {
  const map = { CONNECTED: 'green', PENDING: 'orange', DISCONNECTED: 'red' }
  return map[status] || 'default'
}

// 告警类型标签
const alertTypeMap = {
  DISPUTE: { label: '支付争议', color: 'red' },
  INFRINGEMENT: { label: '侵权', color: 'volcano' },
  ACCOUNT_RESTRICTED: { label: '账号受限', color: 'red' },
  PAYMENT_HOLD: { label: '资金冻结', color: 'orange' },
  APP_UNINSTALLED: { label: '应用卸载', color: 'gold' },
  SHOP_ALERT: { label: '店铺提示', color: 'blue' },
  DAILY_SUMMARY: { label: '日报', color: 'cyan' },
  MONTHLY_SUMMARY: { label: '月报', color: 'geekblue' },
  QUARTERLY_SUMMARY: { label: '季报', color: 'purple' },
  YEARLY_SUMMARY: { label: '年报', color: 'purple' }
}

const getAlertTypeLabel = (type) => alertTypeMap[type]?.label || type
const getAlertTypeColor = (type) => alertTypeMap[type]?.color || 'default'

const getStatusLabel = (status) => {
  const map = { SENT: '已发送', FAILED: '失败', PENDING: '待发送' }
  return map[status] || status
}

// 页面跳转
const goToPage = (path) => {
  router.push(path)
}

onMounted(() => {
  fetchStats()
  // 每5分钟自动刷新
  refreshTimer = setInterval(fetchStats, 5 * 60 * 1000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.dashboard-container {
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 64px);
}

/* ===== 统计卡片 ===== */
.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 22px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.04);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: white;
  margin-bottom: 14px;
}

.icon-shops { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.icon-orders { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }
.icon-revenue { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.icon-alerts { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }

.stat-content {
  flex: 1;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.stat-value small {
  font-size: 16px;
  font-weight: 400;
  color: #8c8c8c;
}

.stat-label {
  font-size: 13px;
  color: #8c8c8c;
  margin-top: 4px;
}

.stat-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px solid #f5f5f5;
}

.stat-today {
  font-size: 12px;
  color: #8c8c8c;
}

.trend-up {
  color: #52c41a;
  margin-right: 2px;
}

.stat-arrow {
  color: #bfbfbf;
  font-size: 11px;
}

.stat-card-alert .stat-value {
  color: #fa541c;
}

/* ===== 内容卡片 ===== */
.content-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.04);
  height: 100%;
  max-height: 600px;
  overflow-y: auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  position: sticky;
  top: -24px;
  background: white;
  padding: 0 0 12px;
  z-index: 1;
  border-bottom: 1px solid #f5f5f5;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ===== 店铺列表 ===== */
.shop-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.shop-item {
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 16px;
  transition: all 0.2s;
}

.shop-item:hover {
  border-color: #d9d9d9;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.shop-header {
  margin-bottom: 14px;
}

.shop-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.shop-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
}

.shop-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-item {
  font-size: 12px;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  gap: 4px;
}

.domain-text {
  color: #1890ff;
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
  font-size: 11px;
}

.shop-stats {
  display: flex;
  align-items: center;
  gap: 0;
  background: #fafbfc;
  border-radius: 8px;
  padding: 12px 0;
}

.shop-stat {
  flex: 1;
  text-align: center;
}

.shop-stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  font-variant-numeric: tabular-nums;
}

.shop-stat-value.sync-time {
  font-size: 11px;
  font-weight: 400;
  color: #8c8c8c;
}

.shop-stat-label {
  font-size: 11px;
  color: #8c8c8c;
  margin-top: 2px;
}

.shop-stat-divider {
  width: 1px;
  height: 30px;
  background: #e8e8e8;
}

/* ===== 告警列表 ===== */
.alert-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alert-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  transition: all 0.2s;
}

.alert-item:hover {
  background: #fafafa;
}

.alert-indicator {
  width: 4px;
  min-height: 100%;
  border-radius: 2px;
  flex-shrink: 0;
}

.alert-severity-critical .alert-indicator { background: #ff4d4f; }
.alert-severity-high .alert-indicator { background: #fa8c16; }
.alert-severity-medium .alert-indicator { background: #1890ff; }
.alert-severity-info .alert-indicator { background: #52c41a; }

.alert-body {
  flex: 1;
  min-width: 0;
}

.alert-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 6px;
}

.alert-title {
  font-size: 13px;
  font-weight: 500;
  color: #262626;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.alert-meta {
  display: flex;
  gap: 12px;
  margin-bottom: 4px;
}

.alert-meta .meta-item {
  font-size: 11px;
}

.alert-status {
  margin-top: 4px;
}

/* 响应式 */
@media (max-width: 576px) {
  .dashboard-container {
    padding: 12px;
  }

  .stat-card {
    margin-bottom: 12px;
  }

  .shop-stats {
    flex-wrap: wrap;
    gap: 8px;
  }

  .shop-stat-divider {
    display: none;
  }
}
</style>
