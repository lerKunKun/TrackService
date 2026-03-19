import request from '@/utils/request'

const handleResponse = async (requestPromise) => {
  const response = await requestPromise
  if (response.code === 200) {
    return response.data
  } else {
    throw new Error(response.message || '请求失败')
  }
}

export const onlineSessionApi = {
  getList() {
    return handleResponse(request.get('/online-sessions'))
  },
  getCount() {
    return handleResponse(request.get('/online-sessions/count'))
  },
  getUserSessions(userId) {
    return handleResponse(request.get(`/online-sessions/user/${userId}`))
  },
  forceLogout(sessionId) {
    return handleResponse(request.delete(`/online-sessions/${sessionId}`))
  }
}
