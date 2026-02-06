<template>
  <div class="procurement-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-section">
          <h1>采购管理</h1>
          <div class="stats-pills">
            <span 
              class="stat-pill" 
              :class="{ 'active': !filters.status }"
              @click="handleStatClick('')"
            >
              <span class="stat-label">总产品</span>
              <span class="stat-value">{{ stats.totalProducts }}</span>
            </span>
            <span 
              class="stat-pill" 
              :class="{ 'active': filters.status === 'complete' }"
              @click="handleStatClick('complete')"
            >
              <span class="stat-label">已完善</span>
              <span class="stat-value complete">{{ stats.completeProducts }}</span>
            </span>
            <span 
              class="stat-pill" 
              :class="{ 'active': filters.status === 'uncomplete' }"
              @click="handleStatClick('uncomplete')"
            >
              <span class="stat-label">未完善</span>
              <span class="stat-value incomplete">{{ stats.incompleteProducts }}</span>
            </span>
          </div>
        </div>
        <a-space size="small">
          <a-button @click="handleRefresh" :loading="loading">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <a-button 
            type="primary" 
            @click="handleBatchProcurement"
            :disabled="selectedVariantKeys.length === 0"
          >
            <template #icon><EditOutlined /></template>
            批量编辑 ({{ selectedVariantKeys.length }})
          </a-button>
        </a-space>
      </div>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filter-section">
      <a-form layout="inline" class="filter-form">
        <a-form-item class="search-item">
          <a-input
            v-model:value="filters.keyword"
            placeholder="搜索产品名称或SKU..."
            allow-clear
            @pressEnter="handleSearch"
            @change="debouncedSearch"
          >
            <template #prefix>
              <SearchOutlined />
            </template>
          </a-input>
        </a-form-item>
        
        <a-form-item>
          <a-select
            v-model:value="filters.supplier"
            placeholder="采购商"
            allow-clear
            @change="handleSearch"
            style="min-width: 160px"
          >
            <a-select-option value="">全部</a-select-option>
            <a-select-option v-for="supplier in suppliers" :key="supplier" :value="supplier">
              {{ supplier }}
            </a-select-option>
          </a-select>
        </a-form-item>
        
        <a-form-item>
          <a-select
            v-model:value="filters.status"
            placeholder="状态"
            allow-clear
            @change="handleSearch"
            style="min-width: 140px"
          >
            <a-select-option value="">全部</a-select-option>
            <a-select-option value="complete">已完善</a-select-option>
            <a-select-option value="incomplete">部分完善</a-select-option>
            <a-select-option value="uncomplete">全部未完善</a-select-option>
            <a-select-option value="no-info">无采购信息</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item>
          <a-button @click="handleResetFilters" :disabled="!hasActiveFilters">
            重置
          </a-button>
        </a-form-item>
      </a-form>

      <!-- 快捷操作栏 -->
      <div class="quick-actions" v-if="selectedVariantKeys.length > 0">
        <div class="selection-info">
          <CheckCircleOutlined />
          已选择 {{ selectedVariantKeys.length }} 个变体
        </div>
        <a-space size="small">
          <a-button size="small" @click="handleClearSelection">
            取消选择
          </a-button>
          <a-button size="small" type="primary" @click="handleBatchProcurement">
            批量编辑
          </a-button>
        </a-space>
      </div>
    </div>

    <!-- 产品列表 -->
    <div class="products-container">
      <a-spin :spinning="loading">
        <div v-if="productGroups.length === 0 && !loading" class="empty-state">
          <InboxOutlined class="empty-icon" />
          <p class="empty-text">{{ filters.keyword || filters.supplier || filters.status ? '没有找到匹配的产品' : '暂无产品数据' }}</p>
          <a-button v-if="hasActiveFilters" @click="handleResetFilters">
            清除筛选
          </a-button>
        </div>

        <div v-else class="product-list">
          <div 
            v-for="product in productGroups" 
            :key="product.key"
            class="product-card"
            :class="{ 'is-expanded': expandedRowKeys.includes(product.key) }"
          >
            <!-- 产品头部 -->
            <div class="product-header" @click="toggleExpand(product.key)">
              <div class="product-info">
                <div class="product-image-wrapper">
                  <img 
                    v-if="product.imageUrl" 
                    :src="product.imageUrl" 
                    :alt="product.title"
                    class="product-image"
                  />
                  <div v-else class="product-image-placeholder">
                    <PictureOutlined />
                  </div>
                </div>
                
                <div class="product-details">
                  <h3 class="product-title">{{ product.title }}</h3>
                  <div class="product-meta">
                    <a-tag color="blue" class="variant-count">
                      {{ product.variantCount }} 个变体
                    </a-tag>
                    <span class="status-badge" :class="getProductStatusClass(product)">
                      {{ getProductStatusText(product) }}
                    </span>
                    <span class="status-badge" v-if="product.publishStatus === 1" :class="'status-exported'">
                      已导出
                    </span>
                  </div>
                </div>
              </div>

              <div class="product-actions">
                <a-button 
                  type="text" 
                  size="small"
                  @click.stop="handleSelectAllVariants(product)"
                >
                  {{ areAllVariantsSelected(product) ? '取消全选' : '全选' }}
                </a-button>
                <DownOutlined 
                  class="expand-icon" 
                  :class="{ 'is-expanded': expandedRowKeys.includes(product.key) }"
                />
              </div>
            </div>

            <!-- 变体列表 -->
            <transition name="expand">
              <div v-show="expandedRowKeys.includes(product.key)" class="variants-container">
                <div 
                  v-for="variant in product.variants" 
                  :key="variant.key"
                  class="variant-row"
                  :class="{ 
                    'is-selected': selectedVariantKeys.includes(variant.key),
                    'is-complete': isVariantComplete(variant)
                  }"
                >
                  <a-checkbox
                    :checked="selectedVariantKeys.includes(variant.key)"
                    @change="(e) => handleVariantSelect(variant.key, e.target.checked)"
                    class="variant-checkbox"
                  />

                  <div class="variant-content">
                    <div class="variant-main">
                      <div class="variant-title-section">
                        <div v-if="getVariantOptions(variant).length > 0" class="variant-options-list">
                          <div v-for="(opt, idx) in getVariantOptions(variant)" :key="idx" class="variant-option-item">
                            <span class="opt-name">{{ opt.name }}:</span>
                            <span class="opt-value">{{ opt.value }}</span>
                          </div>
                        </div>
                        <span v-else class="variant-name">{{ formatVariantTitle(variant) }}</span>
                        
                        <div class="sku-tag-wrapper">
                          <a-tag v-if="variant.sku" size="small" color="blue">{{ variant.sku }}</a-tag>
                          <a-tag v-else size="small">无SKU</a-tag>
                        </div>
                      </div>

                      <div class="variant-procurement">
                        <div class="procurement-item">
                          <label>采购链接:</label>
                          <a 
                            v-if="variant.procurementUrl" 
                            :href="variant.procurementUrl" 
                            target="_blank"
                            class="procurement-link"
                            @click.stop
                          >
                            <LinkOutlined />
                            查看链接
                          </a>
                          <span v-else class="empty-value">未设置</span>
                        </div>

                        <div class="procurement-item">
                          <label>采购价:</label>
                          <span v-if="variant.procurementPrice" class="price-value">
                            ${{ variant.procurementPrice.toFixed(2) }}
                          </span>
                          <span v-else class="empty-value">未设置</span>
                        </div>

                        <div class="procurement-item">
                          <label>采购商:</label>
                          <a-tag v-if="variant.supplier" color="purple" size="small">
                            {{ variant.supplier }}
                          </a-tag>
                          <span v-else class="empty-value">未设置</span>
                        </div>

                        <div class="procurement-item" v-if="variant.price && variant.procurementPrice">
                          <label>利润:</label>
                          <span 
                            class="profit-value"
                            :class="getProfit(variant) > 0 ? 'positive' : 'negative'"
                          >
                            ${{ getProfit(variant).toFixed(2) }}
                            <span class="profit-percent">
                              ({{ getProfitMargin(variant) }}%)
                            </span>
                          </span>
                        </div>
                      </div>
                    </div>

                    <a-button 
                      type="link" 
                      size="small" 
                      @click="handleEdit(variant)"
                      class="edit-button"
                    >
                      编辑
                    </a-button>
                    <a-popconfirm
                      title="确定要删除这个变体吗？"
                      @confirm="handleDeleteVariant(variant)"
                    >
                      <a-button 
                        type="link" 
                        danger
                        size="small" 
                        class="delete-button"
                      >
                        删除
                      </a-button>
                    </a-popconfirm>
                  </div>
                </div>
              </div>
            </transition>
          </div>
        </div>
      </a-spin>

      <!-- 分页 -->
      <div v-if="productGroups.length > 0" class="pagination-wrapper">
        <a-pagination
          v-model:current="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :show-size-changer="true"
          :show-total="total => `共 ${total} 个产品`"
          :page-size-options="['5', '10', '20', '50']"
          @change="handlePageChange"
        />
      </div>
    </div>

    <!-- 编辑模态框 -->
    <a-modal
      v-model:open="editModalVisible"
      :title="editForm?.variantId ? '编辑采购信息' : ''"
      :confirm-loading="editLoading"
      @ok="handleEditSubmit"
      @cancel="handleEditCancel"
      width="600px"
      :destroy-on-close="true"
    >
      <a-form layout="vertical" v-if="editForm" :model="editForm">
        <!-- 产品信息预览 -->
        <div class="edit-product-preview">
          <div class="preview-title">{{ editForm.productTitle }}</div>
          <div v-if="editForm.variantTitle" class="preview-variant">
            变体: {{ editForm.variantTitle }}
          </div>
        </div>

        <a-divider />

        <!-- SKU -->
        <a-form-item label="SKU" name="sku">
          <a-input 
            v-model:value="editForm.sku" 
            placeholder="请输入产品SKU"
            allow-clear
          />
        </a-form-item>

        <!-- 采购链接 -->
        <a-form-item 
          label="采购链接" 
          name="procurementUrl"
          :rules="[
            { type: 'url', message: '请输入有效的URL地址' }
          ]"
        >
          <a-input 
            v-model:value="editForm.procurementUrl" 
            placeholder="https://example.com/product"
            allow-clear
          >
            <template #prefix>
              <LinkOutlined />
            </template>
          </a-input>
        </a-form-item>

        <!-- 采购价格 -->
        <a-form-item label="采购价格" name="procurementPrice">
          <a-input-number
            v-model:value="editForm.procurementPrice"
            :min="0"
            :precision="2"
            :step="0.01"
            placeholder="0.00"
            style="width: 100%"
          >
            <template #addonBefore>$</template>
          </a-input-number>
        </a-form-item>

        <!-- 采购商 -->
        <a-form-item label="采购商" name="supplier">
          <a-auto-complete
            v-model:value="editForm.supplier"
            :options="supplierOptions"
            placeholder="请输入或选择采购商"
            :filter-option="filterSupplier"
            allow-clear
          />
        </a-form-item>

        <!-- 价格对比 -->
        <a-divider>价格分析</a-divider>

        <div class="price-analysis">
          <div class="analysis-row">
            <span class="analysis-label">销售价格:</span>
            <span class="analysis-value price">${{ (editForm.price || 0).toFixed(2) }}</span>
          </div>
          
          <div class="analysis-row">
            <span class="analysis-label">采购价格:</span>
            <span class="analysis-value">
              ${{ (editForm.procurementPrice || 0).toFixed(2) }}
            </span>
          </div>
          
          <div v-if="editForm.price && editForm.procurementPrice" class="analysis-row highlight">
            <span class="analysis-label">预计利润:</span>
            <span 
              class="analysis-value profit"
              :class="(editForm.price - editForm.procurementPrice) > 0 ? 'positive' : 'negative'"
            >
              ${{ (editForm.price - editForm.procurementPrice).toFixed(2) }}
              <span class="margin">
                ({{ (((editForm.price - editForm.procurementPrice) / editForm.price) * 100).toFixed(1) }}%)
              </span>
            </span>
          </div>
        </div>
      </a-form>
    </a-modal>

    <!-- 批量编辑模态框 -->
    <a-modal
      v-model:open="batchModalVisible"
      title="批量编辑采购信息"
      :confirm-loading="batchLoading"
      @ok="handleBatchSubmit"
      @cancel="handleBatchCancel"
      width="600px"
      :destroy-on-close="true"
    >
      <a-alert
        :message="`已选择 ${selectedVariantKeys.length} 个变体`"
        type="info"
        show-icon
        style="margin-bottom: 20px"
      >
        <template #description>
          只有填写的字段会被更新,未填写的字段保持不变
        </template>
      </a-alert>

      <a-form layout="vertical" v-if="batchForm" :model="batchForm">
        <!-- 采购链接 -->
        <a-form-item label="采购链接" name="procurementUrl">
          <a-input 
            v-model:value="batchForm.procurementUrl" 
            placeholder="https://example.com/product (留空则不修改)"
            allow-clear
          >
            <template #prefix>
              <LinkOutlined />
            </template>
          </a-input>
          <div class="form-hint">所有选中变体将使用相同的采购链接</div>
        </a-form-item>

        <!-- 采购价格 -->
        <a-form-item label="采购价格" name="procurementPrice">
          <a-input-number
            v-model:value="batchForm.procurementPrice"
            :min="0"
            :precision="2"
            :step="0.01"
            placeholder="留空则不修改"
            style="width: 100%"
          >
            <template #addonBefore>$</template>
          </a-input-number>
          <div class="form-hint">所有选中变体将使用相同的采购价格</div>
        </a-form-item>

        <!-- 采购商 -->
        <a-form-item label="采购商" name="supplier">
          <a-auto-complete
            v-model:value="batchForm.supplier"
            :options="supplierOptions"
            placeholder="请输入或选择采购商 (留空则不修改)"
            :filter-option="filterSupplier"
            allow-clear
          />
          <div class="form-hint">所有选中变体将使用相同的采购商</div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  EditOutlined,
  LinkOutlined,
  DownOutlined,
  CheckCircleOutlined,
  InboxOutlined,
  PictureOutlined
} from '@ant-design/icons-vue'
import productApi from '@/api/product'

