import request from '@/utils/request'

export const userShopApi = {
    // Get accessible shops for current user
    getMyShops() {
        return request({
            url: '/user-shops/list',
            method: 'get'
        })
    },

    // Get user roles in specific shop
    getMyRolesInShop(shopId) {
        return request({
            url: `/user-shops/${shopId}/roles`,
            method: 'get'
        })
    },

    // Check access to specific shop
    checkShopAccess(shopId) {
        return request({
            url: `/user-shops/${shopId}/check-access`,
            method: 'get'
        })
    }
}
