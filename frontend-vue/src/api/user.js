import request from '@/utils/request'

// 统一响应处理函数
const handleResponse = async (requestPromise) => {
  const response = await requestPromise
  if (response.code === 200) {
    return response.data
  } else {
    throw new Error(response.message || '请求失败')
  }
}

export const userApi = {
  // 获取用户列表（分页）
  getList(params) {
    return handleResponse(request.get('/users', { params }))
  },

  // 获取所有用户
  getAll() {
    return handleResponse(request.get('/users/all'))
  },

  // 获取用户详情
  getDetail(id) {
    return handleResponse(request.get(`/users/${id}`))
  },

  // 获取当前登录用户信息
  getCurrentUser() {
    return handleResponse(request.get('/auth/current'))
  },

  // 创建用户
  create(data) {
    return handleResponse(request.post('/users', data))
  },

  // 更新用户
  update(id, data) {
    return handleResponse(request.put(`/users/${id}`, data))
  },

  // 修改密码
  changePassword(id, data) {
    return handleResponse(request.post(`/users/${id}/password`, data))
  },

  // 更新用户状态
  updateStatus(id, status) {
    return handleResponse(request.put(`/users/${id}/status`, null, { params: { status } }))
  },

  // 删除用户
  delete(id) {
    return handleResponse(request.delete(`/users/${id}`))
  }
}
