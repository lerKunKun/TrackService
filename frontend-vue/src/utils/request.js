import axios from 'axios'
import { message } from 'ant-design-vue'
import router from '@/router'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // Add Shop Context Header
    const shopId = localStorage.getItem('currentShopId')
    if (shopId) {
      config.headers['X-Shop-Id'] = shopId
    }

    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    // 后端返回格式: { code: 0, data: {...}, message: "..." }
    // code === 0 表示成功
    return response.data
  },
  error => {
    if (error.response) {
      const { status } = error.response

      if (status === 401) {
        message.error('登录已过期，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('username')
        router.push('/login')
      } else if (status === 403) {
        message.error('没有权限访问')
      } else if (status === 500) {
        message.error('服务器错误')
      } else {
        message.error(error.response.data?.message || '请求失败')
      }
    } else {
      message.error('网络错误，请检查网络连接')
    }

    return Promise.reject(error)
  }
)

export default request
