<template>
  <div class="page-container">
    <!-- 统计卡片区域 -->
    <a-row :gutter="16" class="stats-row">
      <a-col :span="6">
        <div class="stat-card" @click="goToPage('/tracking')">
          <div class="stat-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%)">
            <FileTextOutlined />
          </div>
          <div class="stat-content">
            <div class="stat-value">
              <a-spin v-if="loading" size="small" />
              <span v-else>{{ stats.totalTrackings || 0 }}</span>
            </div>
            <div class="stat-label">运单总数</div>
          </div>
          <div class="stat-footer">
            <span class="stat-today">今日新增 {{ stats.todayTrackings || 0 }}</span>
            <RightOutlined class="stat-arrow" />
          </div>
        </div>
      </a-col>

      <a-col :span="6">
        <div class="stat-card" @click="goToPage('/shops')">
          <div class="stat-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%)">
            <ShopOutlined />
          </div>
          <div class="stat-content">
            <div class="stat-value">
              <a-spin v-if="loading" size="small" />
              <span v-else>{{ stats.totalShops || 0 }}</span>
            </div>
            <div class="stat-label">店铺总数</div>
          </div>
          <div class="stat-footer">
            <span class="stat-today">已授权店铺</span>
            <RightOutlined class="stat-arrow" />
          </div>
        </div>
      </a-col>

      <a-col :span="6">
        <div class="stat-card" @click="goToPage('/users')">
          <div class="stat-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%)">
            <TeamOutlined />
          </div>
          <div class="stat-content">
            <div class="stat-value">
              <a-spin v-if="loading" size="small" />
              <span v-else>{{ stats.totalUsers || 0 }}</span>
            </div>
            <div class="stat-label">用户总数</div>
          </div>
          <div class="stat-footer">
            <span class="stat-today">系统用户</span>
            <RightOutlined class="stat-arrow" />
          </div>
        </div>
      </a-col>

      <a-col :span="6">
        <div class="stat-card stat-card-exception" @click="goToTracking('Exception')">
          <div class="stat-icon" style="background: linear-gradient(135deg, #ff4d4f 0%, #cf1322 100%)">
            <WarningOutlined />
          </div>
          <div class="stat-content">
            <div class="stat-value">
              <a-spin v-if="loading" size="small" />
              <span v-else>{{ stats.statusStats?.exception || 0 }}</span>
            </div>
            <div class="stat-label">异常运单</div>
          </div>
          <div class="stat-footer">
            <span class="stat-today">需要关注</span>
            <RightOutlined class="stat-arrow" />
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 详细统计区域 -->
    <a-row :gutter="16" class="detail-row">
      <!-- 运单状态分布 -->
      <a-col :span="12">
        <div class="content-card">
          <div class="card-header">
            <h3>运单状态分布</h3>
          </div>
          <div class="status-list">
            <div class="status-item" @click="goToTracking('InfoReceived')">
              <div class="status-info">
                <a-tag color="default">信息收录</a-tag>
                <span class="status-count">{{ stats.statusStats?.infoReceived || 0 }}</span>
              </div>
              <a-progress
                :percent="getStatusPercent('infoReceived')"
                :show-info="false"
                stroke-color="#d9d9d9"
              />
            </div>
            <div class="status-item" @click="goToTracking('InTransit')">
              <div class="status-info">
                <a-tag color="processing">运输中</a-tag>
                <span class="status-count">{{ stats.statusStats?.inTransit || 0 }}</span>
              </div>
              <a-progress
                :percent="getStatusPercent('inTransit')"
                :show-info="false"
                stroke-color="#1890ff"
              />
            </div>
            <div class="status-item" @click="goToTracking('Delivered')">
              <div class="status-info">
                <a-tag color="success">已送达</a-tag>
                <span class="status-count">{{ stats.statusStats?.delivered || 0 }}</span>
              </div>
              <a-progress
                :percent="getStatusPercent('delivered')"
                :show-info="false"
                stroke-color="#52c41a"
              />
            </div>
            <div class="status-item" @click="goToTracking('Exception')">
              <div class="status-info">
                <a-tag color="error">异常</a-tag>
                <span class="status-count">{{ stats.statusStats?.exception || 0 }}</span>
              </div>
              <a-progress
                :percent="getStatusPercent('exception')"
                :show-info="false"
                stroke-color="#ff4d4f"
              />
            </div>
          </div>
        </div>
      </a-col>

      <!-- 承运商排行 -->
      <a-col :span="12">
        <div class="content-card">
          <div class="card-header">
            <h3>承运商排行 (Top 10)</h3>
          </div>
          <div class="carrier-list" v-if="stats.carrierStats && stats.carrierStats.length > 0">
            <div
              class="carrier-item"
              v-for="(carrier, index) in stats.carrierStats"
              :key="carrier.carrierCode"
              @click="goToCarrier(carrier.carrierCode)"
            >
              <div class="carrier-rank" :class="'rank-' + (index + 1)">
                {{ index + 1 }}
              </div>
              <div class="carrier-info">
                <span class="carrier-name">{{ carrier.carrierCode?.toUpperCase() || '-' }}</span>
                <span class="carrier-count">{{ carrier.count }} 单</span>
              </div>
              <a-progress
                :percent="getCarrierPercent(carrier.count)"
                :show-info="false"
                size="small"
                stroke-color="#1890ff"
              />
            </div>
          </div>
          <a-empty v-else description="暂无数据" />
        </div>
      </a-col>
    </a-row>

    <!-- 趋势图表 -->
    <div class="content-card trend-card">
      <div class="card-header">
        <h3>近7天运单趋势</h3>
      </div>
      <div class="trend-chart">
        <div class="chart-bars" v-if="stats.dailyStats && stats.dailyStats.length > 0">
          <div
            class="chart-bar-item"
            v-for="day in stats.dailyStats"
            :key="day.date"
          >
            <div class="bar-value">{{ day.count }}</div>
            <div
              class="bar"
              :style="{ height: getBarHeight(day.count) + 'px' }"
            ></div>
            <div class="bar-label">{{ formatDate(day.date) }}</div>
          </div>
        </div>
        <a-empty v-else description="暂无数据" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  FileTextOutlined,
  ShopOutlined,
  TeamOutlined,
  WarningOutlined,
  RightOutlined
} from '@ant-design/icons-vue'
import { statsApi } from '@/api/stats'

