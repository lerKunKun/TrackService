<template>
  <div class="page-content">
    <a-tabs v-model:activeKey="activeTab">
      <!-- ===================== 通知接收人 Tab ===================== -->
      <a-tab-pane key="recipients" tab="通知接收人">
        <a-card :bordered="false">
          <template #extra>
            <a-button type="primary" @click="showRecipientModal()">
              <PlusOutlined /> 添加接收人
            </a-button>
          </template>

          <a-table
            :dataSource="recipients"
            :columns="recipientColumns"
            :loading="recipientLoading"
            :rowKey="record => record.id"
            :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'alertTypes'">
                <a-tag v-if="record.alertTypes === 'ALL'" color="blue">全部</a-tag>
                <template v-else>
                  <a-tag v-for="t in record.alertTypes.split(',')" :key="t" color="cyan" style="margin-bottom:2px">
                    {{ alertTypeLabels[t.trim()] || t }}
                  </a-tag>
                </template>
              </template>
              <template v-if="column.key === 'isEnabled'">
                <a-switch :checked="record.isEnabled" @change="toggleRecipient(record)" size="small" />
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="testRecipient(record)" :loading="record._testing">测试</a-button>
                  <a-button type="link" size="small" @click="showRecipientModal(record)">编辑</a-button>
                  <a-popconfirm title="确定删除此接收人？" @confirm="deleteRecipient(record)">
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>


      <!-- ===================== 邮箱监控 Tab ===================== -->
      <a-tab-pane key="emailMonitors" tab="监控邮箱">
        <a-card :bordered="false">
          <template #extra>
            <a-button type="primary" @click="showEmailModal()">
              <PlusOutlined /> 添加邮箱
            </a-button>
          </template>

          <a-table
            :dataSource="emailConfigs"
            :columns="emailColumns"
            :loading="emailLoading"
            :rowKey="record => record.id"
            :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="record.lastCheckStatus === 'SUCCESS' ? 'green' : record.lastCheckStatus === 'FAILED' ? 'red' : 'default'">
                  {{ record.lastCheckStatus || '未检测' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'isEnabled'">
                <a-switch :checked="record.isEnabled" @change="toggleEmail(record)" size="small" />
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="testEmailConnection(record)" :loading="record._testing">
                    测试连接
                  </a-button>
                  <a-button type="link" size="small" @click="showEmailModal(record)">编辑</a-button>
                  <a-popconfirm title="确定删除此邮箱配置？" @confirm="deleteEmail(record)">
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <!-- ================ 接收人编辑对话框 ================ -->
    <a-modal
      v-model:open="recipientModalVisible"
      :title="recipientForm.id ? '编辑接收人' : '添加接收人'"
      @ok="handleRecipientSubmit"
      :confirmLoading="submitting">
      <a-form :model="recipientForm" layout="vertical">
        <a-form-item label="添加方式" required>
          <a-radio-group v-model:value="recipientSource" button-style="solid">
            <a-radio-button value="manual">手动输入</a-radio-button>
            <a-radio-button value="system">选择系统用户</a-radio-button>
          </a-radio-group>
        </a-form-item>

        <a-form-item v-if="recipientSource === 'system'" label="选择用户" required>
          <a-select
            v-model:value="selectedSystemUser"
            placeholder="请选择已同步钉钉的用户"
            show-search
            :filter-option="filterSystemUser"
            :loading="systemUserLoading"
            @change="handleSystemUserChange">
            <a-select-option v-for="user in systemUsers" :key="user.id" :value="user.id">
              {{ user.realName }} ({{ user.dingUserId }})
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="姓名" required>
          <a-input v-model:value="recipientForm.name" placeholder="请输入姓名" :disabled="recipientSource === 'system'" />
        </a-form-item>
        <a-form-item label="钉钉userId" required>
          <a-input v-model:value="recipientForm.dingtalkUserid" placeholder="钉钉通讯录中的userId" :disabled="recipientSource === 'system'" />
          <div v-if="recipientSource === 'manual'" style="color: #999; font-size: 12px; margin-top: 4px">
            请确保输入正确的钉钉userId，否则无法收到消息
          </div>
        </a-form-item>

        <a-form-item label="订阅类型">
          <a-select
            v-model:value="selectedAlertTypes"
            mode="multiple"
            placeholder="请选择订阅类型"
            style="width: 100%">
            <a-select-option value="ALL">全部 (ALL)</a-select-option>
            <a-select-option value="DISPUTE">支付争议</a-select-option>
            <a-select-option value="INFRINGEMENT">IP侵权</a-select-option>
            <a-select-option value="ACCOUNT_RESTRICTED">账号受限</a-select-option>
            <a-select-option value="PAYMENT_HOLD">资金冻结</a-select-option>
            <a-select-option value="APP_UNINSTALLED">应用卸载</a-select-option>
            <a-select-option value="SHOP_ALERT">店铺提示</a-select-option>
            <a-select-option value="DAILY_SUMMARY">每日汇总</a-select-option>
            <a-select-option value="MONTHLY_SUMMARY">月度汇总</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="recipientForm.remark" placeholder="可选备注" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ================ 邮箱编辑对话框 ================ -->
    <a-modal
      v-model:open="emailModalVisible"
      :title="emailForm.id ? '编辑邮箱配置' : '添加邮箱配置'"
      @ok="handleEmailSubmit"
      :confirmLoading="submitting"
      width="560px">
      <a-form :model="emailForm" layout="vertical">
        <a-form-item label="配置名称" required>
          <a-input v-model:value="emailForm.name" placeholder="如: Shopify主邮箱" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="16">
            <a-form-item label="IMAP服务器" required>
              <a-input v-model:value="emailForm.host" placeholder="如: imap.gmail.com" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="端口">
              <a-input-number v-model:value="emailForm.port" :min="1" :max="65535" style="width:100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="邮箱账号" required>
              <a-input v-model:value="emailForm.username" placeholder="email@example.com" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="密码/授权码" required>
              <a-input-password v-model:value="emailForm.password" placeholder="密码或应用专用密码" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="协议">
              <a-select v-model:value="emailForm.protocol">
                <a-select-option value="imaps">IMAPS (SSL)</a-select-option>
                <a-select-option value="imap">IMAP</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="检查间隔(秒)">
              <a-input-number v-model:value="emailForm.checkInterval" :min="60" :max="3600" style="width:100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="发件人过滤">
          <a-input v-model:value="emailForm.senderFilter" placeholder="noreply@shopify.com" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { recipientApi, emailMonitorApi } from '@/api/alert-config'
import { userApi } from '@/api/user'

// ==================== 公共 ====================
const activeTab = ref('recipients')
const submitting = ref(false)

const alertTypeLabels = {
  DISPUTE: '支付争议',
  INFRINGEMENT: 'IP侵权',
  ACCOUNT_RESTRICTED: '账号受限',
  PAYMENT_HOLD: '资金冻结',
  APP_UNINSTALLED: '应用卸载',
  SHOP_ALERT: '店铺提示',
  DAILY_SUMMARY: '每日汇总',
  MONTHLY_SUMMARY: '月度汇总'
}

// ==================== 接收人管理 ====================
const recipientLoading = ref(false)
const recipients = ref([])
const recipientModalVisible = ref(false)
const recipientForm = ref({})
// 增加用户选择相关状态
const recipientSource = ref('manual') // 'manual' | 'system'
const systemUsers = ref([])
const systemUserLoading = ref(false)
const selectedSystemUser = ref(undefined)
// 增加alertTypes数组形式的状态，用于多选控件绑定
const selectedAlertTypes = ref([])

const recipientColumns = [
  { title: '姓名', dataIndex: 'name', key: 'name', width: 120 },
  { title: '钉钉userId', dataIndex: 'dingtalkUserid', key: 'dingtalkUserid', width: 160 },
  { title: '订阅类型', key: 'alertTypes' },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 150 },
  { title: '启用', key: 'isEnabled', width: 80 },
  { title: '操作', key: 'action', width: 140 }
]

const loadRecipients = async () => {
  recipientLoading.value = true
  try {
    const res = await recipientApi.getList()
    recipients.value = res.data || res || []
  } catch (e) {
    console.error('加载接收人失败:', e)
  } finally {
    recipientLoading.value = false
  }
}

// 加载系统用户（仅加载一次）
const loadSystemUsers = async () => {
  if (systemUsers.value.length > 0) return
  systemUserLoading.value = true
  try {
    // 获取所有用户，接口返回 UserDTO 列表
    const res = await userApi.getAll() 
    // 过滤出有 dingUserId 的用户
    const users = (res.data || res || []).filter(u => u.dingUserId)
    systemUsers.value = users
  } catch (e) {
    console.error('加载系统用户失败:', e)
  } finally {
    systemUserLoading.value = false
  }
}

const handleSystemUserChange = (userId) => {
  const user = systemUsers.value.find(u => u.id === userId)
  if (user) {
    recipientForm.value.name = user.realName || user.username
    recipientForm.value.dingtalkUserid = user.dingUserId
  }
}

const filterSystemUser = (input, option) => {
  const user = systemUsers.value.find(u => u.id === option.value)
  if (!user) return false
  const text = (user.realName || '') + ' ' + (user.username || '') + ' ' + (user.dingUserId || '')
  return text.toLowerCase().includes(input.toLowerCase())
}

const showRecipientModal = (record) => {
  if (record) {
    recipientForm.value = { ...record }
    // 解析 alertTypes 字符串为数组
    if (record.alertTypes === 'ALL') {
      selectedAlertTypes.value = ['ALL']
    } else {
      selectedAlertTypes.value = record.alertTypes ? record.alertTypes.split(',').map(s => s.trim()) : []
    }
    recipientSource.value = 'manual' // 编辑模式默认手动，避免覆盖
  } else {
    recipientForm.value = { name: '', dingtalkUserid: '', alertTypes: 'ALL', remark: '' }
    selectedAlertTypes.value = ['ALL']
    recipientSource.value = 'system' // 新增模式默认推荐选择系统用户
    loadSystemUsers() // 预加载用户
  }
  selectedSystemUser.value = undefined
  recipientModalVisible.value = true
}

const handleRecipientSubmit = async () => {
  // 处理 alertTypes
  if (selectedAlertTypes.value.includes('ALL')) {
    recipientForm.value.alertTypes = 'ALL'
  } else if (selectedAlertTypes.value.length > 0) {
    recipientForm.value.alertTypes = selectedAlertTypes.value.join(',')
  } else {
    message.warning('请选择至少一种订阅类型')
    return
  }

  if (!recipientForm.value.name || !recipientForm.value.dingtalkUserid) {
    message.warning('请填写姓名和钉钉userId')
    return
  }
  
  submitting.value = true
  try {
    const res = recipientForm.value.id
      ? await recipientApi.update(recipientForm.value.id, recipientForm.value)
      : await recipientApi.create(recipientForm.value)
    if (res.success !== false) {
      message.success(recipientForm.value.id ? '更新成功' : '添加成功')
      recipientModalVisible.value = false
      loadRecipients()
    } else {
      message.error(res.message || '操作失败')
    }
  } catch (e) {
    message.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const toggleRecipient = async (record) => {
  try {
    await recipientApi.toggle(record.id)
    loadRecipients()
  } catch (e) {
    message.error('切换状态失败')
  }
}

const testRecipient = async (record) => {
  record._testing = true
  try {
    const res = await recipientApi.test(record.id)
    if (res.success) {
      message.success('测试消息已发送 ✅')
    } else {
      message.warning('发送失败: ' + (res.message || ''))
    }
  } catch (e) {
    message.error('测试失败: ' + (e.message || ''))
  } finally {
    record._testing = false
  }
}

const deleteRecipient = async (record) => {
  try {
    await recipientApi.delete(record.id)
    message.success('删除成功')
    loadRecipients()
  } catch (e) {
    message.error('删除失败')
  }
}

// ==================== 邮箱监控管理 ====================
const emailLoading = ref(false)
const emailConfigs = ref([])
const emailModalVisible = ref(false)
const emailForm = ref({})

const emailColumns = [
  { title: '名称', dataIndex: 'name', key: 'name', width: 140 },
  { title: '邮箱', dataIndex: 'username', key: 'username' },
  { title: 'IMAP 服务器', dataIndex: 'host', key: 'host', width: 160 },
  { title: '连接状态', key: 'status', width: 100 },
  { title: '启用', key: 'isEnabled', width: 80 },
  { title: '操作', key: 'action', width: 220 }
]

const loadEmailConfigs = async () => {
  emailLoading.value = true
  try {
    const res = await emailMonitorApi.getList()
    emailConfigs.value = (res.data || res || []).map(c => ({ ...c, _testing: false }))
  } catch (e) {
    console.error('加载邮箱配置失败:', e)
  } finally {
    emailLoading.value = false
  }
}

const showEmailModal = (record) => {
  emailForm.value = record
    ? { ...record }
    : { name: '', host: '', port: 993, protocol: 'imaps', username: '', password: '', senderFilter: 'noreply@shopify.com', checkInterval: 300 }
  emailModalVisible.value = true
}

const handleEmailSubmit = async () => {
  if (!emailForm.value.name || !emailForm.value.host || !emailForm.value.username || !emailForm.value.password) {
    message.warning('请填写完整配置信息')
    return
  }
  submitting.value = true
  try {
    const res = emailForm.value.id
      ? await emailMonitorApi.update(emailForm.value.id, emailForm.value)
      : await emailMonitorApi.create(emailForm.value)
    if (res.success !== false) {
      message.success(emailForm.value.id ? '更新成功' : '添加成功')
      emailModalVisible.value = false
      loadEmailConfigs()
    } else {
      message.error(res.message || '操作失败')
    }
  } catch (e) {
    message.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const toggleEmail = async (record) => {
  try {
    await emailMonitorApi.toggle(record.id)
    loadEmailConfigs()
  } catch (e) {
    message.error('切换状态失败')
  }
}

const deleteEmail = async (record) => {
  try {
    await emailMonitorApi.delete(record.id)
    message.success('删除成功')
    loadEmailConfigs()
  } catch (e) {
    message.error('删除失败')
  }
}

const testEmailConnection = async (record) => {
  record._testing = true
  try {
    const res = await emailMonitorApi.testConnection(record.id)
    if (res.success) {
      message.success('连接成功 ✅')
    } else {
      message.warning('连接失败: ' + (res.message || ''))
    }
    loadEmailConfigs()
  } catch (e) {
    message.error('测试失败: ' + (e.message || ''))
  } finally {
    record._testing = false
  }
}

// ==================== 初始化 ====================
onMounted(() => {
  loadRecipients()
  loadEmailConfigs()
})
</script>

<style scoped>
.page-content {
  padding: 24px;
}
</style>