// 响应式数据
const loading = ref(false)
const productGroups = ref([])
const suppliers = ref([])
const selectedVariantKeys = ref([])
const expandedRowKeys = ref([])

// 统计数据
const statsData = reactive({
  totalCount: 0,
  completeCount: 0,
  incompleteCount: 0
})

// 筛选条件
const filters = reactive({
  keyword: '',
  supplier: '',
  status: ''
})

// 分页
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0  // 总数从服务端获取
})

// 编辑模态框
const editModalVisible = ref(false)
const editLoading = ref(false)
const editForm = ref(null)

// 批量编辑模态框
const batchModalVisible = ref(false)
const batchLoading = ref(false)
const batchForm = ref({
  procurementUrl: '',
  procurementPrice: null,
  supplier: ''
})

// 计算属性

// 统计数据
// 统计数据
const stats = computed(() => {
  return {
    totalProducts: statsData.totalCount,
    completeProducts: statsData.completeCount,
    incompleteProducts: statsData.incompleteCount
  }
})

// 是否有激活的筛选条件
const hasActiveFilters = computed(() => {
  return !!(filters.keyword || filters.supplier || filters.status)
})

// 供应商选项
const supplierOptions = computed(() => {
  return suppliers.value.map(s => ({ value: s }))
})

// 方法

