import request from '@/utils/request'

// ==================== 通知接收人管理 ====================

export const recipientApi = {
    // 获取所有接收人
    getList() {
        return request.get('/alert-config/recipients')
    },

    // 添加接收人
    create(data) {
        return request.post('/alert-config/recipients', data)
    },

    // 更新接收人
    update(id, data) {
        return request.put(`/alert-config/recipients/${id}`, data)
    },

    // 删除接收人
    delete(id) {
        return request.delete(`/alert-config/recipients/${id}`)
    },

    // 启停接收人
    toggle(id) {
        return request.put(`/alert-config/recipients/${id}/toggle`)
    },

    // 测试通知
    test(id) {
        return request.post(`/alert-config/recipients/${id}/test`)
    }
}

// ==================== 邮箱监控配置管理 ====================

export const emailMonitorApi = {
    // 获取所有邮箱配置
    getList() {
        return request.get('/alert-config/email-monitors')
    },

    // 添加邮箱配置
    create(data) {
        return request.post('/alert-config/email-monitors', data)
    },

    // 更新邮箱配置
    update(id, data) {
        return request.put(`/alert-config/email-monitors/${id}`, data)
    },

    // 删除邮箱配置
    delete(id) {
        return request.delete(`/alert-config/email-monitors/${id}`)
    },

    // 启停邮箱监控
    toggle(id) {
        return request.put(`/alert-config/email-monitors/${id}/toggle`)
    },

    // 测试邮箱连接
    testConnection(id) {
        return request.post(`/alert-config/email-monitors/${id}/test-connection`)
    }
}
