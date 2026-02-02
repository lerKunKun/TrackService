import request from '@/utils/request'

const API_BASE_URL = '/product'  // request已经有/api/v1 baseURL

export default {
    /**
     * 导入Shopify CSV文件
     * @param {File} file CSV文件
     * @returns {Promise}
     */
    async importCsv(file) {
        const formData = new FormData()
        formData.append('file', file)

        const response = await request.post(`${API_BASE_URL}/import`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        })
        return response
    },

    /**
     * 分页查询产品列表
     * @param {Object} params - 查询参数
     * @returns {Promise}
     */
    async getProductList(params) {
        const response = await request.get(`${API_BASE_URL}/list`, { params })
        return response
    },

    /**
     * 导出Shopify CSV
     * @param {Array} productIds 产品ID列表
     * @returns {Promise}
     */
    async exportShopifyCsv(productIds) {
        return request({
            url: '/listing/export/csv',
            method: 'post',
            data: { productIds },
            responseType: 'blob' // 重要：接收文件流
        })
    },

    /**
     * 根据ID查询产品详情
     * @param {Number} id 产品ID
     * @returns {Promise}
     */
    async getProductById(id) {
        const response = await request.get(`${API_BASE_URL}/${id}`)
        return response
    },

    /**
     * 更新产品
     * @param {Number} id 产品ID
     * @param {Object} data 更新数据
     * @returns {Promise}
     */
    async updateProduct(id, data) {
        const response = await request.put(`${API_BASE_URL}/${id}`, data)
        return response
    },

    /**
     * 删除产品
     * @param {Number} id 产品ID
     * @returns {Promise}
     */
    async deleteProduct(id) {
        const response = await request.delete(`${API_BASE_URL}/${id}`)
        return response
    },

    /**
     * 获取产品的所有变体
     * @param {Number} productId 产品ID
     * @returns {Promise}
     */
    async getProductVariants(productId) {
        const response = await request.get(`${API_BASE_URL}/${productId}/variants`)
        return response
    },

    /**
     * 批量更新产品所有变体的价格
     * @param {Number} productId 产品ID
     * @param {Object} priceData - 价格数据 { price, compareAtPrice }
     * @returns {Promise}
     */
    async updateProductPrice(productId, priceData) {
        const response = await request.put(`${API_BASE_URL}/${productId}/price`, priceData)
        return response
    },

    /**
     * 更新单个变体的价格
     * @param {Number} variantId 变体ID
     * @param {Object} priceData - 价格数据 { price, compareAtPrice }
     * @returns {Promise}
     */
    async updateVariantPrice(variantId, priceData) {
        const response = await request.put(`${API_BASE_URL}/variants/${variantId}/price`, priceData)
        return response
    },

    /**
     * 获取采购管理列表（分页+摘要，性能优化版）
     * @param {Object} params - 查询参数 { page, pageSize, keyword, status }
     * @returns {Promise}
     */
    async getProcurementList(params) {
        const response = await request.get(`${API_BASE_URL}/procurement-list`, { params })
        return response
    },

    /**
     * 更新变体采购信息
     * @param {Number} variantId 变体ID
     * @param {Object} procurementData - 采购数据 { sku, procurementUrl, procurementPrice, supplier }
     * @returns {Promise}
     */
    async updateVariantProcurement(variantId, procurementData) {
        const response = await request.put(`${API_BASE_URL}/variants/${variantId}/procurement`, procurementData)
        return response
    }
}
