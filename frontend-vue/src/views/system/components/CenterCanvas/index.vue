<template>
  <div
    class="center-canvas"
    :class="{ 'left-collapsed': leftCollapsed, 'right-collapsed': rightCollapsed }"
    ref="containerRef"
    @dragover.prevent
    @drop="handleDrop"
  >
    <!-- 画布容器 -->
    <div ref="graphRef" class="graph-container"></div>

    <!-- 缩放控制 -->
    <div class="zoom-control">
      <span class="zoom-value">{{ Math.round(zoom * 100) }}%</span>
    </div>

    <!-- 空状态提示 -->
    <div v-if="isEmpty" class="empty-hint">
      <div class="hint-icon">
        <DragOutlined />
      </div>
      <div class="hint-text">从左侧拖拽模块到画布</div>
      <div class="hint-sub">建立组织、部门、用户、角色的关联关系</div>
    </div>

    <!-- 小地图 -->
    <div ref="minimapRef" class="minimap-container"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { Graph, Shape } from '@antv/x6'
import { DragOutlined } from '@ant-design/icons-vue'
import { nodeConfig, getNodeStyle } from './nodeConfig'

const props = defineProps({
  leftCollapsed: { type: Boolean, default: false },
  rightCollapsed: { type: Boolean, default: true }
})

const emit = defineEmits(['node-select', 'node-unselect', 'canvas-change'])

// 引用
const containerRef = ref(null)
const graphRef = ref(null)
const minimapRef = ref(null)

// 状态
const graph = ref(null)
const zoom = ref(1)
const isEmpty = ref(true)

// 节点计数器
let nodeCounter = 0

// 初始化画布
const initGraph = () => {
  if (!graphRef.value) return

  graph.value = new Graph({
    container: graphRef.value,
    autoResize: true,
    background: {
      color: '#f8f9fa'
    },
    grid: {
      visible: true,
      type: 'doubleMesh',
      args: [
        { color: '#e8e8e8', thickness: 1 },
        { color: '#ddd', thickness: 1, factor: 4 }
      ]
    },
    mousewheel: {
      enabled: true,
      modifiers: ['ctrl', 'meta'],
      minScale: 0.5,
      maxScale: 2
    },
    panning: {
      enabled: true,
      modifiers: 'shift'
    },
    selecting: {
      enabled: true,
      rubberband: true,
      showNodeSelectionBox: true
    },
    connecting: {
      snap: true,
      allowBlank: false,
      allowLoop: false,
      allowNode: true,
      connector: {
        name: 'smooth',
        args: { direction: 'V' }
      },
      connectionPoint: 'anchor',
      anchor: 'center',
      createEdge() {
        return new Shape.Edge({
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
          zIndex: 0
        })
      },
      validateConnection({ sourceCell, targetCell }) {
        // 验证连接规则
        return validateConnection(sourceCell, targetCell)
      }
    },
    highlighting: {
      magnetAvailable: {
        name: 'stroke',
        args: { padding: 4, attrs: { strokeWidth: 2, stroke: '#1890ff' } }
      }
    },
    history: true, // 启用撤销/重做
    clipboard: true,
    keyboard: true
  })

  // 事件监听
  setupEvents()

  // 初始化小地图
  initMinimap()
}

// 设置事件
const setupEvents = () => {
  if (!graph.value) return

  // 节点选中
  graph.value.on('node:selected', ({ node }) => {
    const data = node.getData()
    emit('node-select', { id: node.id, ...data })
  })

  // 取消选中
  graph.value.on('cell:unselected', () => {
    emit('node-unselect')
  })

  // 缩放变化
  graph.value.on('scale', ({ sx }) => {
    zoom.value = sx
  })

  // 画布变化
  graph.value.on('cell:added', updateCanvasState)
  graph.value.on('cell:removed', updateCanvasState)
  graph.value.on('history:change', updateHistoryState)

  // 快捷键
  graph.value.bindKey('delete', () => {
    const cells = graph.value.getSelectedCells()
    if (cells.length) {
      graph.value.removeCells(cells)
    }
  })

  graph.value.bindKey(['ctrl+z', 'cmd+z'], () => undo())
  graph.value.bindKey(['ctrl+shift+z', 'cmd+shift+z'], () => redo())
}

// 初始化小地图
const initMinimap = () => {
  if (!graph.value || !minimapRef.value) return
  
  // AntV X6 的小地图功能
  graph.value.use(
    new (require('@antv/x6-plugin-minimap').MiniMap)({
      container: minimapRef.value,
      width: 150,
      height: 100,
      padding: 10
    })
  )
}