// 跟踪正在加载变体的产品
const loadingVariants = ref(new Set())

// 懒加载变体详情
const loadVariantDetails = async (productId) => {
  if (loadingVariants.value.has(productId)) return
  
  loadingVariants.value.add(productId)
  try {
    const response = await productApi.getProductVariants(productId)
    const variants = response.data || []
    
    // Backfill option names
    const option1Name = variants.find(v => v.option1Name)?.option1Name
    const option2Name = variants.find(v => v.option2Name)?.option2Name
    const option3Name = variants.find(v => v.option3Name)?.option3Name
    
    variants.forEach(v => {
      if (!v.option1Name && option1Name) v.option1Name = option1Name
      if (!v.option2Name && option2Name) v.option2Name = option2Name
      if (!v.option3Name && option3Name) v.option3Name = option3Name
    })
    
    // 更新产品的variants
    const product = productGroups.value.find(p => p.productId === productId)
    if (product) {
      product.variants = variants.map(v => ({
        ...v,
        key: `variant-${v.id}`,
        variantId: v.id,
        productId: productId,
        productTitle: product.title
      }))
    }
  } catch (error) {
    console.error(`获取产品${productId}变体失败:`, error)
    message.error('加载变体失败')
  } finally {
    loadingVariants.value.delete(productId)
  }
}

