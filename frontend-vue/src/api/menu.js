import request from '@/utils/request'

/**
 * 菜单相关API
 */
export const menuApi = {
    /**
     * 获取当前用户的菜单树
     */
    getUserMenuTree() {
        return request.get('/menus/user')
    },

    /**
     * 获取所有菜单树（需要权限）
     */
    getMenuTree() {
        return request.get('/menus/tree')
    },

    /**
     * 获取所有菜单列表
     */
    getAllMenus() {
        return request.get('/menus')
    },

    /**
     * 根据ID获取菜单
     */
    getMenuById(id) {
        return request.get(`/menus/${id}`)
    },

    /**
     * 创建菜单
     */
    createMenu(data) {
        return request.post('/menus', data)
    },

    /**
     * 更新菜单
     */
    updateMenu(id, data) {
        return request.put(`/menus/${id}`, data)
    },

    /**
     * 删除菜单
     */
    deleteMenu(id) {
        return request.delete(`/menus/${id}`)
    }
}

export default menuApi
