<template>
  <div class="page-container">
    <div class="content-card">
      <!-- 工具栏 -->
      <div class="toolbar">
        <h2>运单列表</h2>
        <a-space>
          <a-button
            v-if="selectedRowKeys.length > 0"
            danger
            @click="handleBatchDelete"
          >
            <DeleteOutlined />
            批量删除 ({{ selectedRowKeys.length }})
          </a-button>
          <a-button @click="showBatchImportModal">
            <UploadOutlined />
            批量导入
          </a-button>
          <a-button type="primary" @click="showModal">
            <PlusOutlined />
            添加运单
          </a-button>
        </a-space>
      </div>

      <!-- 搜索筛选 -->
      <a-form layout="inline" class="search-form">
        <a-form-item label="搜索">
          <a-input
            v-model:value="searchParams.keyword"
            placeholder="运单号/订单号"
            style="width: 200px"
            @press-enter="handleSearch"
          >
            <template #suffix>
              <SearchOutlined />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item label="店铺">
          <a-select
            v-model:value="searchParams.shopId"
            placeholder="全部"
            style="width: 150px"
            allow-clear
          >
            <a-select-option
              v-for="shop in shopList"
              :key="shop.id"
              :value="shop.id"
            >
              {{ shop.shopName }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="状态">
          <a-select
            v-model:value="searchParams.status"
            placeholder="全部"
            style="width: 150px"
            allow-clear
          >
            <a-select-option value="InfoReceived">信息收录</a-select-option>
            <a-select-option value="InTransit">运输中</a-select-option>
            <a-select-option value="Delivered">已送达</a-select-option>
            <a-select-option value="Exception">异常</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="承运商">
          <a-select
            v-model:value="searchParams.carrierCode"
            placeholder="全部"
            style="width: 150px"
            allow-clear
          >
            <a-select-option
              v-for="carrier in carrierOptions"
              :key="carrier"
              :value="carrier"
            >
              {{ carrier.toUpperCase() }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="创建日期">
          <a-range-picker
            v-model:value="searchParams.dateRange"
            style="width: 240px"
            format="YYYY-MM-DD"
          />
        </a-form-item>

        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">
              <SearchOutlined />
              查询
            </a-button>
            <a-button @click="handleReset">
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        @change="handleTableChange"
        row-key="id"
      >
        <!-- 运单号 -->
        <template #trackingNumber="{ record }">
          <a-typography-text copyable>
            {{ record.trackingNumber }}
          </a-typography-text>
        </template>

        <!-- 承运商 -->
        <template #carrierCode="{ record }">
          <a-tag color="blue">
            {{ record.carrierCode?.toUpperCase() }}
          </a-tag>
        </template>

        <!-- 状态 -->
        <template #trackStatus="{ record }">
          <a-tag :color="getStatusColor(record.trackStatus)">
            {{ getStatusText(record.trackStatus) }}
          </a-tag>
        </template>

        <!-- 来源 -->
        <template #source="{ record }">
          {{ getSourceText(record.source) }}
        </template>

        <!-- 操作列 -->
        <template #action="{ record }">
          <a-space>
            <a-button type="link" size="small" @click="handleView(record)">
              查看
            </a-button>
            <a-button type="link" size="small" @click="handleSync(record.id)">
              同步
            </a-button>
            <a-popconfirm
              title="确定要删除该运单吗？"
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
    </div>

    <!-- 添加运单弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      title="添加运单"
      width="500px"
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
        <a-form-item label="运单号" name="trackingNumber">
          <a-input
            v-model:value="formState.trackingNumber"
            placeholder="请输入运单号（系统将自动识别承运商）"
          />
        </a-form-item>

        <a-form-item label="备注" name="remarks">
          <a-textarea
            v-model:value="formState.remarks"
            placeholder="可选"
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 运单详情弹窗 -->
    <a-modal
      v-model:open="detailModalVisible"
      title="运单详情"
      width="700px"
      :footer="null"
    >
      <a-descriptions bordered :column="2" v-if="currentDetail">
        <a-descriptions-item label="运单号" :span="2">
          <a-typography-text copyable>
            {{ currentDetail.trackingNumber }}
          </a-typography-text>
        </a-descriptions-item>
        <a-descriptions-item label="承运商">
          {{ currentDetail.carrierCode?.toUpperCase() }}
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(currentDetail.trackStatus)">
            {{ getStatusText(currentDetail.trackStatus) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="来源">
          {{ getSourceText(currentDetail.source) }}
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">
          {{ formatDate(currentDetail.createdAt) }}
        </a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">
          <div v-if="!editingRemarks" style="display: flex; align-items: center; gap: 8px;">
            <span>{{ currentDetail.remarks || '暂无备注' }}</span>
            <a-button type="link" size="small" @click="startEditRemarks">
              编辑
            </a-button>
          </div>
          <div v-else style="display: flex; gap: 8px;">
            <a-textarea
              v-model:value="remarksEditValue"
              :rows="2"
              placeholder="请输入备注"
              style="flex: 1"
            />
            <div style="display: flex; flex-direction: column; gap: 4px;">
              <a-button type="primary" size="small" @click="handleSaveRemarks">
                保存
              </a-button>
              <a-button size="small" @click="cancelEditRemarks">
                取消
              </a-button>
            </div>
          </div>
        </a-descriptions-item>
        <a-descriptions-item label="最后同步">
          {{ formatDate(currentDetail.lastSyncAt) }}
        </a-descriptions-item>
        <a-descriptions-item label="下次同步">
          {{ formatDate(currentDetail.nextSyncAt) }}
        </a-descriptions-item>
      </a-descriptions>

      <!-- 物流轨迹 -->
      <a-divider>物流轨迹</a-divider>
      <a-timeline v-if="currentDetail?.events && currentDetail.events.length > 0">
        <a-timeline-item
          v-for="event in currentDetail.events"
          :key="event.id"
          :color="getEventColor(event.stage)"
        >
          <p><strong>{{ formatDate(event.eventTime) }}</strong></p>
          <p>{{ event.status }}</p>
          <p v-if="event.location" class="event-location">
            <EnvironmentOutlined /> {{ event.location }}
          </p>
          <p class="event-desc">{{ event.description }}</p>
        </a-timeline-item>
      </a-timeline>
      <a-empty v-else description="暂无物流轨迹" />
    </a-modal>

    <!-- 批量导入弹窗 -->
    <a-modal
      v-model:open="batchImportModalVisible"
      title="批量导入运单"
      width="800px"
      @ok="handleBatchImportSubmit"
      @cancel="handleBatchImportCancel"
      :confirm-loading="batchImportLoading"
    >
      <a-alert
        message="导入说明"
        type="info"
        show-icon
        style="margin-bottom: 16px"
      >
        <template #description>
          <div>1. 下载CSV模板填写运单信息</div>
          <div>2. 上传CSV文件进行批量导入</div>
          <div>3. 承运商代码可留空，系统将自动识别</div>
          <div>4. 支持承运商：ups, fedex, usps, dhl, 4px, CHINA-POST,等3000+承运商</div>
        </template>
      </a-alert>

      <a-space style="margin-bottom: 16px">
        <a-button @click="downloadTemplate">
          <DownloadOutlined />
          下载CSV模板
        </a-button>
        <a-upload
          :before-upload="handleFileUpload"
          :show-upload-list="false"
          accept=".csv"
        >
          <a-button type="primary">
            <UploadOutlined />
            选择CSV文件
          </a-button>
        </a-upload>
      </a-space>

      <!-- 文件信息显示 -->
      <div v-if="uploadedFileName" style="margin-bottom: 16px">
        <a-card size="small">
          <div style="display: flex; align-items: center; justify-content: space-between">
            <div>
              <a-tag color="blue">已选择文件</a-tag>
              <span style="margin-left: 8px">{{ uploadedFileName }}</span>
              <span style="margin-left: 16px; color: #8c8c8c">
                共 {{ uploadedDataCount }} 条记录
              </span>
            </div>
            <a-button size="small" danger @click="clearUploadedFile">
              清除
            </a-button>
          </div>
        </a-card>
      </div>

      <!-- 预览前几行数据 -->
      <div v-if="batchImportText && uploadedDataCount > 0" style="margin-bottom: 16px">
        <a-divider orientation="left" style="margin: 16px 0">数据预览（前5行）</a-divider>
        <a-table
          :columns="previewColumns"
          :data-source="previewData"
          :pagination="false"
          size="small"
          bordered
        />
      </div>

      <a-empty v-else description="请选择CSV文件" />
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  SearchOutlined,
  DeleteOutlined,
  EnvironmentOutlined,
  UploadOutlined,
  DownloadOutlined
} from '@ant-design/icons-vue'
import { trackingApi } from '@/api/tracking'
import { shopApi } from '@/api/shop'
import dayjs from 'dayjs'

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80
  },
  {
    title: '运单号',
    dataIndex: 'trackingNumber',
    width: 200,
    slots: { customRender: 'trackingNumber' }
  },
  {
    title: '承运商',
    dataIndex: 'carrierCode',
    width: 120,
    slots: { customRender: 'carrierCode' }
  },
  {
    title: '状态',
    dataIndex: 'trackStatus',
    width: 120,
    slots: { customRender: 'trackStatus' }
  },
  {
    title: '来源',
    dataIndex: 'source',
    width: 100,
    slots: { customRender: 'source' }
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

const searchParams = reactive({
  keyword: '',
  shopId: undefined,
  status: undefined,
  carrierCode: undefined,
  dateRange: []
})

const shopList = ref([])
const carrierOptions = ref([])

const selectedRowKeys = ref([])
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys) => {
    selectedRowKeys.value = keys
  }
}))

