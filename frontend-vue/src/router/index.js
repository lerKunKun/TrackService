import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/dingtalk/callback',
    name: 'DingTalkCallback',
    component: () => import('@/views/DingTalkCallback.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页概览' }
      },
      {
        path: 'shops',
        name: 'Shops',
        component: () => import('@/views/Shops.vue'),
        meta: { title: '店铺管理' }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/Orders.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'tracking',
        name: 'Tracking',
        component: () => import('@/views/Tracking.vue'),
        meta: { title: '运单管理' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/Users.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'roles',
        name: 'Roles',
        component: () => import('@/views/system/Roles.vue'),
        meta: { title: '角色管理', requiresAdmin: true }
      },
      {
        path: 'menus',
        name: 'Menus',
        component: () => import('@/views/system/Menus.vue'),
        meta: { title: '菜单管理', requiresAdmin: true }
      },
      {
        path: 'permissions',
        name: 'Permissions',
        component: () => import('@/views/system/Permissions.vue'),
        meta: { title: '权限管理', requiresAdmin: true }
      },
      {
        path: 'allowed-corp-ids',
        name: 'AllowedCorpIds',
        component: () => import('@/views/AllowedCorpIds.vue'),
        meta: { title: '企业CorpID管理', requiresAdmin: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人主页' }
      },
      {
        path: 'dingtalk-sync',
        name: 'DingtalkSync',
        component: () => import('@/views/DingtalkSync.vue'),
        meta: { title: '钉钉组织同步', requiresAdmin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.path === '/login' && userStore.isLoggedIn) {
    next('/')
  } else {
    next()
  }
})

export default router
