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

export const shopApi = {
  // 获取店铺列表
  getList(params) {
    return handleResponse(request.get('/shops', { params }))
  },

  // 获取店铺详情
  getDetail(id) {
    return handleResponse(request.get(`/shops/${id}`))
  },

  // 删除店铺
  delete(id) {
    return handleResponse(request.delete(`/shops/${id}`))
  },

  // 获取商店详细信息（实时从Shopify获取）
  getShopInfo(id) {
    return handleResponse(request.get(`/shops/${id}/info`))
  },

  // 刷新商店信息
  refreshShopInfo(id) {
    return handleResponse(request.post(`/shops/${id}/refresh-info`))
  },

  // 验证店铺连接
  validateConnection(id) {
    return handleResponse(request.post(`/shops/${id}/validate`))
  },

  // 获取店铺的webhooks
  getWebhooks(id) {
    return handleResponse(request.get(`/shops/${id}/webhooks`))
  },

  // 注册webhooks
  registerWebhooks(id) {
    return handleResponse(request.post(`/shops/${id}/webhooks/register`))
  },

  // 删除webhooks
  deleteWebhooks(id) {
    return handleResponse(request.delete(`/shops/${id}/webhooks`))
  }
}
