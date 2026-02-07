import request from '@/utils/request'

/**
 * 角色管理 API
 */
export const roleApi = {
    /**
     * 获取所有角色
     * @returns {Promise}
     */
    async getAll() {
        const response = await request.get('/roles')
        return response.code === 200 ? response.data : []
    },

    /**
     * 获取角色详情
     * @param {Number} id 角色ID
     * @returns {Promise}
     */
    async getById(id) {
        const response = await request.get(`/roles/${id}`)
        return response.code === 200 ? response.data : null
    },

    /**
     * 创建角色
     * @param {Object} data 角色数据
     * @returns {Promise}
     */
    async create(data) {
        return request.post('/roles', data)
    },

    /**
     * 更新角色
     * @param {Number} id 角色ID
     * @param {Object} data 角色数据
     * @returns {Promise}
     */
    async update(id, data) {
        return request.put(`/roles/${id}`, data)
    },

    /**
     * 删除角色
     * @param {Number} id 角色ID
     * @returns {Promise}
     */
    async delete(id) {
        return request.delete(`/roles/${id}`)
    },

    /**
     * 获取角色的权限
     * @param {Number} roleId 角色ID
     * @returns {Promise}
     */
    async getPermissions(roleId) {
        const response = await request.get(`/roles/${roleId}/permissions`)
        return response.code === 200 ? response.data : []
    },

    /**
     * 更新角色权限
     * @param {Number} roleId 角色ID
     * @param {Array} permissionIds 权限ID列表
     * @returns {Promise}
     */
    async updatePermissions(roleId, permissionIds) {
        return request.put(`/roles/${roleId}/permissions`, { permissionIds })
    },

    /**
     * 获取所有角色（带权限信息）
     * @returns {Promise}
     */
    async getAllWithPermissions() {
        const response = await request.get('/roles/with-permissions')
        return response.code === 200 ? response.data : []
    },

    /**
     * 更新用户的角色
     * @param {Number} userId 用户ID
     * @param {Array} roleIds 角色ID列表
     * @returns {Promise}
     */
    async updateUserRoles(userId, roleIds) {
        return request.put(`/users/${userId}/roles`, { roleIds })
    }
}

export default roleApi
