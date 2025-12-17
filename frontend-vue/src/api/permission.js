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

export const permissionApi = {
    // 获取所有权限
    getAll() {
        return handleResponse(request.get('/permissions'))
    },

    // 获取权限详情
    getDetail(id) {
        return handleResponse(request.get(`/permissions/${id}`))
    },

    // 创建权限
    create(data) {
        return handleResponse(request.post('/permissions', data))
    },

    // 更新权限
    update(id, data) {
        return handleResponse(request.put(`/permissions/${id}`, data))
    },

    // 删除权限
    delete(id) {
        return handleResponse(request.delete(`/permissions/${id}`))
    }
}