const modalVisible = ref(false)
const confirmLoading = ref(false)
const formRef = ref()

const formState = reactive({
  trackingNumber: '',
  remarks: ''
})

const rules = {
  trackingNumber: [{ required: true, message: '请输入运单号', trigger: 'blur' }]
}

const detailModalVisible = ref(false)
const currentDetail = ref(null)

// 备注编辑状态
const editingRemarks = ref(false)
const remarksEditValue = ref('')

// 批量导入状态
const batchImportModalVisible = ref(false)
const batchImportLoading = ref(false)
const batchImportText = ref('')
const uploadedFileName = ref('')
const uploadedDataCount = ref(0)

// 预览表格列定义
const previewColumns = [
  { title: '序号', dataIndex: 'index', width: 60 },
  { title: '运单号', dataIndex: 'trackingNumber', ellipsis: true },
  { title: '承运商代码', dataIndex: 'carrierCode', width: 120 },
  { title: '备注', dataIndex: 'remarks', ellipsis: true }
]

// 预览数据（前5行）
const previewData = computed(() => {
  if (!batchImportText.value) return []

  const lines = batchImportText.value.split('\n').filter(line => line.trim())
  return lines.slice(0, 5).map((line, index) => {
    const parts = line.split(',').map(p => p.trim())
    return {
      index: index + 1,
      trackingNumber: parts[0] || '-',
      carrierCode: parts[1] || '自动识别',
      remarks: parts[2] || '-'
    }
  })
})

