<template>
  <div class="page-container">
    <div class="content-card">
      <h2>主题智能迁移</h2>
      
      <!-- 步骤条 -->
      <a-steps :current="currentStep" size="small" style="margin-bottom: 24px">
        <a-step title="上传新版本" />
        <a-step title="差异分析" />
        <a-step title="确认规则" />
        <a-step title="执行迁移" />
      </a-steps>

      <!-- 步骤1: 上传新版本 -->
      <div v-if="currentStep === 0" class="step-content">
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
          <a-form-item label="主题名称" required>
            <a-input v-model:value="migrationForm.themeName" placeholder="例如: Shrine Pro" />
          </a-form-item>
          <a-form-item label="目标版本号" required>
            <a-input v-model:value="migrationForm.newVersion" placeholder="例如: 3.0.0" />
          </a-form-item>
          <a-form-item label="新版本ZIP" required>
            <a-upload
              :before-upload="handleBeforeUpload"
              :file-list="fileList"
              @remove="handleRemove"
              accept=".zip"
            >
              <a-button>
                <UploadOutlined />
                选择ZIP文件
              </a-button>
            </a-upload>
          </a-form-item>
          <a-form-item :wrapper-col="{ offset: 6, span: 14 }">
            <a-space>
              <a-button type="primary" :loading="analyzing" @click="handleStartAnalysis">
                <RocketOutlined />
开始分析
              </a-button>
              <a-button @click="$router.back()">取消</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- 步骤2: 差异分析结果 -->
      <div v-if="currentStep === 1" class="step-content">
        <a-spin :spinning="analyzing">
          <a-alert
            message="分析完成"
            description="已完成Git Diff分析，检测到以下Section变更"
            type="success"
            show-icon
            style="margin-bottom: 16px"
          />
          
          <a-descriptions title="版本信息" bordered :column="2">
            <a-descriptions-item label="主题名称">{{ session.themeName }}</a-descriptions-item>
            <a-descriptions-item label="迁移路径">
              {{ session.fromVersion }} → {{ session.toVersion }}
            </a-descriptions-item>
          </a-descriptions>

          <a-divider />

          <h3>Section变更统计</h3>
          <a-row :gutter="16">
            <a-col :span="6">
              <a-statistic title="新增" :value="sectionChanges.added?.length || 0" :value-style="{ color: '#3f8600' }">
                <template #prefix><PlusCircleOutlined /></template>
              </a-statistic>
            </a-col>
            <a-col :span="6">
              <a-statistic title="删除" :value="sectionChanges.deleted?.length || 0" :value-style="{ color: '#cf1322' }">
                <template #prefix><MinusCircleOutlined /></template>
              </a-statistic>
            </a-col>
            <a-col :span="6">
              <a-statistic title="重命名" :value="Object.keys(sectionChanges.renamed || {}).length" :value-style="{ color: '#1890ff' }">
                <template #prefix><SwapOutlined /></template>
              </a-statistic>
            </a-col>
            <a-col :span="6">
              <a-statistic title="修改" :value="sectionChanges.modified?.length || 0" :value-style="{ color: '#faad14' }">
                <template #prefix><EditOutlined /></template>
              </a-statistic>
            </a-col>
          </a-row>

          <a-divider />

          <a-button type="primary" @click="currentStep = 2">
            下一步：查看规则
            <RightOutlined />
          </a-button>
        </a-spin>
      </div>

      <!-- 步骤3: 确认映射规则 -->
      <div v-if="currentStep === 2" class="step-content">
        <a-alert
          message="智能规则建议"
          description="请确认以下自动生成的映射规则，可手动调整"
          type="info"
          show-icon
          style="margin-bottom: 16px"
        />

        <a-table
          :columns="ruleColumns"
          :data-source="ruleMappings"
          :pagination="false"
          row-key="oldSection"
          size="small"
        >
          <template #confidence="{ record }">
            <a-tag :color="getConfidenceColor(record.confidence)">
              {{ record.confidence }}
            </a-tag>
          </template>

          <template #action="{ record }">
            <a-button type="link" size="small" @click="handleEditRule(record)">
              编辑
            </a-button>
          </template>
        </a-table>

        <div style="margin-top: 16px">
          <a-space>
            <a-button @click="currentStep = 1">
              <LeftOutlined />
              上一步
            </a-button>
            <a-button type="primary" :loading="executing" @click="handleExecuteMigration">
              <ThunderboltOutlined />
              执行迁移
            </a-button>
          </a-space>
        </div>
      </div>

      <!-- 步骤4: 执行结果 -->
      <div v-if="currentStep === 3" class="step-content">
        <a-result
          :status="migrationResult.success ? 'success' : 'error'"
          :title="migrationResult.success ? '迁移成功' : '迁移失败'"
          :sub-title="migrationResult.message"
        >
          <template #extra>
            <a-descriptions bordered :column="2">
              <a-descriptions-item label="更新的模板数量">
                {{ migrationResult.templatesUpdated }}
              </a-descriptions-item>
              <a-descriptions-item label="历史记录ID">
                {{ migrationResult.historyId }}
              </a-descriptions-item>
            </a-descriptions>
            
            <div style="margin-top: 16px">
              <a-space>
                <!-- 下载迁移后的主题 -->
                <a-button 
                  v-if="migrationResult.success" 
                  type="primary" 
                  @click="downloadMigratedTheme"
                >
                  <DownloadOutlined /> 下载迁移后的主题
                </a-button>
                
                <a-button type="default" @click="$router.push({ name: 'ThemeVersions' })">
                  返回版本管理
                </a-button>
                <a-button @click="resetForm">再次迁移</a-button>
              </a-space>
            </div>
          </template>
        </a-result>
      </div>
    </div>

    <!-- 编辑规则弹窗 -->
    <a-modal
      v-model:open="editRuleVisible"
      title="编辑映射规则"
      @ok="handleSaveRule"
    >
      <a-form :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="旧Section">
          <a-input v-model:value="editingRule.oldSection" disabled />
        </a-form-item>
        <a-form-item label="新Section">
          <a-input v-model:value="editingRule.newSection" />
        </a-form-item>
        <a-form-item label="置信度">
          <a-select v-model:value="editingRule.confidence">
            <a-select-option value="CONFIRMED">CONFIRMED</a-select-option>
            <a-select-option value="HIGH">HIGH</a-select-option>
            <a-select-option value="MEDIUM">MEDIUM</a-select-option>
            <a-select-option value="LOW">LOW</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute } from 'vue-router'
