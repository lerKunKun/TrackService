import request from '@/utils/request'

export const productPublishApi = {
    // Publish products to shops
    publish(data) {
        return request({
            url: '/product-publish/publish',
            method: 'post',
            data
        })
    },

    // Unpublish products from shops
    unpublish(data) {
        return request({
            url: '/product-publish/unpublish',
            method: 'post',
            data
        })
    }
}