// 获取产品列表（服务端分页）
const fetchProcurementList = async () => {
  loading.value = true
  try {
    const response = await productApi.getProcurementList({
      page: pagination.current,
      pageSize: pagination.pageSize,
      keyword: filters.keyword || null,
      status: filters.status || null
    })
    
    if (response.success && response.data) {
      productGroups.value = response.data.list.map(product => ({
        key: `product-${product.id}`,
        productId: product.id,
        title: product.title,
        imageUrl: product.imageUrl,
        variantCount: product.variantCount,
        allComplete: product.variantCount > 0 && 
                     product.completeVariantCount === product.variantCount,
        hasProcurement: product.completeVariantCount > 0,
        publishStatus: product.publishStatus,
        lastExportTime: product.lastExportTime,
        variants: []  // 初始为空，展开时才加载
      }))
      
      pagination.total = response.data.total
      
      // 从所有变体中提取供应商（需要单独查询或保留现有逻辑）
      // 注意：由于现在不加载所有变体，供应商筛选可能需要后端支持
      // 这里暂时保留现有逻辑，如果需要供应商筛选功能，需要后端返回供应商列表
    }
  } catch (error) {
    console.error('获取采购信息列表失败:', error)
    message.error('获取数据失败,请重试')
  } finally {
    loading.value = false
  }
}

