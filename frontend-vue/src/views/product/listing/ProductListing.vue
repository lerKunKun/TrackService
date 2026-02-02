<template>
  <div class="product-listing-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="title-section">
        <h1>产品刊登管理</h1>
        <p class="subtitle">选择产品导出为 Shopify CSV 格式，快速完成批量刊登</p>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="content-wrapper">
      <!-- 筛选和操作栏 -->
      <div class="toolbar">
        <div class="filter-group">
          <a-input
            v-model:value="filters.keyword"
            placeholder="搜索产品标题、SKU 或 Handle"
            allow-clear
            @pressEnter="handleSearch"
            class="search-input"
          >
            <template #prefix><SearchOutlined /></template>
          </a-input>
          
          <a-select
            v-model:value="filters.status"
            placeholder="刊登状态"
            allow-clear
            class="status-filter"
            @change="handleSearch"
          >
            <a-select-option value="">全部状态</a-select-option>
            <a-select-option value="0">未刊登</a-select-option>
            <a-select-option value="1">已导出</a-select-option>
          </a-select>
          
          <a-button @click="handleSearch" :loading="loading">
            <template #icon><SearchOutlined /></template>
            搜索
          </a-button>
          
          <a-button @click="handleReset" :disabled="loading">
            重置
          </a-button>
        </div>

        <div class="action-group">
          <a-button 
            type="primary" 
            size="large"
            @click="handleExport"
            :loading="exporting"
            :disabled="selectedRowKeys.length === 0"
          >
            <template #icon><DownloadOutlined /></template>
            导出选中产品 {{ selectedRowKeys.length > 0 ? `(${selectedRowKeys.length})` : '' }}
          </a-button>
        </div>
      </div>

      <!-- 批量操作提示条 -->
      <transition name="slide-fade">
        <div v-if="selectedRowKeys.length > 0" class="selection-bar">
          <div class="selection-info">
            <a-checkbox 
              :checked="isAllSelected"
              :indeterminate="selectedRowKeys.length > 0 && !isAllSelected"
              @change="handleSelectAll"
            >
              已选择 <strong>{{ selectedRowKeys.length }}</strong> 个产品
            </a-checkbox>
          </div>
          <div class="selection-actions">
            <a-button size="small" @click="clearSelection">取消选择</a-button>
          </div>
        </div>
      </transition>

      <!-- 产品列表 -->
      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        row-key="id"
        @change="handleTableChange"
        class="product-table"
      >
        <!-- 产品信息列 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'product'">
            <div class="product-cell">
              <div class="product-image">
                <a-image
                  :src="record.imageUrl || '/images/placeholder.png'"
                  :width="64"
                  :height="64"
                  :preview="{ src: record.imageUrl }"
                  fallback="/images/placeholder.png"
                />
              </div>
              <div class="product-info">
                <div class="product-title" :title="record.title">
                  {{ record.title }}
                </div>
                <div class="product-meta">
                  <span class="meta-item">
                    <span class="meta-label">Handle:</span>
                    <span class="meta-value">{{ record.handle || '-' }}</span>
                  </span>
                  <span v-if="record.sku" class="meta-item">
                    <span class="meta-label">SKU:</span>
                    <span class="meta-value">{{ record.sku }}</span>
                  </span>
                </div>
              </div>
            </div>
          </template>

          <!-- 变体数量列 -->
          <template v-if="column.key === 'variants'">
            <a-tag color="blue" class="variant-tag">
              {{ record.variantCount || 0 }} 个变体
            </a-tag>
          </template>

          <!-- 价格列 -->
          <template v-if="column.key === 'price'">
            <div class="price-cell">
              <div class="price-amount">¥{{ formatPrice(record.price) }}</div>
              <div v-if="record.compareAtPrice" class="price-compare">
                原价: ¥{{ formatPrice(record.compareAtPrice) }}
              </div>
            </div>
          </template>

          <!-- 库存列 -->
          <template v-if="column.key === 'inventory'">
            <a-tag :color="getInventoryColor(record.inventory)">
              {{ record.inventory || 0 }}
            </a-tag>
          </template>

          <!-- 状态列 -->
          <template v-if="column.key === 'status'">
            <div class="status-cell">
              <a-badge 
                :status="record.publishStatus === 1 ? 'success' : 'default'" 
                :text="record.publishStatus === 1 ? '已导出' : '未刊登'" 
              />
              <div v-if="record.lastExportTime" class="export-time">
                {{ formatTime(record.lastExportTime) }}
              </div>
            </div>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import productApi from '@/api/product'
import dayjs from 'dayjs'

const loading = ref(false)
const exporting = ref(false)
const list = ref([])
const selectedRowKeys = ref([])

