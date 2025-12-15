import request from '@/utils/request'

export const userApi = {
  // 获取用户列表（分页）
  getList(params) {
    return request.get('/users', { params })
  },

  // 获取所有用户
  getAll() {
    return request.get('/users/all')
  },

  // 获取用户详情
  getDetail(id) {
    return request.get(`/users/${id}`)
  },

  // 获取当前登录用户信息
  getCurrentUser() {
    return request.get('/auth/current')
  },

  // 创建用户
  create(data) {
    return request.post('/users', data)
  },

  // 更新用户
  update(id, data) {
    return request.put(`/users/${id}`, data)
  },

  // 修改密码
  changePassword(id, data) {
    return request.post(`/users/${id}/password`, data)
  },

  // 更新用户状态
  updateStatus(id, status) {
    return request.put(`/users/${id}/status`, null, { params: { status } })
  },

  // 删除用户
  delete(id) {
    return request.delete(`/users/${id}`)
  }
}