// 获取统计信息
const fetchStats = async () => {
    try {
        const response = await productApi.getProcurementStats({
            keyword: filters.keyword || null
        })
        if (response.success && response.data) {
            statsData.totalCount = response.data.totalCount
            statsData.completeCount = response.data.completeCount
            statsData.incompleteCount = response.data.incompleteCount
        }
    } catch (error) {
        console.error('获取统计信息失败:', error)
    }
}

// 防抖搜索
let searchTimeout = null
const debouncedSearch = () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    handleSearch()
  }, 500)
}

// 搜索（重置分页并重新加载）
const handleSearch = () => {
  pagination.current = 1
  fetchProcurementList()
  fetchStats()
}

// 重置筛选
const handleResetFilters = () => {
  filters.keyword = ''
  filters.supplier = ''
  filters.status = ''
  handleSearch()
}

// 刷新
const handleRefresh = () => {
  handleResetFilters()
}

// 切换展开（懒加载变体）
const toggleExpand = async (key) => {
  const index = expandedRowKeys.value.indexOf(key)
  if (index > -1) {
    // 收起
    expandedRowKeys.value.splice(index, 1)
  } else {
    // 展开 - 先检查是否已加载变体
    const product = productGroups.value.find(p => p.key === key)
    if (product && product.variants.length === 0) {
      await loadVariantDetails(product.productId)
    }
    expandedRowKeys.value.push(key)
  }
}

// 分页改变（重新加载数据）
const handlePageChange = (page, pageSize) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchProcurementList()
}

// 变体选择
const handleVariantSelect = (key, checked) => {
  if (checked) {
    if (!selectedVariantKeys.value.includes(key)) {
      selectedVariantKeys.value.push(key)
    }
  } else {
    const index = selectedVariantKeys.value.indexOf(key)
    if (index > -1) {
      selectedVariantKeys.value.splice(index, 1)
    }
  }
}

// 全选产品的所有变体
const handleSelectAllVariants = (product) => {
  const variantKeys = product.variants.map(v => v.key)
  const allSelected = variantKeys.every(key => selectedVariantKeys.value.includes(key))
  
  if (allSelected) {
    // 取消选择
    selectedVariantKeys.value = selectedVariantKeys.value.filter(
      key => !variantKeys.includes(key)
    )
  } else {
    // 全选
    variantKeys.forEach(key => {
      if (!selectedVariantKeys.value.includes(key)) {
        selectedVariantKeys.value.push(key)
      }
    })
  }
}

