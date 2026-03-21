<template>
  <div class="page-content">
    <a-card title="操作日志" :bordered="false">
      <!-- 筛选栏 -->
      <div class="table-operations">
        <a-space wrap>
          <a-input-search
            v-model:value="filters.operation"
            placeholder="搜索操作内容"
            allow-clear
            style="width: 200px"
            @search="handleSearch"
          />
          <a-select
            v-model:value="filters.module"
            placeholder="模块"
            allow-clear
            style="width: 140px"
            @change="handleSearch"
          >
            <a-select-option v-for="m in moduleOptions" :key="m" :value="m">{{ m }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="filters.result"
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
          <template v-if="column.key === 'result'">
            <a-tag :color="record.result === 'SUCCESS' ? 'green' : 'red'">
              {{ record.result === 'SUCCESS' ? '成功' : '失败' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'executionTime'">
            <span>{{ record.executionTime }}ms</span>
          </template>
          <template v-if="column.key === 'createdAt'">
            {{ formatTime(record.createdAt) }}
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="showDetail(record)">详情</a-button>
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
              @click="showDetail(item)"
            >
              <div class="mobile-card-header">
                <span class="mobile-card-title">{{ item.operation }}</span>
                <a-tag :color="item.result === 'SUCCESS' ? 'green' : 'red'" size="small">
                  {{ item.result === 'SUCCESS' ? '成功' : '失败' }}
                </a-tag>
              </div>
              <div class="mobile-card-body">
                <div>用户：{{ item.username || '-' }}</div>
                <div>模块：{{ item.module || '-' }}</div>
                <div>IP：{{ item.ipAddress || '-' }}</div>
                <div>时间：{{ formatTime(item.createdAt) }}</div>
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

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="操作日志详情"
      :footer="null"
      width="640px"
    >
      <a-descriptions :column="1" bordered size="small" v-if="currentDetail">
        <a-descriptions-item label="操作">{{ currentDetail.operation }}</a-descriptions-item>
        <a-descriptions-item label="模块">{{ currentDetail.module || '-' }}</a-descriptions-item>
        <a-descriptions-item label="用户">{{ currentDetail.username || '-' }}</a-descriptions-item>
        <a-descriptions-item label="结果">
          <a-tag :color="currentDetail.result === 'SUCCESS' ? 'green' : 'red'">
            {{ currentDetail.result === 'SUCCESS' ? '成功' : '失败' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="方法">
          <span style="word-break: break-all; font-size: 12px;">{{ currentDetail.method || '-' }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="请求参数">
          <pre style="max-height: 200px; overflow: auto; font-size: 12px; margin: 0;">{{ formatParams(currentDetail.params) }}</pre>
        </a-descriptions-item>
        <a-descriptions-item label="错误信息" v-if="currentDetail.errorMsg">
          <span style="color: #f5222d;">{{ currentDetail.errorMsg }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="IP地址">{{ currentDetail.ipAddress || '-' }}</a-descriptions-item>
        <a-descriptions-item label="User-Agent">
          <span style="word-break: break-all; font-size: 12px;">{{ currentDetail.userAgent || '-' }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="执行耗时">{{ currentDetail.executionTime }}ms</a-descriptions-item>
        <a-descriptions-item label="操作时间">{{ formatTime(currentDetail.createdAt) }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Grid } from 'ant-design-vue'
import { auditLogApi } from '@/api/audit-log'
import { formatDateTime as formatTime } from '@/utils/datetime'
import { usePagination } from '@/composables/usePagination'

const { useBreakpoint } = Grid
const screens = useBreakpoint()
const isMobile = computed(() => !screens.value.md)

const loading = ref(false)
const tableData = ref([])
const detailVisible = ref(false)
const currentDetail = ref(null)

const filters = reactive({
  operation: '',
  module: null,
  result: null,
  dateRange: null
})

const moduleOptions = ['用户管理', '角色管理', '权限管理', '菜单管理', '店铺管理', '订单管理', '产品管理', '运单管理']

const { pagination, handleTableChange, handleSearch } = usePagination(fetchData)

const columns = [
  { title: '时间', key: 'createdAt', dataIndex: 'createdAt', width: 170 },
  { title: '用户', dataIndex: 'username', width: 100 },
  { title: '操作', dataIndex: 'operation', width: 150 },
  { title: '模块', dataIndex: 'module', width: 100 },
  { title: '结果', key: 'result', dataIndex: 'result', width: 80 },
  { title: 'IP', dataIndex: 'ipAddress', width: 130 },
  { title: '耗时', key: 'executionTime', dataIndex: 'executionTime', width: 80 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.current,
      pageSize: pagination.pageSize,
      operation: filters.operation || undefined,
      module: filters.module || undefined,
      result: filters.result || undefined,
      startTime: filters.dateRange?.[0]?.format('YYYY-MM-DD HH:mm:ss') || undefined,
      endTime: filters.dateRange?.[1]?.format('YYYY-MM-DD HH:mm:ss') || undefined
    }
    const data = await auditLogApi.getList(params)
    tableData.value = data.list || []
    pagination.total = data.total || 0
  } catch (e) {
    console.error('Failed to fetch audit logs', e)
  } finally {
    loading.value = false
  }
}

const showDetail = (record) => {
  currentDetail.value = record
  detailVisible.value = true
}

const formatParams = (params) => {
  if (!params) return '-'
  try {
    return JSON.stringify(JSON.parse(params), null, 2)
  } catch {
    return params
  }
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
  cursor: pointer;
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
