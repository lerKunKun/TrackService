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

          <!-- 接收人筛选栏 -->
          <div style="margin-bottom: 16px; display: flex; gap: 8px; flex-wrap: wrap;">
            <a-input-search
              v-model:value="recipientKeyword"
              placeholder="搜索姓名/钉钉ID/备注"
              allow-clear
              style="width: 220px"
            />
            <a-select
              v-model:value="recipientFilterAlertType"
              placeholder="订阅类型"
              allow-clear
              style="width: 140px"
            >
              <a-select-option v-for="(label, val) in alertTypeLabels" :key="val" :value="val">
                {{ label }}
              </a-select-option>
            </a-select>
          </div>

          <a-table
            :dataSource="filteredRecipients"
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

          <!-- 邮箱筛选栏 -->
          <div style="margin-bottom: 16px; display: flex; gap: 8px; flex-wrap: wrap;">
            <a-input-search
              v-model:value="emailKeyword"
              placeholder="搜索名称/邮箱/主机"
              allow-clear
              style="width: 220px"
            />
            <a-select
              v-model:value="emailFilterEnabled"
              placeholder="启用状态"
              allow-clear
              style="width: 120px"
            >
              <a-select-option :value="true">已启用</a-select-option>
              <a-select-option :value="false">已禁用</a-select-option>
            </a-select>
          </div>

          <a-table
            :dataSource="filteredEmailConfigs"
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
      :title="emailForm.id ? '编辑邮箱配置' : '添加监控邮箱'"
      :footer="null"
      width="520px">
      <a-form :model="emailForm" layout="vertical">

        <!-- 邮箱地址 + 一键识别 -->
        <a-form-item label="邮箱地址" required>
          <a-input-search
            v-model:value="emailForm.username"
            placeholder="请输入邮箱地址，如 shop@gmail.com"
            enter-button="一键识别"
            @search="detectProvider"
            @blur="detectProvider" />
        </a-form-item>

        <!-- 识别结果提示（非 Microsoft） -->
        <div v-if="detectedProvider && !isMicrosoftOAuth" style="margin: -8px 0 16px 0">
          <a-alert
            :message="`已自动识别：${detectedProvider.name}`"
            :description="detectedProvider.guidance"
            type="success"
            show-icon
            :closable="false"
            style="font-size: 13px" />
        </div>
        <div v-else-if="!isMicrosoftOAuth && emailForm.username && emailForm.username.includes('@') && !detectedProvider"
             style="margin: -8px 0 16px 0">
          <a-alert
            message="未识别到邮件服务商，请在高级设置中手动填写 IMAP 服务器"
            type="warning"
            show-icon
            :closable="false"
            style="font-size: 13px" />
        </div>

        <!-- ===== Microsoft OAuth 授权区域 ===== -->
        <div v-if="isMicrosoftOAuth" style="margin: -8px 0 16px 0">
          <!-- 已完成 OAuth 授权 -->
          <a-alert
            v-if="emailForm.authType === 'OAUTH2_MICROSOFT'"
            type="success"
            show-icon
            message="已完成 Microsoft OAuth2 授权"
            description="连接凭证由 Microsoft 颁发并自动刷新，无需填写密码。若需重置请点击下方「重新授权」。"
            :closable="false"
            style="font-size: 13px; margin-bottom: 0" />
          <!-- 尚未授权 -->
          <a-alert
            v-else
            type="info"
            show-icon
            message="Outlook 需要 Microsoft OAuth2 授权"
            description="此邮箱不支持普通密码登录，需通过 Microsoft 账号授权。保存配置后点击「授权 Microsoft」完成认证。"
            :closable="false"
            style="font-size: 13px; margin-bottom: 0" />
        </div>

        <!-- ===== 密码（非 Microsoft 邮箱） ===== -->
        <a-form-item v-if="!isMicrosoftOAuth" required>
          <template #label>
            <span>密码 / 授权码</span>
            <a v-if="detectedProvider && detectedProvider.helpUrl"
               :href="detectedProvider.helpUrl" target="_blank"
               style="margin-left: 8px; font-size: 12px">
              如何获取？
            </a>
          </template>
          <a-input-password
            v-model:value="emailForm.password"
            :placeholder="detectedProvider ? detectedProvider.passwordHint : '请输入密码或应用专用密码'" />
        </a-form-item>

        <!-- 配置名称（自动填入，可修改） -->
        <a-form-item label="配置名称" required>
          <a-input v-model:value="emailForm.name" placeholder="如: Shopify主邮箱" />
        </a-form-item>

        <!-- 高级设置折叠 -->
        <a-collapse v-model:activeKey="advancedOpen" :bordered="false"
                    style="background: #fafafa; border-radius: 6px; margin-bottom: 4px">
          <a-collapse-panel key="advanced" header="高级设置（服务器 / 过滤 / 间隔）">
            <a-row :gutter="16">
              <a-col :span="16">
                <a-form-item label="IMAP 服务器" style="margin-bottom: 12px">
                  <a-input v-model:value="emailForm.host" placeholder="如: imap.gmail.com" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="端口" style="margin-bottom: 12px">
                  <a-input-number v-model:value="emailForm.port" :min="1" :max="65535" style="width:100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="协议" style="margin-bottom: 12px">
                  <a-select v-model:value="emailForm.protocol">
                    <a-select-option value="imaps">IMAPS (SSL)</a-select-option>
                    <a-select-option value="imap">IMAP</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="检查间隔(秒)" style="margin-bottom: 12px">
                  <a-input-number v-model:value="emailForm.checkInterval" :min="60" :max="3600" style="width:100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="发件人过滤" style="margin-bottom: 0">
              <a-input v-model:value="emailForm.senderFilter" placeholder="noreply@shopify.com" />
            </a-form-item>
          </a-collapse-panel>
        </a-collapse>

      </a-form>

      <!-- 自定义底部按钮 -->
      <div style="text-align: right; margin-top: 16px; padding-top: 12px; border-top: 1px solid #f0f0f0">
        <a-space>
          <a-button @click="emailModalVisible = false">取消</a-button>

          <!-- Microsoft OAuth 邮箱：保存 + 授权两个按钮 -->
          <template v-if="isMicrosoftOAuth">
            <a-button @click="handleEmailSubmit" :loading="submitting">
              仅保存
            </a-button>
            <a-button type="primary" :loading="oauthLoading" @click="handleMicrosoftOAuth">
              {{ emailForm.authType === 'OAUTH2_MICROSOFT' ? '重新授权 Microsoft' : '保存并授权 Microsoft' }}
            </a-button>
          </template>

          <!-- 普通邮箱：直接保存 -->
          <a-button v-else type="primary" :loading="submitting" @click="handleEmailSubmit">
            保存
          </a-button>
        </a-space>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { recipientApi, emailMonitorApi } from '@/api/alert-config'
