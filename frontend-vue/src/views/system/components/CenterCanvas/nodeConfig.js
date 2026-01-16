/**
 * 节点样式配置
 * 定义不同类型节点的颜色和样式
 */

// 节点类型配置
export const nodeConfig = {
    organization: {
        label: '组织',
        icon: 'BankOutlined',
        color: '#1890ff',
        gradient: 'linear-gradient(135deg, #1890ff, #096dd9)'
    },
    department: {
        label: '部门',
        icon: 'ApartmentOutlined',
        color: '#52c41a',
        gradient: 'linear-gradient(135deg, #52c41a, #389e0d)'
    },
    user: {
        label: '用户',
        icon: 'UserOutlined',
        color: '#13c2c2',
        gradient: 'linear-gradient(135deg, #13c2c2, #08979c)'
    },
    role: {
        label: '角色',
        icon: 'TeamOutlined',
        color: '#722ed1',
        gradient: 'linear-gradient(135deg, #722ed1, #531dab)'
    },
    permission: {
        label: '权限',
        icon: 'LockOutlined',
        color: '#fa8c16',
        gradient: 'linear-gradient(135deg, #fa8c16, #d46b08)'
    },
    menu: {
        label: '菜单',
        icon: 'MenuOutlined',
        color: '#eb2f96',
        gradient: 'linear-gradient(135deg, #eb2f96, #c41d7f)'
    }
}

/**
 * 获取节点样式
 * @param {string} type 节点类型
 * @returns {object} 样式对象
 */
export const getNodeStyle = (type) => {
    const config = nodeConfig[type] || nodeConfig.organization

    return {
        backgroundColor: config.color,
        borderColor: config.color,
        gradient: config.gradient,
        textColor: '#ffffff'
    }
}

/**
 * 连接规则
 * 定义节点之间的连接关系
 */
export const connectionRules = {
    organization: ['department'],      // 组织 → 部门
    department: ['user'],              // 部门 → 用户
    user: ['role'],                    // 用户 → 角色
    role: ['permission', 'menu']       // 角色 → 权限/菜单
}

/**
 * 验证连接是否合法
 * @param {string} sourceType 源节点类型
 * @param {string} targetType 目标节点类型
 * @returns {boolean} 是否合法
 */
export const validateConnection = (sourceType, targetType) => {
    const allowed = connectionRules[sourceType] || []
    return allowed.includes(targetType)
}

/**
 * 边样式配置
 */
export const edgeConfig = {
    attrs: {
        line: {
            stroke: '#8c8c8c',
            strokeWidth: 2,
            targetMarker: {
                name: 'block',
                width: 12,
                height: 8
            }
        }
    },
    connector: {
        name: 'smooth',
        args: { direction: 'V' }
    },
    router: {
        name: 'manhattan',
        args: { padding: 20 }
    }
}

export default {
    nodeConfig,
    getNodeStyle,
    connectionRules,
    validateConnection,
    edgeConfig
}
