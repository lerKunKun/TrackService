# 用户认证系统使用指南

## 概述

已完成Track17物流追踪系统的用户认证系统改造，从硬编码验证升级为基于数据库的安全认证系统。

## 🔐 核心特性

### 1. 数据库持久化
- ✅ 用户信息存储在MySQL数据库
- ✅ 支持多用户管理
- ✅ 用户角色控制（ADMIN/USER）
- ✅ 用户状态管理（启用/禁用）

### 2. 安全性增强
- ✅ 密码使用BCrypt加密存储
- ✅ JWT Token认证
- ✅ 登录IP记录
- ✅ 最后登录时间追踪
- ✅ 账号状态验证

### 3. 用户管理功能
- ✅ 创建用户
- ✅ 更新用户信息
- ✅ 修改密码
- ✅ 启用/禁用用户
- ✅ 删除用户
- ✅ 用户列表查询

## 📦 新增文件清单

### 数据库相关
```
docs/
├── database.sql          # 完整数据库建表脚本（包含users表）
└── init_admin.sql        # 初始化管理员账号脚本
```

### 后端代码
```
src/main/java/com/logistics/track17/
├── entity/
│   └── User.java                        # 用户实体类
├── dto/
│   ├── UserDTO.java                     # 用户数据传输对象（无密码）
│   ├── CreateUserRequest.java           # 创建用户请求
│   ├── UpdateUserRequest.java           # 更新用户请求
│   └── ChangePasswordRequest.java       # 修改密码请求
├── mapper/
│   └── UserMapper.java                  # 用户Mapper接口
├── service/
│   └── UserService.java                 # 用户服务层
├── controller/
│   ├── UserController.java              # 用户管理控制器（新增）
│   └── AuthController.java              # 认证控制器（已修改）
└── util/
    └── PasswordGenerator.java           # 密码生成工具类

src/main/resources/mapper/
└── UserMapper.xml                       # MyBatis映射文件
```

### 依赖更新
```xml
<!-- pom.xml 新增 -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

## 🚀 部署步骤

### 1. 创建数据库表

首先，执行完整的数据库建表脚本：

```bash
# 方式1：使用MySQL命令行
mysql -u root -p < docs/database.sql

# 方式2：登录MySQL后执行
mysql -u root -p
source /path/to/docs/database.sql;
```

这将创建以下数据表：
- `users` - 用户表（新增）
- `shops` - 店铺表
- `orders` - 订单表
- `parcels` - 包裹表
- `tracking_numbers` - 运单表
- `tracking_events` - 物流事件表
- `carriers` - 承运商表
- `webhook_logs` - Webhook日志表
- `sync_jobs` - 同步任务表

### 2. 初始化管理员账号

执行初始化管理员账号脚本：

```bash
mysql -u root -p < docs/init_admin.sql
```

默认管理员账号信息：
- **用户名**: `admin`
- **密码**: `admin123`
- **邮箱**: `admin@track17.com`
- **角色**: `ADMIN`

⚠️ **重要提示**：首次登录后请立即修改默认密码！

### 3. 更新依赖

确保项目依赖已更新：

```bash
mvn clean install
```

### 4. 启动应用

```bash
mvn spring-boot:run
```

## 📡 API 接口文档

### 认证相关

#### 1. 用户登录
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "username": "admin",
    "expiresIn": 86400000
  },
  "timestamp": 1700000000000
}
```

#### 2. 验证Token
```http
GET /api/v1/auth/validate
Authorization: Bearer {token}
```

#### 3. 获取当前用户信息
```http
GET /api/v1/auth/current
Authorization: Bearer {token}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@track17.com",
    "realName": "系统管理员",
    "role": "ADMIN",
    "status": 1,
    "lastLoginTime": "2025-11-23T17:00:00",
    "lastLoginIp": "127.0.0.1",
    "createdAt": "2025-11-23T10:00:00"
  }
}
```

### 用户管理相关

#### 1. 获取用户列表（分页）
```http
GET /api/v1/users?page=1&size=10
Authorization: Bearer {token}
```

**响应示例：**
```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "username": "admin",
        "email": "admin@track17.com",
        "role": "ADMIN",
        "status": 1
      }
    ],
    "total": 1,
    "page": 1,
    "size": 10
  }
}
```

#### 2. 创建用户
```http
POST /api/v1/users
Authorization: Bearer {token}
Content-Type: application/json

{
  "username": "user001",
  "password": "password123",
  "email": "user001@example.com",
  "phone": "13800138000",
  "realName": "张三",
  "role": "USER"
}
```

**字段验证规则：**
- `username`: 必填，3-50个字符，只能包含字母、数字和下划线
- `password`: 必填，6-20个字符
- `email`: 可选，必须是有效的邮箱格式
- `phone`: 可选，必须是有效的手机号格式（中国）
- `role`: 可选，默认为`USER`，可选值：`ADMIN`, `USER`

#### 3. 更新用户信息
```http
PUT /api/v1/users/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "email": "newemail@example.com",
  "phone": "13900139000",
  "realName": "李四",
  "avatar": "https://example.com/avatar.jpg"
}
```