import { userApi } from '@/api/user'

// Microsoft OAuth 支持的域名
const MICROSOFT_OAUTH_DOMAINS = new Set(['outlook.com', 'hotmail.com', 'live.com', 'msn.com'])

// ==================== 邮件服务商 IMAP 配置库 ====================
const EMAIL_PROVIDERS = {
  'gmail.com':       { name: 'Gmail',       host: 'imap.gmail.com',           port: 993, protocol: 'imaps', passwordHint: '请输入 Google 应用专用密码', guidance: '需开启两步验证后生成应用专用密码：Google账号 → 安全性 → 两步验证 → 应用专用密码', helpUrl: 'https://myaccount.google.com/apppasswords' },
  'googlemail.com':  { name: 'Gmail',       host: 'imap.gmail.com',           port: 993, protocol: 'imaps', passwordHint: '请输入 Google 应用专用密码', guidance: '需开启两步验证后生成应用专用密码：Google账号 → 安全性 → 两步验证 → 应用专用密码', helpUrl: 'https://myaccount.google.com/apppasswords' },
  'outlook.com':     { name: 'Outlook',     host: 'outlook.office365.com',    port: 993, protocol: 'imaps', passwordHint: '请输入 Microsoft 账户密码或应用密码', guidance: '第一步：开启 IMAP（点击"如何获取？"直达设置页）→ 启用 IMAP；第二步：若开启了二步验证，需生成应用密码后填入', helpUrl: 'https://outlook.live.com/mail/options/mail/accounts/popImap' },
  'hotmail.com':     { name: 'Hotmail',     host: 'outlook.office365.com',    port: 993, protocol: 'imaps', passwordHint: '请输入 Microsoft 账户密码或应用密码', guidance: '第一步：开启 IMAP（点击"如何获取？"直达设置页）→ 启用 IMAP；第二步：若开启了二步验证，需生成应用密码后填入', helpUrl: 'https://outlook.live.com/mail/options/mail/accounts/popImap' },
  'live.com':        { name: 'Live Mail',   host: 'outlook.office365.com',    port: 993, protocol: 'imaps', passwordHint: '请输入 Microsoft 账户密码或应用密码', guidance: '第一步：开启 IMAP（点击"如何获取？"直达设置页）→ 启用 IMAP；第二步：若开启了二步验证，需生成应用密码后填入', helpUrl: 'https://outlook.live.com/mail/options/mail/accounts/popImap' },
  'msn.com':         { name: 'MSN Mail',    host: 'outlook.office365.com',    port: 993, protocol: 'imaps', passwordHint: '请输入 Microsoft 账户密码或应用密码', guidance: '第一步：开启 IMAP（点击"如何获取？"直达设置页）→ 启用 IMAP；第二步：若开启了二步验证，需生成应用密码后填入', helpUrl: 'https://outlook.live.com/mail/options/mail/accounts/popImap' },
  'yahoo.com':       { name: 'Yahoo Mail',  host: 'imap.mail.yahoo.com',      port: 993, protocol: 'imaps', passwordHint: '请输入 Yahoo 应用密码', guidance: '需生成应用专用密码：Yahoo账户设置 → 安全 → 生成应用密码', helpUrl: 'https://login.yahoo.com/account/security' },
  'qq.com':          { name: 'QQ邮箱',      host: 'imap.qq.com',              port: 993, protocol: 'imaps', passwordHint: '请输入 QQ邮箱授权码', guidance: '需开启IMAP并获取授权码：QQ邮箱 → 设置 → 账户 → 开启IMAP服务 → 生成授权码', helpUrl: 'https://wx.mail.qq.com/list/readtemplate?name=app_intro.html' },
  'foxmail.com':     { name: 'Foxmail',     host: 'imap.qq.com',              port: 993, protocol: 'imaps', passwordHint: '请输入 QQ邮箱授权码', guidance: 'Foxmail 使用QQ邮箱服务器，需在QQ邮箱中开启IMAP并生成授权码' },
  '163.com':         { name: '网易163邮箱', host: 'imap.163.com',             port: 993, protocol: 'imaps', passwordHint: '请输入邮箱客户端授权密码', guidance: '需开启IMAP并获取授权密码：163邮箱 → 设置 → POP3/SMTP/IMAP → 开启服务 → 获取授权码' },
  '126.com':         { name: '网易126邮箱', host: 'imap.126.com',             port: 993, protocol: 'imaps', passwordHint: '请输入邮箱客户端授权密码', guidance: '需开启IMAP并获取授权密码：126邮箱 → 设置 → POP3/SMTP/IMAP → 开启服务 → 获取授权码' },
  'yeah.net':        { name: '网易Yeah邮箱',host: 'imap.yeah.net',            port: 993, protocol: 'imaps', passwordHint: '请输入邮箱客户端授权密码', guidance: '需在网易邮箱设置中开启IMAP服务并获取授权密码' },
  'sina.com':        { name: '新浪邮箱',    host: 'imap.sina.com',            port: 993, protocol: 'imaps', passwordHint: '请输入邮箱密码', guidance: '请使用新浪邮箱账户密码，若开启了二次验证请先关闭' },
  'sohu.com':        { name: '搜狐邮箱',    host: 'imap.sohu.com',            port: 993, protocol: 'imaps', passwordHint: '请输入邮箱密码', guidance: '请使用搜狐邮箱账户密码' },
  '139.com':         { name: '中国移动139', host: 'imap.139.com',             port: 993, protocol: 'imaps', passwordHint: '请输入邮箱密码', guidance: '请使用139邮箱账户密码' },
  'icloud.com':      { name: 'iCloud邮件', host: 'imap.mail.me.com',          port: 993, protocol: 'imaps', passwordHint: '请输入 iCloud 应用专用密码', guidance: '需生成应用专用密码：Apple ID → 安全 → 生成应用专用密码', helpUrl: 'https://appleid.apple.com' },
  'me.com':          { name: 'iCloud邮件', host: 'imap.mail.me.com',          port: 993, protocol: 'imaps', passwordHint: '请输入 iCloud 应用专用密码', guidance: '需生成应用专用密码：Apple ID → 安全 → 生成应用专用密码' },
  'mac.com':         { name: 'iCloud邮件', host: 'imap.mail.me.com',          port: 993, protocol: 'imaps', passwordHint: '请输入 iCloud 应用专用密码', guidance: '需生成应用专用密码：Apple ID → 安全 → 生成应用专用密码' },
  'zoho.com':        { name: 'Zoho Mail',  host: 'imap.zoho.com',            port: 993, protocol: 'imaps', passwordHint: '请输入 Zoho 账户密码或应用密码', guidance: '可直接使用账户密码，或在 Zoho 账户设置中生成应用专用密码' },
  'aol.com':         { name: 'AOL Mail',   host: 'imap.aol.com',             port: 993, protocol: 'imaps', passwordHint: '请输入 AOL 应用密码', guidance: '需在 AOL 账户安全设置中生成应用密码' },
  'yandex.com':      { name: 'Yandex Mail',host: 'imap.yandex.com',          port: 993, protocol: 'imaps', passwordHint: '请输入 Yandex 账户密码', guidance: '可直接使用账户密码（需在邮件客户端设置中开启IMAP访问）' },
  'gmx.com':         { name: 'GMX Mail',   host: 'imap.gmx.com',             port: 993, protocol: 'imaps', passwordHint: '请输入 GMX 账户密码', guidance: '请使用 GMX 账户密码' },
}

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
const recipientKeyword = ref('')
const recipientFilterAlertType = ref(undefined)

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

