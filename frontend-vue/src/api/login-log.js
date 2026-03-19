import request from '@/utils/request'

const handleResponse = async (requestPromise) => {
  const response = await requestPromise
  if (response.code === 200) {
    return response.data
  } else {
    throw new Error(response.message || '请求失败')
  }
}

export const loginLogApi = {
  getList(params) {
    return handleResponse(request.get('/login-logs', { params }))
  }
}
