<template>
  <div class="page-container">
    <div class="content-card">
      <!-- 工具栏 -->
      <div class="toolbar">
        <h2>产品开发</h2>
        <a-space>

          <a-button type="primary" @click="showUploadModal">
            <UploadOutlined />
            导入CSV
          </a-button>
          <a-dropdown v-if="selectedRowKeys.length > 0">
            <template #overlay>
              <a-menu @click="handleBatchMenuClick">
                <a-menu-item key="edit">批量编辑 (标签/状态)</a-menu-item>
                <a-menu-item key="shop">批量分配商店</a-menu-item>
                <a-menu-item key="delete" danger>批量删除</a-menu-item>
              </a-menu>
            </template>
            <a-button>
              批量操作 <DownOutlined />
            </a-button>
          </a-dropdown>
        </a-space>
      </div>

      <!-- 搜索筛选栏 -->
      <div class="filter-bar">
        <a-form layout="inline">
          <a-form-item>
            <a-input
              v-model:value="filters.title"
              placeholder="搜索产品名称"
              style="width: 200px"
              allow-clear
              @pressEnter="handleSearch"
            >
              <template #prefix>
                <SearchOutlined />
              </template>
            </a-input>
          </a-form-item>
          <a-form-item>
            <a-input
              v-model:value="filters.tags"
              placeholder="搜索标签"
              style="width: 200px"
              allow-clear
              @pressEnter="handleSearch"
            >
              <template #prefix>
                <TagOutlined />
              </template>
            </a-input>
          </a-form-item>
          <a-form-item>
            <a-select
              v-model:value="filters.published"
              placeholder="上架状态"
              style="width: 150px"
              allow-clear
              @change="handleSearch"
            >
              <a-select-option :value="null">全部状态</a-select-option>
              <a-select-option :value="0">草稿</a-select-option>
              <a-select-option :value="1">已上架</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-select
              v-model:value="filters.shopId"
              placeholder="选择商店"
              style="width: 200px"
              allow-clear
              @change="handleSearch"
            >
              <a-select-option :value="null">全部商店</a-select-option>
              <a-select-option v-for="shop in shops" :key="shop.id" :value="shop.id">
                {{ shop.shopName }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="handleSearch">
              <SearchOutlined />
              搜索
            </a-button>
          </a-form-item>
        </a-form>
      </div>

      <!-- 产品表格 (PC端) -->
      <a-table
        v-if="!isMobile"
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        @change="handleTableChange"
        row-key="id"
      >
        <!-- 产品图片和名称 -->
        <template #product="{ record }">
          <div class="product-info">
            <img 
              v-if="record.imageUrl" 
              :src="record.imageUrl" 
              :alt="record.title"
              class="product-image"
              @error="handleImageError"
            />
            <div v-else class="product-image-placeholder">
              <PictureOutlined />
            </div>
            <div class="product-title">
              <strong>{{ record.title }}</strong>
              <div class="product-handle">{{ record.handle }}</div>
            </div>
          </div>
        </template>

        <!-- 价格 -->
        <template #price="{ record }">
          <div v-if="record.price">
            <div class="price-current">${{ record.price }}</div>
            <div v-if="record.compareAtPrice" class="price-compare">
              原价: ${{ record.compareAtPrice }}
            </div>
          </div>
          <span v-else>-</span>
        </template>

        <!-- 标签 -->
        <template #tags="{ record }">
          <div v-if="record.tags" class="tags-wrapper">
            <a-tag v-for="(tag, index) in getTagsArray(record.tags)" :key="index" color="blue">
              {{ tag }}
            </a-tag>
          </div>
          <span v-else>-</span>
        </template>

        <!-- 上架状态 -->
        <template #published="{ record }">
          <a-tag :color="record.published === 1 ? 'green' : 'default'">
            {{ record.published === 1 ? '已上架' : '草稿' }}
          </a-tag>
        </template>

        <!-- 所属商店 -->
        <template #shops="{ record }">
          <div v-if="record.shopIds && record.shopIds.length > 0" class="shops-wrapper">
            <a-tag v-for="shopId in record.shopIds" :key="shopId" color="purple">
              {{ getShopName(shopId) }}
            </a-tag>
          </div>
          <span v-else class="text-muted">未分配</span>
        </template>

        <!-- 操作列 -->
        <template #action="{ record }">
          <a-space :size="4">
            <a-tooltip title="编辑">
              <a-button type="link" size="small" @click="handleEdit(record)">
                <EditOutlined />
              </a-button>
            </a-tooltip>
            <a-popconfirm
              title="确定要删除该产品吗？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="handleDelete(record.id)"
            >
              <a-tooltip title="删除">
                <a-button type="link" danger size="small">
                  <DeleteOutlined />
                </a-button>
              </a-tooltip>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>

      <!-- 卡片列表 (移动端) -->
      <div v-else class="mobile-list">
        <a-spin :spinning="loading">
          <div v-if="tableData.length > 0">
            <div v-for="item in tableData" :key="item.id" class="mobile-card">
              <div class="card-header">
                <img 
                  v-if="item.imageUrl" 
                  :src="item.imageUrl" 
                  :alt="item.title"
                  class="mobile-product-image"
                  @error="handleImageError"
                />
                <div class="mobile-product-info">
                  <span class="product-name">{{ item.title }}</span>
                  <a-tag :color="item.published === 1 ? 'green' : 'default'">
                    {{ item.published === 1 ? '已上架' : '草稿' }}
                  </a-tag>
                </div>
              </div>
              <div class="card-body">
                <div class="info-row" v-if="item.price">
                  <span class="label">价格:</span>
                  <span class="value">${{ item.price }}</span>
                </div>
                <div class="info-row" v-if="item.compareAtPrice">
                  <span class="label">原价:</span>
                  <span class="value">${{ item.compareAtPrice }}</span>
                </div>
                <div class="info-row" v-if="item.tags">
                  <span class="label">标签:</span>
                  <div class="value tags-wrapper">
                    <a-tag v-for="(tag, index) in getTagsArray(item.tags)" :key="index" color="blue">
                      {{ tag }}
                    </a-tag>
                  </div>
                </div>
                <div class="info-row" v-if="item.shopIds && item.shopIds.length > 0">
                  <span class="label">商店:</span>
                  <div class="value shops-wrapper">
                    <a-tag v-for="shopId in item.shopIds" :key="shopId" color="purple">
                      {{ getShopName(shopId) }}
                    </a-tag>
                  </div>
                </div>
              </div>
              <div class="card-actions">
                <a-button type="link" size="small" @click="handleEdit(item)">
                  编辑
                </a-button>
                <a-popconfirm
                  title="确定要删除吗？"
                  ok-text="确定"
                  cancel-text="取消"
                  @confirm="handleDelete(item.id)"
                >
                  <a-button type="link" danger size="small">
                    删除
                  </a-button>
                </a-popconfirm>
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

    <!-- CSV上传弹窗 -->
    <a-modal
      v-model:open="uploadModalVisible"
      title="导入Shopify CSV"
      width="600px"
      :footer="null"
    >
      <a-upload-dragger
        :file-list="fileList"
        :before-upload="beforeUpload"
        :remove="handleRemoveFile"
        accept=".csv"
        :max-count="1"
      >
        <p class="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p class="ant-upload-text">点击或拖拽CSV文件到此区域上传</p>
        <p class="ant-upload-hint">
          支持Shopify标准CSV格式，包含产品和变体信息
        </p>
      </a-upload-dragger>

      <div style="margin-top: 16px; text-align: right">
        <a-space>
          <a-button @click="uploadModalVisible = false">取消</a-button>
          <a-button 
            type="primary" 
            :loading="uploading"
            :disabled="fileList.length === 0"
            @click="handleUpload"
          >
            开始导入
          </a-button>
        </a-space>
      </div>
    </a-modal>

    <!-- 编辑产品弹窗 -->
    <a-modal
      v-model:open="editModalVisible"
      title="编辑产品"
      width="700px"
      :confirm-loading="editLoading"
      @ok="handleEditSubmit"
      @cancel="handleEditCancel"
    >
      <a-form
        v-if="editForm"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 19 }"
      >
        <a-form-item label="产品Handle">
          <a-input v-model:value="editForm.handle" placeholder="产品唯一标识" />
        </a-form-item>

        <a-form-item label="产品标题">
          <a-input v-model:value="editForm.title" placeholder="产品名称" />
        </a-form-item>

        <a-form-item label="品牌/制造商">
          <a-input v-model:value="editForm.vendor" placeholder="品牌或制造商" />
        </a-form-item>

        <a-form-item label="标签">
          <a-input 
            v-model:value="editForm.tags" 
            placeholder="用逗号分隔，例如: 服装,T恤,夏季"
          />
          <div style="color: #999; font-size: 12px; margin-top: 4px">
            用逗号[英文(半角)]分隔多个标签
          </div>
        </a-form-item>

        <a-form-item label="上架状态">
          <a-radio-group v-model:value="editForm.published">
            <a-radio :value="0">草稿</a-radio>
            <a-radio :value="1">已上架</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="关联商店">
          <a-select
            v-model:value="editForm.shopIds"
            mode="multiple"
            placeholder="选择商店（可多选）"
            style="width: 100%"
          >
            <a-select-option v-for="shop in shops" :key="shop.id" :value="shop.id">
              {{ shop.shopName }}
            </a-select-option>
          </a-select>
          <div style="color: #999; font-size: 12px; margin-top: 4px">
            一个产品可以分配给多个商店
          </div>
        </a-form-item>

        <a-divider>价格设置</a-divider>

        <a-form-item label="销售价格">
          <a-input-number
            v-model:value="editForm.price"
            :min="0"
            :precision="2"
            placeholder="销售价格"
            style="width: 100%"
          >
            <template #prefix>$</template>
          </a-input-number>
          <div style="display: flex; gap: 16px; margin-top: 8px; background: #f5f5f5; padding: 8px; border-radius: 4px;">
            <div style="flex: 1">
              <span style="color: #666; font-size: 12px">平均采购价:</span>
              <div style="font-weight: 500">${{ avgProcurementPrice }}</div>
            </div>
            <div style="flex: 1">
              <span style="color: #666; font-size: 12px">预计利润:</span>
              <div :style="{ color: estimatedProfit >= 0 ? '#52c41a' : '#ff4d4f', fontWeight: '500' }">
                ${{ estimatedProfit }}
                <span v-if="editForm.price > 0" style="font-size: 12px; color: #999; margin-left: 4px">
                   ({{ profitMargin }}%)
                </span>
              </div>
            </div>
          </div>
          <div style="color: #999; font-size: 12px; margin-top: 4px">
            此价格将应用到该产品的所有变体
          </div>
        </a-form-item>

        <a-form-item label="原价（划线价）">
          <div style="display: flex; align-items: center; gap: 8px">
            <a-input-number
              v-model:value="editForm.compareAtPrice"
              :min="0"
              :precision="2"
              placeholder="原价（对比价格）"
              style="width: 100%"
            >
              <template #prefix>$</template>
            </a-input-number>
            <a-tag color="error" v-if="discountPercentage > 0">
              -{{ discountPercentage }}% OFF
            </a-tag>
          </div>
          <div style="color: #999; font-size: 12px; margin-top: 4px">
            显示为划线价,用于展示折扣
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 批量编辑弹窗 -->
    <a-modal
      v-model:open="batchEditModal.visible"
      title="批量编辑 (标签/状态)"
      :confirm-loading="batchEditModal.loading"
      @ok="handleBatchEditSubmit"
    >
      <a-form :model="batchEditModal.form" layout="vertical">
        <a-form-item label="标签 (追加或覆盖)">
          <a-input 
            v-model:value="batchEditModal.form.tags" 
            placeholder="输入标签，多个用逗号分隔" 
          />
          <div style="color: #999; font-size: 12px">注意: 这里会直接替换原有的标签</div>
        </a-form-item>
        <a-form-item label="上架状态">
          <a-select v-model:value="batchEditModal.form.published" placeholder="不修改">
             <a-select-option :value="null">不修改</a-select-option>
             <a-select-option :value="1">已上架</a-select-option>
             <a-select-option :value="0">草稿</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 批量分配商店弹窗 -->
    <a-modal
      v-model:open="batchShopModal.visible"
      title="批量分配商店"
      :confirm-loading="batchShopModal.loading"
      @ok="handleBatchShopSubmit"
    >
      <a-form layout="vertical">
        <a-form-item label="选择商店 (将覆盖原有分配)">
          <a-select
            v-model:value="batchShopModal.shopIds"
            mode="multiple"
            placeholder="选择要分配的商店"
            style="width: 100%"
          >
            <a-select-option v-for="shop in shops" :key="shop.id" :value="shop.id">
              {{ shop.shopName }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { message, Grid } from 'ant-design-vue'
import {
  UploadOutlined,

  SearchOutlined,
  TagOutlined,
  EditOutlined,
  DeleteOutlined,
  PictureOutlined,
  InboxOutlined,
  DownOutlined
} from '@ant-design/icons-vue'
import { Modal } from 'ant-design-vue'
import productApi from '@/api/product'
import { shopApi } from '@/api/shop'

const useBreakpoint = Grid.useBreakpoint
const screens = useBreakpoint()
const isMobile = computed(() => !screens.value.md)

const loading = ref(false)
const tableData = ref([])
const shops = ref([])
const selectedRowKeys = ref([])

// 表格选择配置
const rowSelection = {
  selectedRowKeys: selectedRowKeys,
  onChange: (keys) => {
    selectedRowKeys.value = keys
  }
}

const filters = reactive({
  title: null,
  tags: null,
  published: null,
  shopId: null
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

const columns = [
  {
    title: '产品',
    key: 'product',
    width: 300,
    slots: { customRender: 'product' }
  },
  {
    title: '价格',
    key: 'price',
    width: 150,
    slots: { customRender: 'price' }
  },
  {
    title: '标签',
    key: 'tags',
    width: 200,
    slots: { customRender: 'tags' }
  },
  {
    title: '上架状态',
    key: 'published',
    width: 100,
    slots: { customRender: 'published' }
  },
  {
    title: '所属商店',
    key: 'shops',
    width: 200,
    slots: { customRender: 'shops' }
  },
  {
    title: '操作',
    key: 'action',
    width: 120,
    fixed: 'right',
    slots: { customRender: 'action' }
  }
]

// 获取产品列表
const fetchProducts = async () => {
  loading.value = true
  try {
    const params = {
      title: filters.title,
      tags: filters.tags,
      published: filters.published,
      shopId: filters.shopId,
      page: pagination.current,
      pageSize: pagination.pageSize
    }

    const response = await productApi.getProductList(params)
    
    // request拦截器已返回response.data，后端格式: { success: true, data: { list, total } }
    if (response && response.data) {
      tableData.value = response.data.list || []
      pagination.total = response.data.total || 0
    }
  } catch (error) {
    console.error('获取产品列表失败:', error)
    message.error('获取产品列表失败')
  } finally {
    loading.value = false
  }
}

// 获取商店列表
const fetchShops = async () => {
  try {
    const data = await shopApi.getList({ page: 1, pageSize: 100 })
    if (data && data.list) {
      shops.value = data.list || []
    }
  } catch (error) {
    console.error('获取商店列表失败:', error)
  }
}

// 表格分页变化
const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchProducts()
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchProducts()
}

// 获取标签数组
const getTagsArray = (tags) => {
  if (!tags) return []
  return tags.split(',').map(t => t.trim()).filter(t => t)
}

// 获取商店名称
const getShopName = (shopId) => {
  const shop = shops.value.find(s => s.id === shopId)
  return shop ? shop.shopName : `店铺${shopId}`
}

// 图片加载错误处理
const handleImageError = (e) => {
  e.target.style.display = 'none'
}

// CSV上传相关
const uploadModalVisible = ref(false)
const uploading = ref(false)
const fileList = ref([])

const showUploadModal = () => {
  fileList.value = []
  uploadModalVisible.value = true
}

const beforeUpload = (file) => {
  // 验证文件类型
  if (!file.name.toLowerCase().endsWith('.csv')) {
    message.error('只支持CSV文件！')
    return false
  }
  
  fileList.value = [file]
  return false // 阻止自动上传
}

const handleRemoveFile = () => {
  fileList.value = []
}

const handleUpload = async () => {
  if (fileList.value.length === 0) {
    message.warning('请选择CSV文件')
    return
  }

  uploading.value = true
  try {
    const response = await productApi.importCsv(fileList.value[0])
    
    // request拦截器已返回response.data，后端格式: { success: true, data: { totalRecords, successRecords } }
    if (response && response.success && response.data) {
      const { totalRecords, successRecords } = response.data
      message.success(`导入成功！共${totalRecords}条记录，成功${successRecords}条`)
      uploadModalVisible.value = false
      fileList.value = []
      fetchProducts() // 刷新列表
    } else {
      message.error(response?.message || '导入失败')
    }
  } catch (error) {
    console.error('CSV导入失败:', error)
    message.error('CSV导入失败，请检查文件格式')
  } finally {
    uploading.value = false
  }
}

// 编辑相关
const editModalVisible = ref(false)
const editLoading = ref(false)
const editForm = ref(null)
const avgProcurementPrice = ref('0.00')

// 计算预计利润
const estimatedProfit = computed(() => {
  if (!editForm.value || !editForm.value.price) return '0.00'
  const price = Number(editForm.value.price) || 0
  const cost = Number(avgProcurementPrice.value) || 0
  return (price - cost).toFixed(2)
})

// 计算利润率
const profitMargin = computed(() => {
  if (!editForm.value || !editForm.value.price) return '0'
  const price = Number(editForm.value.price) || 0
  const cost = Number(avgProcurementPrice.value) || 0
  if (price <= 0) return '0'
  return (((price - cost) / price) * 100).toFixed(0)
})

// 计算折扣百分比
const discountPercentage = computed(() => {
  if (!editForm.value || !editForm.value.price || !editForm.value.compareAtPrice) return 0
  const price = Number(editForm.value.price)
  const compare = Number(editForm.value.compareAtPrice)
  if (compare <= price || compare <= 0) return 0
  return Math.round(((compare - price) / compare) * 100)
})

const handleEdit = async (record) => {
  // 获取产品的第一个变体价格
  let variantPrice = null
  let variantComparePrice = null
  
  try {
    const variantsResponse = await productApi.getProductVariants(record.id)
    if (variantsResponse && variantsResponse.data && variantsResponse.data.length > 0) {
      const firstVariant = variantsResponse.data[0]
      variantPrice = firstVariant.price
      variantComparePrice = firstVariant.compareAtPrice
      
      // 计算平均采购价
      const validCosts = variantsResponse.data
        .map(v => Number(v.procurementPrice))
        .filter(p => !isNaN(p) && p > 0)
      
      if (validCosts.length > 0) {
        const totalCost = validCosts.reduce((a, b) => a + b, 0)
        avgProcurementPrice.value = (totalCost / validCosts.length).toFixed(2)
      } else {
        avgProcurementPrice.value = '0.00'
      }
    } else {
      avgProcurementPrice.value = '0.00'
    }
  } catch (error) {
    console.error('获取变体信息失败:', error)
    avgProcurementPrice.value = '0.00'
  }
  
  editForm.value = {
    id: record.id,
    handle: record.handle,
    title: record.title,
    vendor: record.vendor,
    tags: record.tags,
    published: record.published,
    shopIds: record.shopIds || [],
    price: variantPrice || record.price,
    compareAtPrice: variantComparePrice || record.compareAtPrice
  }
  editModalVisible.value = true
}

const handleEditSubmit = async () => {
  if (!editForm.value.title) {
    message.warning('请输入产品标题')
    return
  }

  editLoading.value = true
  try {
    // 1. 更新产品基本信息
    await productApi.updateProduct(editForm.value.id, {
      handle: editForm.value.handle,
      title: editForm.value.title,
      vendor: editForm.value.vendor,
      tags: editForm.value.tags,
      published: editForm.value.published,
      shopIds: editForm.value.shopIds
    })
    
    // 2. 如果有价格变更,则更新价格
    if (editForm.value.price !== null && editForm.value.price !== undefined) {
      await productApi.updateProductPrice(editForm.value.id, {
        price: editForm.value.price,
        compareAtPrice: editForm.value.compareAtPrice
      })
    }
    
    message.success('更新成功')
    editModalVisible.value = false
    fetchProducts() // 刷新列表
  } catch (error) {
    console.error('更新产品失败:', error)
    message.error('更新失败，请重试')
  } finally {
    editLoading.value = false
  }
}

const handleEditCancel = () => {
  editModalVisible.value = false
  editForm.value = null
}

// 删除
const handleDelete = async (id) => {
  try {
    await productApi.deleteProduct(id)
    message.success('删除成功')
    fetchProducts() // 刷新列表
  } catch (error) {
    console.error('删除产品失败:', error)
    message.error('删除失败,请重试')
  }
}



onMounted(() => {
  fetchShops()
  fetchProducts()
})

// 批量操作相关
const batchEditModal = reactive({
  visible: false,
  loading: false,
  form: {
    tags: '',
    published: null
  }
})

const batchShopModal = reactive({
  visible: false,
  loading: false,
  shopIds: []
})

const handleBatchMenuClick = ({ key }) => {
  if (key === 'delete') {
    handleBatchDelete()
  } else if (key === 'edit') {
    batchEditModal.visible = true
    batchEditModal.form = { tags: '', published: null }
  } else if (key === 'shop') {
    batchShopModal.visible = true
    batchShopModal.shopIds = []
  }
}

const handleBatchDelete = () => {
  Modal.confirm({
    title: '确认批量删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个产品吗？此操作不可恢复。`,
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await productApi.batchDeleteProducts(selectedRowKeys.value)
        message.success('批量删除成功')
        selectedRowKeys.value = [] // 清空选择
        fetchProducts()
      } catch (error) {
        console.error('批量删除失败:', error)
        message.error('批量删除失败')
      }
    }
  })
}

