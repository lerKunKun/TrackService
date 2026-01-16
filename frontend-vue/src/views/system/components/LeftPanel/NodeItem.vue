<template>
  <div
    class="node-item"
    :class="[`type-${type}`, { dragging }]"
    draggable="true"
    @dragstart="handleDragStart"
    @dragend="handleDragEnd"
  >
    <div class="node-icon">
      <component :is="iconComponent" />
    </div>
    <div class="node-info">
      <div class="node-name">{{ displayName }}</div>
      <div class="node-meta">{{ displayMeta }}</div>
    </div>
    <div class="drag-handle">
      <HolderOutlined />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import {
  BankOutlined,
  ApartmentOutlined,
  UserOutlined,
  TeamOutlined,
  LockOutlined,
  MenuOutlined,
  HolderOutlined
} from '@ant-design/icons-vue'

const props = defineProps({
  data: { type: Object, required: true },
  type: { type: String, required: true }
})

const emit = defineEmits(['drag-start'])

const dragging = ref(false)

// 图标映射
const iconMap = {
  organization: BankOutlined,
  department: ApartmentOutlined,
  user: UserOutlined,
  role: TeamOutlined,
  permission: LockOutlined,
  menu: MenuOutlined
}

const iconComponent = computed(() => iconMap[props.type] || BankOutlined)

// 显示名称
const displayName = computed(() => {
  const { data, type } = props
  switch (type) {
    case 'organization': return data.name
    case 'department': return data.deptName
    case 'user': return data.realName || data.username
    case 'role': return data.roleName
    case 'permission': return data.permissionName
    case 'menu': return data.menuName
    default: return data.name || data.id
  }
})

// 显示元信息
const displayMeta = computed(() => {
  const { data, type } = props
  switch (type) {
    case 'user': return data.username
    case 'role': return data.roleCode
    case 'permission': return data.permissionCode
    case 'menu': return data.menuCode
    default: return `ID: ${data.id}`
  }
})

// 拖拽开始
const handleDragStart = (e) => {
  dragging.value = true
  
  // 设置拖拽数据
  const nodeData = {
    ...props.data,
    nodeType: props.type,
    label: displayName.value
  }
  
  e.dataTransfer.setData('application/json', JSON.stringify(nodeData))
  e.dataTransfer.effectAllowed = 'copy'
  
  // 触发父组件事件
  emit('drag-start', nodeData)
}

// 拖拽结束
const handleDragEnd = () => {
  dragging.value = false
}
</script>

<style scoped>
.node-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  margin: 4px 0;
  background: #fafafa;
  border-radius: 8px;
  cursor: grab;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
}

.node-item:hover {
  background: #f0f0f0;
  transform: translateX(2px);
}

.node-item:active,
.node-item.dragging {
  cursor: grabbing;
  opacity: 0.8;
  transform: scale(0.98);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.node-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
  font-size: 16px;
  color: #fff;
}

/* 类型颜色 */
.type-organization .node-icon { background: linear-gradient(135deg, #1890ff, #096dd9); }
.type-department .node-icon { background: linear-gradient(135deg, #52c41a, #389e0d); }
.type-user .node-icon { background: linear-gradient(135deg, #13c2c2, #08979c); }
.type-role .node-icon { background: linear-gradient(135deg, #722ed1, #531dab); }
.type-permission .node-icon { background: linear-gradient(135deg, #fa8c16, #d46b08); }
.type-menu .node-icon { background: linear-gradient(135deg, #eb2f96, #c41d7f); }

.node-info {
  flex: 1;
  min-width: 0;
}

.node-name {
  font-size: 13px;
  font-weight: 500;
  color: #262626;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-meta {
  font-size: 11px;
  color: #8c8c8c;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.drag-handle {
  color: #bfbfbf;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.2s;
}

.node-item:hover .drag-handle {
  opacity: 1;
}

/* 移动端优化 */
@media (max-width: 768px) {
  .node-item {
    padding: 12px 14px;
  }
  
  .drag-handle {
    opacity: 1;
  }
}
</style>
