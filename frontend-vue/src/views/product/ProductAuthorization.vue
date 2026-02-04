<template>
  <div class="product-authorization">
    <a-card title="产品可见性授权" :bordered="false">
      <template #extra>
        <a-space>
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索产品名称/SKU"
            style="width: 250px"
            @search="handleSearch"
          />
        </a-space>
      </template>

      <a-table
        :columns="columns"
        :data-source="products"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'image'">
            <a-image
              v-if="record.imageUrl"
              :src="record.imageUrl"
              :width="50"
              style="object-fit: cover"
            />
            <div v-else style="width: 50px; height: 50px; background: #f0f0f0; display: flex; align-items: center; justify-content: center;">
              <FileImageOutlined style="font-size: 20px; color: #ccc" />
            </div>
          </template>
          
          <template v-if="column.key === 'action'">
            <a-button type="link" @click="handleAuthorize(record)">授权管理</a-button>
          </template>
        </template>
      </a-table>

      <!-- Authorization Modal -->
      <a-modal
        v-model:visible="modalVisible"
        title="授权管理"
        :confirmLoading="modalLoading"
        width="800px"
        @ok="handleModalCancel"
        :footer="null"
      >
        <div v-if="currentProduct" style="margin-bottom: 24px">
          <h3>正在管理产品: {{ currentProduct.title }}</h3>
          <p class="product-meta">SKU: {{ currentProduct.handle }}</p>
        </div>

        <a-tabs default-active-key="list">
          <!-- Tab 1: Existing Authorizations -->
          <a-tab-pane key="list" tab="已授权列表">
            <a-table 
              :columns="authColumns" 
              :data-source="authorizationList"
              :loading="authListLoading"
              row-key="id"
              size="small"
              :pagination="false"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'target'">
                  <span>
                    {{ record.userId ? '用户' : '角色' }}: 
                    {{ getTargetName(record) }}
                  </span>
                </template>
                <template v-if="column.key === 'shop'">
                    {{ getShopName(record.shopId) }}
                </template>
                <template v-if="column.key === 'action'">
                    <a-popconfirm title="确定要撤销授权吗?" @confirm="handleRevoke(record)">
                        <a-button type="link" danger size="small">撤销</a-button>
                    </a-popconfirm>
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <!-- Tab 2: Grant New -->
          <a-tab-pane key="grant" tab="新增授权">
             <a-form layout="vertical">
              <a-form-item label="授权类型">
                <a-radio-group v-model:value="authForm.targetType">
                  <a-radio-button value="USER">指定用户</a-radio-button>
                  <a-radio-button value="ROLE">指定角色</a-radio-button>
                </a-radio-group>
              </a-form-item>

              <a-form-item label="选择对象" required>
                <a-select
                  v-if="authForm.targetType === 'USER'"
                  v-model:value="authForm.targetId"
                  placeholder="请选择用户"
                  show-search
                  :filter-option="filterOption"
                >
                  <a-select-option v-for="user in userList" :key="user.id" :value="user.id">
                    {{ user.realName || user.username }} ({{ user.username }})
                  </a-select-option>
                </a-select>

                <a-select
                  v-else
                  v-model:value="authForm.targetId"
                  placeholder="请选择角色"
                >
                  <a-select-option v-for="role in roleList" :key="role.id" :value="role.id">
                    {{ role.roleName }} ({{ role.roleCode }})
                  </a-select-option>
                </a-select>
              </a-form-item>

              <a-form-item label="授权范围 (店铺)">
                 <a-select
                  v-model:value="authForm.shopId"
                  placeholder="默认为所有店铺 (全局)"
                  allow-clear
                >
                  <a-select-option v-for="shop in shopList" :key="shop.id" :value="shop.id">
                    {{ shop.shopName }} ({{ shop.platform }})
                  </a-select-option>
                </a-select>
                <div class="form-tip">留空则表示该用户/角色在所有店铺均可见此产品</div>
              </a-form-item>

              <a-form-item label="过期时间 (可选)">
                <a-date-picker 
                  v-model:value="authForm.expiresAt" 
                  show-time 
                  style="width: 100%" 
                  placeholder="选择过期时间，留空则永久有效"
                />
              </a-form-item>

              <a-form-item>
                  <a-button type="primary" :loading="modalLoading" @click="handleGrant">确认授权</a-button>
              </a-form-item>
            </a-form>
          </a-tab-pane>
        </a-tabs>
      </a-modal>
    </a-card>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, watch } from 'vue'
import { message } from 'ant-design-vue'
import { FileImageOutlined } from '@ant-design/icons-vue'
import { productVisibilityApi } from '@/api/product-visibility'
import { userApi } from '@/api/user'
import { roleApi } from '@/api/role'
import { userShopApi } from '@/api/user-shop'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const searchKeyword = ref('')
const loading = ref(false)
const modalLoading = ref(false)
const authListLoading = ref(false)
const products = ref([])
const modalVisible = ref(false)
const currentProduct = ref(null)
const authorizationList = ref([])