#### 4. 修改密码
```http
POST /api/v1/users/{id}/password
Authorization: Bearer {token}
Content-Type: application/json

{
  "oldPassword": "admin123",
  "newPassword": "newpassword123"
}
```

#### 5. 启用/禁用用户
```http
PUT /api/v1/users/{id}/status?status=0
Authorization: Bearer {token}
```

参数说明：
- `status=1`: 启用用户
- `status=0`: 禁用用户

#### 6. 删除用户
```http
DELETE /api/v1/users/{id}
Authorization: Bearer {token}
```

⚠️ **注意**：不允许删除管理员账号

## 🗄️ 数据库表结构

### users 表

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 用户ID（主键） |
| username | VARCHAR(50) | 用户名（唯一） |
| password | VARCHAR(255) | 密码（BCrypt加密） |
| email | VARCHAR(100) | 邮箱（唯一） |
| phone | VARCHAR(20) | 手机号 |
| real_name | VARCHAR(50) | 真实姓名 |
| role | VARCHAR(20) | 角色：ADMIN/USER |
| status | TINYINT | 状态：0-禁用，1-启用 |
| avatar | VARCHAR(255) | 头像URL |
| last_login_time | DATETIME | 最后登录时间 |
| last_login_ip | VARCHAR(50) | 最后登录IP |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## 🔒 安全特性说明

### 1. 密码加密
- 使用BCrypt算法加密密码
- 每次加密生成不同的哈希值（加盐）
- 不可逆，无法从哈希值反推原始密码
- 验证时使用`BCryptPasswordEncoder.matches()`方法

### 2. 登录验证流程
```
用户输入用户名和密码
    ↓
从数据库查询用户
    ↓
检查用户是否存在
    ↓
检查用户状态是否启用
    ↓
验证密码（BCrypt.matches）
    ↓
生成JWT Token
    ↓
更新最后登录时间和IP
    ↓
返回Token给前端
```

### 3. JWT Token
- 有效期：24小时（可配置）
- 每次请求自动携带Token
- Token过期后需重新登录
- Token包含用户名信息

### 4. IP记录
- 记录每次登录的真实IP地址
- 支持代理服务器IP识别
- 处理X-Forwarded-For等代理头

## 🛠️ 工具使用

### 生成BCrypt密码

如果需要手动生成BCrypt加密密码，可以运行工具类：

```bash
mvn exec:java -Dexec.mainClass="com.logistics.track17.util.PasswordGenerator"
```

输出示例：
```
原始密码: admin123
BCrypt加密后: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2B.0A6VhNcVzLv0GNTD8VQ2

插入SQL语句:
INSERT INTO `users` (`username`, `password`, `email`, `real_name`, `role`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2B.0A6VhNcVzLv0GNTD8VQ2', 'admin@track17.com', '系统管理员', 'ADMIN', 1);
```

## 🐛 常见问题

### Q1: 编译失败，提示找不到BCryptPasswordEncoder
**A**: 确保已添加Spring Security Crypto依赖并执行`mvn clean install`

### Q2: 登录失败，提示用户名或密码错误
**A**:
- 检查是否已执行`init_admin.sql`初始化管理员账号
- 确认数据库中users表有数据
- 检查密码是否正确（默认：admin123）

### Q3: 如何重置管理员密码？
**A**: 重新执行`docs/init_admin.sql`脚本，会删除旧账号并重新创建

### Q4: 为什么不能删除管理员账号？
**A**: 为了系统安全，UserService中禁止删除角色为ADMIN的账号

### Q5: 前端登录后如何保存Token？
**A**: 前端已实现，Token保存在localStorage中，并自动添加到请求头

## 📝 注意事项

1. **生产环境部署**：
   - 务必修改默认管理员密码
   - 修改JWT secret（application.yml）
   - 配置数据库密码
   - 启用HTTPS

2. **密码安全**：
   - 建议密码长度至少8位
   - 包含大小写字母、数字和特殊字符
   - 定期更换密码
   - 不要在多个系统使用相同密码

3. **用户管理**：
   - 及时禁用离职员工账号
   - 定期审查用户权限
   - 记录敏感操作日志

4. **数据备份**：
   - 定期备份users表数据
   - 保护用户隐私信息

## 🎯 后续优化建议

1. **功能增强**：
   - [ ] 忘记密码功能（邮箱找回）
   - [ ] 两步验证（2FA）
   - [ ] 登录验证码
   - [ ] 密码复杂度策略配置
   - [ ] 登录失败次数限制

2. **安全增强**：
   - [ ] 操作审计日志
   - [ ] 敏感操作二次验证
   - [ ] IP白名单
   - [ ] 会话管理

3. **用户体验**：
   - [ ] 记住登录状态
   - [ ] 单点登录（SSO）
   - [ ] OAuth2集成

## 📞 技术支持

如有问题，请查看：
- `docs/API_DOCUMENTATION.md` - API详细文档
- `README.md` - 项目总体说明

---

**最后更新**: 2025-11-23
**版本**: v1.1.0
**作者**: Jax
