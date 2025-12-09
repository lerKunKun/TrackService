<template>
  <div class="page-content">
    <a-card title="用户管理" :bordered="false">
      <!-- 操作栏 -->
      <div class="table-operations">
        <a-button type="primary" @click="showCreateModal">
          <PlusOutlined />
          新增用户
        </a-button>
      </div>

      <!-- 用户表格 -->
      <!-- 用户表格 (PC端) -->
      <a-table
        v-if="!isMobile"
        :columns="columns"
        :data-source="userList"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'role'">
            <a-tag :color="record.role === 'ADMIN' ? 'blue' : 'default'">
              {{ record.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="showEditModal(record)">
                编辑
              </a-button>
              <a-button type="link" size="small" @click="showPasswordModal(record)">
                改密
              </a-button>
              <a-button
                type="link"
                size="small"
                @click="handleToggleStatus(record)"
              >
                {{ record.status === 1 ? '禁用' : '启用' }}
              </a-button>
              <a-popconfirm
                title="确定要删除此用户吗？"
                @confirm="handleDelete(record.id)"
                :disabled="record.role === 'ADMIN'"
              >
                <a-button
                  type="link"
                  size="small"
                  danger
                  :disabled="record.role === 'ADMIN'"
                >
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>

      <!-- 卡片列表 (移动端) -->
      <div v-else class="mobile-list">
        <a-spin :spinning="loading">
          <div v-if="userList.length > 0">
            <div v-for="item in userList" :key="item.id" class="mobile-card">
              <div class="card-header">
                <span class="username">{{ item.username }}</span>
                <a-tag :color="item.role === 'ADMIN' ? 'blue' : 'default'">
                  {{ item.role === 'ADMIN' ? '管理员' : '普通用户' }}
                </a-tag>
              </div>
              <div class="card-body">
                <div class="info-row">
                  <span class="label">真实姓名:</span>
                  <span class="value">{{ item.realName || '-' }}</span>
                </div>
                <div class="info-row">
                  <span class="label">邮箱:</span>
                  <span class="value">{{ item.email || '-' }}</span>
                </div>
                <div class="info-row">
                  <span class="label">状态:</span>
                  <span class="value">
                    <a-tag :color="item.status === 1 ? 'green' : 'red'">
                      {{ item.status === 1 ? '启用' : '禁用' }}
                    </a-tag>
                  </span>
                </div>
                <div class="info-row">
                  <span class="label">创建时间:</span>
                  <span class="value">{{ item.createdAt }}</span>
                </div>
              </div>
              <div class="card-actions">
                <a-button type="link" size="small" @click="showEditModal(item)">
                  编辑
                </a-button>
                <a-button type="link" size="small" @click="showPasswordModal(item)">
                  改密
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  @click="handleToggleStatus(item)"
                >
                  {{ item.status === 1 ? '禁用' : '启用' }}
                </a-button>
                <a-popconfirm
                  title="确定要删除此用户吗？"
                  @confirm="handleDelete(item.id)"
                  :disabled="item.role === 'ADMIN'"
                >
                  <a-button
                    type="link"
                    size="small"
                    danger
                    :disabled="item.role === 'ADMIN'"
                  >
                    删除
                  </a-button>
                </a-popconfirm>
              </div>
            </div>
            <div class="mobile-pagination">
              <a-pagination
                v-model:current="pagination.current"
                :total="pagination.total"
                :page-size="pagination.pageSize"
                simple
                @change="(page) => handleTableChange({ current: page, pageSize: pagination.pageSize })"
              />
            </div>
          </div>
          <a-empty v-else description="暂无数据" />
        </a-spin>
      </div>
    </a-card>

    <!-- 新增/编辑用户弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑用户' : '新增用户'"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
      :confirm-loading="modalLoading"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="用户名" name="username" v-if="!isEdit">
          <a-input v-model:value="formState.username" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="密码" name="password" v-if="!isEdit">
          <a-input-password v-model:value="formState.password" placeholder="请输入密码" />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="formState.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item label="手机号" name="phone">
          <a-input v-model:value="formState.phone" placeholder="请输入手机号" />
        </a-form-item>
        <a-form-item label="真实姓名" name="realName">
          <a-input v-model:value="formState.realName" placeholder="请输入真实姓名" />
        </a-form-item>
        <a-form-item label="角色" name="role" v-if="!isEdit">
          <a-select v-model:value="formState.role" placeholder="请选择角色">
            <a-select-option value="USER">普通用户</a-select-option>
            <a-select-option value="ADMIN">管理员</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 修改密码弹窗 -->
    <a-modal
      v-model:open="passwordModalVisible"
      title="修改密码"
      @ok="handlePasswordOk"
      @cancel="handlePasswordCancel"
      :confirm-loading="passwordModalLoading"
    >
      <a-form
        ref="passwordFormRef"
        :model="passwordFormState"
        :rules="passwordFormRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="旧密码" name="oldPassword">
          <a-input-password v-model:value="passwordFormState.oldPassword" placeholder="请输入旧密码" />
        </a-form-item>
        <a-form-item label="新密码" name="newPassword">
          <a-input-password v-model:value="passwordFormState.newPassword" placeholder="请输入新密码" />
        </a-form-item>
        <a-form-item label="确认密码" name="confirmPassword">
          <a-input-password v-model:value="passwordFormState.confirmPassword" placeholder="请再次输入新密码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { message, Grid } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { userApi } from '@/api/user'

// 表格列定义
const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '手机号', dataIndex: 'phone', key: 'phone' },
  { title: '真实姓名', dataIndex: 'realName', key: 'realName' },
  { title: '角色', dataIndex: 'role', key: 'role', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 200 }
]

