<template>
  <div class="permission-matrix">
    <!-- 顶部工具栏 -->
    <div class="matrix-header">
      <div class="header-left">
        <h2>权限矩阵</h2>
        <span class="subtitle">可视化管理用户、角色和权限关系</span>
      </div>
      <div class="header-center">
        <a-segmented v-model:value="activeView" :options="viewOptions" />
      </div>
      <div class="header-right">
        <a-button type="primary" @click="handleNewConnection">
          <PlusOutlined />
          新建关联
        </a-button>
      </div>
    </div>

    <!-- 三栏主体 -->
    <div class="matrix-body">
      <!-- 左侧：用户/角色列表 -->
      <div class="left-panel">
        <div class="panel-title">
          <span>{{ activeView === 'users' ? '用户列表' : '角色列表' }}</span>
          <span class="count">{{ leftList.length }}</span>
        </div>
        
        <a-input-search
          v-model:value="searchText"
          placeholder="搜索..."
          class="search-input"
          allow-clear
        />
        
        <!-- 分类标签 -->
        <div class="filter-tags">
          <a-tag 
            v-for="tag in filterTags" 
            :key="tag.value"
            :color="activeFilter === tag.value ? 'blue' : 'default'"
            @click="activeFilter = tag.value"
            class="filter-tag"
          >
            {{ tag.label }}
          </a-tag>
        </div>

        <!-- 列表 -->
        <div class="item-list">
          <div
            v-for="item in filteredLeftList"
            :key="item.id"
            :class="['list-item', { active: selectedLeft?.id === item.id }]"
            @click="handleSelectLeft(item)"
          >
            <a-avatar :src="item.avatar" :size="40" class="item-avatar">
              <template #icon><UserOutlined /></template>
            </a-avatar>
            <div class="item-info">
              <div class="item-name">{{ item.name }}</div>
              <div class="item-desc">{{ item.description }}</div>
            </div>
            <div class="item-badge" v-if="item.connectionCount">
              <a-badge :count="item.connectionCount" :number-style="{ backgroundColor: '#1890ff' }" />
            </div>
          </div>
          <a-empty v-if="filteredLeftList.length === 0" description="暂无数据" />
        </div>

        <!-- 钉钉同步按钮 -->
        <div class="sync-button">
          <a-button block @click="handleSyncDingtalk" :loading="syncing">
            <SyncOutlined />
            同步钉钉
          </a-button>
        </div>
      </div>

      <!-- 中间：关系视图 -->
      <div class="center-panel">
        <div class="center-header">
          <span class="active-view-label">当前视图：</span>
          <a-tag color="blue">{{ getViewLabel() }}</a-tag>
          <a-button type="text" @click="handleFilterAssets">
            <FilterOutlined />
            筛选资产
          </a-button>
        </div>

        <!-- 关系卡片组 -->
        <div class="relation-groups" v-if="selectedLeft">
          <!-- 角色分组 -->
          <div class="relation-group" v-if="activeView === 'users'">
            <div class="group-header">
              <TeamOutlined />
              <span>已分配角色</span>
            </div>
            <div class="relation-cards">
              <div
                v-for="role in selectedLeft.roles"
                :key="role.id"
                :class="['relation-card', { selected: selectedRelation?.id === role.id && selectedRelationType === 'role' }]"
                @click="handleSelectRelation(role, 'role')"
              >
                <div class="card-status" :class="role.status === 1 ? 'active' : 'inactive'">
                  {{ role.status === 1 ? '启用' : '禁用' }}
                </div>
                <div class="card-icon">
                  <TeamOutlined />
                </div>
                <div class="card-content">
                  <div class="card-title">{{ role.roleName }}</div>
                  <div class="card-desc">{{ role.roleCode }}</div>
                </div>
                <div class="card-members" v-if="role.userCount">
                  <a-avatar-group :max-count="3" size="small">
                    <a-avatar v-for="n in Math.min(role.userCount, 3)" :key="n" />
                  </a-avatar-group>
                  <span v-if="role.userCount > 3">+{{ role.userCount - 3 }} 人</span>
                </div>
              </div>
              <div class="add-card" @click="handleAddRole" v-if="activeView === 'users'">
                <PlusOutlined />
                <span>添加角色</span>
              </div>
            </div>
          </div>

          <!-- 权限分组 -->
          <div class="relation-group">
            <div class="group-header">
              <LockOutlined />
              <span>权限列表</span>
            </div>
            <div class="relation-cards permissions">
              <div
                v-for="perm in getPermissionList()"
                :key="perm.id"
                :class="['relation-card permission-card', { selected: selectedRelation?.id === perm.id && selectedRelationType === 'permission' }]"
                @click="handleSelectRelation(perm, 'permission')"
              >
                <div class="perm-type-badge" :class="perm.permissionType?.toLowerCase()">
                  {{ getPermTypeLabel(perm.permissionType) }}
                </div>
                <div class="card-content">
                  <div class="card-title">{{ perm.permissionName }}</div>
                  <div class="card-desc">{{ perm.permissionCode }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div class="empty-center" v-else>
          <div class="empty-icon">
            <ApartmentOutlined />
          </div>
          <div class="empty-text">选择左侧用户或角色查看关联关系</div>
        </div>

        <!-- 连接线动画 -->
        <svg class="connection-lines" v-if="selectedLeft && selectedRelation">
          <defs>
            <linearGradient id="lineGradient" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" style="stop-color:#1890ff;stop-opacity:1" />
              <stop offset="100%" style="stop-color:#52c41a;stop-opacity:1" />
            </linearGradient>
          </defs>
          <path 
            :d="connectionPath" 
            fill="none" 
            stroke="url(#lineGradient)" 
            stroke-width="2"
            stroke-dasharray="5,5"
            class="animated-line"
          />
        </svg>
      </div>

      <!-- 右侧：详情面板 -->
      <div class="right-panel" :class="{ visible: showRightPanel }">
        <div class="panel-header">
          <span>关联详情</span>
          <a-button type="text" @click="showRightPanel = false">
            <CloseOutlined />
          </a-button>
        </div>

        <template v-if="selectedLeft && selectedRelation">
          <!-- 关联双方 -->
          <div class="link-parties">
            <div class="party">
              <a-avatar :src="selectedLeft.avatar" :size="48">
                <template #icon><UserOutlined /></template>
              </a-avatar>
              <span>{{ selectedLeft.name }}</span>
            </div>
            <div class="link-icon">
              <SwapOutlined />
            </div>
            <div class="party">
              <a-avatar :size="48" style="background: #1890ff">
                <template #icon>
                  <TeamOutlined v-if="selectedRelationType === 'role'" />
                  <LockOutlined v-else />
                </template>
              </a-avatar>
              <span>{{ selectedRelation.roleName || selectedRelation.permissionName }}</span>
            </div>
          </div>

          <a-divider />

          <!-- 角色选择 (当选中的是用户且关联的是角色时) -->
          <div class="detail-section" v-if="selectedRelationType === 'role'">
            <div class="section-title">分配角色</div>
            <div class="role-options">
              <a-tag
                v-for="role in allRoles"
                :key="role.id"
                :color="isRoleAssigned(role.id) ? 'blue' : 'default'"
                @click="toggleRoleAssignment(role)"
                class="role-tag"
              >
                {{ role.roleName }}
              </a-tag>
            </div>
          </div>

          <!-- 权限开关 -->
          <div class="detail-section">
            <div class="section-title">权限控制</div>
            <div class="permission-switches">
              <div 
                v-for="perm in getDetailPermissions()" 
                :key="perm.id" 
                class="permission-switch-item"
              >
                <div class="perm-info">
                  <div class="perm-name">{{ perm.permissionName }}</div>
                  <div class="perm-desc">{{ perm.description || perm.permissionCode }}</div>
                </div>
                <a-switch 
                  :checked="isPermissionEnabled(perm.id)"
                  @change="(checked) => togglePermission(perm.id, checked)"
                />
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-buttons">
            <a-button type="primary" block @click="handleSaveChanges" :loading="saving">
              <SaveOutlined />
              保存更改
            </a-button>
            <a-button danger block @click="handleRevokeAccess" class="revoke-btn">
              <DeleteOutlined />
              撤销关联
            </a-button>
          </div>
        </template>

        <div class="empty-right" v-else>
          <SelectOutlined style="font-size: 32px; color: #d9d9d9" />
          <span>选择关联项查看详情</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  UserOutlined,
  TeamOutlined,
  LockOutlined,
  SyncOutlined,
  FilterOutlined,
  CloseOutlined,
  SwapOutlined,
  SaveOutlined,
  DeleteOutlined,
  SelectOutlined,
  ApartmentOutlined
} from '@ant-design/icons-vue'
import { userApi } from '@/api/user'
import { roleApi } from '@/api/role'
import { permissionApi } from '@/api/permission'

