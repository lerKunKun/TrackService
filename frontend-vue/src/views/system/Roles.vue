<template>
  <div class="page-content">
    <a-card title="角色管理" :bordered="false">
      <template #extra>
        <a-button type="primary" @click="showCreateModal">
          <PlusOutlined /> 新增角色
        </a-button>
      </template>

      <!-- 角色列表 -->
      <a-table 
        :dataSource="roles" 
        :columns="columns"
        :loading="loading"
        :rowKey="record => record.id">
        
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="assignMenus(record)">
                分配菜单
              </a-button>
              <a-button type="link" size="small" @click="assignPermissions(record)">
                分配权限
              </a-button>
              <a-button type="link" size="small" @click="editRole(record)">
                编辑
              </a-button>
              <a-popconfirm
                title="确定要删除此角色吗？"
                @confirm="deleteRole(record)"
                :disabled="record.roleCode === 'SUPER_ADMIN'">
                <a-button 
                  type="link" 
                  size="small" 
                  danger
                  :disabled="record.roleCode === 'SUPER_ADMIN'">
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 角色编辑对话框 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      @ok="handleSubmit">
      <a-form :model="formData" layout="vertical">
        <a-form-item label="角色名称" required>
          <a-input v-model:value="formData.roleName" placeholder="请输入角色名称" />
        </a-form-item>
        <a-form-item label="角色编码" required>
          <a-input v-model:value="formData.roleCode" placeholder="请输入角色编码（英文）" :disabled="!!formData.id" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="formData.description" placeholder="请输入角色描述" />
        </a-form-item>
        <a-form-item label="状态">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 菜单分配对话框 -->
    <a-modal
      v-model:open="menuModalVisible"
      title="分配菜单"
      @ok="handleAssignMenus"
      width="600px">
      <a-tree
        v-model:checkedKeys="selectedMenuIds"
        checkable
        :tree-data="menuTree"
        :field-names="{ children: 'children', title: 'menuName', key: 'id' }" />
    </a-modal>

    <!-- 权限分配对话框 -->
    <a-modal
      v-model:open="permissionModalVisible"
      title="分配权限"
      @ok="handleAssignPermissions"
      width="600px">
      <a-checkbox-group v-model:value="selectedPermissionIds" style="width: 100%">
        <a-row>
          <a-col :span="24" v-for="permission in permissions" :key="permission.id" style="margin-bottom: 8px">
            <a-checkbox :value="permission.id">
              {{ permission.permissionName }} ({{ permission.permissionCode }})
            </a-checkbox>
          </a-col>
        </a-row>
      </a-checkbox-group>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const roles = ref([])
const modalVisible = ref(false)
const modalTitle = ref('新增角色')
const formData = ref({
  roleName: '',
  roleCode: '',
  description: '',
  status: 1
})

const menuModalVisible = ref(false)
const permissionModalVisible = ref(false)
const menuTree = ref([])
const permissions = ref([])
const selectedMenuIds = ref([])
const selectedPermissionIds = ref([])
const currentRole = ref(null)

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '角色名称', dataIndex: 'roleName', key: 'roleName' },
  { title: '角色编码', dataIndex: 'roleCode', key: 'roleCode' },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 320 }
]

onMounted(() => {
  loadRoles()
})

const loadRoles = async () => {
  loading.value = true
  try {
    const response = await request.get('/roles')
    roles.value = response || []
  } catch (error) {
    message.error('加载角色列表失败: ' + (error.message || ''))
  } finally {
    loading.value = false
  }
}

const showCreateModal = () => {
  modalTitle.value = '新增角色'
  formData.value = { roleName: '', roleCode: '', description: '', status: 1 }
  modalVisible.value = true
}

const editRole = (role) => {
  modalTitle.value = '编辑角色'
  formData.value = { ...role }
  modalVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (formData.value.id) {
      await request.put(`/roles/${formData.value.id}`, formData.value)
      message.success('更新角色成功')
    } else {
      await request.post('/roles', formData.value)
      message.success('创建角色成功')
    }
    modalVisible.value = false
    loadRoles()
  } catch (error) {
    message.error(error.message || '操作失败')
  }
}

const deleteRole = async (role) => {
  try {
    await request.delete(`/roles/${role.id}`)
    message.success('删除角色成功')
    loadRoles()
  } catch (error) {
    message.error(error.message || '删除失败')
  }
}

const assignMenus = async (role) => {
  currentRole.value = role
  try {
    // 加载菜单树
    const menuResponse = await request.get('/menus/tree')
    menuTree.value = menuResponse || []
    
    // 加载角色已有的菜单
    const roleMenuResponse = await request.get(`/roles/${role.id}/menus`)
    selectedMenuIds.value = (roleMenuResponse || []).map(m => m.id)
    
    menuModalVisible.value = true
  } catch (error) {
    message.error('加载菜单失败: ' + (error.message || ''))
  }
}

const handleAssignMenus = async () => {
  try {
    await request.post(`/roles/${currentRole.value.id}/menus`, {
      menuIds: selectedMenuIds.value
    })
    message.success('分配菜单成功')
    menuModalVisible.value = false
  } catch (error) {
    message.error(error.message || '分配菜单失败')
  }
}

const assignPermissions = async (role) => {
  currentRole.value = role
  try {
    // 加载所有权限
    const permResponse = await request.get('/permissions')
    permissions.value = permResponse || []
    
    // 加载角色已有的权限
    const rolePermResponse = await request.get(`/roles/${role.id}/permissions`)
    selectedPermissionIds.value = (rolePermResponse || []).map(p => p.id)
    
    permissionModalVisible.value = true
  } catch (error) {
    message.error('加载权限失败: ' + (error.message || ''))
  }
}

const handleAssignPermissions = async () => {
  try {
    await request.post(`/roles/${currentRole.value.id}/permissions`, {
      permissionIds: selectedPermissionIds.value
    })
    message.success('分配权限成功')
    permissionModalVisible.value = false
  } catch (error) {
    message.error(error.message || '分配权限失败')
  }
}
</script>

<style scoped>
.page-content {
  padding: 24px;
}
</style>