// 验证连接规则
const validateConnection = (source, target) => {
  if (!source || !target) return false
  
  const sourceType = source.getData()?.nodeType
  const targetType = target.getData()?.nodeType
  
  // 连接规则
  const rules = {
    organization: ['department'],
    department: ['user'],
    user: ['role'],
    role: ['permission', 'menu']
  }
  
  const allowed = rules[sourceType] || []
  return allowed.includes(targetType)
}

// 更新画布状态
const updateCanvasState = () => {
  isEmpty.value = graph.value?.getCells().length === 0
}

// 更新历史状态
const updateHistoryState = () => {
  emit('canvas-change', {
    canUndo: graph.value?.canUndo(),
    canRedo: graph.value?.canRedo()
  })
}

// 处理拖放
const handleDrop = (e) => {
  e.preventDefault()
  
  const dataStr = e.dataTransfer.getData('application/json')
  if (!dataStr) return
  
  try {
    const nodeData = JSON.parse(dataStr)
    const rect = containerRef.value.getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    
    // 转换为画布坐标
    const point = graph.value.clientToLocal({ x, y })
    
    addNode(nodeData, point)
  } catch (error) {
    console.error('解析拖拽数据失败:', error)
  }
}

// 添加节点
const addNode = (nodeData, position) => {
  if (!graph.value) return
  
  const { nodeType, label } = nodeData
  const style = getNodeStyle(nodeType)
  
  nodeCounter++
  const nodeId = `${nodeType}-${nodeData.id}-${nodeCounter}`
  
  graph.value.addNode({
    id: nodeId,
    x: position.x - 60,
    y: position.y - 25,
    width: 120,
    height: 50,
    shape: 'rect',
    attrs: {
      body: {
        fill: style.backgroundColor,
        stroke: style.borderColor,
        strokeWidth: 2,
        rx: 8,
        ry: 8,
        filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.1))'
      },
      label: {
        text: label,
        fill: '#fff',
        fontSize: 13,
        fontWeight: 500
      }
    },
    ports: {
      groups: {
        top: { position: 'top', attrs: { circle: { r: 4, magnet: true, stroke: '#8c8c8c', strokeWidth: 1, fill: '#fff' } } },
        bottom: { position: 'bottom', attrs: { circle: { r: 4, magnet: true, stroke: '#8c8c8c', strokeWidth: 1, fill: '#fff' } } }
      },
      items: [
        { id: 'port-top', group: 'top' },
        { id: 'port-bottom', group: 'bottom' }
      ]
    },
    data: nodeData
  })
  
  isEmpty.value = false
}

// 公开方法
const getGraph = () => graph.value
const startDrag = (nodeData) => { /* 拖拽由 HTML5 drag API 处理 */ }

const updateNode = (nodeData) => {
  const node = graph.value?.getCellById(nodeData.id)
  if (node) {
    node.setData(nodeData)
    node.attr('label/text', nodeData.label)
  }
}

const deleteNode = (nodeId) => {
  const node = graph.value?.getCellById(nodeId)
  if (node) {
    graph.value.removeCell(node)
  }
}

const undo = () => graph.value?.undo()
const redo = () => graph.value?.redo()
const zoomIn = () => graph.value?.zoom(0.1)
const zoomOut = () => graph.value?.zoom(-0.1)
const fitView = () => graph.value?.zoomToFit({ padding: 40, maxScale: 1 })

const exportLayout = () => {
  return graph.value?.toJSON()
}

const importLayout = (data) => {
  if (data && graph.value) {
    graph.value.fromJSON(data)
    isEmpty.value = graph.value.getCells().length === 0
  }
}

// 暴露方法
defineExpose({
  getGraph, startDrag, updateNode, deleteNode,
  undo, redo, zoomIn, zoomOut, fitView,
  exportLayout, importLayout
})

// 监听尺寸变化
watch([() => props.leftCollapsed, () => props.rightCollapsed], () => {
  nextTick(() => graph.value?.resize())
})

onMounted(() => {
  nextTick(() => initGraph())
})

onUnmounted(() => {
  graph.value?.dispose()
})
</script>

<style scoped>
.center-canvas {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: #f8f9fa;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.graph-container {
  width: 100%;
  height: 100%;
}

.zoom-control {
  position: absolute;
  bottom: 16px;
  left: 16px;
  background: rgba(255, 255, 255, 0.9);
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  color: #666;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

.empty-hint {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  pointer-events: none;
}

.hint-icon {
  font-size: 48px;
  color: #d9d9d9;
  margin-bottom: 16px;
}

.hint-text {
  font-size: 16px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.hint-sub {
  font-size: 13px;
  color: #bfbfbf;
}

.minimap-container {
  position: absolute;
  bottom: 16px;
  right: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

/* 响应式 */
@media (max-width: 768px) {
  .minimap-container {
    display: none;
  }
  
  .zoom-control {
    bottom: 8px;
    left: 8px;
  }
}
</style>
