import request from '@/utils/request'

export const productVisibilityApi = {
    // Get authorized products
    getAuthorizedProducts(params) {
        return request({
            url: '/product-visibility/products',
            method: 'get',
            params
        })
    },

    // Count authorized products
    countAuthorizedProducts(params) {
        return request({
            url: '/product-visibility/products/count',
            method: 'get',
            params
        })
    },

    // Check specific product visibility
    checkProductVisibility(productId, shopId) {
        return request({
            url: `/product-visibility/products/${productId}/check`,
            method: 'get',
            params: { shopId }
        })
    },

    // Grant visibility
    grantVisibility(data) {
        return request({
            url: '/product-visibility/grant',
            method: 'post',
            data
        })
    },

    // Revoke visibility
    revokeVisibility(data) {
        return request({
            url: '/product-visibility/revoke',
            method: 'post',
            data
        })
    },

    // Get authorizations for a product
    getAuthorizations(productId) {
        return request({
            url: `/product-visibility/products/${productId}/authorizations`,
            method: 'get'
        })
    }
}
