<template>
  <div class="shop-select">
    <a-dropdown>
      <a class="ant-dropdown-link" @click.prevent>
        <ShopOutlined /> 
        <span class="shop-name">{{ currentShopName }}</span>
        <DownOutlined />
      </a>
      <template #overlay>
        <a-menu @click="handleShopChange" :selectedKeys="[String(userStore.currentShopId)]">
          <a-menu-item key="" :disabled="userStore.accessibleShops.length === 0">
            <template #icon><AppstoreOutlined /></template>
            <span>{{ userStore.accessibleShops.length > 0 ? '全部店铺 (全局视图)' : '暂无可用店铺' }}</span>
          </a-menu-item>
          <a-menu-divider v-if="userStore.accessibleShops.length > 0" />
          <a-menu-item v-for="shop in userStore.accessibleShops" :key="String(shop.id)">
             <template #icon>
               <component :is="getPlatformIcon(shop.platform)" />
             </template>
             <span>{{ shop.shopName }}</span>
             <span class="platform-tag">{{ shop.platform }}</span>
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { 
  ShopOutlined, 
  DownOutlined, 
  AppstoreOutlined,
  GlobalOutlined,
  ShoppingOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

const userStore = useUserStore()

const currentShopName = computed(() => {
  if (!userStore.currentShopId) return '全部店铺'
  const shop = userStore.accessibleShops.find(s => s.id === userStore.currentShopId)
  return shop ? shop.shopName : '选择店铺'
})

const getPlatformIcon = (platform) => {
  // Simple icon mapping or return generic
  if (platform === 'shopify') return ShoppingOutlined
  return GlobalOutlined
}

const handleShopChange = ({ key }) => {
  const shopId = key ? Number(key) : null
  if (shopId === userStore.currentShopId) return
  
  userStore.switchShop(shopId)
  message.success(`已切换到：${currentShopName.value}`)
  // Reload to ensure all data is refreshed with new context
  setTimeout(() => window.location.reload(), 500)
}

onMounted(() => {
  if (userStore.isLoggedIn) {
    userStore.loadAccessibleShops()
  }
})
</script>

<style scoped>
.shop-select {
  display: inline-flex;
  align-items: center;
  margin-right: 16px;
  cursor: pointer;
}

.ant-dropdown-link {
  color: rgba(0, 0, 0, 0.65);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.3s;
}

.ant-dropdown-link:hover {
  background: rgba(0, 0, 0, 0.025);
  color: #1890ff;
}

.shop-name {
  font-weight: 500;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.platform-tag {
  font-size: 12px;
  color: #999;
  margin-left: 8px;
  background: #f5f5f5;
  padding: 1px 4px;
  border-radius: 2px;
}
</style>