// 检查产品的所有变体是否都被选中
const areAllVariantsSelected = (product) => {
  const variantKeys = product.variants.map(v => v.key)
  return variantKeys.length > 0 && variantKeys.every(key => selectedVariantKeys.value.includes(key))
}

// 清除选择
const handleClearSelection = () => {
  selectedVariantKeys.value = []
}

// 判断变体是否完整
const isVariantComplete = (variant) => {
  return !!(variant.sku && variant.procurementUrl && variant.procurementPrice && variant.supplier)
}

// 获取产品状态类名
const getProductStatusClass = (product) => {
  if (product.allComplete) return 'status-complete'
  if (product.hasProcurement) return 'status-incomplete'
  return 'status-empty'
}

// 获取产品状态文本
const getProductStatusText = (product) => {
  if (product.allComplete) return '已完善'
  if (product.hasProcurement) return '部分完善'
  return '无采购信息'
}

// 获取变体属性列表
const getVariantOptions = (variant) => {
    const options = []
    
    if (variant.option1Value && variant.option1Value !== 'Default Title') {
        // 如果有name就显示name，没有就只显示value
        options.push({
            name: variant.option1Name || 'Option 1',
            value: variant.option1Value
        })
    }
    if (variant.option2Value) {
        options.push({
            name: variant.option2Name || 'Option 2',
            value: variant.option2Value
        })
    }
    if (variant.option3Value) {
        options.push({
            name: variant.option3Name || 'Option 3',
            value: variant.option3Value
        })
    }
    
    return options
}

// 格式化变体标题 (用于纯文本展示，如编辑框)
const formatVariantTitle = (variant) => {
    const options = getVariantOptions(variant)
    if (options.length > 0) {
        return options.map(o => `${o.name}:${o.value}`).join(' ')
    }
    
    if (variant.title && variant.title !== 'Default Title' && variant.title !== 'Default Variant') {
        return variant.title
    }
    return '单变体'
}

// 计算利润
const getProfit = (variant) => {
  if (!variant.price || !variant.procurementPrice) return 0
  return variant.price - variant.procurementPrice
}

// 计算利润率
const getProfitMargin = (variant) => {
  if (!variant.price || !variant.procurementPrice) return '0.0'
  const margin = ((variant.price - variant.procurementPrice) / variant.price) * 100
  return margin.toFixed(1)
}

// 统计点击筛选
const handleStatClick = (status) => {
    filters.status = status
    handleSearch()
}

// 编辑单个变体
const handleEdit = (variant) => {
  editForm.value = {
    variantId: variant.variantId || variant.id,
    productId: variant.productId,
    productTitle: variant.productTitle,
    variantTitle: formatVariantTitle(variant),
    sku: variant.sku || '',
    price: variant.price || 0,
    procurementUrl: variant.procurementUrl || '',
    procurementPrice: variant.procurementPrice || null,
    supplier: variant.supplier || ''
  }
  editModalVisible.value = true
}

// 删除变体
const handleDeleteVariant = async (variant) => {
  const variantId = variant.variantId || variant.id
  try {
    await productApi.deleteVariant(variantId)
    message.success('删除成功')
    
    // 从列表中移除
    const product = productGroups.value.find(p => p.productId === variant.productId)
    if (product) {
      product.variants = product.variants.filter(v => (v.variantId || v.id) !== variantId)
      product.variantCount = Math.max(0, product.variantCount - 1)
      if (product.variants.length === 0) {
        // 如果没有变体了，重新加载列表或者移除产品
         fetchProcurementList()
      }
    }
  } catch (error) {
    console.error('删除变体失败:', error)
    message.error('删除失败,请重试')
  }
}

// 提交编辑
const handleEditSubmit = async () => {
  editLoading.value = true
  try {
    // 只提交非空字段
    const updateData = {}
    if (editForm.value.sku) updateData.sku = editForm.value.sku
    if (editForm.value.procurementUrl) updateData.procurementUrl = editForm.value.procurementUrl
    if (editForm.value.procurementPrice !== null) updateData.procurementPrice = editForm.value.procurementPrice
    if (editForm.value.supplier) updateData.supplier = editForm.value.supplier
    
    await productApi.updateVariantProcurement(editForm.value.variantId, updateData)
    
    message.success('更新成功')
    editModalVisible.value = false
    await fetchProcurementList()
  } catch (error) {
    console.error('更新采购信息失败:', error)
    message.error('更新失败,请重试')
  } finally {
    editLoading.value = false
  }
}