// 视图切换
const activeView = ref('users')
const viewOptions = [
  { value: 'users', label: '用户视图' },
  { value: 'roles', label: '角色视图' }
]

// 搜索和筛选
const searchText = ref('')
const activeFilter = ref('all')

// 数据
const users = ref([])
const roles = ref([])
const allRoles = ref([])
const allPermissions = ref([])

// 选中状态
const selectedLeft = ref(null)
const selectedRelation = ref(null)
const selectedRelationType = ref('')
const showRightPanel = ref(false)

// 加载状态
const syncing = ref(false)
const saving = ref(false)

// 动态连接线路径
const connectionPath = ref('M 0 0')

// 用户权限关联（临时存储变更）
const pendingChanges = ref({
  roleIds: [],
  permissionIds: []
})

// 筛选标签
const filterTags = computed(() => {
  if (activeView.value === 'users') {
    const departments = [...new Set(users.value.map(u => u.department).filter(Boolean))]
    return [
      { label: '全部', value: 'all' },
      ...departments.map(d => ({ label: d, value: d }))
    ]
  }
  return [
    { label: '全部', value: 'all' },
    { label: '启用', value: 'enabled' },
    { label: '禁用', value: 'disabled' }
  ]
})

// 左侧列表
const leftList = computed(() => {
  if (activeView.value === 'users') {
    return users.value.map(u => ({
      id: u.id,
      name: u.realName || u.username,
      description: u.department || u.email,
      avatar: u.avatar,
      roles: u.roles || [],
      connectionCount: (u.roles?.length || 0)
    }))
  }
  return roles.value.map(r => ({
    id: r.id,
    name: r.roleName,
    description: r.roleCode,
    permissions: r.permissions || [],
    connectionCount: r.permissions?.length || 0
  }))
})

