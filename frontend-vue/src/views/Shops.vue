<template>
  <div class="page-container">
    <div class="content-card">
      <!-- 工具栏 -->
      <div class="toolbar">
        <h2>店铺列表</h2>
        <a-space>
          <a-button type="primary" @click="showOAuthModal">
            <ShopOutlined />
            Shopify OAuth授权
          </a-button>
        </a-space>
      </div>

      <!-- 表格 -->
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
        <!-- 平台类型标签 -->
        <template #platform="{ record }">
          <a-tag :color="getPlatformColor(record.platform)">
            {{ record.platform.toUpperCase() }}
          </a-tag>
        </template>

        <!-- 操作列 -->
        <template #action="{ record }">
          <a-space :size="4">
            <a-tooltip title="查看详情">
              <a-button type="link" size="small" @click="handleViewDetails(record)">
                <EyeOutlined />
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.platform === 'shopify'" title="刷新店铺信息">
              <a-button type="link" size="small" @click="handleRefreshShopInfo(record.id)">
                <ReloadOutlined />
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.platform === 'shopify'" title="注册Webhooks">
              <a-button type="link" size="small" @click="handleRegisterWebhooks(record.id)">
                <ApiOutlined />
              </a-button>
            </a-tooltip>
            <a-popconfirm
              title="确定要删除该店铺吗？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="handleDelete(record.id)"
            >
              <a-tooltip title="删除店铺">
                <a-button type="link" danger size="small">
                  <DeleteOutlined />
                </a-button>
              </a-tooltip>
            </a-popconfirm>
          </a-space>
        </template>

        <!-- 时间格式化 -->
        <template #createdAt="{ record }">
          {{ formatDate(record.createdAt) }}
        </template>
      </a-table>

      <!-- 卡片列表 (移动端) -->
      <div v-else class="mobile-list">
        <a-spin :spinning="loading">
          <div v-if="tableData.length > 0">
            <div v-for="item in tableData" :key="item.id" class="mobile-card">
              <div class="card-header">
                <span class="shop-name">{{ item.shopName }}</span>
                <a-tag :color="getPlatformColor(item.platform)">
                  {{ item.platform.toUpperCase() }}
                </a-tag>
              </div>
              <div class="card-body">
                <div class="info-row">
                  <span class="label">URL:</span>
                  <span class="value text-ellipsis">{{ item.storeUrl }}</span>
                </div>
                <div class="info-row">
                  <span class="label">创建时间:</span>
                  <span class="value">{{ formatDate(item.createdAt) }}</span>
                </div>
              </div>
              <div class="card-actions">
                <a-button type="link" size="small" @click="handleViewDetails(item)">
                  查看
                </a-button>
                <a-button 
                  v-if="item.platform === 'shopify'" 
                  type="link" 
                  size="small" 
                  @click="handleRefreshShopInfo(item.id)"
                >
                  刷新
                </a-button>
                <a-button 
                  v-if="item.platform === 'shopify'" 
                  type="link" 
                  size="small" 
                  @click="handleRegisterWebhooks(item.id)"
                >
                  注册Webhooks
                </a-button>
                <a-popconfirm
                  title="确定要删除该店铺吗？"
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

    <!-- Shopify OAuth授权弹窗 -->
    <a-modal
      v-model:open="oauthModalVisible"
      title="Shopify OAuth授权"
      width="500px"
      :confirm-loading="oauthLoading"
      @ok="handleOAuthSubmit"
      @cancel="handleOAuthCancel"
    >
      <a-form
        :label-col="{ span: 7 }"
        :wrapper-col="{ span: 17 }"
      >
        <a-form-item label="Shopify店铺域名">
          <a-input
            v-model:value="shopifyDomain"
            placeholder="例如: mystore.myshopify.com"
          />
          <div style="color: #999; font-size: 12px; margin-top: 4px">
            请输入您的Shopify店铺完整域名
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 商店详情弹窗 -->
    <a-modal
      v-model:open="detailModalVisible"
      title="商店详细信息"
      width="700px"
      :footer="null"
    >
      <a-spin :spinning="detailLoading">
        <a-descriptions v-if="shopDetails" :column="2" bordered size="small">
          <a-descriptions-item label="店铺名称">
            {{ shopDetails.shopName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="平台">
            <a-tag :color="getPlatformColor(shopDetails.platform)">
              {{ shopDetails.platform?.toUpperCase() }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="货币">
            {{ shopDetails.currency || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="计划">
            {{ shopDetails.planDisplayName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="Shopify Plus">
            <a-tag :color="shopDetails.isShopifyPlus ? 'green' : 'default'">
              {{ shopDetails.isShopifyPlus ? '是' : '否' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="联系邮箱">
            {{ shopDetails.contactEmail || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="店主邮箱">
            {{ shopDetails.ownerEmail || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="主域名">
            {{ shopDetails.primaryDomain || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="时区">
            {{ shopDetails.ianaTimezone || shopDetails.timezone || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="店铺URL" :span="2">
            {{ shopDetails.storeUrl || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ formatDate(shopDetails.createdAt) }}
          </a-descriptions-item>
          <a-descriptions-item label="更新时间">
            {{ formatDate(shopDetails.updatedAt) }}
          </a-descriptions-item>
        </a-descriptions>
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { message, Grid } from 'ant-design-vue'
import { 
  PlusOutlined, 
  ShopOutlined, 
  EyeOutlined, 
  ReloadOutlined, 
  ApiOutlined, 
  DeleteOutlined 
} from '@ant-design/icons-vue'
import { shopApi } from '@/api/shop'
import dayjs from 'dayjs'

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80
  },
  {
    title: '店铺名称',
    dataIndex: 'shopName',
    width: 180
  },
  {
    title: '平台',
    dataIndex: 'platform',
    width: 100,
    slots: { customRender: 'platform' }
  },
  {
    title: '货币',
    dataIndex: 'currency',
    width: 80
  },
  {
    title: '计划',
    dataIndex: 'planDisplayName',
    width: 120,
    ellipsis: true
  },
  {
    title: '联系邮箱',
    dataIndex: 'contactEmail',
    width: 180,
    ellipsis: true
  },
  {
    title: '店铺URL',
    dataIndex: 'storeUrl',
    ellipsis: true
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
    width: 200,
    fixed: 'right',
    slots: { customRender: 'action' }
  }
]

const loading = ref(false)
const tableData = ref([])

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

const useBreakpoint = Grid.useBreakpoint
const screens = useBreakpoint()
const isMobile = computed(() => !screens.value.md)

// 获取店铺列表
const fetchShops = async () => {
  loading.value = true
  try {
    const data = await shopApi.getList({
      page: pagination.current,
      pageSize: pagination.pageSize
    })
    tableData.value = data.list || []
    pagination.total = data.total || 0
  } catch (error) {
    console.error('获取店铺列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 表格分页变化
const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchShops()
}

// 删除
const handleDelete = async (id) => {
  try {
    await shopApi.delete(id)
    message.success('删除成功')
    fetchShops()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

// 获取平台颜色
const getPlatformColor = (platform) => {
  const colors = {
    shopify: 'green',
    shopline: 'blue',
    tiktokshop: 'purple'
  }
  return colors[platform?.toLowerCase()] || 'default'
}

// 格式化日期
const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'
}

// Shopify OAuth相关
const oauthModalVisible = ref(false)
const oauthLoading = ref(false)
const shopifyDomain = ref('')

// 显示OAuth弹窗
const showOAuthModal = () => {
  shopifyDomain.value = ''
  oauthModalVisible.value = true
}

// 处理OAuth授权
const handleOAuthSubmit = () => {
  if (!shopifyDomain.value) {
    message.warning('请输入Shopify店铺域名')
    return
  }

  // 验证域名格式
  if (!shopifyDomain.value.endsWith('.myshopify.com')) {
    message.warning('请输入正确的Shopify域名格式 (xxx.myshopify.com)')
    return
  }

  oauthLoading.value = true

  // 跳转到Shopify OAuth授权页面
  const authUrl = `/api/v1/oauth/shopify/authorize?shopDomain=${encodeURIComponent(shopifyDomain.value)}`
  window.location.href = authUrl
}

// 取消OAuth弹窗
const handleOAuthCancel = () => {
  oauthModalVisible.value = false
  shopifyDomain.value = ''
}

// 商店详情相关
const detailModalVisible = ref(false)
const detailLoading = ref(false)
const shopDetails = ref(null)

// 查看商店详情
const handleViewDetails = async (record) => {
  shopDetails.value = record
  detailModalVisible.value = true
}

// 刷新商店信息
const handleRefreshShopInfo = async (shopId) => {
  try {
    loading.value = true
    const response = await shopApi.refreshShopInfo(shopId)
    message.success('商店信息已刷新')
    await fetchShops()  // 重新加载列表
  } catch (error) {
    console.error('刷新商店信息失败:', error)
    message.error('刷新失败，请重试')
  } finally {
    loading.value = false
  }
}

// 注册Webhooks
const handleRegisterWebhooks = async (shopId) => {
  try {
    loading.value = true
    // 注意：data已经是后端返回的data字段（由Axios拦截器提取）
    const data = await shopApi.registerWebhooks(shopId)
    
    if (!data || data.totalSuccess === undefined) {
      throw new Error('响应数据格式异常')
    }
    
    const { totalSuccess, totalFailed } = data
    if (totalFailed === 0) {
      message.success(`Webhooks注册成功！`)
    } else if (totalSuccess === 0) {
      message.error(`Webhooks注册失败！${totalFailed}个注册失败`)
    } else {
      message.warning(`Webhooks部分注册成功：${totalSuccess}个成功，${totalFailed}个失败`)
    }
  } catch (error) {
    console.error('注册Webhooks失败:', error)
    message.error(error.message || '注册Webhooks失败，请重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchShops()

  // 检查URL中是否有OAuth回调参数
  const urlParams = new URLSearchParams(window.location.search)
  if (urlParams.get('oauth') === 'success') {
    message.success('Shopify店铺授权成功！')
    // 清除URL参数
    window.history.replaceState({}, document.title, window.location.pathname)
    fetchShops()
  } else if (urlParams.get('oauth') === 'error') {
    message.error('Shopify授权失败，请重试')
    window.history.replaceState({}, document.title, window.location.pathname)
  }
})
</script>

<style scoped>
.page-container {
  padding: 24px;
}

.content-card {
  background: white;
  border-radius: 8px;
  padding: 24px;
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

/* Mobile Styles */
.mobile-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mobile-card {
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
}

.mobile-card:active {
  background: #fafafa;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.shop-name {
  font-weight: 600;
  font-size: 16px;
  color: #262626;
}

.card-body {
  margin-bottom: 12px;
}

.info-row {
  display: flex;
  margin-bottom: 8px;
  font-size: 14px;
  line-height: 1.5;
}

.info-row .label {
  color: #8c8c8c;
  width: 70px;
  flex-shrink: 0;
}

.info-row .value {
  color: #262626;
  flex: 1;
  min-width: 0;
  word-break: break-all;
}

.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.card-actions .ant-btn {
  padding: 4px 12px;
  height: auto;
}

.mobile-pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

@media (max-width: 576px) {
  .toolbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .toolbar h2 {
    margin-bottom: 4px;
  }
  
  .page-container {
    padding: 12px;
  }
  
  .content-card {
    padding: 12px;
  }
}
</style>
