<template>
  <div class="left-panel" :class="{ collapsed }">
    <!-- 折叠按钮 -->
    <div class="collapse-btn" @click="$emit('toggle')">
      <RightOutlined v-if="collapsed" />
      <LeftOutlined v-else />
    </div>

    <!-- 面板内容 -->
    <div v-show="!collapsed" class="panel-content">
      <div class="panel-header">
        <span class="title">模块列表</span>
        <a-input-search
          v-model:value="searchText"
          placeholder="搜索..."
          size="small"
          style="width: 120px"
        />
      </div>

      <!-- 模块分组 -->
      <a-collapse v-model:activeKey="activeKeys" ghost expandIconPosition="start">
        <!-- 组织管理 -->
        <a-collapse-panel key="organization" header="组织管理">
          <template #extra>
            <a-badge :count="organizations.length" :number-style="badgeStyle" />
          </template>
          <div class="node-list">
            <NodeItem
              v-for="item in filteredOrganizations"
              :key="item.id"
              :data="item"
              type="organization"
              @drag-start="handleDragStart"
            />
            <a-empty v-if="filteredOrganizations.length === 0" :image="simpleImage" description="暂无数据" />
          </div>
        </a-collapse-panel>

        <!-- 部门管理 -->
        <a-collapse-panel key="department" header="部门管理">
          <template #extra>
            <a-badge :count="departments.length" :number-style="badgeStyle" />
          </template>
          <div class="node-list">
            <NodeItem
              v-for="item in filteredDepartments"
              :key="item.id"
              :data="item"
              type="department"
              @drag-start="handleDragStart"
            />
            <a-empty v-if="filteredDepartments.length === 0" :image="simpleImage" description="暂无数据" />
          </div>
        </a-collapse-panel>

        <!-- 用户管理 -->
        <a-collapse-panel key="user" header="用户管理">
          <template #extra>
            <a-badge :count="users.length" :number-style="badgeStyle" />
          </template>
          <div class="node-list">
            <NodeItem
              v-for="item in filteredUsers"
              :key="item.id"
              :data="item"
              type="user"
              @drag-start="handleDragStart"
            />
            <a-empty v-if="filteredUsers.length === 0" :image="simpleImage" description="暂无数据" />
          </div>
        </a-collapse-panel>

        <!-- 角色管理 -->
        <a-collapse-panel key="role" header="角色管理">
          <template #extra>
            <a-badge :count="roles.length" :number-style="badgeStyle" />
          </template>
          <div class="node-list">
            <NodeItem
              v-for="item in filteredRoles"
              :key="item.id"
              :data="item"
              type="role"
              @drag-start="handleDragStart"
            />
            <a-empty v-if="filteredRoles.length === 0" :image="simpleImage" description="暂无数据" />
          </div>
        </a-collapse-panel>

        <!-- 权限管理 -->
        <a-collapse-panel key="permission" header="权限管理">
          <template #extra>
            <a-badge :count="permissions.length" :number-style="badgeStyle" />
          </template>
          <div class="node-list">
            <NodeItem
              v-for="item in filteredPermissions"
              :key="item.id"
              :data="item"
              type="permission"
              @drag-start="handleDragStart"
            />
            <a-empty v-if="filteredPermissions.length === 0" :image="simpleImage" description="暂无数据" />
          </div>
        </a-collapse-panel>

        <!-- 菜单管理 -->
        <a-collapse-panel key="menu" header="菜单管理">
          <template #extra>
            <a-badge :count="menus.length" :number-style="badgeStyle" />
          </template>
          <div class="node-list">
            <NodeItem
              v-for="item in filteredMenus"
              :key="item.id"
              :data="item"
              type="menu"
              @drag-start="handleDragStart"
            />
            <a-empty v-if="filteredMenus.length === 0" :image="simpleImage" description="暂无数据" />
          </div>
        </a-collapse-panel>

        <!-- 钉钉同步 -->
        <a-collapse-panel key="sync" header="钉钉同步">
          <div class="sync-actions">
            <a-button type="primary" ghost block @click="handleSyncDingtalk">
              <SyncOutlined />
              立即同步
            </a-button>
            <div class="sync-info">
              <span>上次同步: {{ lastSyncTime || '从未同步' }}</span>
            </div>
          </div>
        </a-collapse-panel>
      </a-collapse>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { message, Empty } from 'ant-design-vue'
import { LeftOutlined, RightOutlined, SyncOutlined } from '@ant-design/icons-vue'
import NodeItem from './NodeItem.vue'
import { userApi } from '@/api/user'
import { roleApi } from '@/api/role'
import { permissionApi } from '@/api/permission'

const props = defineProps({
  collapsed: { type: Boolean, default: false }
})

