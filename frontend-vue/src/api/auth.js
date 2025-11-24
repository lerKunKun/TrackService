import request from '@/utils/request'

export const authApi = {
  // 登录
  login(data) {
    return request.post('/auth/login', data)
  },

  // 验证 Token
  validateToken() {
    return request.get('/auth/validate')
  },

  // 获取当前用户
  getCurrentUser() {
    return request.get('/auth/current')
  }
}