// 取消编辑
const handleEditCancel = () => {
  editModalVisible.value = false
  editForm.value = null
}

// 批量编辑
const handleBatchProcurement = () => {
  if (selectedVariantKeys.value.length === 0) {
    message.warning('请先选择要编辑的变体')
    return
  }
  batchForm.value = {
    procurementUrl: '',
    procurementPrice: null,
    supplier: ''
  }
  batchModalVisible.value = true
}

// 提交批量编辑
const handleBatchSubmit = async () => {
  // 检查是否至少填写了一个字段
  const hasUpdate = !!(
    batchForm.value.procurementUrl ||
    batchForm.value.procurementPrice ||
    batchForm.value.supplier
  )
  
  if (!hasUpdate) {
    message.warning('请至少填写一个字段')
    return
  }
  
  batchLoading.value = true
  try {
    const updateData = {}
    if (batchForm.value.procurementUrl) updateData.procurementUrl = batchForm.value.procurementUrl
    if (batchForm.value.procurementPrice !== null) updateData.procurementPrice = batchForm.value.procurementPrice
    if (batchForm.value.supplier) updateData.supplier = batchForm.value.supplier
    
    const updates = selectedVariantKeys.value.map(key => {
      const variantId = parseInt(key.split('-')[1])
      return productApi.updateVariantProcurement(variantId, updateData)
    })
    
    await Promise.all(updates)
    
    message.success(`成功更新 ${selectedVariantKeys.value.length} 个变体`)
    batchModalVisible.value = false
    selectedVariantKeys.value = []
    await fetchProcurementList()
  } catch (error) {
    console.error('批量更新失败:', error)
    message.error('批量更新失败,请重试')
  } finally {
    batchLoading.value = false
  }
}

// 取消批量编辑
const handleBatchCancel = () => {
  batchModalVisible.value = false
  batchForm.value = {
    procurementUrl: '',
    procurementPrice: null,
    supplier: ''
  }
}

// 供应商筛选
const filterSupplier = (input, option) => {
  return option.value.toLowerCase().includes(input.toLowerCase())
}

// 页面挂载时加载数据
// 页面挂载时加载数据
onMounted(() => {
  fetchProcurementList()
  fetchStats()
})
</script>

<style scoped>
/* existing styles */

/* New status style */
.status-badge.status-exported {
  background-color: #52c41a;
  color: #fff;
}

/* 使用更现代的配色方案 */
.procurement-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  padding: 24px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* 页面头部 */
.page-header {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-section h1 {
  margin: 0 0 12px 0;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
}

.stats-pills {
  display: flex;
  gap: 12px;
}

.stat-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: #f5f7fa;
  border-radius: 20px;
  font-size: 13px;
}

.stat-label {
  color: #666;
}

.stat-value {
  font-weight: 600;
  color: #1a1a1a;
}

.stat-value.complete {
  color: #52c41a;
}

.stat-value.incomplete {
  color: #faad14;
}

/* 筛选区域 */
.filter-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.filter-form {
  margin-bottom: 0;
}

.search-item {
  flex: 1;
  max-width: 400px;
}

.search-item :deep(.ant-input) {
  border-radius: 8px;
}

/* 快捷操作栏 */
.quick-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding: 12px 16px;
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 8px;
}

.selection-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0958d9;
  font-weight: 500;
  font-size: 14px;
}

/* 产品容器 */
.products-container {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  min-height: 400px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
}

.empty-icon {
  font-size: 64px;
  color: #d9d9d9;
  margin-bottom: 16px;
}

.empty-text {
  color: #999;
  font-size: 16px;
  margin-bottom: 16px;
}

/* 产品列表 */
.product-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 产品卡片 */
.product-card {
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  overflow: hidden;
  transition: all 0.3s ease;
  background: white;
}

.product-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #d9d9d9;
}

