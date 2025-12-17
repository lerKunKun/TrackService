<template>
  <div class="menus-page">
    <a-card title="菜单管理" :bordered="false">
      <!-- 工具栏 -->
      <template #extra>
        <a-space>
          <a-button type="primary" @click="showAddDialog">
            <template #icon><PlusOutlined /></template>
            新建菜单
          </a-button>
          <a-button @click="loadMenuTree">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </a-space>
      </template>

      <!-- 菜单树表格 -->
      <a-table
        :columns="columns"
        :data-source="menuTree"
        :loading="loading"
        :pagination="false"
        :row-key="record => record.id"
        :default-expand-all-rows="true"
        :childrenColumnName="'children'"
      >
        <!-- 菜单名称 -->
        <template #menuName="{ record }">
          <a-space>
            <component :is="getIcon(record.icon)" v-if="record.icon" />
            <span>{{ record.menuName }}</span>
          </a-space>
        </template>

        <!-- 菜单类型 -->
        <template #menuType="{ record }">
          <a-tag :color="record.menuType === 'MENU' ? 'blue' : 'green'">
            {{ record.menuType === 'MENU' ? '菜单' : '按钮' }}
          </a-tag>
        </template>

        <!-- 状态 -->
        <template #status="{ record }">
          <a-badge
            :status="record.status === 1 ? 'success' : 'default'"
            :text="record.status === 1 ? '启用' : '禁用'"
          />
        </template>

        <!-- 可见性 -->
        <template #visible="{ record }">
          <a-tag :color="record.visible === 1 ? 'success' : 'default'">
            {{ record.visible === 1 ? '显示' : '隐藏' }}
          </a-tag>
        </template>

        <!-- 操作 -->
        <template #action="{ record }">
          <a-space>
            <a-button type="link" size="small" @click="showEditDialog(record)">编辑</a-button>
            <a-button type="link" size="small" @click="showAddDialog(record)">添加子菜单</a-button>
            <a-popconfirm
              title="确定要删除该菜单吗？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="deleteMenu(record.id)"
            >
              <a-button type="link" danger size="small">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <!-- 新建/编辑菜单对话框 -->
    <a-modal
      v-model:open="dialogVisible"
      :title="dialogTitle"
      :width="700"
      @ok="handleSubmit"
      @cancel="handleCancel"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="上级菜单" name="parentId">
          <a-tree-select
            v-model:value="formData.parentId"
            :tree-data="menuTreeOptions"
            placeholder="选择上级菜单（不选则为顶级菜单）"
            allow-clear
            :field-names="{ label: 'menuName', value: 'id', children: 'children' }"
          />
        </a-form-item>

        <a-form-item label="菜单名称" name="menuName" required>
          <a-input v-model:value="formData.menuName" placeholder="请输入菜单名称" />
        </a-form-item>

        <a-form-item label="菜单编码" name="menuCode" required>
          <a-input v-model:value="formData.menuCode" placeholder="请输入菜单编码（唯一）" />
        </a-form-item>

        <a-form-item label="菜单类型" name="menuType" required>
          <a-radio-group v-model:value="formData.menuType">
            <a-radio value="MENU">菜单</a-radio>
            <a-radio value="BUTTON">按钮</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="路由路径" name="path">
          <a-input v-model:value="formData.path" placeholder="如：/system/menus" />
        </a-form-item>

        <a-form-item label="组件路径" name="component">
          <a-input v-model:value="formData.component" placeholder="如：@/views/system/Menus.vue" />
        </a-form-item>

        <a-form-item label="菜单图标" name="icon">
          <a-input v-model:value="formData.icon" placeholder="如：MenuOutlined">
            <template #prefix>
              <component :is="getIcon(formData.icon)" v-if="formData.icon" />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item label="排序" name="sortOrder">
          <a-input-number
            v-model:value="formData.sortOrder"
            :min="0"
            :max="9999"
            placeholder="数字越小越靠前"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="是否显示" name="visible">
          <a-radio-group v-model:value="formData.visible">
            <a-radio :value="1">显示</a-radio>
            <a-radio :value="0">隐藏</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, createVNode } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'
import * as Icons from '@ant-design/icons-vue'

