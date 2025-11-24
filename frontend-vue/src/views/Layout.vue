<template>
  <a-layout style="min-height: 100vh">
    <!-- 顶部导航栏 -->
    <a-layout-header class="header">
      <div class="header-content">
        <div class="logo">
          <RocketOutlined class="logo-icon" />
          <span class="logo-text">比欧物流追踪系统</span>
        </div>

        <div class="user-info">
          <a-space>
            <UserOutlined />
            <span>{{ userStore.displayName }}</span>
            <a-button type="link" @click="handleLogout">
              <LogoutOutlined />
              退出登录
            </a-button>
          </a-space>
        </div>
      </div>
    </a-layout-header>

    <a-layout>
      <!-- 侧边菜单 -->
      <a-layout-sider v-model:collapsed="collapsed" :trigger="null" collapsible>
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="inline"
          :theme="'dark'"
          @click="handleMenuClick"
        >
          <a-menu-item key="shops">
            <ShopOutlined />
            <span>店铺管理</span>
          </a-menu-item>
          <a-menu-item key="tracking">
            <CarOutlined />
            <span>运单管理</span>
          </a-menu-item>
          <a-menu-item key="users">
            <TeamOutlined />
            <span>用户管理</span>
          </a-menu-item>
        </a-menu>
      </a-layout-sider>

      <!-- 主内容区 -->
      <a-layout>
        <a-layout-content class="page-container">
          <router-view />
        </a-layout-content>
      </a-layout>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Modal } from 'ant-design-vue'
import {
  RocketOutlined,
  UserOutlined,
  LogoutOutlined,
  ShopOutlined,
  CarOutlined,
  TeamOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const collapsed = ref(false)
const selectedKeys = ref(['shops'])

// 监听路由变化，更新菜单选中状态
watch(
  () => route.path,
  (path) => {
    if (path.includes('shops')) {
      selectedKeys.value = ['shops']
    } else if (path.includes('tracking')) {
      selectedKeys.value = ['tracking']
    } else if (path.includes('users')) {
      selectedKeys.value = ['users']
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
</script>

<style scoped>
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

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  font-size: 24px;
  color: #1890ff;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
}

.user-info {
  color: #595959;
}

.ant-layout-sider {
  background: #001529;
}

.page-container {
  margin: 0;
  padding: 0;
  background: #f0f2f5;
}
</style>
