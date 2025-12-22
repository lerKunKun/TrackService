<template>
  <div class="page-container">
    <div class="content-card">
      <!-- 工具栏 -->
      <div class="toolbar">
        <h2>主题版本管理</h2>
        <a-space>
          <a-input-search
            v-model:value="themeName"
            placeholder="输入主题名称查询"
            style="width: 200px"
            @search="fetchVersionHistory"
          />
          <a-button type="primary" @click="showUploadModal">
            <UploadOutlined />
            上传新版本
          </a-button>
        </a-space>
      </div>

      <!-- 版本列表 -->
      <a-table
        :columns="columns"
        :data-source="versionList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
      >
        <!-- 是否当前版本 -->
        <template #isCurrent="{ record }">
          <a-tag v-if="record.isCurrent" color="green">
            <CheckCircleOutlined /> 当前版本
          </a-tag>
          <span v-else>-</span>
        </template>

        <!-- 文件大小 -->
        <template #zipFileSize="{ record }">
          {{ formatFileSize(record.zipFileSize) }}
        </template>

        <!-- 时间格式化 -->
        <template #uploadedAt="{ record }">
          {{ formatDate(record.uploadedAt) }}
        </template>

        <!-- 操作列 -->
        <template #action="{ record }">
          <a-space :size="4">
            <a-tooltip v-if="!record.isCurrent" title="设为当前版本">
              <a-button type="link" size="small" @click="handleSetCurrent(record)">
                <CheckOutlined />
              </a-button>
            </a-tooltip>
            <a-tooltip title="深度分析">
              <a-button type="link" size="small" @click="handleDeepAnalysis(record)">
                <FundOutlined />
              </a-button>
            </a-tooltip>
<!--            <a-tooltip title="开始迁移">-->
<!--              <a-button type="link" size="small" @click="handleStartMigration(record)">-->
<!--                <SwapOutlined />-->
<!--              </a-button>-->
<!--            </a-tooltip>-->
            <a-popconfirm
              title="确定要删除此版本吗？"
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
    </div>

    <!-- 上传版本弹窗 -->
    <a-modal
      v-model:open="uploadModalVisible"
      title="上传主题版本"
      width="500px"
      :confirm-loading="uploadLoading"
      @ok="handleUploadSubmit"
      @cancel="handleUploadCancel"
    >
      <a-form
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-item label="主题名称" required>
          <a-input
            v-model:value="uploadForm.themeName"
            placeholder="例如: Shrine Pro"
          />
        </a-form-item>
        <a-form-item label="版本号" required>
          <a-input
            v-model:value="uploadForm.version"
            placeholder="例如: 3.0.0"
          />
        </a-form-item>
        <a-form-item label="主题ZIP文件" required>
          <a-upload
            :before-upload="handleBeforeUpload"
            :file-list="fileList"
            @remove="handleRemove"
            accept=".zip"
          >
            <a-button>
              <UploadOutlined />
              选择文件
            </a-button>
          </a-upload>
        </a-form-item>
        <a-form-item label="版本说明">
          <a-textarea
            v-model:value="uploadForm.notes"
            placeholder="可选：添加版本说明"
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 深度分析弹窗 -->
    <a-modal
      v-model:open="analysisModalVisible"
      title="版本深度分析"
      width="600px"
      :confirm-loading="analysisLoading"
      @ok="handleAnalysisSubmit"
      @cancel="analysisModalVisible = false"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="当前版本">
          <a-input v-model:value="analysisForm.fromVersion" disabled />
        </a-form-item>
        <a-form-item label="目标版本" required>
          <a-select
            v-model:value="analysisForm.toVersion"
            placeholder="选择要对比的版本"
            :options="versionOptions"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 规则查看弹窗 -->
    <a-modal
      v-model:open="rulesModalVisible"
      title="迁移规则"
      width="900px"
      :footer="null"
    >
      <a-tabs v-model:activeKey="rulesActiveTab">
        <a-tab-pane key="rename" tab="Section重命名">
          <a-table
            :columns="renameColumns"
            :data-source="renameRules"
            :pagination="false"
            size="small"
          >
            <template #confidence="{ record }">
              <a-tag :color="getConfidenceColor(record.confidence)">
                {{ record.confidence }}
              </a-tag>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="field" tab="字段映射">
          <a-table
            :columns="fieldColumns"
            :data-source="fieldRules"
            :pagination="false"
            size="small"
          >
            <template #confidence="{ record }">
              <a-tag :color="getConfidenceColor(record.confidence)">
                {{ record.confidence }}
              </a-tag>
            </template>
            <template #similarity="{ record }">
              {{ (record.similarity * 100).toFixed(0) }}%
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="default" tab="默认值">
          <a-table
            :columns="defaultColumns"
            :data-source="defaultRules"
            :pagination="false"
            size="small"
          >
            <template #defaultValue="{ record }">
              <a-tag>{{ record.defaultValue }}</a-tag>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { UploadOutlined, CheckCircleOutlined, CheckOutlined, SwapOutlined, DeleteOutlined, FundOutlined } from '@ant-design/icons-vue'
