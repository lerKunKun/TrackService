<template>
  <div class="page-container">
    <div class="content-card">
      <!-- 工具栏 -->
      <div class="toolbar">
        <h2>店铺列表</h2>
        <a-space>
          <a-button type="primary" @click="showModal">
            <PlusOutlined />
            添加店铺
          </a-button>
          <a-button type="primary" ghost @click="showOAuthModal">
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
          <a-space>
            <a-button type="link" size="small" @click="handleEdit(record)">
              编辑
            </a-button>
            <a-popconfirm
              title="确定要删除该店铺吗？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="handleDelete(record.id)"
            >
              <a-button type="link" danger size="small">
                删除
              </a-button>
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
                <a-button type="link" size="small" @click="handleEdit(item)">
                  编辑
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

    <!-- 添加/编辑店铺弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="600px"
      :confirm-loading="confirmLoading"
      @ok="handleSubmit"
      @cancel="handleCancel"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-item label="店铺名称" name="shopName">
          <a-input v-model:value="formState.shopName" placeholder="请输入店铺名称" />
        </a-form-item>

        <a-form-item label="平台类型" name="platform">
          <a-select v-model:value="formState.platform" placeholder="请选择平台类型">
            <a-select-option value="shopify">Shopify</a-select-option>
            <a-select-option value="shopline">Shopline</a-select-option>
            <a-select-option value="tiktokshop">TikTok Shop</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="店铺URL" name="storeUrl">
          <a-input
            v-model:value="formState.storeUrl"
            placeholder="https://yourstore.myshopify.com"
          />
        </a-form-item>

        <a-form-item label="API Key" name="apiKey">
          <a-input v-model:value="formState.apiKey" placeholder="请输入API Key" />
        </a-form-item>

        <a-form-item label="API Secret" name="apiSecret">
          <a-input-password
            v-model:value="formState.apiSecret"
            placeholder="请输入API Secret"
          />
        </a-form-item>

        <a-form-item label="Access Token" name="accessToken">
          <a-input-password
            v-model:value="formState.accessToken"
            placeholder="请输入Access Token"
          />
        </a-form-item>

        <a-form-item label="时区" name="timezone">
          <a-input v-model:value="formState.timezone" placeholder="UTC" />
        </a-form-item>
      </a-form>
    </a-modal>

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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { message, Grid } from 'ant-design-vue'
import { PlusOutlined, ShopOutlined } from '@ant-design/icons-vue'
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
    width: 200
  },
  {
    title: '平台',
    dataIndex: 'platform',
    width: 120,
    slots: { customRender: 'platform' }
  },
  {
    title: '店铺URL',
    dataIndex: 'storeUrl',
    ellipsis: true
  },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    width: 180,
    slots: { customRender: 'createdAt' }
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
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

const modalVisible = ref(false)
const confirmLoading = ref(false)
const formRef = ref()
const editingId = ref(null)

const formState = reactive({
  shopName: '',
  platform: undefined,
  storeUrl: '',
  apiKey: '',
  apiSecret: '',
  accessToken: '',
  timezone: 'UTC'
})

const rules = {
  shopName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  platform: [{ required: true, message: '请选择平台类型', trigger: 'change' }],
  apiKey: [{ required: true, message: '请输入API Key', trigger: 'blur' }],
  apiSecret: [{ required: true, message: '请输入API Secret', trigger: 'blur' }],
  accessToken: [{ required: true, message: '请输入Access Token', trigger: 'blur' }]
}

const modalTitle = computed(() => {
  return editingId.value ? '编辑店铺' : '添加店铺'
})

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

// 显示弹窗
const showModal = () => {
  editingId.value = null
  resetForm()
  modalVisible.value = true
}

// 编辑
const handleEdit = (record) => {
  editingId.value = record.id
  Object.assign(formState, {
    shopName: record.shopName,
    platform: record.platform,
    storeUrl: record.storeUrl,
    apiKey: '',
    apiSecret: '',
    accessToken: '',
    timezone: record.timezone || 'UTC'
  })
  modalVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    confirmLoading.value = true

    if (editingId.value) {
      await shopApi.update(editingId.value, formState)
      message.success('更新成功')
    } else {
      await shopApi.create(formState)
      message.success('创建成功')
    }

    modalVisible.value = false
    fetchShops()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    confirmLoading.value = false
  }
}

// 取消
const handleCancel = () => {
  modalVisible.value = false
  resetForm()
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

// 重置表单
const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(formState, {
    shopName: '',
    platform: undefined,
    storeUrl: '',
    apiKey: '',
    apiSecret: '',
    accessToken: '',
    timezone: 'UTC'
  })
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
