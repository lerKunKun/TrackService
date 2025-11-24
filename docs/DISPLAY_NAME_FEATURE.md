# 用户显示名称功能

## 功能说明
登录后，右上角显示用户的**真实姓名**而不是用户名。

## 实现方式

### 后端修改

#### 1. LoginResponse.java
添加 `realName` 字段：
```java
public class LoginResponse {
    private String token;
    private String username;
    private String realName;   // 真实姓名（用于显示）
    private Long expiresIn;
}
```

#### 2. AuthController.java
登录时返回真实姓名：
```java
LoginResponse response = new LoginResponse(
    token,
    user.getUsername(),
    user.getRealName(),  // 添加真实姓名
    expiration
);
```

### 前端修改

#### 1. stores/user.js
添加 `displayName` 字段存储显示名称：
```javascript
const displayName = ref(localStorage.getItem('displayName') || '')

const login = async (credentials) => {
  const data = await authApi.login(credentials)
  // 优先使用真实姓名，如果没有则使用用户名
  displayName.value = data.realName || data.username
  localStorage.setItem('displayName', data.realName || data.username)
}
```

#### 2. views/Layout.vue
使用 `displayName` 显示：
```vue
<span>{{ userStore.displayName }}</span>
```

## 效果展示

**优化前**:
```
用户信息: admin  退出登录
```

**优化后**:
```
用户信息: 系统管理员  退出登录
```

## 降级策略
如果用户没有设置真实姓名（realName为空），系统会自动降级显示用户名：
```javascript
displayName.value = data.realName || data.username
```

## 测试验证

### API测试
```bash
# 登录接口
curl -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 响应示例
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGc...",
    "username": "admin",
    "realName": "系统管理员",
    "expiresIn": 86400000
  }
}
```

### 前端测试
1. 登录系统
2. 查看右上角用户信息
3. 应显示"系统管理员"而不是"admin"

## 数据库
用户表已有 `real_name` 字段，无需修改数据库。

## 修改文件清单

### 后端
- `src/main/java/com/logistics/track17/dto/LoginResponse.java`
- `src/main/java/com/logistics/track17/controller/AuthController.java`

### 前端
- `frontend-vue/src/stores/user.js`
- `frontend-vue/src/views/Layout.vue`

---

**实施时间**: 2025-11-23 22:09
**状态**: ✅ 已完成并测试通过
