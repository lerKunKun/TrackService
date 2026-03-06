<template>
  <div class="product-template-container">
    <a-card :bordered="false">
      <template #title>
        <div class="card-header">
          <span>产品模板管理</span>
          <div class="header-actions">
            <a-tag v-if="devStore" color="green">
              开发店铺: {{ devStore.shopName }}
            </a-tag>
            <a-tag v-else color="red">未设置开发店铺</a-tag>
          </div>
        </div>
      </template>

      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="productId"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <!-- 模板名称（inline 编辑） -->
          <template v-if="column.key === 'templateName'">
            <div class="inline-edit" @click="startEditField(record, 'templateName')">
              <a-input
                v-if="record._editingField === 'templateName'"
                v-model:value="record._editTemplateName"
                size="small"
                placeholder="例如 custom-bra"
                @blur="saveField(record, 'templateName')"
                @pressEnter="saveField(record, 'templateName')"
                :ref="el => setInputRef(el, record.productId, 'templateName')"
              />
              <span v-else class="editable-text">
                {{ record.templateName || '点击设置' }}
                <EditOutlined class="edit-icon" />
              </span>
            </div>
          </template>

          <!-- 源店铺（下拉选择） -->
          <template v-else-if="column.key === 'sourceShop'">
            <a-select
              v-model:value="record._editSourceShopId"
              size="small"
              style="width: 100%"
              placeholder="选择源店铺"
              allowClear
              :options="shopOptions"
              @change="val => saveSourceShop(record, val)"
            />
          </template>

          <!-- 模板版本 -->
          <template v-else-if="column.key === 'templateVersion'">
            <span>{{ record.templateVersion || '-' }}</span>
          </template>

          <!-- 状态 -->
          <template v-else-if="column.key === 'status'">
            <div class="status-info">
              <div v-if="record.lastPullTime" class="status-item">
                <CloudDownloadOutlined />
                <span class="status-time">{{ formatTime(record.lastPullTime) }}</span>
              </div>
              <div v-if="record.lastPushTime" class="status-item">
                <CloudUploadOutlined />
                <span class="status-time">{{ formatTime(record.lastPushTime) }}</span>
              </div>
              <span v-if="!record.lastPullTime && !record.lastPushTime" class="muted">-</span>
            </div>
          </template>

          <!-- 操作 -->
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-tooltip title="拉取主题文件">
                <a-button
                  size="small"
                  :loading="record._pulling"
                  :disabled="!record._editSourceShopId || !record.templateName"
                  @click="handlePull(record)"
                >
                  <template #icon><CloudDownloadOutlined /></template>
                  拉取
                </a-button>
              </a-tooltip>
              <a-tooltip title="推送并预览">
                <a-button
                  type="primary"
                  size="small"
                  :loading="record._previewing"
                  :disabled="!devStore"
                  @click="handlePreview(record)"
                >
                  <template #icon><EyeOutlined /></template>
                  预览
                </a-button>
              </a-tooltip>
              <a-tooltip title="查看主题文件">
                <a-button
                  type="text"
                  size="small"
                  @click="handleViewFiles(record)"
                >
                  <template #icon><FileSearchOutlined /></template>
                </a-button>
              </a-tooltip>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 主题文件查看弹窗 -->
    <a-modal
      v-model:open="themeFileModal.visible"
      title="已缓存的主题文件"
      :footer="null"
      width="640px"
    >
      <a-descriptions :column="1" bordered size="small" v-if="themeFileModal.data">
        <a-descriptions-item label="模板名称">
          {{ themeFileModal.data.templateName || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="settings_data.json">
          <a-tag :color="themeFileModal.data.hasSettings ? 'green' : 'default'">
            {{ themeFileModal.data.hasSettings ? '已拉取' : '未拉取' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="footer-group.json">
          <a-tag :color="themeFileModal.data.hasFooterGroup ? 'green' : 'default'">
            {{ themeFileModal.data.hasFooterGroup ? '已拉取' : '未拉取' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="product.xxx.json">
          <a-tag :color="themeFileModal.data.hasProductJson ? 'green' : 'default'">
            {{ themeFileModal.data.hasProductJson ? '已拉取' : '未拉取' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="最近拉取">
          {{ themeFileModal.data.lastPullTime || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="最近推送">
          {{ themeFileModal.data.lastPushTime || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="开发店铺产品 ID">
          {{ themeFileModal.data.devProductId || '-' }}
        </a-descriptions-item>
      </a-descriptions>
      <a-empty v-else description="暂无数据" />
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import {
  EyeOutlined, EditOutlined,
  CloudDownloadOutlined, CloudUploadOutlined,
  FileSearchOutlined
} from '@ant-design/icons-vue'
import {
  getProductTemplateList, updateProductTemplateInfo,
  pullThemeFiles, previewProductTemplate, getThemeFiles,
  getDevStore
} from '@/api/product-media-template'
import { shopApi } from '@/api/shop'

const loading = ref(false)
const tableData = ref([])
const shopOptions = ref([])
const devStore = ref(null)

const inputRefs = {}
function setInputRef(el, productId, field) {
  if (el) inputRefs[`${productId}_${field}`] = el
}

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: total => `共 ${total} 条`
})

const columns = [
  { title: '产品名称', dataIndex: 'productName', key: 'productName', width: 220, ellipsis: true },
  { title: '模板名称', key: 'templateName', width: 180 },
  { title: '模板版本', key: 'templateVersion', width: 100 },
  { title: '源店铺', key: 'sourceShop', width: 200 },
  { title: '状态', key: 'status', width: 180 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
]

const themeFileModal = reactive({ visible: false, data: null })

function formatTime(t) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

async function fetchShops() {
  try {
    const data = await shopApi.getList({ page: 1, pageSize: 200 })
    const list = data?.records || data || []
    shopOptions.value = list.map(s => ({ label: s.shopName, value: s.id }))
  } catch { /* ignore */ }
}

async function fetchDevStore() {
  try {
    const res = await getDevStore()
    devStore.value = res.code === 200 ? res.data : null
  } catch { /* ignore */ }
}

function fetchData() {
  loading.value = true
  getProductTemplateList({ current: pagination.current, size: pagination.pageSize })
    .then(res => {
      if (res.code === 200) {
        const records = res.data.records || []
        tableData.value = records.map(item => ({
          ...item,
          _editTemplateName: item.templateName,
          _editSourceShopId: item.sourceShopId || undefined,
          _editingField: null,
          _pulling: false,
          _previewing: false
        }))
        pagination.total = res.data.total || 0
      }
    })
    .catch(err => message.error('加载失败: ' + (err.message || '网络错误')))
    .finally(() => { loading.value = false })
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

function startEditField(record, field) {
  if (record._editingField === field) return
  record._editingField = field
  nextTick(() => {
    const el = inputRefs[`${record.productId}_${field}`]
    if (el && el.focus) el.focus()
  })
}

function saveField(record, field) {
  record._editingField = null
  const newVal = record._editTemplateName?.trim() || null
  if (newVal === record.templateName) return

  updateProductTemplateInfo(record.productId, { templateName: newVal })
    .then(res => {
      if (res.code === 200) {
        record.templateName = newVal
        message.success('模板名称已保存')
      } else {
        message.error(res.message || '保存失败')
      }
    })
    .catch(err => message.error('保存失败: ' + (err.message || '网络错误')))
}

function saveSourceShop(record, val) {
  updateProductTemplateInfo(record.productId, { sourceShopId: val || '' })
    .then(res => {
      if (res.code === 200) {
        record.sourceShopId = val
        message.success('源店铺已保存')
      } else {
        message.error(res.message || '保存失败')
      }
    })
    .catch(err => message.error('保存失败: ' + (err.message || '网络错误')))
}

function handlePull(record) {
  record._pulling = true
  pullThemeFiles(record.productId)
    .then(res => {
      if (res.code === 200) {
        const d = res.data
        const pulledCount = d.pulled?.length || 0
        const errorCount = d.errors?.length || 0
        if (errorCount === 0) {
          message.success(`成功拉取 ${pulledCount} 个主题文件`)
        } else {
          message.warning(`拉取了 ${pulledCount} 个文件，${errorCount} 个失败`)
        }
        record.lastPullTime = d.lastPullTime
        record.templateVersion = String(d.themeId || '')
      } else {
        message.error(res.message || '拉取失败')
      }
    })
    .catch(err => message.error('拉取失败: ' + (err.message || '网络错误')))
    .finally(() => { record._pulling = false })
}

function handlePreview(record) {
  record._previewing = true
  message.info('正在推送产品和主题文件到开发店铺，请稍候...')

  previewProductTemplate(record.productId)
    .then(res => {
      if (res.code === 200 && res.data) {
        message.success('推送完成，正在打开预览...')
        window.open(res.data, '_blank')
        record.lastPushTime = new Date().toISOString()
      } else {
        message.error(res.message || '预览生成失败')
      }
    })
    .catch(err => message.error('预览失败: ' + (err.message || '网络错误')))
    .finally(() => { record._previewing = false })
}

function handleViewFiles(record) {
  getThemeFiles(record.productId)
    .then(res => {
      themeFileModal.data = res.code === 200 ? res.data : null
      themeFileModal.visible = true
    })
    .catch(() => {
      themeFileModal.data = null
      themeFileModal.visible = true
    })
}

onMounted(() => {
  fetchShops()
  fetchDevStore()
  fetchData()
})
</script>

<style scoped>
.product-template-container {
  padding: 24px;
  background: #f0f2f5;
  min-height: 100%;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.inline-edit {
  cursor: pointer;
  min-height: 32px;
  display: flex;
  align-items: center;
}
.editable-text {
  color: #333;
  border-bottom: 1px dashed #d9d9d9;
  padding-bottom: 2px;
  transition: all 0.2s;
}
.editable-text:hover {
  border-bottom-color: #1890ff;
  color: #1890ff;
}
.edit-icon {
  margin-left: 6px;
  font-size: 12px;
  color: #bbb;
}
.editable-text:hover .edit-icon {
  color: #1890ff;
}
.status-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #888;
}
.status-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
.status-time {
  white-space: nowrap;
}
.muted {
  color: #ccc;
}
</style>
