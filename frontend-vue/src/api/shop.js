import request from '@/utils/request'

export const shopApi = {
  // 获取店铺列表
  getList(params) {
    return request.get('/shops', { params })
  },

  // 获取店铺详情
  getDetail(id) {
    return request.get(`/shops/${id}`)
  },

  // 创建店铺
  create(data) {
    return request.post('/shops', data)
  },

  // 更新店铺
  update(id, data) {
    return request.put(`/shops/${id}`, data)
  },

  // 删除店铺
  delete(id) {
    return request.delete(`/shops/${id}`)
  }
}
