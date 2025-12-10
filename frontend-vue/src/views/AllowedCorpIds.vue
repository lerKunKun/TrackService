<template>
  <div class="allowed-corp-ids-container">
    <div class="page-header">
      <h2>企业CorpID管理</h2>
      <a-button type="primary" @click="showAddModal">
        <PlusOutlined /> 添加企业
      </a-button>
    </div>

    <!-- 数据表格 -->
    <a-table
      :columns="columns"
      :data-source="corpIds"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'success' : 'default'">
            {{ record.status === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        
        <template v-else-if="column.key === 'createdAt'">
          {{ formatDate(record.createdAt) }}
        </template>

        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button
              type="link"
              size="small"
              @click="toggleStatus(record)"
            >
              {{ record.status === 1 ? '禁用' : '启用' }}
            </a-button>
            <a-popconfirm
              title="确定要删除这个企业吗？"
              @confirm="deleteCorpId(record.corpId)"
            >
              <a-button type="link" size="small" danger>
                删除
              </a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 添加企业弹窗 -->
    <a-modal
      v-model:open="addModalVisible"
      title="添加允许登录的企业"
      @ok="handleAdd"
      @cancel="handleCancel"
    >
      <a-form
        :model="formState"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="企业CorpID" required>
          <a-input
            v-model:value="formState.corpId"
            placeholder="请输入企业CorpID"
          />
        </a-form-item>
        
        <a-form-item label="企业名称">
          <a-input
            v-model:value="formState.corpName"
            placeholder="请输入企业名称（可选）"
          />
        </a-form-item>

        <a-form-item label="状态">
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
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import axios from 'axios'

// 表格列定义
const columns = [
  {
    title: '企业CorpID',
    dataIndex: 'corpId',
    key: 'corpId',
    width: 280
  },
  {
    title: '企业名称',
    dataIndex: 'corpName',
    key: 'corpName'
  },
  {
    title: '状态',
    key: 'status',
    dataIndex: 'status',
    width: 100
  },
  {
    title: '创建人',
    dataIndex: 'createdBy',
    key: 'createdBy',
    width: 120
  },
  {
    title: '创建时间',
    key: 'createdAt',
    dataIndex: 'createdAt',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    width: 150
  }
]

// 数据状态
const loading = ref(false)
const corpIds = ref([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

// 添加弹窗状态
const addModalVisible = ref(false)
const formState = reactive({
  corpId: '',
  corpName: '',
  status: 1
})

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/v1/allowed-corp-ids', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    if (response.data.code === 200) {
      corpIds.value = response.data.data
      pagination.total = response.data.data.length
    } else {
      message.error(response.data.message || '加载失败')
    }
  } catch (error) {
    console.error('Load error:', error)
    message.error('加载失败，请检查网络或权限')
  } finally {
    loading.value = false
  }
}

// 显示添加弹窗
const showAddModal = () => {
  formState.corpId = ''
  formState.corpName = ''
  formState.status = 1
  addModalVisible.value = true
}

// 处理添加
const handleAdd = async () => {
  if (!formState.corpId) {
    message.error('请输入企业CorpID')
    return
  }

  try {
    const token = localStorage.getItem('token')
    const response = await axios.post('/api/v1/allowed-corp-ids', formState, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    })

    if (response.data.code === 200) {
      message.success('添加成功')
      addModalVisible.value = false
      loadData()
    } else {
      message.error(response.data.message || '添加失败')
    }
  } catch (error) {
    console.error('Add error:', error)
    message.error('添加失败，请检查网络或权限')
  }
}

// 取消添加
const handleCancel = () => {
  addModalVisible.value = false
}

// 切换状态
const toggleStatus = async (record) => {
  const newStatus = record.status === 1 ? 0 : 1
  
  try {
    const token = localStorage.getItem('token')
    const response = await axios.put(
      `/api/v1/allowed-corp-ids/${record.corpId}/status?status=${newStatus}`,
      null,
      {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      }
    )

    if (response.data.code === 200) {
      message.success('状态更新成功')
      loadData()
    } else {
      message.error(response.data.message || '更新失败')
    }
  } catch (error) {
    console.error('Update error:', error)
    message.error('更新失败，请检查网络或权限')
  }
}

// 删除企业
const deleteCorpId = async (corpId) => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.delete(`/api/v1/allowed-corp-ids/${corpId}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    if (response.data.code === 200) {
      message.success('删除成功')
      loadData()
    } else {
      message.error(response.data.message || '删除失败')
    }
  } catch (error) {
    console.error('Delete error:', error)
    message.error('删除失败，请检查网络或权限')
  }
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

// 页面加载时获取数据
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.allowed-corp-ids-container {
  padding: 24px;
  background: #fff;
  min-height: calc(100vh - 64px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
</style>
