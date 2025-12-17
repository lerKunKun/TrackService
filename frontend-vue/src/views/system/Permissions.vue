<template>
  <div class="page-content">
    <a-card title="权限管理" :bordered="false">
      <!-- 操作栏 -->
      <template #extra>
        <a-space>
          <a-button type="primary" @click="showCreateModal" v-if="hasPermission('system:permission:create')">
            <PlusOutlined />
            新增权限
          </a-button>
        </a-space>
      </template>

      <!-- 筛选栏 -->
      <a-form layout="inline" class="filter-form">
        <a-form-item label="权限类型">
          <a-select
            v-model:value="filters.permissionType"
            placeholder="全部"
            style="width: 150px"
            allow-clear
            @change="loadPermissions"
          >
            <a-select-option value="MENU">菜单权限</a-select-option>
            <a-select-option value="BUTTON">按钮权限</a-select-option>
            <a-select-option value="DATA">数据权限</a-select-option>
          </a-select>
        </a-form-item>
        
        <a-form-item label="状态">
          <a-select
            v-model:value="filters.status"
            placeholder="全部"
            style="width: 120px"
            allow-clear
            @change="loadPermissions"
          >
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>

      <!-- 权限列表表格 -->
      <a-table
        :columns="columns"
        :data-source="filteredPermissions"
        :loading="loading"
        :pagination="false"
        row-key="id"
        class="permissions-table"
      >
        <!-- 权限类型 -->
        <template #permissionType="{ record }">
          <a-tag :color="getTypeColor(record.permissionType)">
            {{ getTypeText(record.permissionType) }}
          </a-tag>
        </template>

        <!-- 状态 -->
        <template #status="{ record }">
          <a-tag :color="record.status === 1 ? 'success' : 'default'">
            {{ record.status === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>

        <!-- 操作列 -->
        <template #action="{ record }">
          <a-space>
            <a-button
              type="link"
              size="small"
              @click="showEditModal(record)"
              v-if="hasPermission('system:permission:update')"
            >
              编辑
            </a-button>
            <a-popconfirm
              title="确定要删除此权限吗？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="handleDelete(record.id)"
              v-if="hasPermission('system:permission:delete')"
            >
              <a-button type="link" size="small" danger>
                删除
              </a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑权限弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑权限' : '新增权限'"
      width="600px"
      :confirm-loading="modalLoading"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="权限名称" name="permissionName">
          <a-input v-model:value="formState.permissionName" placeholder="请输入权限名称" />
        </a-form-item>

        <a-form-item label="权限编码" name="permissionCode">
          <a-input
            v-model:value="formState.permissionCode"
            placeholder="例如: system:user:create"
            :disabled="isEdit"
          />
          <div class="form-tip">权限编码一旦创建不可修改</div>
        </a-form-item>

        <a-form-item label="权限类型" name="permissionType">
          <a-select v-model:value="formState.permissionType" placeholder="请选择权限类型">
            <a-select-option value="MENU">菜单权限</a-select-option>
            <a-select-option value="BUTTON">按钮权限</a-select-option>
            <a-select-option value="DATA">数据权限</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="资源类型" name="resourceType">
          <a-input v-model:value="formState.resourceType" placeholder="例如: MENU, API" />
        </a-form-item>

        <a-form-item label="资源ID" name="resourceId">
          <a-input-number
            v-model:value="formState.resourceId"
            placeholder="关联的资源ID（可选）"
            style="width: 100%"
            :min="0"
          />
        </a-form-item>

        <a-form-item label="权限描述" name="description">
          <a-textarea
            v-model:value="formState.description"
            placeholder="请输入权限描述"
            :rows="3"
          />
        </a-form-item>

        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formState.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { permissionApi } from '@/api/permission'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 权限检查
const hasPermission = (permissionCode) => {
  return userStore.permissions?.includes(permissionCode) || false
}

// 表格列定义
const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id',
    width: 80
  },
  {
    title: '权限名称',
    dataIndex: 'permissionName',
    key: 'permissionName',
    width: 150
  },
  {
    title: '权限编码',
    dataIndex: 'permissionCode',
    key: 'permissionCode',
    width: 200
  },
  {
    title: '权限类型',
    dataIndex: 'permissionType',
    key: 'permissionType',
    width: 120,
    slots: { customRender: 'permissionType' }
  },
  {
    title: '资源类型',
    dataIndex: 'resourceType',
    key: 'resourceType',
    width: 120
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description',
    ellipsis: true
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 80,
    slots: { customRender: 'status' }
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
    slots: { customRender: 'action' }
  }
]