// 获取运单列表
const fetchTrackings = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchParams.keyword,
      shopId: searchParams.shopId,
      status: searchParams.status,
      carrierCode: searchParams.carrierCode
    }

    // 处理日期范围
    if (searchParams.dateRange && searchParams.dateRange.length === 2) {
      params.startDate = dayjs(searchParams.dateRange[0]).format('YYYY-MM-DD')
      params.endDate = dayjs(searchParams.dateRange[1]).format('YYYY-MM-DD')
    }

    const data = await trackingApi.getList(params)
    tableData.value = data.list || []
    pagination.total = data.total || 0
  } catch (error) {
    console.error('获取运单列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取店铺列表
const fetchShops = async () => {
  try {
    const data = await shopApi.getList({ pageSize: 100 })
    shopList.value = data.list || []
  } catch (error) {
    console.error('获取店铺列表失败:', error)
  }
}

// 获取已有的承运商列表
const fetchCarriers = async () => {
  try {
    const data = await trackingApi.getUsedCarriers()
    carrierOptions.value = data || []
  } catch (error) {
    console.error('获取承运商列表失败:', error)
  }
}

// 表格分页变化
const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchTrackings()
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchTrackings()
}

// 重置
const handleReset = () => {
  Object.assign(searchParams, {
    keyword: '',
    shopId: undefined,
    status: undefined,
    carrierCode: undefined,
    dateRange: []
  })
  handleSearch()
}

// 显示弹窗
const showModal = () => {
  resetForm()
  modalVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    confirmLoading.value = true

    await trackingApi.create({
      ...formState,
      source: 'manual'
    })
    message.success('创建成功')

    modalVisible.value = false
    fetchTrackings()
    fetchCarriers()  // 刷新承运商列表
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

// 查看详情
const handleView = async (record) => {
  try {
    currentDetail.value = await trackingApi.getDetail(record.id)
    editingRemarks.value = false
    remarksEditValue.value = ''
    detailModalVisible.value = true
  } catch (error) {
    console.error('获取详情失败:', error)
  }
}

// 开始编辑备注
const startEditRemarks = () => {
  remarksEditValue.value = currentDetail.value.remarks || ''
  editingRemarks.value = true
}

// 保存备注
const handleSaveRemarks = async () => {
  try {
    await trackingApi.updateRemarks(currentDetail.value.id, remarksEditValue.value)
    message.success('备注更新成功')
    currentDetail.value.remarks = remarksEditValue.value
    editingRemarks.value = false
    fetchTrackings() // 刷新列表
  } catch (error) {
    console.error('更新备注失败:', error)
  }
}

// 取消编辑备注
const cancelEditRemarks = () => {
  editingRemarks.value = false
  remarksEditValue.value = ''
}

// 同步运单
const handleSync = async (id) => {
  try {
    await trackingApi.sync(id)
    message.success('同步成功')
    fetchTrackings()
  } catch (error) {
    console.error('同步失败:', error)
  }
}

// 删除
const handleDelete = async (id) => {
  try {
    await trackingApi.delete(id)
    message.success('删除成功')
    fetchTrackings()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

// 批量删除
const handleBatchDelete = () => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个运单吗？`,
    okText: '确定',
    cancelText: '取消',
    async onOk() {
      try {
        await trackingApi.batchDelete(selectedRowKeys.value)
        message.success('批量删除成功')
        selectedRowKeys.value = []
        fetchTrackings()
      } catch (error) {
        console.error('批量删除失败:', error)
      }
    }
  })
}

// 显示批量导入弹窗
const showBatchImportModal = () => {
  batchImportText.value = ''
  uploadedFileName.value = ''
  uploadedDataCount.value = 0
  batchImportModalVisible.value = true
}

// 清除已上传的文件
const clearUploadedFile = () => {
  batchImportText.value = ''
  uploadedFileName.value = ''
  uploadedDataCount.value = 0
}

// 批量导入提交
const handleBatchImportSubmit = async () => {
  const lines = batchImportText.value.split('\n').filter(line => line.trim())

  if (lines.length === 0) {
    message.warning('请选择CSV文件')
    return
  }

  const items = []
  const errors = []

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim()
    const parts = line.split(',').map(p => p.trim())

    // 至少需要运单号
    if (parts.length < 1 || !parts[0]) {
      errors.push(`第 ${i + 1} 行：运单号不能为空`)
      continue
    }

    items.push({
      trackingNumber: parts[0],
      carrierCode: parts[1] || '',  // carrier可以为空，自动识别
      remarks: parts[2] || ''
    })
  }

  if (errors.length > 0) {
    Modal.error({
      title: '数据格式错误',
      content: errors.join('\n')
    })
    return
  }

  batchImportLoading.value = true
  try {
    const result = await trackingApi.batchImport(items)
    message.success(`导入完成：成功 ${result.success} 条，失败 ${result.failed} 条`)
    batchImportModalVisible.value = false
    clearUploadedFile()
    fetchTrackings()
    fetchCarriers()  // 刷新承运商列表
  } catch (error) {
    console.error('批量导入失败:', error)
  } finally {
    batchImportLoading.value = false
  }
}

// 批量导入取消
const handleBatchImportCancel = () => {
  batchImportModalVisible.value = false
  clearUploadedFile()
}

// 下载CSV模板
const downloadTemplate = () => {
  const template = `运单号,承运商代码,备注
1Z999AA10123456784,ups,示例运单1
123456789012,,自动识别承运商
9400100000000000000000,usps,示例运单3`

  const blob = new Blob(['\ufeff' + template], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)

  link.setAttribute('href', url)
  link.setAttribute('download', '运单导入模板.csv')
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  message.success('模板下载成功')
}

// 处理文件上传
const handleFileUpload = (file) => {
  const reader = new FileReader()

  reader.onload = (e) => {
    try {
      let content = e.target.result

      // 处理BOM
      if (content.charCodeAt(0) === 0xFEFF) {
        content = content.slice(1)
      }

      // 解析CSV
      const lines = content.split(/\r?\n/).filter(line => line.trim())

      // 跳过标题行（如果存在）
      const firstLine = lines[0].toLowerCase()
      const hasHeader = firstLine.includes('运单号') || firstLine.includes('tracking')
      const dataLines = hasHeader ? lines.slice(1) : lines

      // 验证并转换格式
      const validLines = []
      for (let i = 0; i < dataLines.length; i++) {
        const line = dataLines[i].trim()
        if (line) {
          validLines.push(line)
        }
      }

      if (validLines.length === 0) {
        message.warning('CSV文件中没有有效数据')
        return
      }

      batchImportText.value = validLines.join('\n')
      uploadedFileName.value = file.name
      uploadedDataCount.value = validLines.length
      message.success(`成功读取 ${validLines.length} 条记录`)
    } catch (error) {
      console.error('文件解析失败:', error)
      message.error('文件解析失败，请检查文件格式')
    }
  }

  reader.onerror = () => {
    message.error('文件读取失败')
  }

  reader.readAsText(file, 'UTF-8')

  // 阻止默认上传行为
  return false
}

// 重置表单
const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(formState, {
    trackingNumber: '',
    remarks: ''
  })
}

// 获取状态颜色
const getStatusColor = (status) => {
  const colors = {
    InfoReceived: 'default',
    InTransit: 'processing',
    Delivered: 'success',
    Exception: 'error'
  }
  return colors[status] || 'default'
}

// 获取状态文本
const getStatusText = (status) => {
  const texts = {
    InfoReceived: '信息收录',
    InTransit: '运输中',
    Delivered: '已送达',
    Exception: '异常'
  }
  return texts[status] || status
}

// 获取来源文本
const getSourceText = (source) => {
  const texts = {
    manual: '手动添加',
    batch_import: '批量导入',
    api: 'API接口',
    webhook: 'Webhook',
    system: '系统自动'
  }
  return texts[source] || source || '-'
}

// 获取事件颜色
const getEventColor = (stage) => {
  const colors = {
    InfoReceived: 'gray',
    InTransit: 'blue',
    Delivered: 'green',
    Exception: 'red'
  }
  return colors[stage] || 'gray'
}

// 格式化日期
const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'
}

onMounted(() => {
  fetchShops()
  fetchCarriers()
  fetchTrackings()
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

.search-form {
  margin-bottom: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
}

.event-location {
  color: #8c8c8c;
  font-size: 13px;
}

.event-desc {
  color: #595959;
  margin-top: 4px;
}
</style>
