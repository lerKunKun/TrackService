<template>
  <div class="page-container">
    <div class="content-card">
      <h2>主题一键迁移</h2>
      <p class="subtitle">上传您店铺的当前主题，选择目标版本，一键完成升级</p>

      <a-form 
        :label-col="{ span: 6 }" 
        :wrapper-col="{ span: 14 }"
        style="max-width: 800px; margin: 24px auto"
      >
        <!-- 主题名称 -->
        <a-form-item label="主题名称" required>
          <a-select
            v-model:value="migrationForm.themeName"
            placeholder="选择要迁移的主题"
            :options="themeOptions"
            @change="handleThemeChange"
          />
          <div class="hint">选择您使用的主题名称</div>
        </a-form-item>

        <!-- 目标版本 -->
        <a-form-item label="目标版本" required>
          <a-select
            v-model:value="migrationForm.targetVersion"
            placeholder="选择要升级到的版本"
            :options="versionOptions"
            :disabled="!migrationForm.themeName"
          />
          <div class="hint">选择您要升级到的版本</div>
        </a-form-item>

        <!-- 上传当前主题 -->
        <a-form-item label="当前主题ZIP" required>
          <a-upload
            :before-upload="handleBeforeUpload"
            :file-list="fileList"
            @remove="handleRemove"
            accept=".zip"
            :max-count="1"
          >
            <a-button>
              <UploadOutlined />
              选择店铺导出的主题文件
            </a-button>
          </a-upload>
          <div class="hint">上传从Shopify店铺导出的主题ZIP文件（包含您的自定义配置）</div>
        </a-form-item>

        <!-- 操作按钮 -->
        <a-form-item :wrapper-col="{ offset: 6, span: 14 }">
          <a-space>
            <a-button 
              type="primary" 
              size="large"
              :loading="migrationLoading" 
              :disabled="!canStartMigration"
              @click="handleStartMigration"
            >
              <RocketOutlined v-if="!migrationLoading" />
              {{ migrationLoading ? '迁移中...' : '一键迁移' }}
            </a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <!-- 进度提示 -->
      <div v-if="migrationLoading" class="progress-container">
        <a-spin size="large" />
        <p class="progress-text">{{ progressMessage }}</p>
        <a-progress 
          :percent="progressPercent" 
          :status="progressPercent === 100 ? 'success' : 'active'"
        />
      </div>

      <!-- 迁移结果 -->
      <a-result
        v-if="migrationResult.success !== undefined"
        :status="migrationResult.success ? 'success' : 'error'"
        :title="migrationResult.success ? '迁移成功！' : '迁移失败'"
        :sub-title="migrationResult.message"
      >
        <template #extra>
          <a-descriptions 
            v-if="migrationResult.success" 
            bordered 
            :column="2"
            size="small"
            style="margin-bottom: 16px"
          >
            <a-descriptions-item label="主题名称">
              {{ migrationResult.themeName }}
            </a-descriptions-item>
            <a-descriptions-item label="目标版本">
              {{ migrationResult.targetVersion }}
            </a-descriptions-item>
            <a-descriptions-item label="更新模板数">
              {{ migrationResult.templatesUpdated }}
            </a-descriptions-item>
            <a-descriptions-item label="应用规则数">
              {{ migrationResult.rulesApplied }}
            </a-descriptions-item>
          </a-descriptions>

          <a-space>
            <a-button 
              v-if="migrationResult.success" 
              type="primary" 
              size="large"
              @click="handleDownload"
            >
              <DownloadOutlined /> 下载迁移后的主题
            </a-button>
            <a-button @click="handleReset">再次迁移</a-button>
          </a-space>
        </template>
      </a-result>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { UploadOutlined, RocketOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import { themeApi } from '@/api/theme'

// 表单数据
const migrationForm = reactive({
  themeName: '',
  targetVersion: ''
})

const fileList = ref([])
const migrationLoading = ref(false)
const progressMessage = ref('')
const progressPercent = ref(0)
const migrationResult = ref({})

// 主题列表
const themeList = ref([])
const themeOptions = computed(() => {
  // 从主题列表中提取唯一的主题名称
  const uniqueThemes = [...new Set(themeList.value.map(v => v.themeName))]
  return uniqueThemes.map(name => ({
    label: name,
    value: name
  }))
})

// 版本列表
const versionOptions = computed(() => {
  if (!migrationForm.themeName) return []
  
  // 过滤出选中主题的所有版本
  return themeList.value
    .filter(v => v.themeName === migrationForm.themeName)
    .map(v => ({
      label: `v${v.version}${v.isCurrent ? ' (当前)' : ''}`,
      value: v.version
    }))
})

// 是否可以开始迁移
const canStartMigration = computed(() => {
  return migrationForm.themeName && 
         migrationForm.targetVersion && 
         fileList.value.length > 0
})

// 加载主题版本列表
const loadThemeVersions = async () => {
  try {
    // 加载所有主题的版本（这里可以调整为只加载特定主题）
    const themes = ['Shrine Pro'] // 默认主题列表
    const allVersions = []
    
    for (const themeName of themes) {
      try {
        const response = await themeApi.getVersionHistory(themeName)
        if (response.data && Array.isArray(response.data)) {
          allVersions.push(...response.data)
        }
      } catch (err) {
        console.warn(`Failed to load versions for ${themeName}:`, err)
      }
    }
    
    themeList.value = allVersions
  } catch (error) {
    console.error('Failed to load theme versions:', error)
  }
}

// 主题改变时重置目标版本
const handleThemeChange = () => {
  migrationForm.targetVersion = ''
}

// 文件上传处理
const handleBeforeUpload = (file) => {
  // 检查文件类型
  if (!file.name.endsWith('.zip')) {
    message.error('只能上传ZIP文件')
    return false
  }
  
  // 检查文件大小（限制50MB）
  const isLt50M = file.size / 1024 / 1024 < 50
  if (!isLt50M) {
    message.error('文件大小不能超过50MB')
    return false
  }
  
  fileList.value = [file]
  return false
}

const handleRemove = () => {
  fileList.value = []
}

// 模拟进度更新
const simulateProgress = () => {
  const stages = [
    { percent: 10, message: '正在上传主题文件...' },
    { percent: 30, message: '正在分析schema差异...' },
    { percent: 50, message: '正在生成迁移规则...' },
    { percent: 70, message: '正在迁移模板文件...' },
    { percent: 90, message: '正在打包迁移后的主题...' },
    { percent: 100, message: '迁移完成！' }
  ]
  
  let currentStage = 0
  const interval = setInterval(() => {
    if (currentStage < stages.length && migrationLoading.value) {
      progressPercent.value = stages[currentStage].percent
      progressMessage.value = stages[currentStage].message
      currentStage++
    } else {
      clearInterval(interval)
    }
  }, 2000) // 每2秒更新一次
  
  return interval
}

// 开始迁移
const handleStartMigration = async () => {
  migrationLoading.value = true
  progressPercent.value = 0
  migrationResult.value = {}
  
  const progressInterval = simulateProgress()
  
  try {
    const formData = new FormData()
    formData.append('currentThemeZip', fileList.value[0])
    formData.append('themeName', migrationForm.themeName)
    formData.append('targetVersion', migrationForm.targetVersion)
    
    const response = await themeApi.quickMigration(formData)
    
    // 确保进度显示到100%
    progressPercent.value = 100
    progressMessage.value = '迁移完成！'
    
    migrationResult.value = {
      success: true,
      message: '主题迁移成功完成',
      ...response.data
    }
    
    message.success('迁移成功！')
  } catch (error) {
    console.error('Migration failed:', error)
    clearInterval(progressInterval)
    
    migrationResult.value = {
      success: false,
      message: error.response?.data?.message || error.message || '迁移失败，请重试'
    }
    
    message.error('迁移失败')
  } finally {
    migrationLoading.value = false
    clearInterval(progressInterval)
  }
}

// 下载迁移后的主题
const handleDownload = () => {
  if (!migrationResult.value.downloadPath) {
    message.error('无法找到下载路径')
    return
  }
  
  const downloadUrl = `/api/v1/theme/quick-migration/download?path=${encodeURIComponent(migrationResult.value.downloadPath)}`
  
  // 创建隐藏的a标签触发下载
  const link = document.createElement('a')
  link.href = downloadUrl
  link.setAttribute('download', '')
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  
  message.success('开始下载迁移后的主题')
}

// 重置表单
const handleReset = () => {
  migrationForm.themeName = ''
  migrationForm.targetVersion = ''
  fileList.value = []
  migrationResult.value = {}
  progressPercent.value = 0
  progressMessage.value = ''
}

onMounted(() => {
  loadThemeVersions()
})
</script>

<style scoped>
.page-container {
  padding: 24px;
  min-height: calc(100vh - 64px);
}

.content-card {
  background: white;
  border-radius: 8px;
  padding: 32px;
  max-width: 1200px;
  margin: 0 auto;
}

.content-card h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  text-align: center;
}

.subtitle {
  text-align: center;
  color: #666;
  margin-bottom: 32px;
}

.hint {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.progress-container {
  max-width: 600px;
  margin: 32px auto;
  padding: 32px;
  background: #f5f5f5;
  border-radius: 8px;
  text-align: center;
}

.progress-text {
  margin: 16px 0;
  font-size: 14px;
  color: #666;
}

@media (max-width: 576px) {
  .page-container {
    padding: 12px;
  }
  
  .content-card {
    padding: 16px;
  }
}
</style>
