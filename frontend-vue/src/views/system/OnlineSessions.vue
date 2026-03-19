<template>
  <div class="page-content">
    <!-- 统计卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px;">
      <a-col :xs="12" :sm="6">
        <a-card size="small">
          <a-statistic title="在线用户" :value="stats.userCount" :value-style="{ color: '#1890ff' }">
            <template #prefix><TeamOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="6">
        <a-card size="small">
          <a-statistic title="在线设备" :value="stats.sessionCount" :value-style="{ color: '#52c41a' }">
            <template #prefix><LaptopOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <a-card title="在线用户" :bordered="false">
      <template #extra>
        <a-space>
          <a-input-search
            v-model:value="searchUsername"
            placeholder="搜索用户名"
            allow-clear
            style="width: 180px"
          />
          <a-button @click="fetchData">
            <ReloadOutlined />
            刷新
          </a-button>
        </a-space>
      </template>

      <!-- 表格 -->
      <a-table
        v-if="!isMobile"
        :columns="columns"
        :data-source="filteredSessions"
        :loading="loading"
        row-key="sessionId"
        :pagination="false"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'loginSource'">
            <a-tag v-if="record.loginSource === 'PASSWORD'" color="blue">密码</a-tag>
            <a-tag v-else-if="record.loginSource === 'DINGTALK'" color="cyan">钉钉</a-tag>
            <a-tag v-else>{{ record.loginSource }}</a-tag>
          </template>
          <template v-if="column.key === 'deviceInfo'">
            <span>{{ [record.device, record.browser, record.os].filter(Boolean).join(' / ') || '-' }}</span>
          </template>
          <template v-if="column.key === 'loginTime'">
            {{ formatTime(record.loginTime) }}
          </template>
          <template v-if="column.key === 'action'">
            <a-popconfirm
              title="确定强制下线该设备？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="handleForceLogout(record)"
            >
              <a-button type="link" size="small" danger>强制下线</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>

      <!-- 移动端列表 -->
      <div v-else class="mobile-list">
        <a-spin :spinning="loading">
          <a-empty v-if="filteredSessions.length === 0" description="暂无在线用户" />
          <div v-else>
            <a-card
              v-for="item in filteredSessions"
              :key="item.sessionId"
              size="small"
              class="mobile-card"
            >
              <div class="mobile-card-header">
                <span class="mobile-card-title">{{ item.username }}</span>
                <a-tag v-if="item.loginSource === 'PASSWORD'" color="blue">密码</a-tag>
                <a-tag v-else-if="item.loginSource === 'DINGTALK'" color="cyan">钉钉</a-tag>
              </div>
              <div class="mobile-card-body">
                <div>IP：{{ item.ipAddress || '-' }}</div>
                <div>设备：{{ [item.device, item.browser, item.os].filter(Boolean).join(' / ') || '-' }}</div>
                <div>登录时间：{{ formatTime(item.loginTime) }}</div>
              </div>
              <div class="mobile-card-actions">
                <a-popconfirm
                  title="确定强制下线该设备？"
                  ok-text="确定"
                  cancel-text="取消"
                  @confirm="handleForceLogout(item)"
                >
                  <a-button type="link" size="small" danger>强制下线</a-button>
                </a-popconfirm>
              </div>
            </a-card>
          </div>
        </a-spin>
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Grid, message } from 'ant-design-vue'
import { TeamOutlined, LaptopOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { onlineSessionApi } from '@/api/online-session'
import dayjs from 'dayjs'

const { useBreakpoint } = Grid
const screens = useBreakpoint()
const isMobile = ref(false)

const checkMobile = () => {
  isMobile.value = !screens.value.md
}
onMounted(() => { checkMobile() })

const loading = ref(false)
const sessions = ref([])
const searchUsername = ref('')
const stats = ref({ userCount: 0, sessionCount: 0 })
let refreshTimer = null

const filteredSessions = computed(() => {
  if (!searchUsername.value) return sessions.value
  const keyword = searchUsername.value.toLowerCase()
  return sessions.value.filter(s => s.username?.toLowerCase().includes(keyword))
})

const columns = [
  { title: '用户', dataIndex: 'username', width: 100 },
  { title: 'IP地址', dataIndex: 'ipAddress', width: 130 },
  { title: '设备 / 浏览器 / 系统', key: 'deviceInfo', width: 220 },
  { title: '登录方式', key: 'loginSource', dataIndex: 'loginSource', width: 90 },
  { title: '登录时间', key: 'loginTime', dataIndex: 'loginTime', width: 170 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const [sessionList, countData] = await Promise.all([
      onlineSessionApi.getList(),
      onlineSessionApi.getCount()
    ])
    sessions.value = sessionList || []
    stats.value = countData || { userCount: 0, sessionCount: 0 }
  } catch (e) {
    console.error('Failed to fetch online sessions', e)
  } finally {
    loading.value = false
  }
}

const handleForceLogout = async (record) => {
  try {
    await onlineSessionApi.forceLogout(record.sessionId)
    message.success(`已强制下线用户 ${record.username}`)
    fetchData()
  } catch (e) {
    message.error('操作失败: ' + e.message)
  }
}

const formatTime = (time) => {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-'
}

onMounted(() => {
  fetchData()
  // 30秒自动刷新
  refreshTimer = setInterval(fetchData, 30000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.table-operations {
  margin-bottom: 16px;
}
.mobile-list {
  margin-top: 12px;
}
.mobile-card {
  margin-bottom: 8px;
}
.mobile-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.mobile-card-title {
  font-weight: 500;
}
.mobile-card-body {
  font-size: 13px;
  color: #666;
  line-height: 1.8;
}
.mobile-card-actions {
  margin-top: 8px;
  text-align: right;
}
</style>
