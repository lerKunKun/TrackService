<template>
  <div class="callback-container">
    <div v-if="loading" class="callback-loading">
      <a-spin size="large" />
      <p>正在登录，请稍候...</p>
    </div>

    <a-result
      v-else-if="error"
      status="error"
      title="登录失败"
      :sub-title="error"
    >
      <template #extra>
        <a-button type="primary" @click="$router.push('/login')">
          返回登录
        </a-button>
      </template>
    </a-result>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    // 1. 从URL获取后端重定向携带的参数
    const token = route.query.token
    const username = route.query.username  
    const realName = route.query.realName
    const errorMsg = route.query.error

    if (errorMsg) {
      throw new Error(errorMsg)
    }

    if (!token || !username) {
      throw new Error('未获取到登录凭证')
    }

    // 2. 保存token和用户信息
    localStorage.setItem('token', token)
    localStorage.setItem('username', username)
    localStorage.setItem('realName', realName || username)
    
    // 3. 更新用户store
    userStore.setUser({
      username: username,
      realName: realName || username
    })
    
    message.success('登录成功')
    
    // 4. 跳转到主页
    await router.push('/')
    
  } catch (err) {
    error.value = err.message
    console.error('DingTalk login error:', err)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.callback-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f0f2f5;
}

.callback-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.callback-loading p {
  color: #666;
  font-size: 16px;
}
</style>