// 数据状态
const loading = ref(false)
const permissions = ref([])
const filters = reactive({
  permissionType: undefined,
  status: undefined
})

// 筛选后的权限列表
const filteredPermissions = computed(() => {
  let result = permissions.value

  if (filters.permissionType) {
    result = result.filter(p => p.permissionType === filters.permissionType)
  }

  if (filters.status !== undefined && filters.status !== null) {
    result = result.filter(p => p.status === filters.status)
  }

  return result
})

// 弹窗状态
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const formRef = ref()
const formState = reactive({
  id: null,
  permissionName: '',
  permissionCode: '',
  permissionType: 'MENU',
  resourceType: '',
  resourceId: null,
  description: '',
  status: 1
})

// 表单验证规则
const formRules = {
  permissionName: [
    { required: true, message: '请输入权限名称', trigger: 'blur' }
  ],
  permissionCode: [
    { required: true, message: '请输入权限编码', trigger: 'blur' },
    { pattern: /^[a-z:]+$/, message: '权限编码只能包含小写字母和冒号', trigger: 'blur' }
  ],
  permissionType: [
    { required: true, message: '请选择权限类型', trigger: 'change' }
  ]
}

// 加载权限列表
const loadPermissions = async () => {
  loading.value = true
  try {
    const data = await permissionApi.getAll()
    permissions.value = data || []
  } catch (error) {
    console.error('加载权限列表失败:', error)
    message.error('加载权限列表失败')
  } finally {
    loading.value = false
  }
}

// 显示新增弹窗
const showCreateModal = () => {
  isEdit.value = false
  resetForm()
  modalVisible.value = true
}

// 显示编辑弹窗
const showEditModal = (record) => {
  isEdit.value = true
  Object.assign(formState, {
    id: record.id,
    permissionName: record.permissionName,
    permissionCode: record.permissionCode,
    permissionType: record.permissionType,
    resourceType: record.resourceType || '',
    resourceId: record.resourceId,
    description: record.description || '',
    status: record.status
  })
  modalVisible.value = true
}

// 重置表单
const resetForm = () => {
  Object.assign(formState, {
    id: null,
    permissionName: '',
    permissionCode: '',
    permissionType: 'MENU',
    resourceType: '',
    resourceId: null,
    description: '',
    status: 1
  })
  formRef.value?.resetFields()
}

// 弹窗确认
const handleModalOk = async () => {
  try {
    await formRef.value.validate()
    modalLoading.value = true

    if (isEdit.value) {
      await permissionApi.update(formState.id, formState)
      message.success('权限更新成功')
    } else {
      await permissionApi.create(formState)
      message.success('权限创建成功')
    }

    modalVisible.value = false
    loadPermissions()
  } catch (error) {
    if (error.errorFields) {
      // 表单验证错误
      return
    }
    console.error('保存权限失败:', error)
    message.error(error.message || '保存权限失败')
  } finally {
    modalLoading.value = false
  }
}

// 弹窗取消
const handleModalCancel = () => {
  modalVisible.value = false
  resetForm()
}

// 删除权限
const handleDelete = async (id) => {
  try {
    await permissionApi.delete(id)
    message.success('删除权限成功')
    loadPermissions()
  } catch (error) {
    console.error('删除权限失败:', error)
    message.error(error.message || '删除权限失败')
  }
}

// 获取类型颜色
const getTypeColor = (type) => {
  const colors = {
    MENU: 'blue',
    BUTTON: 'green',
    DATA: 'orange'
  }
  return colors[type] || 'default'
}

// 获取类型文本
const getTypeText = (type) => {
  const texts = {
    MENU: '菜单权限',
    BUTTON: '按钮权限',
    DATA: '数据权限'
  }
  return texts[type] || type
}

onMounted(() => {
  loadPermissions()
})
</script>

<style scoped>
.page-content {
  padding: 24px;
}

.filter-form {
  margin-bottom: 16px;
}

.permissions-table {
  margin-top: 16px;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