const router = useRouter()
const loading = ref(true)
const stats = ref({})

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

// 计算状态百分比
const totalTrackings = computed(() => stats.value.totalTrackings || 1)

const getStatusPercent = (status) => {
  const statusStats = stats.value.statusStats
  if (!statusStats) return 0
  const count = statusStats[status] || 0
  return Math.round((count / totalTrackings.value) * 100)
}

// 计算承运商百分比
const maxCarrierCount = computed(() => {
  if (!stats.value.carrierStats || stats.value.carrierStats.length === 0) return 1
  return stats.value.carrierStats[0].count || 1
})

const getCarrierPercent = (count) => {
  return Math.round((count / maxCarrierCount.value) * 100)
}

// 计算柱状图高度
const maxDailyCount = computed(() => {
  if (!stats.value.dailyStats || stats.value.dailyStats.length === 0) return 1
  return Math.max(...stats.value.dailyStats.map(d => d.count), 1)
})

const getBarHeight = (count) => {
  const maxHeight = 120
  return Math.max((count / maxDailyCount.value) * maxHeight, 4)
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 页面跳转
const goToPage = (path) => {
  router.push(path)
}

const goToTracking = (status) => {
  router.push({ path: '/tracking', query: { status } })
}

const goToCarrier = (carrierCode) => {
  router.push({ path: '/tracking', query: { carrierCode } })
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.page-container {
  padding: 24px;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  border: 1px solid #f0f0f0;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  margin-bottom: 16px;
}

.stat-content {
  margin-bottom: 12px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #262626;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #8c8c8c;
  margin-top: 4px;
}

.stat-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.stat-today {
  font-size: 13px;
  color: #8c8c8c;
}

.stat-arrow {
  color: #bfbfbf;
  font-size: 12px;
}

.detail-row {
  margin-bottom: 16px;
}

.content-card {
  background: white;
  border-radius: 8px;
  padding: 24px;
  height: 100%;
}

.card-header {
  margin-bottom: 20px;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-item {
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: background 0.2s;
}

.status-item:hover {
  background: #fafafa;
}

.status-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.status-count {
  font-size: 16px;
  font-weight: 500;
  color: #262626;
}

.carrier-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.carrier-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}

.carrier-item:hover {
  background: #fafafa;
}

.carrier-rank {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: #8c8c8c;
}

.carrier-rank.rank-1 {
  background: linear-gradient(135deg, #ffd700 0%, #ffb800 100%);
  color: white;
}

.carrier-rank.rank-2 {
  background: linear-gradient(135deg, #c0c0c0 0%, #a8a8a8 100%);
  color: white;
}

.carrier-rank.rank-3 {
  background: linear-gradient(135deg, #cd7f32 0%, #b87333 100%);
  color: white;
}

.carrier-info {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-width: 0;
}

.carrier-name {
  font-weight: 500;
  color: #262626;
}

.carrier-count {
  font-size: 13px;
  color: #8c8c8c;
  margin-left: 8px;
}

.carrier-item .ant-progress {
  width: 80px;
  flex-shrink: 0;
}

.trend-card {
  margin-top: 16px;
}

.trend-chart {
  padding: 20px 0;
}

.chart-bars {
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  height: 180px;
  padding: 0 20px;
}

.chart-bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.bar-value {
  font-size: 13px;
  font-weight: 500;
  color: #262626;
}

.bar {
  width: 40px;
  background: linear-gradient(180deg, #1890ff 0%, #096dd9 100%);
  border-radius: 4px 4px 0 0;
  transition: height 0.3s;
}

.bar-label {
  font-size: 12px;
  color: #8c8c8c;
}
</style>
