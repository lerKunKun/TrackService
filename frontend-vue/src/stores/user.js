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
    const data = await authApi.login(credentials)
    token.value = data.token
    username.value = data.username
    // 优先使用真实姓名，如果没有则使用用户名
    displayName.value = data.realName || data.username

    setStorageItem('token', data.token)
    setStorageItem('username', data.username)
    setStorageItem('displayName', data.realName || data.username)

    return data
  }

  const logout = () => {
    token.value = ''
    username.value = ''
    displayName.value = ''
    removeStorageItem('token')
    removeStorageItem('username')
    removeStorageItem('displayName')
  }

  const setToken = (newToken) => {
    token.value = newToken
    setStorageItem('token', newToken)
  }

  const setUserInfo = (userInfo) => {
    username.value = userInfo.username
    displayName.value = userInfo.realName || userInfo.username
    setStorageItem('username', userInfo.username)
    setStorageItem('displayName', userInfo.realName || userInfo.username)
  }

  return {
    token,
    username,
    displayName,
    isLoggedIn,
    login,
    logout,
    setToken,
    setUserInfo
  }
})
