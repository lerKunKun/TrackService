<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <div class="logo">
          <RocketOutlined class="logo-icon" />
          <h1>BIOU EMP</h1>
        </div>
        <p class="subtitle">比欧中台</p>
      </div>

      <!-- Tab切换：账号密码登录 / 钉钉扫码登录 -->
      <a-tabs v-model:activeKey="loginType" centered class="login-tabs">
        <!-- 账号密码登录 -->
        <a-tab-pane key="password" tab="账号密码登录">
          <a-form
            :model="formState"
            :rules="rules"
            @finish="handleLogin"
            class="login-form"
          >
            <a-form-item name="username">
              <a-input
                v-model:value="formState.username"
                size="large"
                placeholder="请输入用户名"
              >
                <template #prefix>
                  <UserOutlined />
                </template>
              </a-input>
            </a-form-item>

            <a-form-item name="password">
              <a-input-password
                v-model:value="formState.password"
                size="large"
                placeholder="请输入密码"
              >
                <template #prefix>
                  <LockOutlined />
                </template>
              </a-input-password>
            </a-form-item>

            <a-form-item>
              <a-button
                type="primary"
                html-type="submit"
                size="large"
                :loading="loading"
                block
              >
                登录
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <!-- 钉钉扫码登录 -->
        <a-tab-pane key="dingtalk" tab="钉钉扫码登录">
          <div class="qrcode-container">
            <div v-if="qrcodeLoading" class="qrcode-loading">
              <a-spin size="large" />
              <p>正在加载二维码...</p>
            </div>

            <div v-else-if="qrcodeError" class="qrcode-error">
              <a-result
                status="error"
                title="加载失败"
                :sub-title="qrcodeError"
              >
                <template #extra>
                  <a-button type="primary" @click="loadDingTalkQRCode">
                    重新加载
                  </a-button>
                </template>
              </a-result>
            </div>

            <div v-else>
              <div id="dingtalk-qrcode" class="qrcode-wrapper"></div>
              <p class="qrcode-tip">请使用钉钉扫描二维码登录</p>
            </div>
          </div>
        </a-tab-pane>
      </a-tabs>

    </div>
  </div>
</template>

<script setup>
import { reactive, ref, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, RocketOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// 登录方式: password | dingtalk
const loginType = ref('password')

// 账号密码登录表单
const formState = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const loading = ref(false)

// 钉钉二维码相关状态
const qrcodeLoading = ref(false)
const qrcodeError = ref('')

// 监听登录方式切换
watch(loginType, (newType) => {
  if (newType === 'dingtalk') {
    // 切换到钉钉登录时加载二维码
    loadDingTalkQRCode()
  }
})

// 账号密码登录
const handleLogin = async () => {
  loading.value = true
  try {
    await userStore.login(formState)
    message.success('登录成功！')
    router.push('/')
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载钉钉二维码
const loadDingTalkQRCode = async () => {
  try {
    qrcodeLoading.value = true
    qrcodeError.value = ''
    
    // 1. 获取钉钉登录URL
    const response = await fetch('/api/v1/auth/dingtalk/login-url')
    const result = await response.json()
    
    if (!response.ok || result.code !== 200) {
      throw new Error(result.message || '获取登录链接失败')
    }
    
    const loginUrl = result.data
    console.log('Got login URL:', loginUrl)
    
    // 2. 设置loading为false以渲染容器DOM
    qrcodeLoading.value = false
    
    // 3. 等待DOM更新完成后再调用SDK
    await nextTick()
    
    // 4. 使用DingTalk SDK渲染二维码
    renderQRCodeWithSDK(loginUrl)
    
  } catch (error) {
    qrcodeError.value = error.message
    qrcodeLoading.value = false
    console.error('Load QR code error:', error)
  }
}

// 使用DingTalk SDK渲染二维码
const renderQRCodeWithSDK = (loginUrl) => {
  const container = document.getElementById('dingtalk-qrcode')
  if (!container) {
    console.error('Container element not found')
    return
  }
  
  // 解析loginUrl获取参数（OAuth 2.0）
  const url = new URL(loginUrl)
  const appid = url.searchParams.get('client_id')  // OAuth 2.0 使用 client_id
  const redirectUri = url.searchParams.get('redirect_uri')
  const state = url.searchParams.get('state')
  
  console.log('Rendering QR code with params:', { appid, redirectUri, state })
  
  // 使用DingTalk官方SDK渲染二维码
  if (window.DTFrameLogin) {
    try {
      window.DTFrameLogin(
        {
          id: 'dingtalk-qrcode',
          width: 300,
          height: 300
        },
        {
          redirect_uri: redirectUri,
          client_id: appid,
          scope: 'openid',
          response_type: 'code',
          state: state,
          prompt: 'consent'
        },
        async (loginResult) => {
          console.log('✅ DingTalk login success callback:', loginResult)
          
          try {
            const authCode = loginResult.authCode
            if (!authCode) {
              throw new Error('未获取到授权码')
            }
            
            console.log('🔑 Got authCode:', authCode)
            
            // 调用后端登录接口
            const response = await fetch('/api/v1/auth/dingtalk/callback', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ code: authCode })
            })
            
            const result = await response.json()
            console.log('📥 Backend response:', result)
            
            if (result.code === 200) {
              // 保存用户信息和token
              await userStore.setToken(result.data.token)
              await userStore.setUserInfo({
                username: result.data.username,
                realName: result.data.realName
              })
              
              message.success('登录成功！')
              router.push('/')
            } else {
              throw new Error(result.message || '登录失败')
            }
          } catch (error) {
            console.error('❌ Login processing error:', error)
            message.error('钉钉登录失败: ' + error.message)
            qrcodeError.value = error.message
          }
        },
        (errorMsg) => {
          console.error('❌ DingTalk login error callback:', errorMsg)
          message.error('二维码加载失败')
          qrcodeError.value = `登录失败: ${errorMsg}`
        }
      )
    } catch (error) {
      console.error('DTFrameLogin error:', error)
      qrcodeError.value = 'QR码加载失败，请刷新重试'
    }
  } else {
    console.error('DTFrameLogin not found on window')
    qrcodeError.value = 'DingTalk SDK未加载，请刷新页面重试'
  }
}

</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  padding: 20px;
}

.login-box {
  width: 100%;
  max-width: 450px;
  background: white;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 8px;
}

.logo-icon {
  font-size: 36px;
  color: #1890ff;
}

.logo h1 {
  margin: 0;
  font-size: 32px;
  font-weight: 600;
  color: #262626;
}

.subtitle {
  color: #8c8c8c;
  font-size: 14px;
  margin: 0;
}

.login-tabs {
  margin-bottom: 20px;
}

.login-form {
  margin-top: 20px;
}

/* 钉钉二维码容器样式 */
.qrcode-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0;
  min-height: auto;
  justify-content: center;
}

.qrcode-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  gap: 16px;
}

.qrcode-loading p {
  color: #666;
  font-size: 14px;
}

.qrcode-error {
  width: 100%;
}

.qrcode-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0;
  padding: 0;
}

#dingtalk-qrcode {
  margin: 0;
}

.qrcode-tip {
  text-align: center;
  color: #666;
  font-size: 14px;
  margin-top: 8px;
}

.ant-alert {
  border-radius: 8px;
}
</style>
