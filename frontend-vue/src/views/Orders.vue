<template>
  <div class="page-container">
    <div class="content-card">
      <!-- 工具栏 -->
      <div class="toolbar">
        <h2>订单列表</h2>
        <a-space>
          <a-select
            v-model:value="filters.shopId"
            placeholder="选择店铺"
            style="width: 200px"
            allow-clear
            @change="handleFilterChange"
          >
            <a-select-option :value="null">全部店铺</a-select-option>
            <a-select-option v-for="shop in shops" :key="shop.id" :value="shop.id">
              {{ shop.shopName }}
            </a-select-option>
          </a-select>
        </a-space>
      </div>

      <!-- 表格 (PC端) -->
      <a-table
        v-if="!isMobile"
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
      >
        <!-- 订单号 -->
        <template #orderName="{ record }">
          <strong>{{ record.orderName }}</strong>
        </template>

        <!-- 金额 -->
        <template #totalPrice="{ record }">
          {{ record.currency }} {{ record.totalPrice }}
        </template>

        <!-- 支付状态 -->
        <template #financialStatus="{ record }">
          <a-tag :color="getFinancialStatusColor(record.financialStatus)">
            {{ getFinancialStatusText(record.financialStatus) }}
          </a-tag>
        </template>

        <!-- 履约状态 -->
        <template #fulfillmentStatus="{ record }">
          <a-tag :color="getFulfillmentStatusColor(record.fulfillmentStatus)">
            {{ getFulfillmentStatusText(record.fulfillmentStatus) }}
          </a-tag>
        </template>

        <!-- 时间格式化 -->
        <template #createdAt="{ record }">
          {{ formatDate(record.createdAt) }}
        </template>

        <!-- 操作列 -->
        <template #action="{ record }">
          <a-space>
            <a-button type="link" size="small" @click="handleViewDetails(record)">
              查看
            </a-button>
          </a-space>
        </template>
      </a-table>

      <!-- 卡片列表 (移动端) -->
      <div v-else class="mobile-list">
        <a-spin :spinning="loading">
          <div v-if="tableData.length > 0">
            <div v-for="item in tableData" :key="item.id" class="mobile-card">
              <div class="card-header">
                <span class="order-name">{{ item.orderName }}</span>
                <a-tag :color="getFinancialStatusColor(item.financialStatus)">
                  {{ getFinancialStatusText(item.financialStatus) }}
                </a-tag>
              </div>
              <div class="card-body">
                <div class="info-row">
                  <span class="label">客户:</span>
                  <span class="value">{{ item.customerName || item.customerEmail }}</span>
                </div>
                <div class="info-row">
                  <span class="label">金额:</span>
                  <span class="value">{{ item.currency }} {{ item.totalPrice }}</span>
                </div>
                <div class="info-row">
                  <span class="label">履约:</span>
                  <a-tag :color="getFulfillmentStatusColor(item.fulfillmentStatus)">
                    {{ getFulfillmentStatusText(item.fulfillmentStatus) }}
                  </a-tag>
                </div>
                <div class="info-row">
                  <span class="label">时间:</span>
                  <span class="value">{{ formatDate(item.createdAt) }}</span>
                </div>
              </div>
              <div class="card-actions">
                <a-button type="link" size="small" @click="handleViewDetails(item)">
                  查看详情
                </a-button>
              </div>
            </div>
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
          <a-empty v-else description="暂无数据" />
        </a-spin>
      </div>
    </div>

    <!-- 订单详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="订单详情"
      width="800px"
      :footer="null"
    >
      <a-descriptions v-if="currentOrder" bordered :column="2">
        <a-descriptions-item label="订单号" :span="2">
          <strong>{{ currentOrder.orderName }}</strong>
        </a-descriptions-item>
        
        <a-descriptions-item label="店铺">
          {{ currentOrder.shopName }}
        </a-descriptions-item>
        
        <a-descriptions-item label="订单编号">
          {{ currentOrder.orderNumber }}
        </a-descriptions-item>
        
        <a-descriptions-item label="客户姓名">
          {{ currentOrder.customerName || '-' }}
        </a-descriptions-item>
        
        <a-descriptions-item label="客户邮箱">
          {{ currentOrder.customerEmail || '-' }}
        </a-descriptions-item>
        
        <a-descriptions-item label="客户电话">
          {{ currentOrder.customerPhone || '-' }}
        </a-descriptions-item>
        
        <a-descriptions-item label="总金额">
          {{ currentOrder.currency }} {{ currentOrder.totalPrice }}
        </a-descriptions-item>
        
        <a-descriptions-item label="支付状态">
          <a-tag :color="getFinancialStatusColor(currentOrder.financialStatus)">
            {{ getFinancialStatusText(currentOrder.financialStatus) }}
          </a-tag>
        </a-descriptions-item>
        
        <a-descriptions-item label="履约状态">
          <a-tag :color="getFulfillmentStatusColor(currentOrder.fulfillmentStatus)">
            {{ getFulfillmentStatusText(currentOrder.fulfillmentStatus) }}
          </a-tag>
        </a-descriptions-item>
        
        <a-descriptions-item label="收货人" :span="2">
          {{ currentOrder.shippingAddressName || '-' }}
        </a-descriptions-item>
        
        <a-descriptions-item label="收货城市">
          {{ currentOrder.shippingAddressCity || '-' }}
        </a-descriptions-item>
        
        <a-descriptions-item label="收货国家">
          {{ currentOrder.shippingAddressCountry || '-' }}
        </a-descriptions-item>
        
        <a-descriptions-item label="创建时间">
          {{ formatDate(currentOrder.createdAt) }}
        </a-descriptions-item>
        
        <a-descriptions-item label="更新时间">
          {{ formatDate(currentOrder.updatedAt) }}
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Grid } from 'ant-design-vue'
import orderApi from '@/api/order'
import { shopApi } from '@/api/shop'
import dayjs from 'dayjs'