import { themeApi } from '@/api/theme'
import dayjs from 'dayjs'

const router = useRouter()

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80
  },
  {
    title: '主题名称',
    dataIndex: 'themeName',
    width: 150
  },
  {
    title: '版本号',
    dataIndex: 'version',
    width: 100
  },
  {
    title: '文件大小',
    dataIndex: 'zipFileSize',
    width: 120,
    slots: { customRender: 'zipFileSize' }
  },
  {
    title: 'Sections数量',
    dataIndex: 'sectionsCount',
    width: 120
  },
  {
    title: '当前版本',
    dataIndex: 'isCurrent',
    width: 120,
    slots: { customRender: 'isCurrent' }
  },
  {
    title: '上传者',
    dataIndex: 'uploadedBy',
    width: 120
  },
  {
    title: '上传时间',
    dataIndex: 'uploadedAt',
    width: 180,
    slots: { customRender: 'uploadedAt' }
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
    fixed: 'right',
    slots: { customRender: 'action' }
  }
]

const loading = ref(false)
const versionList = ref([])
const themeName = ref('Shrine Pro')

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

// 获取版本历史
const fetchVersionHistory = async () => {
  if (!themeName.value) {
    message.warning('请输入主题名称')
    return
  }

  loading.value = true
  try {
    const response = await themeApi.getVersionHistory(themeName.value)
    // response 是 { code, data, message } 格式
    versionList.value = response.data || []
    pagination.total = versionList.value.length
  } catch (error) {
    console.error('获取版本历史失败:', error)
    message.error('获取版本历史失败')
  } finally {
    loading.value = false
  }
}

// 表格分页变化
const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
}

// 上传相关
const uploadModalVisible = ref(false)
const uploadLoading = ref(false)
const uploadForm = reactive({
  themeName: '',
  version: '',
  notes: ''
})
const fileList = ref([])

const showUploadModal = () => {
  uploadForm.themeName = themeName.value || ''
  uploadForm.version = ''
  uploadForm.notes = ''
  fileList.value = []
  uploadModalVisible.value = true
}

const handleBeforeUpload = (file) => {
  fileList.value = [file]
  return false
}

const handleRemove = () => {
  fileList.value = []
}

const handleUploadSubmit = async () => {
  if (!uploadForm.themeName || !uploadForm.version || fileList.value.length === 0) {
    message.warning('请填写完整信息并选择ZIP文件')
    return
  }

  uploadLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', fileList.value[0])
    formData.append('themeName', uploadForm.themeName)
    formData.append('version', uploadForm.version)
    if (uploadForm.notes) {
      formData.append('notes', uploadForm.notes)
    }

    await themeApi.archiveVersion(formData)
    message.success('版本上传成功')
    uploadModalVisible.value = false
    fetchVersionHistory()
  } catch (error) {
    console.error('上传失败:', error)
    message.error('上传失败')
  } finally {
    uploadLoading.value = false
  }
}

const handleUploadCancel = () => {
  uploadModalVisible.value = false
}

// 设置为当前版本
const handleSetCurrent = async (record) => {
  try {
    await themeApi.setCurrentVersion(record.themeName, record.version)
    message.success('已设置为当前版本')
    fetchVersionHistory()
  } catch (error) {
    console.error('设置失败:', error)
    message.error('设置失败')
  }
}

// 开始迁移
// const handleStartMigration = (record) => {
//   router.push({
//     name: 'ThemeMigration',
//     query: { themeName: record.themeName, toVersion: record.version }
//   })
// }

