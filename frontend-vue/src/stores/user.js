import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // 安全地访问localStorage
  const getStorageItem = (key) => {
    try {
      return localStorage.getItem(key) || ''
    } catch (e) {
      console.warn('localStorage access denied:', e)
      return ''
    }
  }

  const token = ref(getStorageItem('token'))
  const username = ref(getStorageItem('username'))
  // 兼容旧数据：如果没有displayName，使用username作为降级
  const displayName = ref(getStorageItem('displayName') || getStorageItem('username'))
  const avatar = ref(getStorageItem('avatar') || '')
  // 权限列表：从localStorage读取并解析
  const permissions = ref((() => {
    try {
      const stored = getStorageItem('permissions')
      return stored ? JSON.parse(stored) : []
    } catch (e) {
      console.warn('Failed to parse permissions from localStorage:', e)
      return []
    }
  })())

  const isLoggedIn = computed(() => !!token.value)

  // 安全地设置localStorage
  const setStorageItem = (key, value) => {
    try {
      localStorage.setItem(key, value)
    } catch (e) {
      console.warn('localStorage write denied:', e)
    }
  }

  const removeStorageItem = (key) => {
    try {
      localStorage.removeItem(key)
    } catch (e) {
      console.warn('localStorage remove denied:', e)
    }
  }

  const login = async (credentials) => {
    const response = await authApi.login(credentials)
    // API返回格式: { code: 200, message: "登录成功", data: { token, username, ... } }
    const data = response.data

    token.value = data.token
    username.value = data.username
    // 优先使用真实姓名，如果没有则使用用户名
    displayName.value = data.realName || data.username
    avatar.value = data.avatar || ''
    // 保存权限列表
    permissions.value = data.permissions || []

    setStorageItem('token', data.token)
    setStorageItem('username', data.username)
    setStorageItem('displayName', data.realName || data.username)
    setStorageItem('avatar', data.avatar || '')
    setStorageItem('permissions', JSON.stringify(data.permissions || []))

    return response
  }

  const logout = () => {
    token.value = ''
    username.value = ''
    displayName.value = ''
    avatar.value = ''
    permissions.value = []
    removeStorageItem('token')
    removeStorageItem('username')
    removeStorageItem('displayName')
    removeStorageItem('avatar')
    removeStorageItem('permissions')
  }

  const setToken = (newToken) => {
    token.value = newToken
    setStorageItem('token', newToken)
  }

  const setUserInfo = (userInfo) => {
    username.value = userInfo.username
    displayName.value = userInfo.realName || userInfo.username
    avatar.value = userInfo.avatar || ''
    // 如果userInfo包含permissions，也保存它
    if (userInfo.permissions) {
      permissions.value = userInfo.permissions
      setStorageItem('permissions', JSON.stringify(userInfo.permissions))
    }
    setStorageItem('username', userInfo.username)
    setStorageItem('displayName', userInfo.realName || userInfo.username)
    setStorageItem('avatar', userInfo.avatar || '')
  }

  return {
    token,
    username,
    displayName,
    avatar,
    permissions,
    isLoggedIn,
    login,
    logout,
    setToken,
    setUserInfo
  }
})