const useBreakpoint = Grid.useBreakpoint
const screens = useBreakpoint()
const isMobile = computed(() => !screens.value.md)

const loading = ref(false)
const tableData = ref([])
const shops = ref([])
const detailVisible = ref(false)
const currentOrder = ref(null)

const filters = reactive({
  shopId: null
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

const columns = [
  {
    title: '订单号',
    dataIndex: 'orderName',
    width: 120,
    slots: { customRender: 'orderName' }
  },
  {
    title: '店铺',
    dataIndex: 'shopName',
    width: 150,
    ellipsis: true
  },
  {
    title: '客户',
    dataIndex: 'customerName',
    width: 120,
    ellipsis: true,
    customRender: ({ record }) => record.customerName || record.customerEmail
  },
  {
    title: '金额',
    dataIndex: 'totalPrice',
    width: 120,
    slots: { customRender: 'totalPrice' }
  },
  {
    title: '支付状态',
    dataIndex: 'financialStatus',
    width: 100,
    slots: { customRender: 'financialStatus' }
  },
  {
    title: '履约状态',
    dataIndex: 'fulfillmentStatus',
    width: 100,
    slots: { customRender: 'fulfillmentStatus' }
  },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    width: 160,
    slots: { customRender: 'createdAt' }
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
    fixed: 'right',
    slots: { customRender: 'action' }
  }
]

// 获取订单列表
const fetchOrders = async () => {
  loading.value = true
  try {
    const params = {
      shopId: filters.shopId,
      page: pagination.current,
      pageSize: pagination.pageSize
    }
    
    const data = await orderApi.getList(params)
    
    // 注意：data已经是后端返回的data字段（由Axios拦截器提取）
    if (data && data.list) {
      tableData.value = data.list || []
      pagination.total = data.total || 0
    }
  } catch (error) {
    console.error('获取订单列表失败:', error)
    message.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

// 获取店铺列表
const fetchShops = async () => {
  try {
    const data = await shopApi.getList({ page: 1, pageSize: 100 })
    // 注意：data已经是后端返回的data字段（由Axios拦截器提取）
    if (data && data.list) {
      shops.value = data.list || []
    }
  } catch (error) {
    console.error('获取店铺列表失败:', error)
  }
}

// 表格分页变化
const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchOrders()
}

// 筛选变化
const handleFilterChange = () => {
  pagination.current = 1
  fetchOrders()
}

// 查看详情
const handleViewDetails = async (record) => {
  try {
    const data = await orderApi.getDetail(record.id)  
    if (data) {
      currentOrder.value = data
      detailVisible.value = true
    }
  } catch (error) {
    console.error('获取订单详情失败:', error)
    message.error('获取订单详情失败')
  }
}

// 获取支付状态颜色
const getFinancialStatusColor = (status) => {
  const colors = {
    paid: 'green',
    pending: 'orange',
    refunded: 'red',
    partially_refunded: 'orange',
    voided: 'default'
  }
  return colors[status] || 'default'
}

// 获取支付状态文本
const getFinancialStatusText = (status) => {
  const texts = {
    paid: '已支付',
    pending: '待支付',
    refunded: '已退款',
    partially_refunded: '部分退款',
    voided: '已作废'
  }
  return texts[status] || status || '未知'
}

// 获取履约状态颜色
const getFulfillmentStatusColor = (status) => {
  const colors = {
    fulfilled: 'green',
    partial: 'orange',
    null: 'default'
  }
  return colors[status] || 'default'
}

// 获取履约状态文本
const getFulfillmentStatusText = (status) => {
  const texts = {
    fulfilled: '已履约',
    partial: '部分履约',
    null: '未履约'
  }
  return texts[status] || status || '未履约'
}

// 格式化日期
const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'
}

onMounted(() => {
  fetchShops()
  fetchOrders()
})
</script>

<style scoped>
.page-container {
  padding: 16px;
  min-height: calc(100vh - 64px);
}

.content-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

/* 移动端卡片样式 */
.mobile-list .mobile-card {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.mobile-list .mobile-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8e8e8;
}

.mobile-list .mobile-card .card-header .order-name {
  font-size: 16px;
  font-weight: 600;
  color: #1890ff;
}

.mobile-list .mobile-card .card-body .info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}

.mobile-list .mobile-card .card-body .info-row .label {
  color: #666;
  flex-shrink: 0;
  margin-right: 8px;
}

.mobile-list .mobile-card .card-body .info-row .value {
  color: #333;
  text-align: right;
  word-break: break-all;
}

.mobile-list .mobile-card .card-actions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e8e8e8;
  display: flex;
  justify-content: flex-end;
}

.mobile-list .mobile-pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

/* 响应式 */
@media (max-width: 768px) {
  .page-container {
    padding: 8px;
  }

  .content-card {
    padding: 12px;
  }

  .toolbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .toolbar h2 {
    font-size: 16px;
  }
}
</style>