import { 
  UploadOutlined, 
  RocketOutlined,
  PlusCircleOutlined,
  MinusCircleOutlined,
  SwapOutlined,
  EditOutlined,
  RightOutlined,
  LeftOutlined,
  ThunderboltOutlined
} from '@ant-design/icons-vue'
import { themeApi } from '@/api/theme'

const route = useRoute()

const currentStep = ref(0)
const analyzing = ref(false)
const executing = ref(false)

const migrationForm = reactive({
  themeName: '',
  newVersion: '',
})

const fileList = ref([])
const session = ref({})
const sectionChanges = computed(() => session.value.diffResult?.sectionChanges || {})

// 步骤3: 规则表格
const ruleColumns = [
  { title: '旧Section', dataIndex: 'oldSection', width: '30%' },
  { title: '新Section', dataIndex: 'newSection', width: '30%' },
  { title: '置信度', dataIndex: 'confidence', width: '20%', slots: { customRender: 'confidence' } },
  { title: '原因', dataIndex: 'reason', ellipsis: true },
  { title: '操作', key: 'action', width: 100, slots: { customRender: 'action' } }
]

const ruleMappings = computed(() => {
  const mappings = session.value.suggestedRules?.sectionMappings || {}
  return Object.values(mappings)
})

// 编辑规则
const editRuleVisible = ref(false)
const editingRule = ref({})

const handleEditRule = (record) => {
  editingRule.value = { ...record }
  editRuleVisible.value = true
}

const handleSaveRule = () => {
  const mappings = session.value.suggestedRules.sectionMappings
  const oldKey = editingRule.value.oldSection
  mappings[oldKey] = editingRule.value
  editRuleVisible.value = false
  message.success('规则已更新')
}

// 迁移结果
const migrationResult = ref({})

const handleBeforeUpload = (file) => {
  fileList.value = [file]
  return false
}

const handleRemove = () => {
  fileList.value = []
}

// 开始分析
const handleStartAnalysis = async () => {
  if (!migrationForm.themeName || !migrationForm.newVersion || fileList.value.length === 0) {
    message.warning('请填写完整信息并选择ZIP文件')
    return
  }

  analyzing.value = true
  try {
    const formData = new FormData()
    formData.append('file', fileList.value[0])
    formData.append('themeName', migrationForm.themeName)
    formData.append('newVersion', migrationForm.newVersion)

    const response = await themeApi.startMigration(formData)
    session.value = response.data  // 提取data字段
    currentStep.value = 1
    message.success('分析完成')
  } catch (error) {
    console.error('分析失败:', error)
    message.error('分析失败')
  } finally {
    analyzing.value = false
  }
}

// 执行迁移
const handleExecuteMigration = async () => {
  executing.value = true
  try {
    const response = await themeApi.executeMigration(
      session.value.id,
      session.value.suggestedRules
    )
    migrationResult.value = response.data  // 提取data字段
    currentStep.value = 3
  } catch (error) {
    console.error('迁移失败:', error)
    message.error('迁移失败')
  } finally {
    executing.value = false
  }
}

// 重置表单
const resetForm = () => {
  currentStep.value = 0
  migrationForm.themeName = ''
  migrationForm.newVersion = ''
  fileList.value = []
  session.value = {}
  migrationResult.value = {}
}

// 获取置信度颜色
const getConfidenceColor = (confidence) => {
  const colors = {
    CONFIRMED: 'green',
    HIGH: 'blue',
    MEDIUM: 'orange',
    LOW: 'red'
  }
  return colors[confidence] || 'default'
}

onMounted(() => {
  // 从版本管理页面跳转过来时，自动填充主题名称
  if (route.query.themeName) {
    migrationForm.themeName = route.query.themeName
  }
  // 注意：不自动填充newVersion，因为用户应该上传新版本并手动输入版本号
})

// 下载迁移后的主题
const downloadMigratedTheme = () => {
  if (!migrationResult.value || !migrationResult.value.historyId) {
    message.error('无法找到迁移记录')
    return
  }

  const downloadUrl = `/api/v1/theme/migration/download/${migrationResult.value.historyId}`
  
  // 创建隐藏的a标签触发下载
  const link = document.createElement('a')
  link.href = downloadUrl
  link.setAttribute('download', '')  // 设置download属性
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  
  message.success('开始下载迁移后的主题')
}
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

.content-card h2 {
  margin: 0 0 24px 0;
  font-size: 18px;
  font-weight: 600;
}

.step-content {
  margin-top: 24px;
}

@media (max-width: 576px) {
  .page-container {
    padding: 12px;
  }
  
  .content-card {
    padding: 12px;
  }
}
</style>
