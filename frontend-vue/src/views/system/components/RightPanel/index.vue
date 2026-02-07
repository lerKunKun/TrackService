<template>
  <div class="right-panel" :class="{ collapsed }">
    <!-- 折叠按钮 -->
    <div class="collapse-btn" @click="$emit('toggle')">
      <LeftOutlined v-if="collapsed" />
      <RightOutlined v-else />
    </div>

    <!-- 面板内容 -->
    <div v-show="!collapsed" class="panel-content">
      <!-- 无选中状态 -->
      <div v-if="!selectedNode" class="empty-state">
        <div class="empty-icon">
          <SelectOutlined />
        </div>
        <div class="empty-text">选择节点查看属性</div>
        <div class="empty-sub">点击画布中的节点编辑其属性</div>
      </div>

      <!-- 节点属性编辑 -->
      <template v-else>
        <div class="panel-header">
          <div class="node-type-badge" :style="{ background: nodeColor }">
            <component :is="nodeIcon" />
          </div>
          <div class="header-info">
            <div class="node-type-label">{{ nodeTypeLabel }}</div>
            <div class="node-id">ID: {{ selectedNode.id }}</div>
          </div>
        </div>

        <a-divider style="margin: 12px 0" />

        <!-- 根据节点类型显示不同表单 -->
        <div class="form-content">
          <!-- 组织表单 -->
          <OrganizationForm
            v-if="selectedNode.nodeType === 'organization'"
            :data="selectedNode"
            @update="handleUpdate"
          />

          <!-- 部门表单 -->
          <DepartmentForm
            v-else-if="selectedNode.nodeType === 'department'"
            :data="selectedNode"
            @update="handleUpdate"
          />

          <!-- 用户表单 -->
          <UserForm
            v-else-if="selectedNode.nodeType === 'user'"
            :data="selectedNode"
            @update="handleUpdate"
          />

          <!-- 角色表单 -->
          <RoleForm
            v-else-if="selectedNode.nodeType === 'role'"
            :data="selectedNode"
            @update="handleUpdate"
          />

          <!-- 权限表单 -->
          <PermissionForm
            v-else-if="selectedNode.nodeType === 'permission'"
            :data="selectedNode"
            @update="handleUpdate"
          />

          <!-- 菜单表单 -->
          <MenuForm
            v-else-if="selectedNode.nodeType === 'menu'"
            :data="selectedNode"
            @update="handleUpdate"
          />

          <!-- 默认表单 -->
          <DefaultForm v-else :data="selectedNode" @update="handleUpdate" />
        </div>

        <!-- 操作按钮 -->
        <div class="panel-actions">
          <a-popconfirm
            title="确定要从画布中移除该节点吗？"
            ok-text="确定"
            cancel-text="取消"
            @confirm="handleDelete"
          >
            <a-button danger block>
              <DeleteOutlined />
              移除节点
            </a-button>
          </a-popconfirm>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  LeftOutlined,
  RightOutlined,
  SelectOutlined,
  DeleteOutlined,
  BankOutlined,
  ApartmentOutlined,
  UserOutlined,
  TeamOutlined,
  LockOutlined,
  MenuOutlined
} from '@ant-design/icons-vue'
import { nodeConfig } from '../CenterCanvas/nodeConfig'
import OrganizationForm from './forms/OrganizationForm.vue'
import DepartmentForm from './forms/DepartmentForm.vue'
import UserForm from './forms/UserForm.vue'
import RoleForm from './forms/RoleForm.vue'
import PermissionForm from './forms/PermissionForm.vue'
import MenuForm from './forms/MenuForm.vue'
import DefaultForm from './forms/DefaultForm.vue'

const props = defineProps({
  collapsed: { type: Boolean, default: true },
  selectedNode: { type: Object, default: null }
})

const emit = defineEmits(['toggle', 'node-update', 'node-delete'])

// 图标映射
const iconMap = {
  organization: BankOutlined,
  department: ApartmentOutlined,
  user: UserOutlined,
  role: TeamOutlined,
  permission: LockOutlined,
  menu: MenuOutlined
}

// 节点图标
const nodeIcon = computed(() => {
  return iconMap[props.selectedNode?.nodeType] || BankOutlined
})

// 节点颜色
const nodeColor = computed(() => {
  const type = props.selectedNode?.nodeType
  return nodeConfig[type]?.color || '#1890ff'
})

// 节点类型标签
const nodeTypeLabel = computed(() => {
  const type = props.selectedNode?.nodeType
  return nodeConfig[type]?.label || '未知类型'
})

// 更新节点
const handleUpdate = (data) => {
  emit('node-update', { ...props.selectedNode, ...data })
}

// 删除节点
const handleDelete = () => {
  emit('node-delete', props.selectedNode.id)
}
</script>

<style scoped>
.right-panel {
  position: relative;
  width: 320px;
  min-width: 320px;
  background: #fff;
  border-left: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.right-panel.collapsed {
  width: 0;
  min-width: 0;
  border-left: none;
}

.collapse-btn {
  position: absolute;
  top: 50%;
  left: -12px;
  transform: translateY(-50%);
  width: 24px;
  height: 48px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-right: none;
  border-radius: 8px 0 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  transition: all 0.2s;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.06);
}

.collapse-btn:hover {
  background: #f5f5f5;
}

.collapsed .collapse-btn {
  left: -24px;
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  color: #d9d9d9;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.empty-sub {
  font-size: 13px;
  color: #bfbfbf;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.node-type-badge {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
}

.header-info {
  flex: 1;
}

.node-type-label {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.node-id {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}

.form-content {
  flex: 1;
}

.panel-actions {
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  margin-top: 16px;
}

/* 苹果风格滚动条 */
.panel-content::-webkit-scrollbar {
  width: 6px;
}

.panel-content::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 3px;
}

/* 响应式 */
@media (max-width: 768px) {
  .right-panel {
    position: absolute;
    right: 0;
    top: 0;
    height: 100%;
    z-index: 100;
    box-shadow: -4px 0 16px rgba(0, 0, 0, 0.1);
  }
  
  .collapse-btn {
    display: none;
  }
}
</style>