// 过滤后的列表
const filteredLeftList = computed(() => {
  let list = leftList.value
  
  // 搜索过滤
  if (searchText.value) {
    const search = searchText.value.toLowerCase()
    list = list.filter(item => 
      item.name?.toLowerCase().includes(search) ||
      item.description?.toLowerCase().includes(search)
    )
  }
  
  // 分类过滤
  if (activeFilter.value !== 'all') {
    if (activeView.value === 'users') {
      list = list.filter(item => item.description === activeFilter.value)
    } else {
      list = list.filter(item => 
        activeFilter.value === 'enabled' ? item.status === 1 : item.status === 0
      )
    }
  }
  
  return list
})

// 获取视图标签
const getViewLabel = () => {
  return activeView.value === 'users' ? '用户 → 角色 → 权限' : '角色 → 权限'
}

// 获取权限类型标签
const getPermTypeLabel = (type) => {
  const labels = { MENU: '菜单', BUTTON: '按钮', DATA: '数据' }
  return labels[type] || type
}

// 获取权限列表
const getPermissionList = () => {
  if (activeView.value === 'users' && selectedRelation && selectedRelationType.value === 'role') {
    return selectedRelation.value?.permissions || []
  }
  if (activeView.value === 'roles' && selectedLeft.value) {
    return selectedLeft.value.permissions || []
  }
  return []
}

// 获取详情权限列表
const getDetailPermissions = () => {
  if (selectedRelationType.value === 'role' && selectedRelation.value) {
    return allPermissions.value
  }
  return []
}

// 检查角色是否已分配
const isRoleAssigned = (roleId) => {
  return pendingChanges.value.roleIds.includes(roleId)
}

// 检查权限是否启用
const isPermissionEnabled = (permId) => {
  return pendingChanges.value.permissionIds.includes(permId)
}

// 切换角色分配
const toggleRoleAssignment = (role) => {
  const index = pendingChanges.value.roleIds.indexOf(role.id)
  if (index > -1) {
    pendingChanges.value.roleIds.splice(index, 1)
  } else {
    pendingChanges.value.roleIds.push(role.id)
  }
}