// 数据状态
const loading = ref(false)
const userList = ref([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

const useBreakpoint = Grid.useBreakpoint
const screens = useBreakpoint()
const isMobile = computed(() => !screens.value.md)

// 弹窗状态
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const editingUserId = ref(null)
const formRef = ref()
const formState = reactive({
  username: '',
  password: '',
  email: '',
  phone: '',
  realName: '',
  role: 'USER'
})

// 密码弹窗状态
const passwordModalVisible = ref(false)
const passwordModalLoading = ref(false)
const passwordUserId = ref(null)
const passwordFormRef = ref()
const passwordFormState = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单验证规则
const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const passwordFormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value) => {
        if (value !== passwordFormState.newPassword) {
          return Promise.reject('两次输入的密码不一致')
        }
        return Promise.resolve()
      },
      trigger: 'blur'
    }
  ]
}

// 获取用户列表
const fetchUsers = async () => {
  loading.value = true
  try {
    const data = await userApi.getList({
      page: pagination.current,
      size: pagination.pageSize
    })
    userList.value = data.list
    pagination.total = data.total
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 表格分页变化
const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchUsers()
}

// 显示新增弹窗
const showCreateModal = () => {
  isEdit.value = false
  editingUserId.value = null
  resetForm()
  modalVisible.value = true
}

// 显示编辑弹窗
const showEditModal = (record) => {
  isEdit.value = true
  editingUserId.value = record.id
  Object.assign(formState, {
    username: record.username,
    email: record.email || '',
    phone: record.phone || '',
    realName: record.realName || '',
    role: record.role
  })
  modalVisible.value = true
}

// 重置表单
const resetForm = () => {
  Object.assign(formState, {
    username: '',
    password: '',
    email: '',
    phone: '',
    realName: '',
    role: 'USER'
  })
}

// 弹窗确认
const handleModalOk = async () => {
  try {
    await formRef.value.validate()
    modalLoading.value = true

    if (isEdit.value) {
      await userApi.update(editingUserId.value, {
        email: formState.email,
        phone: formState.phone,
        realName: formState.realName
      })
      message.success('用户更新成功')
    } else {
      await userApi.create(formState)
      message.success('用户创建成功')
    }

    modalVisible.value = false
    fetchUsers()
  } catch (error) {
    console.error('保存用户失败:', error)
  } finally {
    modalLoading.value = false
  }
}

// 弹窗取消
const handleModalCancel = () => {
  modalVisible.value = false
  resetForm()
}

// 显示密码弹窗
const showPasswordModal = (record) => {
  passwordUserId.value = record.id
  Object.assign(passwordFormState, {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  })
  passwordModalVisible.value = true
}

// 密码弹窗确认
const handlePasswordOk = async () => {
  try {
    await passwordFormRef.value.validate()
    passwordModalLoading.value = true

    await userApi.changePassword(passwordUserId.value, {
      oldPassword: passwordFormState.oldPassword,
      newPassword: passwordFormState.newPassword
    })

    message.success('密码修改成功')
    passwordModalVisible.value = false
  } catch (error) {
    console.error('修改密码失败:', error)
  } finally {
    passwordModalLoading.value = false
  }
}

// 密码弹窗取消
const handlePasswordCancel = () => {
  passwordModalVisible.value = false
}

// 切换用户状态
const handleToggleStatus = async (record) => {
  const newStatus = record.status === 1 ? 0 : 1
  try {
    await userApi.updateStatus(record.id, newStatus)
    message.success(`用户已${newStatus === 1 ? '启用' : '禁用'}`)
    fetchUsers()
  } catch (error) {
    console.error('更新状态失败:', error)
  }
}

// 删除用户
const handleDelete = async (id) => {
  try {
    await userApi.delete(id)
    message.success('用户删除成功')
    fetchUsers()
  } catch (error) {
    console.error('删除用户失败:', error)
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.page-content {
  padding: 24px;
}

.table-operations {
  margin-bottom: 16px;
}

/* Mobile Styles */
.mobile-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mobile-card {
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
}

.mobile-card:active {
  background: #fafafa;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.username {
  font-weight: 600;
  font-size: 16px;
  color: #262626;
}

.card-body {
  margin-bottom: 12px;
}

.info-row {
  display: flex;
  margin-bottom: 8px;
  font-size: 14px;
  line-height: 1.5;
}

.info-row .label {
  color: #8c8c8c;
  width: 70px;
  flex-shrink: 0;
}

.info-row .value {
  color: #262626;
  flex: 1;
  min-width: 0;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  flex-wrap: wrap;
}

.card-actions .ant-btn {
  padding: 4px 12px;
  height: auto;
}

.mobile-pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

@media (max-width: 576px) {
  .page-content {
    padding: 12px;
  }
}
</style>