const emit = defineEmits(['toggle', 'drag-start'])

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

// 搜索
const searchText = ref('')

// 展开的面板
const activeKeys = ref(['organization', 'department', 'user', 'role'])

// 数据
const organizations = ref([])
const departments = ref([])
const users = ref([])
const roles = ref([])
const permissions = ref([])
const menus = ref([])
const lastSyncTime = ref('')

// 徽章样式
const badgeStyle = { backgroundColor: '#52c41a', fontSize: '10px' }

// 过滤后的数据
const filteredOrganizations = computed(() => filterBySearch(organizations.value, 'name'))
const filteredDepartments = computed(() => filterBySearch(departments.value, 'deptName'))
const filteredUsers = computed(() => filterBySearch(users.value, 'username'))
const filteredRoles = computed(() => filterBySearch(roles.value, 'roleName'))
const filteredPermissions = computed(() => filterBySearch(permissions.value, 'permissionName'))
const filteredMenus = computed(() => filterBySearch(menus.value, 'menuName'))

// 搜索过滤函数
const filterBySearch = (list, key) => {
  if (!searchText.value) return list
  const search = searchText.value.toLowerCase()
  return list.filter(item => item[key]?.toLowerCase().includes(search))
}

// 拖拽开始
const handleDragStart = (nodeData) => {
  emit('drag-start', nodeData)
}

// 钉钉同步
const handleSyncDingtalk = async () => {
  try {
    message.loading({ content: '正在同步...', key: 'sync' })
    // TODO: 调用钉钉同步API
    await new Promise(resolve => setTimeout(resolve, 2000))
    lastSyncTime.value = new Date().toLocaleString()
    message.success({ content: '同步成功', key: 'sync' })
    loadData()
  } catch (error) {
    message.error({ content: '同步失败', key: 'sync' })
  }
}

// 加载数据
const loadData = async () => {
  try {
    // 加载用户
    const usersRes = await userApi.getList({ page: 1, pageSize: 100 })
    users.value = usersRes?.list || []

    // 加载角色
    const rolesRes = await roleApi.getAll()
    roles.value = rolesRes || []

    // 加载权限
    const permsRes = await permissionApi.getAll()
    permissions.value = permsRes || []

    // 模拟组织和部门数据（根据实际API调整）
    organizations.value = [{ id: 1, name: '默认组织' }]
    departments.value = users.value
      .filter(u => u.department)
      .map(u => ({ id: u.department, deptName: u.department }))
      .filter((v, i, a) => a.findIndex(t => t.id === v.id) === i)

    // 模拟菜单数据
    menus.value = [
      { id: 1, menuName: '首页', menuCode: 'home' },
      { id: 2, menuName: '系统管理', menuCode: 'system' },
      { id: 3, menuName: '产品管理', menuCode: 'product' }
    ]
  } catch (error) {
    console.error('加载数据失败:', error)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.left-panel {
  position: relative;
  width: 280px;
  min-width: 280px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.left-panel.collapsed {
  width: 0;
  min-width: 0;
  border-right: none;
}

.collapse-btn {
  position: absolute;
  top: 50%;
  right: -12px;
  transform: translateY(-50%);
  width: 24px;
  height: 48px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-left: none;
  border-radius: 0 8px 8px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  transition: all 0.2s;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.06);
}

.collapse-btn:hover {
  background: #f5f5f5;
}

.collapsed .collapse-btn {
  right: -24px;
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  position: sticky;
  top: 0;
  background: #fff;
  z-index: 5;
}

.panel-header .title {
  font-weight: 600;
  font-size: 14px;
}

.node-list {
  max-height: 200px;
  overflow-y: auto;
  padding: 4px 0;
}

.sync-actions {
  padding: 8px 0;
}

.sync-info {
  margin-top: 12px;
  font-size: 12px;
  color: #999;
  text-align: center;
}

/* 自定义折叠面板样式 */
:deep(.ant-collapse-header) {
  font-size: 13px;
  font-weight: 500;
}

:deep(.ant-collapse-content-box) {
  padding: 8px !important;
}

/* 苹果风格滚动条 */
.panel-content::-webkit-scrollbar,
.node-list::-webkit-scrollbar {
  width: 6px;
}

.panel-content::-webkit-scrollbar-thumb,
.node-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 3px;
}

.panel-content::-webkit-scrollbar-thumb:hover,
.node-list::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.25);
}

/* 响应式 */
@media (max-width: 768px) {
  .left-panel {
    width: 100%;
    min-width: 100%;
    border-right: none;
    border-bottom: 1px solid #e8e8e8;
  }
  
  .collapse-btn {
    display: none;
  }
}
</style>
