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

export const trackingApi = {
  // 获取运单列表
  getList(params) {
    return handleResponse(request.get('/tracking', { params }))
  },

  // 获取已有的承运商列表（用于筛选）
  getUsedCarriers() {
    return handleResponse(request.get('/tracking/carriers'))
  },

  // 获取运单详情
  getDetail(id) {
    return handleResponse(request.get(`/tracking/${id}`))
  },

  // 创建运单
  create(data) {
    return handleResponse(request.post('/tracking', data))
  },

  // 同步运单
  sync(id) {
    return handleResponse(request.post(`/tracking/${id}/sync`))
  },

  // 更新备注
  updateRemarks(id, remarks) {
    return handleResponse(request.put(`/tracking/${id}/remarks`, { remarks }))
  },

  // 删除运单
  delete(id) {
    return handleResponse(request.delete(`/tracking/${id}`))
  },

  // 批量删除运单
  batchDelete(ids) {
    return handleResponse(request.post('/tracking/batch-delete', { ids }))
  },

  // 批量导入运单
  batchImport(items) {
    return handleResponse(request.post('/tracking/batch-import', { items }))
  }
}
