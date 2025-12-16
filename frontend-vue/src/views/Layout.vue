<template>
  <a-layout class="app-layout">
    <!-- 固定侧边栏 -->
    <a-layout-sider
      v-model:collapsed="collapsed"
      :trigger="null"
      collapsible
      class="fixed-sider"
      :class="{ 'mobile-hidden': isMobile && collapsed }"
      :style="{
        overflow: 'auto',
        height: '100vh',
        position: 'fixed',
        left: 0,
        top: 0,
        bottom: 0,
        zIndex: 1001
      }"
    >
      <!-- Logo 区域 -->
      <div class="sider-logo">
        <img src="../assets/logo.png" alt="比欧网络" class="logo-image" />
        <span v-if="!collapsed" class="logo-text">比欧网络</span>
      </div>

      <!-- 菜单 -->
      <a-menu
        v-model:selectedKeys="selectedKeys"
        mode="inline"
        theme="dark"
        @click="handleMenuClick"
      >
        <a-menu-item key="dashboard">
          <DashboardOutlined />
          <span>首页概览</span>
        </a-menu-item>
        <a-menu-item key="shops">
          <ShopOutlined />
          <span>店铺管理</span>
        </a-menu-item>
        <a-menu-item key="orders">
          <ShoppingOutlined />
          <span>订单管理</span>
        </a-menu-item>
        <a-menu-item key="tracking">
          <CarOutlined />
          <span>运单管理</span>
        </a-menu-item>
        <a-menu-item key="users">
          <TeamOutlined />
          <span>用户管理</span>
        </a-menu-item>
        <a-menu-item key="allowed-corp-ids">
          <SafetyOutlined />
          <span>企业CorpID管理</span>
        </a-menu-item>
        <a-menu-item key="dingtalk-sync">
          <SyncOutlined />
          <span>钉钉组织同步</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <!-- 移动端遮罩层 -->
    <div 
      v-if="isMobile && !collapsed" 
      class="mobile-mask"
      @click="collapsed = true"
    ></div>

    <!-- 主内容区域 -->
    <a-layout :style="mainStyle">
      <!-- 顶部导航栏 -->
      <a-layout-header class="header">
        <div class="header-content">
          <div class="header-left">
            <MenuUnfoldOutlined
              v-if="collapsed"
              class="menu-trigger"
              @click="collapsed = false"
            />
            <MenuFoldOutlined
              v-else
              class="menu-trigger"
              @click="collapsed = true"
            />
            <div class="page-title">
              <span class="logo-text">比欧中台</span>
            </div>
          </div>

          <div class="user-info">
            <a-space :size="12">
              <a-tooltip title="个人主页">
                <a-avatar 
                  :src="userAvatar" 
                  :size="36"
                  @click="goToProfile"
                  class="user-avatar"
                >
                  <template #icon>
                    <UserOutlined />
                  </template>
                </a-avatar>
              </a-tooltip>
              <span class="username-text" @click="goToProfile">
                {{ userStore.displayName }}
              </span>
              <a-button type="link" @click="handleLogout" danger>
                <LogoutOutlined />
                退出登录
              </a-button>
            </a-space>
          </div>
        </div>
      </a-layout-header>

      <!-- 页面内容 -->
      <a-layout-content class="page-container">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Modal, Grid } from 'ant-design-vue'
