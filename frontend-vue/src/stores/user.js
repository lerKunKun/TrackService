import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  // 兼容旧数据：如果没有displayName，使用username作为降级
  const displayName = ref(localStorage.getItem('displayName') || localStorage.getItem('username') || '')

  const isLoggedIn = computed(() => !!token.value)

  const login = async (credentials) => {
    const data = await authApi.login(credentials)
    token.value = data.token
    username.value = data.username
    // 优先使用真实姓名，如果没有则使用用户名
    displayName.value = data.realName || data.username

    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)
    localStorage.setItem('displayName', data.realName || data.username)

    return data
  }

  const logout = () => {
    token.value = ''
    username.value = ''
    displayName.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('displayName')
  }

  return {
    token,
    username,
    displayName,
    isLoggedIn,
    login,
    logout
  }
})
