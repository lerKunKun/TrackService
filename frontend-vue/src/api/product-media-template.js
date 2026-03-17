import request from '@/utils/request'

// ========== 产品媒体 ==========

/** 分页获取产品列表（带各分类文件数量统计） */
export function getProductMediaList(params) {
    return request({ url: '/product-media/list', method: 'get', params })
}

/** 获取某产品某分类下的文件列表 */
export function getProductMediaFiles(productId, category) {
    return request({
        url: `/product-media/${productId}/files`,
        method: 'get',
        params: category ? { category } : {}
    })
}

/** 上传文件（multipart） */
export function uploadProductMediaFile(productId, category, file, onUploadProgress, tags) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('category', category)
    if (tags) formData.append('tags', tags)
    return request({
        url: `/product-media/${productId}/upload`,
        method: 'post',
        data: formData,
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress
    })
}

/** 删除单个文件 */
export function deleteProductMediaFile(fileId) {
    return request({
        url: `/product-media/files/${fileId}`,
        method: 'delete'
    })
}

/** 批量删除文件 */
export function batchDeleteFiles(productId, fileIds) {
    return request({
        url: `/product-media/${productId}/files/batch`,
        method: 'delete',
        data: fileIds
    })
}

/** 更新排序（接收有序 ID 列表） */
export function updateFilesSort(productId, category, sortedIds) {
    return request({
        url: `/product-media/${productId}/files/sort`,
        method: 'put',
        params: { category },
        data: sortedIds
    })
}

/** 移动文件到其他分类 */
export function moveFileCategory(fileId, category) {
    return request({
        url: `/product-media/files/${fileId}/category`,
        method: 'put',
        params: { category }
    })
}

/** 获取对标页链接 */
export function getReferenceLink(productId) {
    return request({ url: `/product-media/${productId}/reference-link`, method: 'get' })
}

/** 更新对标页链接（数组） */
export function updateReferenceLink(productId, referenceLinks) {
    return request({
        url: `/product-media/${productId}/reference-link`,
        method: 'put',
        data: { referenceLinks }
    })
}

/** 批量从 URL 下载媒体文件并保存到 MinIO */
export function downloadFromUrls(productId, tag, urls, tags) {
    return request({
        url: `/product-media/${productId}/download-urls`,
        method: 'post',
        data: { tag, urls, tags: tags || null }
    })
}

/** 单个文件下载（返回 Blob） */
export function downloadMediaFile(fileId) {
    return request({
        url: `/product-media/files/${fileId}/download`,
        method: 'get',
        responseType: 'blob',
        timeout: 60000
    })
}

/** 批量下载 ZIP（返回 Blob） */
export function batchDownloadMediaFiles(fileIds) {
    return request({
        url: '/product-media/files/batch-download',
        method: 'post',
        data: fileIds,
        responseType: 'blob',
        timeout: 120000
    })
}

/** 同步产品主图到 MinIO */
export function syncProductImages(productId) {
    return request({
        url: `/product-media/${productId}/sync-images`,
        method: 'post',
        timeout: 300000  // 5分钟，图片多时下载耗时较长
    })
}

/** 数据迁移（一次性，管理员） */
export function migrateProductMedia() {
    return request({
        url: '/product-media/migrate',
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

export function pullThemeFiles(productId) {
    return request({ url: `/product-templates/${productId}/pull-theme`, method: 'post' })
}

export function previewProductTemplate(productId) {
    return request({ url: `/product-templates/${productId}/preview`, method: 'post' })
}

export function getThemeFiles(productId) {
    return request({ url: `/product-templates/${productId}/theme-files`, method: 'get' })
}

export function getDevStore() {
    return request({ url: '/product-templates/dev-store', method: 'get' })
}

export function setDevStore(shopId) {
    return request({ url: `/product-templates/dev-store/${shopId}`, method: 'put' })
}
