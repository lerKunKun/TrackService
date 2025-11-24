# 快速启动指南

## 一、环境准备

### 1. 安装必要软件

确保已安装以下软件：

- **JDK 8 或更高版本**
  ```bash
  java -version
  ```

- **Maven 3.6+**
  ```bash
  mvn -version
  ```

- **MySQL 8.0+**
  ```bash
  mysql --version
  ```

- **Redis 6.0+** (可选，但推荐)
  ```bash
  redis-server --version
  ```

## 二、数据库配置

### 1. 创建数据库

登录 MySQL：
```bash
mysql -u root -p
```

执行建表脚本（建表语句在项目根目录或参考文档）：
```sql
CREATE DATABASE IF NOT EXISTS logistics_system DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 执行建表语句

运行提供的 SQL 脚本创建所有必要的表。

## 三、配置应用

### 1. 修改配置文件

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    username: root              # 修改为你的MySQL用户名
    password: your_password     # 修改为你的MySQL密码

track17:
  api:
    token: YOUR_17TRACK_TOKEN   # 替换为你的17Track API Token
```

### 2. 获取 17Track API Token

1. 访问 https://www.17track.net/
2. 注册并登录账号
3. 进入开发者中心获取 API Token
4. 将 Token 填入配置文件

## 四、启动应用

### 方式一：使用 Maven（推荐开发环境）

在项目根目录下执行：

```bash
mvn spring-boot:run
```

### 方式二：使用打包后的 JAR（推荐生产环境）

```bash
# 1. 打包
mvn clean package

# 2. 运行
java -jar target/track-17-server-1.0.0.jar
```

### 启动成功标志

看到以下日志表示启动成功：

```
Started Track17Application in X.XXX seconds
```

后端服务运行在：`http://localhost:8080/api/v1`

## 五、访问前端

### 1. 打开浏览器

直接用浏览器打开以下文件：

```
frontend/login.html
```

或者启动一个简单的 HTTP 服务器：

```bash
# 使用 Python
cd frontend
python -m http.server 8000

# 然后访问 http://localhost:8000/login.html
```

### 2. 登录系统

使用默认账号登录：
- **用户名**: `admin`
- **密码**: `admin123`

## 六、快速测试

### 1. 测试店铺管理

1. 登录后进入店铺管理页面
2. 点击"添加店铺"
3. 填写店铺信息：
   - 店铺名称：测试店铺
   - 平台类型：Shopify
   - API Key: test_key
   - API Secret: test_secret
   - Access Token: test_token
4. 保存并查看店铺列表

### 2. 测试运单管理

1. 进入运单管理页面
2. 点击"添加运单"
3. 填写运单信息：
   - 运单号：1Z999AA10123456784
   - 承运商：UPS
4. 保存后查看运单列表

### 3. 测试 API

使用 Postman 或 curl 测试 API：

```bash
# 登录获取 Token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 使用返回的 Token 访问其他接口
curl -X GET http://localhost:8080/api/v1/shops \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 七、常见问题

### 问题 1: 数据库连接失败

**错误信息**: `Communications link failure`

**解决方案**:
1. 检查 MySQL 服务是否启动
2. 确认数据库用户名和密码
3. 检查防火墙设置

### 问题 2: 端口被占用

**错误信息**: `Port 8080 was already in use`

**解决方案**:
1. 修改 `application.yml` 中的端口：
   ```yaml
   server:
     port: 8081
   ```
2. 或者停止占用 8080 端口的程序

### 问题 3: JWT Token 验证失败

**错误信息**: `401 Unauthorized`

**解决方案**:
1. 检查 Token 是否正确复制
2. 确认请求头格式：`Authorization: Bearer {token}`
3. Token 可能已过期，重新登录

### 问题 4: 前端无法连接后端

**解决方案**:
1. 确认后端服务已启动
2. 检查前端页面中的 API_BASE_URL 是否正确
3. 检查浏览器控制台的 CORS 错误

## 八、下一步

系统启动成功后，可以：

1. 📖 查阅 [API 文档](docs/API_DOCUMENTATION.md) 了解所有接口
2. 📋 查看 [用户故事](docs/USER_STORIES.md) 了解功能规划
3. 🔧 根据需要自定义配置
4. 🚀 开始使用系统管理物流

## 九、生产环境部署建议

如果要部署到生产环境，请注意：

1. **修改 JWT Secret**
   ```yaml
   jwt:
     secret: 使用强密码替换
   ```

2. **配置 HTTPS**
   ```yaml
   server:
     ssl:
       enabled: true
       key-store: classpath:keystore.p12
       key-store-password: your_password
   ```

3. **配置日志**
   ```yaml
   logging:
     level:
       com.logistics.track17: INFO
     file:
       name: logs/track17.log
   ```

4. **使用生产环境配置文件**
   ```bash
   java -jar track-17-server.jar --spring.profiles.active=prod
   ```

---

## 🎉 完成！

如果一切顺利，你现在应该能够使用 Track17 物流追踪系统了！

如遇到问题，请查看项目的 Issue 或提交新的 Issue。
