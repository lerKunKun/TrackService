<template>
  <div class="page-content">
    <a-card title="登录日志" :bordered="false">
      <!-- 筛选栏 -->
      <div class="table-operations">
        <a-space wrap>
          <a-input-search
            v-model:value="filters.username"
            placeholder="搜索用户名"
            allow-clear
            style="width: 180px"
            @search="handleSearch"
          />
          <a-select
            v-model:value="filters.loginType"
            placeholder="登录方式"
            allow-clear
            style="width: 120px"
            @change="handleSearch"
          >
            <a-select-option value="PASSWORD">密码登录</a-select-option>
            <a-select-option value="DINGTALK">钉钉登录</a-select-option>
            <a-select-option value="LOGOUT">登出</a-select-option>
          </a-select>
          <a-select
            v-model:value="filters.loginResult"
            placeholder="结果"
            allow-clear
            style="width: 100px"
            @change="handleSearch"
          >
            <a-select-option value="SUCCESS">成功</a-select-option>
            <a-select-option value="FAILURE">失败</a-select-option>
          </a-select>
          <a-range-picker
            v-model:value="filters.dateRange"
            show-time
            format="YYYY-MM-DD HH:mm:ss"
            :placeholder="['开始时间', '结束时间']"
            @change="handleSearch"
          />
        </a-space>
      </div>

      <!-- 表格 -->
      <a-table
        v-if="!isMobile"
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'loginType'">
            <a-tag v-if="record.loginType === 'PASSWORD'" color="blue">密码</a-tag>
            <a-tag v-else-if="record.loginType === 'DINGTALK'" color="cyan">钉钉</a-tag>
            <a-tag v-else-if="record.loginType === 'LOGOUT'" color="default">登出</a-tag>
            <a-tag v-else>{{ record.loginType }}</a-tag>
          </template>
          <template v-if="column.key === 'loginResult'">
            <a-tag :color="record.loginResult === 'SUCCESS' ? 'green' : 'red'">
              {{ record.loginResult === 'SUCCESS' ? '成功' : '失败' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'deviceInfo'">
            <span>{{ [record.device, record.browser, record.os].filter(Boolean).join(' / ') || '-' }}</span>
          </template>
          <template v-if="column.key === 'createdAt'">
            {{ formatTime(record.createdAt) }}
          </template>
          <template v-if="column.key === 'errorMsg'">
            <a-tooltip v-if="record.errorMsg" :title="record.errorMsg">
              <span style="color: #f5222d; cursor: pointer;">{{ record.errorMsg.substring(0, 20) }}{{ record.errorMsg.length > 20 ? '...' : '' }}</span>
            </a-tooltip>
            <span v-else>-</span>
          </template>
        </template>
      </a-table>

      <!-- 移动端列表 -->
      <div v-else class="mobile-list">
        <a-spin :spinning="loading">
          <a-empty v-if="tableData.length === 0" />
          <div v-else>
            <a-card
              v-for="item in tableData"
              :key="item.id"
              size="small"
              class="mobile-card"
            >
              <div class="mobile-card-header">
                <span class="mobile-card-title">{{ item.username || '-' }}</span>
                <a-space>
                  <a-tag v-if="item.loginType === 'PASSWORD'" color="blue">密码</a-tag>
                  <a-tag v-else-if="item.loginType === 'DINGTALK'" color="cyan">钉钉</a-tag>
                  <a-tag v-else-if="item.loginType === 'LOGOUT'" color="default">登出</a-tag>
                  <a-tag :color="item.loginResult === 'SUCCESS' ? 'green' : 'red'">
                    {{ item.loginResult === 'SUCCESS' ? '成功' : '失败' }}
                  </a-tag>
                </a-space>
              </div>
              <div class="mobile-card-body">
                <div>IP：{{ item.ipAddress || '-' }}</div>
                <div>设备：{{ [item.device, item.browser, item.os].filter(Boolean).join(' / ') || '-' }}</div>
                <div>时间：{{ formatTime(item.createdAt) }}</div>
                <div v-if="item.errorMsg" style="color: #f5222d;">原因：{{ item.errorMsg }}</div>
              </div>
            </a-card>
          </div>
        </a-spin>
        <div class="mobile-pagination">
          <a-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            simple
            @change="(page) => handleTableChange({ current: page, pageSize: pagination.pageSize })"
          />
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Grid } from 'ant-design-vue'
import { loginLogApi } from '@/api/login-log'
import dayjs from 'dayjs'

const { useBreakpoint } = Grid
const screens = useBreakpoint()
const isMobile = ref(false)

const checkMobile = () => {
  isMobile.value = !screens.value.md
}
onMounted(() => { checkMobile() })

const loading = ref(false)
const tableData = ref([])

const filters = reactive({
  username: '',
  loginType: null,
  loginResult: null,
  dateRange: null
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

const columns = [
  { title: '时间', key: 'createdAt', dataIndex: 'createdAt', width: 170 },
  { title: '用户', dataIndex: 'username', width: 100 },
  { title: '登录方式', key: 'loginType', dataIndex: 'loginType', width: 90 },
  { title: '结果', key: 'loginResult', dataIndex: 'loginResult', width: 80 },
  { title: 'IP地址', dataIndex: 'ipAddress', width: 130 },
  { title: '设备 / 浏览器 / 系统', key: 'deviceInfo', width: 220 },
  { title: '错误信息', key: 'errorMsg', dataIndex: 'errorMsg', width: 160 }
]

const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.current,
      pageSize: pagination.pageSize,
      username: filters.username || undefined,
      loginType: filters.loginType || undefined,
      loginResult: filters.loginResult || undefined,
      startTime: filters.dateRange?.[0]?.format('YYYY-MM-DD HH:mm:ss') || undefined,
      endTime: filters.dateRange?.[1]?.format('YYYY-MM-DD HH:mm:ss') || undefined
    }
    const data = await loginLogApi.getList(params)
    tableData.value = data.list || []
    pagination.total = data.total || 0
  } catch (e) {
    console.error('Failed to fetch login logs', e)
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const formatTime = (time) => {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-'
}

onMounted(() => {
  fetchData()
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
.mobile-pagination {
  margin-top: 16px;
  text-align: center;
}
</style>