const filteredRecipients = computed(() => {
  let result = recipients.value
  const kw = recipientKeyword.value.trim().toLowerCase()
  if (kw) {
    result = result.filter(r =>
      (r.name && r.name.toLowerCase().includes(kw)) ||
      (r.dingtalkUserid && r.dingtalkUserid.toLowerCase().includes(kw)) ||
      (r.remark && r.remark.toLowerCase().includes(kw))
    )
  }
  if (recipientFilterAlertType.value) {
    result = result.filter(r =>
      r.alertTypes === 'ALL' ||
      (r.alertTypes && r.alertTypes.split(',').map(t => t.trim()).includes(recipientFilterAlertType.value))
    )
  }
  return result
})

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
const emailKeyword = ref('')
const emailFilterEnabled = ref(undefined)

const filteredEmailConfigs = computed(() => {
  let result = emailConfigs.value
  const kw = emailKeyword.value.trim().toLowerCase()
  if (kw) {
    result = result.filter(e =>
      (e.name && e.name.toLowerCase().includes(kw)) ||
      (e.username && e.username.toLowerCase().includes(kw)) ||
      (e.host && e.host.toLowerCase().includes(kw))
    )
  }
  if (emailFilterEnabled.value !== undefined && emailFilterEnabled.value !== null) {
    result = result.filter(e => e.isEnabled === emailFilterEnabled.value)
  }
  return result
})

