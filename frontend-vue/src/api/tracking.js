import request from '@/utils/request'

export const trackingApi = {
  // 获取运单列表
  getList(params) {
    return request.get('/tracking', { params })
  },

  // 获取已有的承运商列表（用于筛选）
  getUsedCarriers() {
    return request.get('/tracking/carriers')
  },

  // 获取运单详情
  getDetail(id) {
    return request.get(`/tracking/${id}`)
  },

  // 创建运单
  create(data) {
    return request.post('/tracking', data)
  },

  // 同步运单
  sync(id) {
    return request.post(`/tracking/${id}/sync`)
  },

  // 更新备注
  updateRemarks(id, remarks) {
    return request.put(`/tracking/${id}/remarks`, { remarks })
  },

  // 删除运单
  delete(id) {
    return request.delete(`/tracking/${id}`)
  },

  // 批量删除运单
  batchDelete(ids) {
    return request.post('/tracking/batch-delete', { ids })
  },

  // 批量导入运单
  batchImport(items) {
    return request.post('/tracking/batch-import', { items })
  }
}