const filters = reactive({
  keyword: '',
  status: undefined
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 个产品`,
  pageSizeOptions: ['10', '20', '50', '100']
})

const columns = [
  {
    title: '产品信息',
    key: 'product',
    width: 400,
    ellipsis: true
  },
  {
    title: '变体',
    key: 'variants',
    width: 100,
    align: 'center'
  },
  {
    title: '价格',
    key: 'price',
    width: 120,
    align: 'right'
  },
  {
    title: '库存',
    key: 'inventory',
    width: 100,
    align: 'center'
  },
  {
    title: '刊登状态',
    key: 'status',
    width: 150
  },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    width: 180,
    customRender: ({ text }) => formatTime(text)
  }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: onSelectChange,
  getCheckboxProps: (record) => ({
    // 可以添加禁用逻辑，比如已刊登的产品
    // disabled: record.publishStatus === 1
  })
}))

const isAllSelected = computed(() => {
  return list.value.length > 0 && selectedRowKeys.value.length === list.value.length
})

onMounted(() => {
  fetchList()
})

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.current,
      pageSize: pagination.pageSize,
      keyword: filters.keyword || undefined,
      status: filters.status
    }
    const res = await productApi.getProductList(params)
    if (res.success) {
      list.value = res.data.list
      pagination.total = res.data.total
    }
  } catch (error) {
    console.error(error)
    message.error('获取产品列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchList()
}

const handleReset = () => {
  filters.keyword = ''
  filters.status = undefined
  pagination.current = 1
  fetchList()
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchList()
}

const onSelectChange = (keys) => {
  selectedRowKeys.value = keys
}

const handleSelectAll = (e) => {
  if (e.target.checked) {
    selectedRowKeys.value = list.value.map(item => item.id)
  } else {
    selectedRowKeys.value = []
  }
}

const clearSelection = () => {
  selectedRowKeys.value = []
}

const handleExport = async () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要导出的产品')
    return
  }
  
  exporting.value = true
  try {
    const res = await productApi.exportShopifyCsv(selectedRowKeys.value)
    
    // 创建并下载CSV文件
    const blob = new Blob([res], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.download = `shopify_products_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(link.href)
    
    message.success(`成功导出 ${selectedRowKeys.value.length} 个产品`)
    
    // 清空选择并刷新列表
    selectedRowKeys.value = []
    fetchList()
  } catch (error) {
    console.error(error)
    message.error('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

const formatTime = (time) => {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

const formatPrice = (price) => {
  if (!price && price !== 0) return '-'
  return Number(price).toFixed(2)
}

const getInventoryColor = (inventory) => {
  if (!inventory || inventory === 0) return 'error'
  if (inventory < 10) return 'warning'
  return 'success'
}
</script>

<style scoped>
.product-listing-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
}

.title-section h1 {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 8px 0;
}

.subtitle {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.content-wrapper {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.search-input {
  width: 280px;
  min-width: 200px;
}

.status-filter {
  width: 160px;
  min-width: 120px;
}

.action-group {
  display: flex;
  gap: 12px;
}

/* 批量选择提示条 */
.selection-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #e6f7ff;
  border-bottom: 1px solid #91d5ff;
}

.selection-info strong {
  color: #1890ff;
  margin: 0 4px;
}

.selection-actions {
  display: flex;
  gap: 8px;
}

/* 动画效果 */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from {
  transform: translateY(-10px);
  opacity: 0;
}

.slide-fade-leave-to {
  transform: translateY(-10px);
  opacity: 0;
}

/* 表格样式 */
.product-table {
  :deep(.ant-table) {
    font-size: 14px;
  }

  :deep(.ant-table-thead > tr > th) {
    background: #fafafa;
    font-weight: 600;
    color: #262626;
  }

  :deep(.ant-table-tbody > tr > td) {
    padding: 16px;
  }

  :deep(.ant-table-tbody > tr:hover > td) {
    background: #fafafa;
  }

  :deep(.ant-pagination) {
    margin: 16px 24px;
  }
}

/* 产品单元格 */
.product-cell {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.product-image {
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
}

.product-image :deep(.ant-image-img) {
  object-fit: cover;
  display: block;
}

.product-info {
  flex: 1;
  min-width: 0;
}

.product-title {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.5;
}

.product-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-item {
  display: flex;
  gap: 6px;
  font-size: 12px;
}

.meta-label {
  color: #8c8c8c;
  flex-shrink: 0;
}

.meta-value {
  color: #595959;
}

/* 变体标签 */
.variant-tag {
  font-size: 13px;
  padding: 2px 10px;
  border-radius: 4px;
}

/* 价格单元格 */
.price-cell {
  text-align: right;
}

.price-amount {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
}

.price-compare {
  font-size: 12px;
  color: #8c8c8c;
  text-decoration: line-through;
  margin-top: 4px;
}

/* 状态单元格 */
.status-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.export-time {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-group {
    flex-wrap: wrap;
  }

  .action-group {
    width: 100%;
  }

  .action-group .ant-btn {
    flex: 1;
  }
}

@media (max-width: 768px) {
  .product-listing-page {
    padding: 16px;
  }

  .search-input {
    width: 100%;
  }

  .status-filter {
    width: 100%;
  }

  .product-cell {
    flex-direction: column;
  }

  .product-image {
    width: 100%;
  }

  .product-image :deep(.ant-image) {
    width: 100%;
  }
}
</style>