// 删除
const handleDelete = async (id) => {
  try {
    await themeApi.deleteVersion(id)
    message.success('删除成功')
    fetchVersionHistory()
  } catch (error) {
    console.error('删除失败:', error)
    message.error('删除失败')
  }
}
const analysisModalVisible = ref(false)
const analysisLoading = ref(false)
const analysisForm = reactive({
  themeName: '',
  fromVersion: '',
  toVersion: ''
})
  // 版本选项（用于下拉选择）
  const versionOptions = computed(() => {
    return versionList.value
        .filter(v => v.version !== analysisForm.fromVersion)
        .map(v => ({
          label: v.version,
          value: v.version
        }))
  })
  // 规则查看弹窗
  const rulesModalVisible = ref(false)
  const rulesActiveTab = ref('rename')
  const renameRules = ref([])
  const fieldRules = ref([])
  const defaultRules = ref([])
  // 表格列配置
  const renameColumns = [
    { title: '旧名称', dataIndex: 'oldName', key: 'oldName' },
    { title: '新名称', dataIndex: 'newName', key: 'newName' },
    { title: '置信度', dataIndex: 'confidence', key: 'confidence', slots: { customRender: 'confidence' } },
    { title: '原因', dataIndex: 'reason', key: 'reason' }
  ]
  const fieldColumns = [
    { title: 'Section', dataIndex: 'sectionName', key: 'sectionName' },
    { title: '旧字段', dataIndex: 'oldFieldId', key: 'oldFieldId' },
    { title: '新字段', dataIndex: 'newFieldId', key: 'newFieldId' },
    { title: '相似度', dataIndex: 'similarity', key: 'similarity', slots: { customRender: 'similarity' } },
    { title: '置信度', dataIndex: 'confidence', key: 'confidence', slots: { customRender: 'confidence' } }
  ]
  const defaultColumns = [
    { title: 'Section', dataIndex: 'sectionName', key: 'sectionName' },
    { title: '字段', dataIndex: 'fieldId', key: 'fieldId' },
    { title: '默认值', dataIndex: 'defaultValue', key: 'defaultValue', slots: { customRender: 'defaultValue' } },
    { title: '类型', dataIndex: 'valueType', key: 'valueType' }
  ]
  // 3. 处理深度分析点击
  const handleDeepAnalysis = (record) => {
    analysisForm.themeName = record.themeName
    analysisForm.fromVersion = record.version
    analysisForm.toVersion = ''
    analysisModalVisible.value = true
  }
  // 4. 提交深度分析
  const handleAnalysisSubmit = async () => {
    if (!analysisForm.toVersion) {
      message.warning('请选择目标版本')
      return
    }
    analysisLoading.value = true
    try {
      const result = await themeApi.analyzeDiff(
          analysisForm.themeName,
          analysisForm.fromVersion,
          analysisForm.toVersion
      )
      message.success(`分析完成！生成${result.data.rulesGenerated}条规则`)
      analysisModalVisible.value = false
      // 加载并显示规则
      await loadAndShowRules(
          analysisForm.themeName,
          analysisForm.fromVersion,
          analysisForm.toVersion
      )
    } catch (error) {
      console.error('深度分析失败:', error)
      message.error('深度分析失败: ' + (error.message || '未知错误'))
    } finally {
      analysisLoading.value = false
    }
  }
  // 5. 加载并显示规则
  const loadAndShowRules = async (themeName, fromVersion, toVersion) => {
    try {
      const result = await themeApi.getMigrationRules(themeName, fromVersion, toVersion)
      const allRules = result.data || []
      // 按类型分组规则
      renameRules.value = allRules
          .filter(r => r.ruleType === 'SECTION_RENAME')
          .map(r => JSON.parse(r.ruleJson))
      const fieldMappingRules = allRules.filter(r => r.ruleType === 'FIELD_MAPPING')
      fieldRules.value = fieldMappingRules.map(r => JSON.parse(r.ruleJson))
      const defaultValueRules = allRules.filter(r => r.ruleType === 'DEFAULT_VALUE')
      defaultRules.value = defaultValueRules.map(r => JSON.parse(r.ruleJson))
      rulesModalVisible.value = true
      rulesActiveTab.value = 'rename'
    } catch (error) {
      console.error('加载规则失败:', error)
      message.error('加载规则失败')
    }
  }
  // 6. 置信度颜色辅助函数
  const getConfidenceColor = (confidence) => {
    const colors = {
      'CONFIRMED': 'green',
      'HIGH': 'blue',
      'MEDIUM': 'orange',
      'LOW': 'red'
    }
    return colors[confidence] || 'default'
  }


// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

// 格式化日期
const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'
}

onMounted(() => {
  fetchVersionHistory()
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

@media (max-width: 576px) {
  .toolbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .page-container {
    padding: 12px;
  }
  
  .content-card {
    padding: 12px;
  }
}
</style>
