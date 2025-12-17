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

/**
 * 订单API
 */
export const orderApi = {
    /**
     * 获取订单列表
     */
    getList(params) {
        return handleResponse(request.get('/orders', { params }))
    },

    /**
     * 获取订单详情
     */
    getDetail(id) {
        return handleResponse(request.get(`/orders/${id}`))
    }
}

export default orderApi