import {
  RocketOutlined,
  UserOutlined,
  LogoutOutlined,
  DashboardOutlined,
  ShopOutlined,
  ShoppingOutlined,
  CarOutlined,
  TeamOutlined,
  SafetyOutlined,
  SyncOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const useBreakpoint = Grid.useBreakpoint
const screens = useBreakpoint()

const isMobile = computed(() => !screens.value.md)
const collapsed = ref(false)
const selectedKeys = ref(['dashboard'])

// 移动端自动收起侧边栏
watch(isMobile, (val) => {
  if (val) {
    collapsed.value = true
  }
}, { immediate: true })

const mainStyle = computed(() => {
  if (isMobile.value) {
    return { marginLeft: '0', transition: 'margin-left 0.2s' }
  }
  return { marginLeft: collapsed.value ? '80px' : '200px', transition: 'margin-left 0.2s' }
})

// 监听路由变化，更新菜单选中状态
watch(
  () => route.path,
  (path) => {
    if (path.includes('dashboard') || path === '/') {
      selectedKeys.value = ['dashboard']
    } else if (path.includes('shops')) {
      selectedKeys.value = ['shops']
    } else if (path.includes('orders')) {
      selectedKeys.value = ['orders']
    } else if (path.includes('tracking')) {
      selectedKeys.value = ['tracking']
    } else if (path.includes('users')) {
      selectedKeys.value = ['users']
    } else if (path.includes('allowed-corp-ids')) {
      selectedKeys.value = ['allowed-corp-ids']
    } else if (path.includes('dingtalk-sync')) {
      selectedKeys.value = ['dingtalk-sync']
    }
  },
  { immediate: true }
)

const handleMenuClick = ({ key }) => {
  router.push(`/${key}`)
}

const handleLogout = () => {
  Modal.confirm({
    title: '确认退出',
    content: '确定要退出登录吗？',
    okText: '确定',
    cancelText: '取消',
    onOk() {
      userStore.logout()
      router.push('/login')
    }
  })
}

// 计算用户头像
const userAvatar = computed(() => {
  // 如果有头像则使用，否则使用默认头像
  if (userStore.avatar) {
    return userStore.avatar
  }
  // 使用默认头像
  return new URL('../assets/default-avatar.png', import.meta.url).href
})

// 导航到个人主页
const goToProfile = () => {
  console.log('Navigating to profile...')
  router.push({ name: 'Profile' })
}
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
}

/* 固定侧边栏样式 */
.fixed-sider {
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
  z-index: 1000;
}

.fixed-sider :deep(.ant-layout-sider-children) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* Logo 区域 */
.sider-logo {
  height: 66px;
  display: flex;
  align-items: center;
  justify-content: left;
  gap: 12px;
  color: #000;
  background: rgb(255, 255, 255);
  padding: 19px 19px;
  transition: all 0.2s;
}

.sider-logo .logo-image {
  height: 48px;
  width: auto;
  object-fit: contain;
}

.sider-logo .logo-text {
  font-size: 20px;
  font-weight: 600;
  white-space: nowrap;
}

/* 菜单样式 */
.fixed-sider :deep(.ant-menu) {
  flex: 1;
  border-right: 0;
}

.fixed-sider :deep(.ant-menu-item) {
  display: flex;
  align-items: center;
  transition: all 0.2s;
}

.fixed-sider :deep(.ant-menu-item .anticon) {
  font-size: 18px;
  min-width: 18px;
}

/* 折叠按钮 */
.collapse-trigger {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.65);
  background: rgba(255, 255, 255, 0.05);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.2s;
}

.collapse-trigger:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}

.collapse-trigger .anticon {
  font-size: 18px;
}

/* 顶部导航栏 */
.header {
  background: #fff;
  padding: 0;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  position: sticky;
  top: 0;
  z-index: 999;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 100%;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title .logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
}

.user-info {
  color: #595959;
}

.user-info :deep(.ant-btn-link) {
  color: #595959;
}

.user-info :deep(.ant-btn-link:hover) {
  color: #ff4d4f;
}

.user-avatar {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.user-avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.username-text {
  cursor: pointer;
  transition: color 0.2s;
}

.username-text:hover {
  color: #1890ff;
}

/* 主内容区 */
.page-container {
  margin: 0;
  padding: 0;
  background: #f0f2f5;
  min-height: calc(100vh - 64px);
}

.mobile-hidden {
  transform: translateX(-100%);
  transition: transform 0.2s;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.menu-trigger {
  font-size: 20px;
  cursor: pointer;
  padding: 0 12px;
  transition: color 0.3s;
}

.menu-trigger:hover {
  color: #1890ff;
}

.mobile-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 1000;
  transition: all 0.3s;
}

@media (max-width: 576px) {
  .header-content {
    padding: 0 12px;
  }
  
  .page-title .logo-text {
    font-size: 16px;
  }
}
</style>
