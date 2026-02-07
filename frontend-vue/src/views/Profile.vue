<template>
  <div class="profile-container">
    <a-card class="profile-card" :bordered="false">
      <!-- 头部标题 -->
      <div class="profile-header">
        <h2>个人主页</h2>
        <a-button type="primary" @click="goToDashboard">
          <DashboardOutlined />
          返回首页
        </a-button>
      </div>

      <a-divider />

      <!-- 用户信息展示 -->
      <div class="profile-content">
        <!-- 头像区域 -->
        <div class="avatar-section">
          <a-avatar :src="userAvatar" :size="120">
          </a-avatar>
        </div>

        <!-- 信息卡片 -->
        <a-row :gutter="[16, 16]" class="info-section">
          <!-- 基本信息 -->
          <a-col :xs="24" :sm="24" :md="12">
            <a-card title="基本信息" :bordered="false" class="info-card">
              <a-descriptions :column="1">
                <a-descriptions-item label="用户名">
                  {{ userStore.username }}
                </a-descriptions-item>
                <a-descriptions-item label="真实姓名">
                  {{ userStore.displayName }}
                </a-descriptions-item>
                <a-descriptions-item label="登录方式">
                  <a-tag :color="loginSourceColor">
                    {{ loginSourceText }}
                  </a-tag>
                </a-descriptions-item>
              </a-descriptions>
            </a-card>
          </a-col>

          <!-- 联系信息 -->
          <a-col :xs="24" :sm="24" :md="12">
            <a-card title="联系信息" :bordered="false" class="info-card">
              <a-descriptions :column="1">
                <a-descriptions-item label="邮箱">
                  <template v-if="userInfo.email">
                    <MailOutlined />
                    {{ userInfo.email }}
                  </template>
                  <template v-else>
                    <span class="empty-text">未设置</span>
                  </template>
                </a-descriptions-item>
                <a-descriptions-item label="手机号">
                  <template v-if="userInfo.phone">
                    <PhoneOutlined />
                    {{ userInfo.phone }}
                  </template>
                  <template v-else>
                    <span class="empty-text">未设置</span>
                  </template>
                </a-descriptions-item>
                <a-descriptions-item label="角色">
                  <a-tag :color="roleColor">
                    {{ roleText }}
                  </a-tag>
                </a-descriptions-item>
              </a-descriptions>
            </a-card>
          </a-col>

          <!-- 账户信息 -->
          <a-col :xs="24">
            <a-card title="账户信息" :bordered="false" class="info-card">
              <a-descriptions :column="columnConfig">
                <a-descriptions-item label="注册时间">
                  <ClockCircleOutlined />
                  {{ formatDate(userInfo.createdAt) }}
                </a-descriptions-item>
                <a-descriptions-item label="最后登录">
                  <LoginOutlined />
                  {{ formatDate(userInfo.lastLoginTime) }}
                </a-descriptions-item>
                <a-descriptions-item label="账户状态">
                  <a-tag :color="userInfo.status === 1 ? 'success' : 'error'">
                    {{ userInfo.status === 1 ? '正常' : '禁用' }}
                  </a-tag>
                </a-descriptions-item>
              </a-descriptions>
            </a-card>
          </a-col>
        </a-row>
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  UserOutlined,
  DashboardOutlined,
  MailOutlined,
  PhoneOutlined,
  ClockCircleOutlined,
  LoginOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()

const userInfo = ref({
  email: '',
  phone: '',
  role: '',
  loginSource: '',
  status: 1,
  createdAt: '',
  lastLoginTime: ''
})

const loading = ref(false)

// 响应式列配置
const columnConfig = { xs: 1, sm: 2, md: 3 }

// 计算头像
const userAvatar = computed(() => {
  if (userStore.avatar) {
    return userStore.avatar
  }
  return new URL('../assets/default-avatar.png', import.meta.url).href
})

// 登录方式颜色和文本
const loginSourceColor = computed(() => {
  return userInfo.value.loginSource === 'DINGTALK' ? 'blue' : 'green'
})

const loginSourceText = computed(() => {
  return userInfo.value.loginSource === 'DINGTALK' ? '钉钉登录' : '密码登录'
})

// 角色颜色和文本
const roleColor = computed(() => {
  return userInfo.value.role === 'ADMIN' ? 'red' : 'blue'
})

const roleText = computed(() => {
  return userInfo.value.role === 'ADMIN' ? '管理员' : '普通用户'
})

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '未知'
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 返回首页
const goToDashboard = () => {
  router.push('/dashboard')
}

// 获取用户详细信息
const fetchUserInfo = async () => {
  loading.value = true
  try {
    const data = await userApi.getCurrentUser()
    userInfo.value = {
      email: data.email || '',
      phone: data.phone || '',
      role: data.role || 'USER',
      loginSource: data.loginSource || 'PASSWORD',
      status: data.status || 1,
      createdAt: data.createdAt || '',
      lastLoginTime: data.lastLoginTime || ''
    }
  } catch (error) {
    console.error('Failed to fetch user info:', error)
    message.error('获取用户信息失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<style scoped>
.profile-container {
  padding: 24px;
  min-height: calc(100vh - 64px);
  background: #f0f2f5;
}

.profile-card {
  max-width: 1000px;
  margin: 0 auto;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #262626;
}

.profile-content {
  margin-top: 24px;
}

.avatar-section {
  text-align: center;
  margin-bottom: 32px;
}

.avatar-section :deep(.ant-avatar) {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.info-section {
  margin-top: 24px;
}

.info-card {
  background: #fafafa;
  border-radius: 8px;
}

.info-card :deep(.ant-card-head-title) {
  font-weight: 600;
  color: #262626;
}

.info-card :deep(.ant-descriptions-item-label) {
  font-weight: 500;
  color: #595959;
}

.empty-text {
  color: #bfbfbf;
  font-style: italic;
}

@media (max-width: 768px) {
  .profile-container {
    padding: 12px;
  }

  .profile-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .profile-header h2 {
    font-size: 20px;
  }

  .avatar-section :deep(.ant-avatar) {
    width: 100px !important;
    height: 100px !important;
  }
}
</style>
