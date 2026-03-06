import request from '@/utils/request'

// ========== 产品媒体（MinIO 版）==========

/** 分页获取产品列表（带各 tag 文件数量统计） */
export function getProductMediaList(params) {
    return request({ url: '/product-media/list', method: 'get', params })
}

/** 获取某产品某 tag 下的文件列表 */
export function getProductMediaFiles(productId, tag) {
    return request({
        url: `/product-media/${productId}/files`,
        method: 'get',
        params: tag ? { tag } : {}
    })
}

/** 上传文件到 MinIO（multipart） */
export function uploadProductMediaFile(productId, tag, file, onUploadProgress) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('tag', tag)
    return request({
        url: `/product-media/${productId}/upload`,
        method: 'post',
        data: formData,
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress
    })
}

/** 删除文件 */
export function deleteProductMediaFile(objectName) {
    return request({
        url: '/product-media/file',
        method: 'delete',
        params: { objectName }
    })
}

/** 获取对标页链接列表 */
export function getReferenceLink(productId) {
    return request({ url: `/product-media/${productId}/reference-link`, method: 'get' })
}

/** 更新对标页链接列表（数组） */
export function updateReferenceLink(productId, referenceLinks) {
    return request({
        url: `/product-media/${productId}/reference-link`,
        method: 'put',
        data: { referenceLinks }
    })
}

/** 批量从 URL 下载媒体文件并保存到 MinIO */
export function downloadFromUrls(productId, tag, urls) {
    return request({
        url: `/product-media/${productId}/download-urls`,
        method: 'post',
        data: { tag, urls }
    })
}

/** 静默拉取产品变体主图到 MinIO（幂等，已存在自动跳过） */
export function syncProductImages(productId) {
    return request({
        url: `/product-media/${productId}/sync-images`,
        method: 'post'
    })
}


// ========== 产品模板 ==========

export function getProductTemplateList(params) {
    return request({ url: '/product-templates/list', method: 'get', params })
}

export function updateProductTemplateInfo(productId, params) {
    return request({ url: `/product-templates/${productId}`, method: 'put', params })
}

export function previewProductTemplate(productId) {
    return request({ url: `/product-templates/${productId}/preview`, method: 'post' })
}