const handleBatchEditSubmit = async () => {
  batchEditModal.loading = true
  try {
    await productApi.batchUpdateProducts({
      ids: selectedRowKeys.value,
      tags: batchEditModal.form.tags,
      published: batchEditModal.form.published
    })
    message.success('批量更新成功')
    batchEditModal.visible = false
    selectedRowKeys.value = []
    fetchProducts()
  } catch (error) {
    console.error('批量更新失败:', error)
    message.error('批量更新失败')
  } finally {
    batchEditModal.loading = false
  }
}

const handleBatchShopSubmit = async () => {
  batchShopModal.loading = true
  try {
    await productApi.batchUpdateProductShops({
      productIds: selectedRowKeys.value,
      shopIds: batchShopModal.shopIds
    })
    message.success('批量分配商店成功')
    batchShopModal.visible = false
    selectedRowKeys.value = []
    fetchProducts()
  } catch (error) {
    console.error('批量分配商店失败:', error)
    message.error('批量分配商店失败')
  } finally {
    batchShopModal.loading = false
  }
}
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

.filter-bar {
  margin-bottom: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
}

/* 产品信息样式 */
.product-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #f0f0f0;
}

.product-image-placeholder {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  color: #bbb;
  font-size: 24px;
}

.product-title {
  flex: 1;
  min-width: 0;
}

.product-handle {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
}

/* 价格样式 */
.price-current {
  color: #ff4d4f;
  font-weight: 600;
  font-size: 16px;
}

.price-compare {
  color: #999;
  font-size: 12px;
  text-decoration: line-through;
}

/* 标签和商店样式 */
.tags-wrapper,
.shops-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.text-muted {
  color: #999;
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
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8e8e8;
}

.mobile-product-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.mobile-product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.product-name {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.mobile-list .mobile-card .card-body .info-row {
  display: flex;
  margin-bottom: 8px;
  font-size: 14px;
}

.mobile-list .mobile-card .card-body .info-row .label {
  color: #666;
  flex-shrink: 0;
  margin-right: 8px;
  width: 60px;
}

.mobile-list .mobile-card .card-body .info-row .value {
  color: #333;
  flex: 1;
  min-width: 0;
}

.mobile-list .mobile-card .card-actions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e8e8e8;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
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

  .filter-bar {
    padding: 12px;
  }
}
</style>
