import request from '@/utils/request'

export const statsApi = {
  // 获取首页统计数据
  getDashboardStats() {
    return request.get('/stats/dashboard')
  }
}
