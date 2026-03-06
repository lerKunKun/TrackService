<template>
  <div class="product-template-container">
    <a-card title="产品模板管理" :bordered="false">
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="productId"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'templateName'">
            <a-input 
              v-if="record.isEditing" 
              v-model:value="record.editTemplateName" 
              size="small"
            />
            <span v-else>{{ record.templateName || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'storeIdentifier'">
            <a-input 
              v-if="record.isEditing" 
              v-model:value="record.editStoreIdentifier" 
              size="small"
            />
            <span v-else>{{ record.storeIdentifier || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'templateVersion'">
            <span>{{ record.templateVersion || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <template v-if="record.isEditing">
              <a-space>
                <a-button size="small" type="primary" @click="saveEdit(record)">保存</a-button>
                <a-button size="small" @click="cancelEdit(record)">取消</a-button>
              </a-space>
            </template>
            <template v-else>
              <a-space>
                <a-button type="link" size="small" @click="startEdit(record)">编辑</a-button>
                <a-tooltip title="预览产品模板">
                  <a-button type="primary" shape="circle" size="small" :loading="record.previewing" @click="handlePreview(record)">
                    <template #icon><EyeOutlined /></template>
                  </a-button>
                </a-tooltip>
              </a-space>
            </template>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { EyeOutlined } from '@ant-design/icons-vue'
import { getProductTemplateList, updateProductTemplateInfo, previewProductTemplate } from '@/api/product-media-template'

const loading = ref(false)
const tableData = ref([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: total => `共 ${total} 条`
})

const columns = [
  {
    title: '产品名',
    dataIndex: 'productName',
    key: 'productName',
    width: 250
  },
  {
    title: '模板名称',
    dataIndex: 'templateName',
    key: 'templateName',
    width: 200
  },
  {
    title: '模板版本',
    dataIndex: 'templateVersion',
    key: 'templateVersion',
    width: 150
  },
  {
    title: '店铺',
    dataIndex: 'storeIdentifier',
    key: 'storeIdentifier',
    width: 200
  },
  {
    title: '操作',
    key: 'action',
    width: 180,
    fixed: 'right'
  }
]

const fetchData = () => {
  loading.value = true
  getProductTemplateList({
    current: pagination.current,
    size: pagination.pageSize
  }).then(res => {
    if (res.code === 200) {
      const records = res.data.records || []
      tableData.value = records.map(item => ({
        ...item,
        isEditing: false,
        editTemplateName: item.templateName,
        editStoreIdentifier: item.storeIdentifier,
        previewing: false
      }))
      pagination.total = res.data.total || 0
    }
  }).finally(() => {
    loading.value = false
  })
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const startEdit = (row) => {
  row.isEditing = true
  row.editTemplateName = row.templateName
  row.editStoreIdentifier = row.storeIdentifier
}

const cancelEdit = (row) => {
  row.isEditing = false
}

const saveEdit = (row) => {
  updateProductTemplateInfo(row.productId, {
    templateName: row.editTemplateName,
    storeIdentifier: row.editStoreIdentifier
  }).then(res => {
    if (res.code === 200) {
      message.success('更新成功')
      row.templateName = row.editTemplateName
      row.storeIdentifier = row.editStoreIdentifier
      row.isEditing = false
    } else {
      message.error(res.message || '更新失败')
    }
  })
}

const handlePreview = (row) => {
  row.previewing = true
  message.info('正在准备预览环境并推送资源，请稍候...')
  
  previewProductTemplate(row.productId).then(res => {
    if (res.code === 200 && res.data) {
      message.success('准备完成，正在打开预览...')
      window.open(res.data, '_blank')
    } else {
      message.error(res.message || '预览生成失败')
    }
  }).finally(() => {
    row.previewing = false
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.product-template-container {
  padding: 24px;
  background: #f0f2f5;
  min-height: 100%;
}
</style>