const emailLoading = ref(false)
const emailConfigs = ref([])
const emailModalVisible = ref(false)
const emailForm = ref({})
const detectedProvider = ref(null)
const advancedOpen = ref([]) // 折叠面板默认关闭
const oauthLoading = ref(false)

// 当前邮箱是否属于 Microsoft OAuth 域名
const isMicrosoftOAuth = computed(() => {
  const email = (emailForm.value.username || '').trim()
  if (!email.includes('@')) return false
  return MICROSOFT_OAUTH_DOMAINS.has(email.split('@')[1].toLowerCase())
})

// 根据邮箱地址自动识别 IMAP 配置
const detectProvider = () => {
  const email = (emailForm.value.username || '').trim()
  if (!email.includes('@')) {
    detectedProvider.value = null
    return
  }
  const domain = email.split('@')[1].toLowerCase()
  const provider = EMAIL_PROVIDERS[domain] || null
  detectedProvider.value = provider

  if (provider) {
    // 自动填入服务器参数（不覆盖用户已手动修改的值）
    emailForm.value.host = provider.host
    emailForm.value.port = provider.port
    emailForm.value.protocol = provider.protocol
    // 自动生成配置名称（如果还是空的）
    if (!emailForm.value.name) {
      emailForm.value.name = `${provider.name} - ${email}`
    }
  } else {
    // 未知服务商时展开高级设置，方便手填
    advancedOpen.value = ['advanced']
  }
}

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
  detectedProvider.value = null
  advancedOpen.value = []
  if (record) {
    emailForm.value = { ...record }
    // 编辑时展开高级设置，方便查看现有配置
    advancedOpen.value = ['advanced']
    // 重新识别一次，显示服务商提示
    detectProvider()
  } else {
    emailForm.value = { name: '', host: '', port: 993, protocol: 'imaps', username: '', password: '', senderFilter: 'noreply@shopify.com', checkInterval: 300 }
  }
  emailModalVisible.value = true
}