// 切换权限
const togglePermission = (permId, checked) => {
  if (checked) {
    if (!pendingChanges.value.permissionIds.includes(permId)) {
      pendingChanges.value.permissionIds.push(permId)
    }
  } else {
    const index = pendingChanges.value.permissionIds.indexOf(permId)
    if (index > -1) {
      pendingChanges.value.permissionIds.splice(index, 1)
    }
  }
}

// 选择左侧项
const handleSelectLeft = async (item) => {
  selectedLeft.value = item
  selectedRelation.value = null
  selectedRelationType.value = ''
  
  // 初始化变更状态
  if (activeView.value === 'users') {
    pendingChanges.value.roleIds = item.roles?.map(r => r.id) || []
  } else {
    pendingChanges.value.permissionIds = item.permissions?.map(p => p.id) || []
  }
}

// 选择关联项
const handleSelectRelation = async (item, type) => {
  selectedRelation.value = item
  selectedRelationType.value = type
  showRightPanel.value = true
  
  // 初始化权限状态
  if (type === 'role') {
    try {
      // 加载该角色的权限
      const permissions = await roleApi.getPermissions(item.id)
      item.permissions = permissions || []
      pendingChanges.value.permissionIds = permissions?.map(p => p.id) || []
    } catch (error) {
      console.error('加载角色权限失败:', error)
      pendingChanges.value.permissionIds = []
    }
  }
}

// 添加角色
const handleAddRole = () => {
  message.info('请在右侧面板选择要分配的角色')
  showRightPanel.value = true
}

// 新建关联
const handleNewConnection = () => {
  message.info('请先选择左侧用户，然后在右侧配置关联')
}

// 筛选资产
const handleFilterAssets = () => {
  message.info('筛选功能开发中')
}

// 同步钉钉
const handleSyncDingtalk = async () => {
  syncing.value = true
  try {
    // TODO: 调用钉钉同步API
    await new Promise(resolve => setTimeout(resolve, 2000))
    message.success('同步成功')
    loadData()
  } catch (error) {
    message.error('同步失败')
  } finally {
    syncing.value = false
  }
}

// 保存更改
const handleSaveChanges = async () => {
  saving.value = true
  try {
    if (activeView.value === 'users' && selectedLeft.value) {
      // 更新用户角色
      await roleApi.updateUserRoles?.(selectedLeft.value.id, pendingChanges.value.roleIds)
    }
    if (selectedRelationType.value === 'role' && selectedRelation.value) {
      // 更新角色权限
      await roleApi.updatePermissions?.(selectedRelation.value.id, pendingChanges.value.permissionIds)
    }
    message.success('保存成功')
    loadData()
  } catch (error) {
    console.error('保存失败:', error)
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 撤销关联
const handleRevokeAccess = () => {
  message.warning('撤销关联功能开发中')
}

// 加载数据
const loadData = async () => {
  try {
    // 加载用户列表（带角色信息）
    const usersRes = await userApi.getListWithRoles({ page: 1, size: 500 })
    users.value = usersRes?.list || []

    // 加载角色列表
    const rolesRes = await roleApi.getAll()
    roles.value = rolesRes || []
    allRoles.value = rolesRes || []

    // 加载权限列表
    const permsRes = await permissionApi.getAll()
    allPermissions.value = permsRes || []
  } catch (error) {
    console.error('加载数据失败:', error)
    message.error('加载数据失败')
  }
}

// 监听视图切换
watch(activeView, () => {
  selectedLeft.value = null
  selectedRelation.value = null
  showRightPanel.value = false
  activeFilter.value = 'all'
})

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.permission-matrix {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px);
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
}

/* 顶部工具栏 */
.matrix-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.matrix-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.subtitle {
  color: #8c8c8c;
  font-size: 13px;
  margin-left: 12px;
}

/* 三栏布局 */
.matrix-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 左侧面板 */
.left-panel {
  width: 280px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  padding: 16px;
}

.panel-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  margin-bottom: 12px;
}

.panel-title .count {
  background: #f0f0f0;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  color: #666;
}

.search-input {
  margin-bottom: 12px;
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}

.filter-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.filter-tag:hover {
  opacity: 0.8;
}

.item-list {
  flex: 1;
  overflow-y: auto;
}

.list-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  margin-bottom: 8px;
}

.list-item:hover {
  background: #f5f7fa;
}

