<template>
  <div class="product-listing-page">
    <div class="page-header">
      <div class="title-section">
        <h1>产品刊登</h1>
        <div class="subtitle">导出产品为Shopify CSV格式</div>
      </div>
      <a-space>
        <a-button 
          type="primary" 
          @click="handleExport"
          :loading="exporting"
          :disabled="selectedRowKeys.length === 0"
        >
          <template #icon><DownloadOutlined /></template>
          导出Shopify CSV ({{ selectedRowKeys.length }})
        </a-button>
      </a-space>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-section">
      <a-form layout="inline">
        <a-form-item>
          <a-input
            v-model:value="filters.keyword"
            placeholder="搜索产品标题或SKU"
            allow-clear
            @pressEnter="fetchList"
            style="width: 240px"
          >
            <template #prefix><SearchOutlined /></template>
          </a-input>
        </a-form-item>
        
        <a-form-item>
          <a-select
            v-model:value="filters.status"
            placeholder="刊登状态"
            allow-clear
            style="width: 160px"
            @change="fetchList"
          >
            <!-- 暂未实现后端筛选API参数, 这里先做前端筛选准备或后续对接 -->
            <a-select-option value="">全部状态</a-select-option>
            <a-select-option value="0">未刊登</a-select-option>
            <a-select-option value="1">已导出</a-select-option>
          </a-select>
        </a-form-item>
        
        <a-form-item>
          <a-button @click="fetchList">搜索</a-button>
        </a-form-item>
      </a-form>
    </div>

    <!-- 列表 -->
    <a-table
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }"
      row-key="id"
      @change="handleTableChange"
    >
      <!-- 产品信息列 -->
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'product'">
          <div class="product-info">
            <a-image
              :src="record.imageUrl"
              :width="60"
              fallback="/images/placeholder.png"
            />
            <div class="product-detail">
              <div class="product-title">{{ record.title }}</div>
              <div class="product-handle text-secondary">Handle: {{ record.handle }}</div>
            </div>
          </div>
        </template>

        <!-- 变体概览 -->
        <template v-if="column.key === 'variants'">
          <a-tag color="blue">{{ record.variantCount || 0 }} 个变体</a-tag>
        </template>

        <!-- 状态列 -->
        <template v-if="column.key === 'status'">
          <a-badge v-if="record.publishStatus === 1" status="success" text="已导出" />
          <a-badge v-else status="default" text="未刊登" />
          <div v-if="record.lastExportTime" class="text-secondary text-small">
            {{ formatTime(record.lastExportTime) }}
          </div>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import productApi from '@/api/product' // 确保这里引入了正确的API
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
  showSizeChanger: true
})

const columns = [
  {
    title: '产品信息',
    key: 'product',
  },
  {
    title: '变体数量',
    key: 'variants',
    width: 120
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

onMounted(() => {
  fetchList()
})

const fetchList = async () => {
  loading.value = true
  try {
    // 假设 getProductList 这里已经包含了 publishStatus 等扩展字段
    // 如果没有，可能需要 productApi.getProductList 返回的DTO里带上
    // 目前后端 getProductList 似乎只返回基础信息? 
    // 需要确保 ProductController.list 返回的 Product VO 包含 publishStatus
    // 暂时先用现有接口，状态可能需要额外获取或假设已经在 list 中
    const params = {
      page: pagination.current,
      pageSize: pagination.pageSize,
      keyword: filters.keyword,
      // status: filters.status
    }
    const res = await productApi.getProductList(params)
    if (res.success) {
      list.value = res.data.list
      pagination.total = res.data.total
    }
  } catch (error) {
    console.error(error)
    message.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchList()
}

const onSelectChange = (keys) => {
  selectedRowKeys.value = keys
}

const handleExport = async () => {
  if (selectedRowKeys.value.length === 0) return
  
  exporting.value = true
  try {
    const res = await productApi.exportShopifyCsv(selectedRowKeys.value)
    // 处理文件下载
    const blob = new Blob([res], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.download = `shopify_products_${dayjs().format('YYYYMMDDHHmmss')}.csv`
    link.click()
    window.URL.revokeObjectURL(link.href)
    message.success('导出成功')
    
    // 刷新列表更新状态
    selectedRowKeys.value = []
    fetchList()
  } catch (error) {
    console.error(error)
    message.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const formatTime = (time) => {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}
</script>

<style scoped>
.product-listing-page {
  padding: 24px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.subtitle {
  color: #999;
  font-size: 14px;
  margin-top: 4px;
}
.filter-section {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  margin-bottom: 16px;
}
.product-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.product-title {
  font-weight: 500;
  margin-bottom: 4px;
}
.text-secondary {
  color: #999;
}
.text-small {
  font-size: 12px;
}
</style>