const handleEmailSubmit = async () => {
  if (!emailForm.value.name || !emailForm.value.host || !emailForm.value.username) {
    message.warning('请填写邮箱地址和配置名称')
    return
  }
  // 非 Microsoft OAuth 邮箱必须填密码
  if (!isMicrosoftOAuth.value && !emailForm.value.password) {
    message.warning('请填写密码或授权码')
    return
  }
  submitting.value = true
  try {
    const payload = { ...emailForm.value }
    if (isMicrosoftOAuth.value) {
      payload.authType = 'OAUTH2_MICROSOFT'
      payload.password = payload.password || ''
    }
    const res = emailForm.value.id
      ? await emailMonitorApi.update(emailForm.value.id, payload)
      : await emailMonitorApi.create(payload)
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

/**
 * Microsoft OAuth 授权：先保存配置，再打开授权弹窗
 */
const handleMicrosoftOAuth = async () => {
  if (!emailForm.value.name || !emailForm.value.username) {
    message.warning('请填写邮箱地址和配置名称')
    return
  }
  oauthLoading.value = true
  try {
    // 1. 若是新配置，先保存（得到 id）
    if (!emailForm.value.id) {
      const payload = {
        ...emailForm.value,
        authType: 'OAUTH2_MICROSOFT',
        password: ''
      }
      const saveRes = await emailMonitorApi.create(payload)
      if (saveRes.success === false) {
        message.error(saveRes.message || '保存失败')
        return
      }
      emailForm.value.id = saveRes.data.id
      loadEmailConfigs()
    }

    // 2. 获取授权 URL
    const res = await emailMonitorApi.getMicrosoftAuthUrl(emailForm.value.id)
    if (!res.success) {
      message.error(res.message || '获取授权链接失败，请检查 Microsoft OAuth2 配置')
      return
    }

    // 3. 打开授权弹窗
    const popup = window.open(res.authUrl, 'ms-oauth',
      'width=520,height=640,left=200,top=100,menubar=no,toolbar=no,location=no')
    if (!popup) {
      message.warning('弹窗被拦截，请允许弹窗后重试')
      return
    }

    // 4. 监听授权结果（postMessage）
    const onMessage = (event) => {
      if (!event.data || event.data.type !== 'ms-oauth-callback') return
      window.removeEventListener('message', onMessage)
      if (event.data.success) {
        message.success('Microsoft 授权成功！')
        emailForm.value.authType = 'OAUTH2_MICROSOFT'
        emailModalVisible.value = false
        loadEmailConfigs()
      } else {
        message.error('Microsoft 授权失败，请重试')
      }
    }
    window.addEventListener('message', onMessage)

    // 5. 防止弹窗长时间未响应
    setTimeout(() => {
      window.removeEventListener('message', onMessage)
    }, 5 * 60 * 1000)

  } catch (e) {
    message.error(e.message || '授权流程异常')
  } finally {
    oauthLoading.value = false
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
