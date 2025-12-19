import request from '@/utils/request'

const BASE_URL = '/theme'

export const themeApi = {
    // ========== 版本管理 ==========

    /**
     * 上传存档主题版本
     */
    archiveVersion(formData) {
        return request.post(`${BASE_URL}/version/archive`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
    },

    /**
     * 获取版本历史
     */
    getVersionHistory(themeName) {
        return request.get(`${BASE_URL}/version/history/${themeName}`)
    },

    /**
     * 获取当前版本
     */
    getCurrentVersion(themeName) {
        return request.get(`${BASE_URL}/version/current/${themeName}`)
    },

    /**
     * 设置为当前版本
     */
    setCurrentVersion(themeName, version) {
        return request.post(`${BASE_URL}/version/set-current`, null, {
            params: { themeName, version }
        })
    },

    /**
     * 删除版本
     */
    deleteVersion(versionId) {
        return request.delete(`${BASE_URL}/version/${versionId}`)
    },

    /**
     * 触发深度差异分析
     */
    analyzeDiff(themeName, fromVersion, toVersion) {
        return request.post(`${BASE_URL}/version/analyze-diff`, null, {
            params: { themeName, fromVersion, toVersion }
        })
    },

    /**
     * 获取迁移规则
     */
    getMigrationRules(themeName, fromVersion, toVersion, ruleType = null) {
        return request.get(`${BASE_URL}/version/migration-rules`, {
            params: { themeName, fromVersion, toVersion, ruleType }
        })
    },

    // ========== 迁移流程 ==========

    /**
     * 开始迁移（分析+规则生成）
     */
    startMigration(formData) {
        return request.post(`${BASE_URL}/migration/start`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
    },

    /**
     * 执行迁移（用户确认规则后）
     */
    executeMigration(sessionId, confirmedRules) {
        return request.post(`${BASE_URL}/migration/execute`, confirmedRules, {
            params: { sessionId }
        })
    },

    /**
     * 获取迁移会话
     */
    getSession(sessionId) {
        return request.get(`${BASE_URL}/migration/session/${sessionId}`)
    },

    /**
     * 下载迁移后的主题
     */
    downloadMigratedTheme(historyId) {
        return request.get(`${BASE_URL}/migration/download/${historyId}`, {
            responseType: 'blob'
        })
    }
}