// 表格列定义
const columns = [
  {
    title: '菜单名称',
    dataIndex: 'menuName',
    key: 'menuName',
    width: 250,
    slots: { customRender: 'menuName' }
  },
  {
    title: '菜单编码',
    dataIndex: 'menuCode',
    key: 'menuCode',
    width: 180
  },
  {
    title: '菜单类型',
    dataIndex: 'menuType',
    key: 'menuType',
    width: 100,
    slots: { customRender: 'menuType' }
  },
  {
    title: '路由路径',
    dataIndex: 'path',
    key: 'path',
    width: 200
  },
  {
    title: '排序',
    dataIndex: 'sortOrder',
    key: 'sortOrder',
    width: 80
  },
  {
    title: '可见性',
    dataIndex: 'visible',
    key: 'visible',
    width: 100,
    slots: { customRender: 'visible' }
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100,
    slots: { customRender: 'status' }
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 250,
    slots: { customRender: 'action' }
  }
]

// 数据状态
const loading = ref(false)
const menuTree = ref([])
const menuTreeOptions = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新建菜单')
const formRef = ref()

// 表单数据
const formData = reactive({
  id: null,
  parentId: 0,
  menuName: '',
  menuCode: '',
  menuType: 'MENU',
  path: '',
  component: '',
  icon: '',
  sortOrder: 0,
  visible: 1,
  status: 1
})

// 表单验证规则
const rules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuCode: [{ required: true, message: '请输入菜单编码', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
}

// 获取图标组件
const getIcon = (iconName) => {
  if (!iconName) return null
  return Icons[iconName] || null
}

// 加载菜单树
const loadMenuTree = async () => {
  loading.value = true
  try {
    const response = await request.get('/menus/tree')
    // response is already response.data due to interceptor
    if (response.code === 200) {
      menuTree.value = response.data
      // 构建树形选择器数据（添加"顶级菜单"选项）
      menuTreeOptions.value = [
        { id: 0, menuName: '顶级菜单', children: response.data }
      ]
    } else {
      message.error(response.message || '获取菜单列表失败')
    }
  } catch (error) {
    console.error('加载菜单树失败:', error)
    message.error('加载菜单树失败')
  } finally {
    loading.value = false
  }
}

// 显示新建对话框
const showAddDialog = (parentMenu = null) => {
  dialogTitle.value = parentMenu ? `添加子菜单 - ${parentMenu.menuName}` : '新建菜单'
  Object.assign(formData, {
    id: null,
    parentId: parentMenu ? parentMenu.id : 0,
    menuName: '',
    menuCode: '',
    menuType: 'MENU',
    path: '',
    component: '',
    icon: '',
    sortOrder: 0,
    visible: 1,
    status: 1
  })
  dialogVisible.value = true
}

// 显示编辑对话框
const showEditDialog = (menu) => {
  dialogTitle.value = '编辑菜单'
  Object.assign(formData, {
    id: menu.id,
    parentId: menu.parentId || 0,
    menuName: menu.menuName,
    menuCode: menu.menuCode,
    menuType: menu.menuType,
    path: menu.path,
    component: menu.component,
    icon: menu.icon,
    sortOrder: menu.sortOrder,
    visible: menu.visible,
    status: menu.status
  })
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    const submitData = { ...formData }
    // 如果parentId为0，设置为null（顶级菜单）
    if (submitData.parentId === 0) {
      submitData.parentId = 0
    }

    if (formData.id) {
      // 编辑
      const response = await request.put(`/menus/${formData.id}`, submitData)
      if (response.code === 200) {
        message.success('更新菜单成功')
        dialogVisible.value = false
        loadMenuTree()
      } else {
        message.error(response.message || '更新菜单失败')
      }
    } else {
      // 新建
      const response = await request.post('/menus', submitData)
      if (response.code === 200) {
        message.success('创建菜单成功')
        dialogVisible.value = false
        loadMenuTree()
      } else {
        message.error(response.message || '创建菜单失败')
      }
    }
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

// 取消对话框
const handleCancel = () => {
  dialogVisible.value = false
  formRef.value?.resetFields()
}

// 删除菜单
const deleteMenu = async (id) => {
  try {
    const response = await request.delete(`/menus/${id}`)
    if (response.code === 200) {
      message.success('删除菜单成功')
      loadMenuTree()
    } else {
      message.error(response.message || '删除菜单失败')
    }
  } catch (error) {
    console.error('删除菜单失败:', error)
    message.error('删除菜单失败')
  }
}

// 页面加载时获取菜单树
onMounted(() => {
  loadMenuTree()
})
</script>

<style scoped>
.menus-page {
  padding: 24px;
}
</style>