.list-item.active {
  background: linear-gradient(135deg, #e6f4ff 0%, #bae0ff 100%);
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
}

.item-avatar {
  margin-right: 12px;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-weight: 500;
  color: #262626;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-desc {
  font-size: 12px;
  color: #8c8c8c;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sync-button {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* 中间面板 */
.center-panel {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  position: relative;
}

.center-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.active-view-label {
  color: #8c8c8c;
  font-size: 13px;
}

.relation-group {
  margin-bottom: 32px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #595959;
  margin-bottom: 16px;
  padding-left: 8px;
  border-left: 3px solid #1890ff;
}

.relation-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.relation-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid transparent;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: relative;
}

.relation-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.relation-card.selected {
  border-color: #1890ff;
  box-shadow: 0 0 0 4px rgba(24, 144, 255, 0.1);
}

.card-status {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.card-status.active {
  background: #f6ffed;
  color: #52c41a;
}

.card-status.inactive {
  background: #fff1f0;
  color: #ff4d4f;
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  margin-bottom: 12px;
}

.card-title {
  font-weight: 600;
  color: #262626;
  margin-bottom: 4px;
}

.card-desc {
  font-size: 12px;
  color: #8c8c8c;
}

.card-members {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  font-size: 12px;
  color: #8c8c8c;
}

.add-card {
  background: #fafafa;
  border: 2px dashed #d9d9d9;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  color: #8c8c8c;
  transition: all 0.2s;
}

.add-card:hover {
  border-color: #1890ff;
  color: #1890ff;
}

/* 权限卡片特殊样式 */
.permission-card {
  padding: 12px 16px;
}

.perm-type-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 500;
  margin-bottom: 8px;
}

.perm-type-badge.menu {
  background: #e6f4ff;
  color: #1890ff;
}

.perm-type-badge.button {
  background: #f6ffed;
  color: #52c41a;
}

.perm-type-badge.data {
  background: #fff7e6;
  color: #fa8c16;
}

/* 空状态 */
.empty-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #bfbfbf;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
}

/* 连接线 */
.connection-lines {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.animated-line {
  animation: dash 1s linear infinite;
}

@keyframes dash {
  to {
    stroke-dashoffset: -10;
  }
}

/* 右侧面板 */
.right-panel {
  width: 0;
  background: #fff;
  border-left: 1px solid #e8e8e8;
  overflow: hidden;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
}

.right-panel.visible {
  width: 320px;
  overflow-y: auto;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  font-weight: 600;
}

.link-parties {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 24px 16px;
}

.party {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #595959;
}

.link-icon {
  font-size: 20px;
  color: #1890ff;
}

.detail-section {
  padding: 16px;
}

.section-title {
  font-weight: 600;
  margin-bottom: 12px;
  color: #595959;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.role-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.role-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.role-tag:hover {
  opacity: 0.8;
}

.permission-switches {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.permission-switch-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
}

.perm-info .perm-name {
  font-weight: 500;
  color: #262626;
  margin-bottom: 2px;
}

.perm-info .perm-desc {
  font-size: 12px;
  color: #8c8c8c;
}

.action-buttons {
  padding: 16px;
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.revoke-btn {
  background: #fff1f0;
  border-color: #ffccc7;
  color: #ff4d4f;
}

.revoke-btn:hover {
  background: #fff1f0;
  border-color: #ff4d4f;
}

.empty-right {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 16px;
  color: #bfbfbf;
}

/* 响应式 */
@media (max-width: 1024px) {
  .left-panel {
    width: 240px;
  }
  
  .right-panel.visible {
    position: absolute;
    right: 0;
    top: 0;
    height: 100%;
    z-index: 100;
    box-shadow: -4px 0 16px rgba(0, 0, 0, 0.1);
  }
}

@media (max-width: 768px) {
  .matrix-header {
    flex-direction: column;
    gap: 12px;
  }
  
  .matrix-body {
    flex-direction: column;
  }
  
  .left-panel {
    width: 100%;
    max-height: 300px;
    border-right: none;
    border-bottom: 1px solid #e8e8e8;
  }
  
  .right-panel.visible {
    width: 100%;
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    height: 60%;
    border-radius: 16px 16px 0 0;
  }
}

/* 滚动条样式 */
.item-list::-webkit-scrollbar,
.center-panel::-webkit-scrollbar {
  width: 6px;
}

.item-list::-webkit-scrollbar-thumb,
.center-panel::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}
</style>
