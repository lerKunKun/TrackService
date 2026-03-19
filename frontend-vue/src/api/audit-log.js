import request from '@/utils/request'

const handleResponse = async (requestPromise) => {
  const response = await requestPromise
  if (response.code === 200) {
    return response.data
  } else {
    throw new Error(response.message || '请求失败')
  }
}

export const auditLogApi = {
  getList(params) {
    return handleResponse(request.get('/audit-logs', { params }))
  },
  getDetail(id) {
    return handleResponse(request.get(`/audit-logs/${id}`))
  }
}