.product-card.is-expanded {
  border-color: #1890ff;
}

/* 产品头部 */
.product-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.product-header:hover {
  background-color: #fafafa;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.product-image-wrapper {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
}

.product-image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 8px;
  color: #bbb;
  font-size: 24px;
}

.product-details {
  flex: 1;
  min-width: 0;
}

.product-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.variant-count {
  margin: 0;
}

.status-badge {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.status-complete {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.status-badge.status-incomplete {
  background: #fffbe6;
  color: #faad14;
  border: 1px solid #ffe58f;
}

.status-badge.status-empty {
  background: #f5f5f5;
  color: #999;
  border: 1px solid #d9d9d9;
}

.product-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.expand-icon {
  font-size: 12px;
  color: #999;
  transition: transform 0.3s ease;
}

.expand-icon.is-expanded {
  transform: rotate(180deg);
}

/* 变体容器 */
.variants-container {
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

/* 展开动画 */
.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  max-height: 2000px;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
}

/* 变体行 */
.variant-row {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s;
  background: white;
}

.variant-row:last-child {
  border-bottom: none;
}

.variant-row:hover {
  background-color: #f5f9ff;
}

.variant-row.is-selected {
  background-color: #e6f7ff;
}

.variant-row.is-complete {
  border-left: 3px solid #52c41a;
}

.variant-checkbox {
  margin-right: 16px;
}

.variant-content {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.variant-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.variant-title-section {
  display: flex;
  align-items: center;
  gap: 8px;
}

.variant-name {
  font-weight: 500;
  color: #1a1a1a;
  font-size: 14px;
}

.variant-procurement {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.procurement-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.procurement-item label {
  color: #666;
  font-weight: 500;
  margin: 0;
}

.procurement-link {
  color: #1890ff;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: color 0.2s;
}

.procurement-link:hover {
  color: #40a9ff;
  text-decoration: underline;
}

.empty-value {
  color: #bbb;
  font-style: italic;
}

.price-value {
  font-weight: 600;
  color: #1a1a1a;
}

.profit-value {
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.profit-value.positive {
  color: #52c41a;
}

.profit-value.negative {
  color: #ff4d4f;
}

.profit-percent {
  font-size: 12px;
  opacity: 0.8;
}

.edit-button {
  flex-shrink: 0;
}

/* 分页 */
.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

/* 模态框样式 */
.edit-product-preview {
  padding: 16px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  border-radius: 8px;
  margin-bottom: 16px;
}

.preview-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.preview-variant {
  font-size: 13px;
  color: #666;
}

.form-hint {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
}

.price-analysis {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
}

.analysis-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.analysis-row.highlight {
  margin-top: 8px;
  padding-top: 12px;
  border-top: 2px solid #e8e8e8;
}

.analysis-label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.analysis-value {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.analysis-value.price {
  color: #1890ff;
}

.analysis-value.profit {
  font-size: 20px;
}

.analysis-value.profit.positive {
  color: #52c41a;
}

.analysis-value.profit.negative {
  color: #ff4d4f;
}

.analysis-value .margin {
  font-size: 14px;
  margin-left: 8px;
  opacity: 0.8;
}

/* 响应式 */
@media (max-width: 768px) {
  .procurement-page {
    padding: 12px;
  }

  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .filter-form {
    flex-direction: column;
  }

  .search-item {
    max-width: 100%;
  }

  .variant-procurement {
    flex-direction: column;
    gap: 8px;
  }

  .product-title {
    white-space: normal;
  }
}
  .variant-options-list {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  
  .variant-option-item {
    font-size: 13px;
    line-height: 1.4;
    color: #333;
  }
  
  .opt-name {
    color: #666;
    margin-right: 4px;
    font-weight: 500;
  }
  
  .opt-value {
    color: #111;
    font-weight: 600;
  }
  
  .sku-tag-wrapper {
    margin-top: 4px;
  }
  
  /* Stat Pills Interactive */
  .stat-pill {
    cursor: pointer;
    transition: all 0.3s;
    border: 1px solid transparent;
  }

  .stat-pill:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  }

  .stat-pill.active {
    background: #e6f7ff;
    border-color: #1890ff;
  }
</style>