// Data sources for selects
const userList = ref([])
const roleList = ref([])
const shopList = ref([])

const authForm = reactive({
  targetType: 'USER',
  targetId: undefined,
  shopId: undefined,
  expiresAt: undefined
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

const columns = [
  { title: '图片', key: 'image', width: 80 },
  { title: '产品标题', dataIndex: 'title', key: 'title' },
  { title: 'SKU', dataIndex: 'handle', key: 'handle' },
  { title: '操作', key: 'action', width: 120 }
]

const authColumns = [
    { title: '授权对象', key: 'target' },
    { title: '店铺范围', key: 'shop' },
    { title: '过期时间', dataIndex: 'expiresAt', key: 'expiresAt', customRender: ({text}) => text ? text.replace('T', ' ') : '永久' },
    { title: '操作', key: 'action', width: 80 }
]

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.current,
      size: pagination.pageSize,
      keyword: searchKeyword.value,
      shopId: userStore.currentShopId 
    }
    
    const res = await productVisibilityApi.getAuthorizedProducts(params)
    if (res.code === 200) {
      products.value = res.data
      const countRes = await productVisibilityApi.countAuthorizedProducts(params)
      if (countRes.code === 200) {
        pagination.total = countRes.data
      }
    }
  } catch (error) {
    console.error('Load products failed:', error)
  } finally {
    loading.value = false
  }
}

const loadAuthorizations = async (productId) => {
    authListLoading.value = true
    try {
        const res = await productVisibilityApi.getAuthorizations(productId)
        if (res.code === 200) {
            authorizationList.value = res.data
        }
    } catch (e) {
        console.error("Failed to load authorizations", e)
    } finally {
        authListLoading.value = false
    }
}

const loadMeta = async () => {
    // Load users
    try {
        const userData = await userApi.getList({ page: 1, size: 1000 })
        if (userData && userData.list) {
            userList.value = userData.list
        }
    } catch (e) { console.error(e) }

    // Load roles
    try {
        const roles = await roleApi.getAll()
        if (roles) {
            roleList.value = roles
        }
    } catch (e) { console.error(e) }

    // Load shops
    try {
        const shopRes = await userShopApi.getMyShops()
        if (shopRes.code === 200) {
            shopList.value = shopRes.data
        }
    } catch (e) { console.error(e) }
}

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

const handleAuthorize = (record) => {
  currentProduct.value = record
  authForm.targetType = 'USER'
  authForm.targetId = undefined
  authForm.shopId = undefined
  authForm.expiresAt = undefined
  
  if (userList.value.length === 0) {
      loadMeta()
  }
  loadAuthorizations(record.id)
  
  modalVisible.value = true
}

const handleModalCancel = () => {
    modalVisible.value = false
}

const handleGrant = async () => {
    if (!authForm.targetId) {
        message.warning('请选择授权对象')
        return
    }

    modalLoading.value = true
    try {
        const payload = {
            productIds: [currentProduct.value.id],
            targetType: authForm.targetType,
            targetId: authForm.targetId,
            shopId: authForm.shopId,
            expiresAt: authForm.expiresAt ? authForm.expiresAt.format('YYYY-MM-DDTHH:mm:ss') : null
        }
        
        const res = await productVisibilityApi.grantVisibility(payload)
        if (res.code === 200) {
            message.success('授权成功')
            // Refresh list
            loadAuthorizations(currentProduct.value.id)
            // Reset form partly
            authForm.targetId = undefined
        } else {
            message.error(res.message || '授权失败')
        }
    } catch (e) {
        console.error(e)
        message.error('系统错误')
    } finally {
        modalLoading.value = false
    }
}

const handleRevoke = async (record) => {
    try {
        const payload = {
            productId: record.productId,
            targetType: record.userId ? 'USER' : 'ROLE',
            targetId: record.userId || record.roleId
        }
        const res = await productVisibilityApi.revokeVisibility(payload)
         if (res.code === 200) {
            message.success('已撤销')
            loadAuthorizations(currentProduct.value.id)
        } else {
            message.error(res.message || '撤销失败')
        }
    } catch(e) {
        message.error('撤销失败')
    }
}

const getTargetName = (record) => {
    if (record.userId) {
        const u = userList.value.find(u => u.id === record.userId)
        return u ? (u.realName || u.username) : `User:${record.userId}`
    } else {
        const r = roleList.value.find(r => r.id === record.roleId)
        return r ? r.roleName : `Role:${record.roleId}`
    }
}

const getShopName = (shopId) => {
    if (!shopId) return '所有店铺 (全局)'
    const s = shopList.value.find(s => s.id === shopId)
    return s ? s.shopName : `Shop:${shopId}`
}

const filterOption = (input, option) => {
  const text = option.children ? option.children[0]?.children : ''
  return String(text).toLowerCase().indexOf(input.toLowerCase()) >= 0
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.product-authorization {
  padding: 24px;
}
.product-meta {
  color: #888;
  font-size: 13px;
}
.form-tip {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
}
</style>
