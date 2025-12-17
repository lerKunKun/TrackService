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

export const statsApi = {
  // 获取首页统计数据
  getDashboardStats() {
    return handleResponse(request.get('/stats/dashboard'))
  }
}
