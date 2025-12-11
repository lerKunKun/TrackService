import request from '@/utils/request'

/**
 * 订单API
 */
export const orderApi = {
    /**
     * 获取订单列表
     */
    getList(params) {
        return request.get('/orders', { params })
    },

    /**
     * 获取订单详情
     */
    getDetail(id) {
        return request.get(`/orders/${id}`)
    }
}

export default orderApi
