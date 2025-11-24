# Shopify OAuth 店铺授权功能实现

## 概述

实现了Shopify店铺通过OAuth 2.0授权连接到物流追踪系统，自动获取访问令牌并管理店铺信息。

## 实现时间

**2025-11-23**

## 功能特性

### 核心功能

1. **OAuth授权流程**
   - 用户输入Shopify店铺域名
   - 重定向到Shopify授权页面
   - 用户授权后回调系统
   - 自动交换授权码获取访问令牌
   - 保存店铺信息到数据库

2. **安全验证**
   - CSRF防护：使用随机state参数
   - HMAC签名验证：验证Shopify回调的真实性
   - 域名格式验证：确保正确的Shopify域名格式

3. **自动管理**
   - 新店铺自动创建
   - 已有店铺更新令牌
   - 无需手动输入API密钥

## 数据库变更

### shops表新增字段

```sql
ALTER TABLE shops
ADD COLUMN shop_domain VARCHAR(255) COMMENT 'Shopify店铺域名 (xxx.myshopify.com)',
ADD COLUMN oauth_state VARCHAR(100) COMMENT 'OAuth state nonce (安全验证)',
ADD COLUMN oauth_scope VARCHAR(500) COMMENT 'OAuth授权的scope',
ADD COLUMN token_expires_at DATETIME COMMENT 'Token过期时间';
```

**字段说明**:
- `shop_domain`: Shopify店铺完整域名，如 `mystore.myshopify.com`
- `oauth_state`: OAuth流程中的state参数，用于防止CSRF攻击
- `oauth_scope`: 授权的权限范围，如 `read_orders,write_fulfillments`
- `token_expires_at`: Token过期时间（Shopify token默认不过期，可为NULL）

## 后端实现

### 1. 实体类更新

**Shop.java** - 新增OAuth相关字段:

```java
private String shopDomain;      // Shopify店铺域名 (xxx.myshopify.com)
private String oauthState;      // OAuth state nonce (安全验证)
private String oauthScope;      // OAuth授权的scope
private LocalDateTime tokenExpiresAt;  // Token过期时间
```

### 2. Mapper更新

**ShopMapper.java** - 新增方法:

```java
Shop selectByShopDomain(@Param("shopDomain") String shopDomain);
```

**ShopMapper.xml** - 更新SQL:
- 添加新字段到ResultMap
- 更新INSERT语句包含新字段
- 更新UPDATE语句支持新字段
- 添加selectByShopDomain查询

### 3. Service层

#### ShopifyOAuthService.java

OAuth核心服务，处理Shopify OAuth流程：

**主要方法**:

1. `generateAuthorizationUrl(shopDomain)` - 生成OAuth授权URL
   ```java
   String authUrl = String.format(
       "https://%s/admin/oauth/authorize?client_id=%s&scope=%s&redirect_uri=%s&state=%s",
       shopDomain, apiKey, scopes, redirectUri, state
   );
   ```

2. `exchangeCodeForToken(code, shopDomain)` - 授权码换取访问令牌
   ```java
   POST https://{shop}/admin/oauth/access_token
   {
     "client_id": "...",
     "client_secret": "...",
     "code": "..."
   }
   ```

3. `validateHmac(params)` - 验证HMAC签名
   ```java
   // 按key排序参数并计算HMAC-SHA256
   String queryString = params.entrySet().stream()
       .sorted(Map.Entry.comparingByKey())
       .map(e -> e.getKey() + "=" + e.getValue())
       .collect(Collectors.joining("&"));
   ```

4. `isValidShopDomain(shopDomain)` - 验证域名格式
   ```java
   return shopDomain.matches("^[a-zA-Z0-9-]+\\.myshopify\\.com$");
   ```

#### ShopService.java

新增方法支持OAuth流程：

```java
// 直接创建Shop对象（用于OAuth）
public Shop create(Shop shop)

// 根据域名查询店铺
public Shop getByShopDomain(String shopDomain)

// 直接更新Shop对象（用于OAuth）
public void update(Shop shop)
```

### 4. Controller层

#### ShopifyOAuthController.java

处理OAuth授权流程的HTTP请求：

**API端点**:

1. `GET /api/v1/oauth/shopify/authorize?shopDomain={domain}`
   - 生成授权URL
   - 重定向用户到Shopify授权页面

2. `GET /api/v1/oauth/shopify/callback`
   - 接收Shopify回调
   - 验证state和HMAC
   - 交换访问令牌
   - 保存店铺信息
   - 重定向回前端

**OAuth流程**:

```
用户输入域名
    ↓
/authorize
    ↓
重定向到Shopify授权页面
    ↓
用户点击授权
    ↓
Shopify回调 /callback
    ↓
验证state和HMAC
    ↓
交换访问令牌
    ↓
保存店铺信息
    ↓
重定向到前端（带success/error参数）
```

## 前端实现

### Shops.vue更新

#### 1. 新增OAuth授权按钮

```vue
<a-button type="primary" ghost @click="showOAuthModal">
  <ShopOutlined />
  Shopify OAuth授权
</a-button>
```

#### 2. OAuth授权弹窗

```vue
<a-modal
  v-model:open="oauthModalVisible"
  title="Shopify OAuth授权"
  @ok="handleOAuthSubmit"
>
  <a-input
    v-model:value="shopifyDomain"
    placeholder="例如: mystore.myshopify.com"
  />
</a-modal>
```

#### 3. OAuth授权处理逻辑

```javascript
const handleOAuthSubmit = () => {
  // 验证域名格式
  if (!shopifyDomain.value.endsWith('.myshopify.com')) {
    message.warning('请输入正确的Shopify域名格式')
    return
  }

  // 跳转到授权页面
  const authUrl = `http://localhost:8080/api/v1/oauth/shopify/authorize?shopDomain=${encodeURIComponent(shopifyDomain.value)}`
  window.location.href = authUrl
}
```

#### 4. 回调结果处理

```javascript
onMounted(() => {
  // 检查URL参数
  const urlParams = new URLSearchParams(window.location.search)
  if (urlParams.get('oauth') === 'success') {
    message.success('Shopify店铺授权成功！')
    window.history.replaceState({}, document.title, window.location.pathname)
    fetchShops()
  } else if (urlParams.get('oauth') === 'error') {
    message.error('Shopify授权失败，请重试')
  }
})
```

## 配置文件

### application.yml

```yaml
# Shopify OAuth Configuration
shopify:
  api:
    key: your-shopify-api-key
    secret: your-shopify-api-secret
  oauth:
    redirect-uri: http://localhost:8080/api/v1/oauth/shopify/callback
    frontend-redirect: http://localhost:5173/shops
```

**配置说明**:
- `shopify.api.key`: Shopify应用的API Key
- `shopify.api.secret`: Shopify应用的API Secret
- `shopify.oauth.redirect-uri`: OAuth回调地址（后端）
- `shopify.oauth.frontend-redirect`: 前端页面地址（回调后重定向）

## OAuth权限范围

默认申请的Shopify API权限：

```
read_orders          - 读取订单信息
read_fulfillments    - 读取物流信息
write_fulfillments   - 更新物流信息
```

可根据业务需求在 `ShopifyOAuthService.generateAuthorizationUrl()` 中调整。

## 安全机制

### 1. CSRF防护

```java
// 生成随机state
private String generateState() {
    SecureRandom random = new SecureRandom();
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return bytesToHex(bytes);
}

// 验证state
String storedShop = stateStore.get(state);
if (storedShop == null || !storedShop.equals(shop)) {
    throw SecurityException("Invalid state");
}
```

### 2. HMAC签名验证

```java
public boolean validateHmac(Map<String, String> params) {
    String hmac = params.get("hmac");

    // 移除hmac并按key排序
    Map<String, String> paramsToSign = new HashMap<>(params);
    paramsToSign.remove("hmac");

    // 计算HMAC-SHA256
    String queryString = paramsToSign.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining("&"));

    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKeySpec = new SecretKeySpec(
        shopifyApiSecret.getBytes(StandardCharsets.UTF_8),
        "HmacSHA256"
    );
    mac.init(secretKeySpec);

    byte[] hash = mac.doFinal(queryString.getBytes(StandardCharsets.UTF_8));
    String calculatedHmac = bytesToHex(hash);

    return calculatedHmac.equalsIgnoreCase(hmac);
}
```

### 3. 域名格式验证

```java
public boolean isValidShopDomain(String shopDomain) {
    if (shopDomain == null || shopDomain.isEmpty()) {
        return false;
    }
    return shopDomain.matches("^[a-zA-Z0-9-]+\\.myshopify\\.com$");
}
```

## 使用流程

### 1. 配置Shopify应用

1. 登录Shopify Partners账号
2. 创建新应用或使用现有应用
3. 配置OAuth回调URL: `http://localhost:8080/api/v1/oauth/shopify/callback`
4. 获取API Key和API Secret
5. 更新 `application.yml` 配置

### 2. 用户授权流程

1. 打开系统店铺列表页面
2. 点击 "Shopify OAuth授权" 按钮
3. 输入Shopify店铺域名（如 `mystore.myshopify.com`）
4. 点击确定，跳转到Shopify授权页面
5. 在Shopify页面点击"安装应用"授权
6. 授权成功后自动跳回系统店铺列表
7. 系统显示授权成功消息

### 3. 查看授权结果

- 新店铺会自动添加到店铺列表
- 已有店铺会更新访问令牌
- access_token自动保存到数据库
- 可在店铺列表中看到新增的Shopify店铺

## 错误处理

### 常见错误及处理

| 错误场景 | 错误码 | 处理方式 |
|---------|--------|----------|
| 无效的店铺域名格式 | - | 前端验证拦截 |
| State验证失败 | invalid_state | 重定向到前端显示错误 |
| HMAC验证失败 | hmac_failed | 重定向到前端显示错误 |
| 令牌交换失败 | - | 记录日志，重定向显示错误 |
| 数据库保存失败 | - | 事务回滚，重定向显示错误 |

### 前端错误显示

```javascript
const urlParams = new URLSearchParams(window.location.search)
const oauth = urlParams.get('oauth')
const reason = urlParams.get('reason')

if (oauth === 'error') {
  const errorMessages = {
    'invalid_state': 'OAuth验证失败，请重试',
    'hmac_failed': 'HMAC签名验证失败',
    'default': 'Shopify授权失败，请重试'
  }
  message.error(errorMessages[reason] || errorMessages.default)
}
```

## 生产环境注意事项

### 1. State存储

当前使用内存Map存储state，生产环境应改用Redis：

```java
// 推荐使用Redis
@Autowired
private RedisTemplate<String, String> redisTemplate;

// 保存state（5分钟过期）
redisTemplate.opsForValue().set(
    "oauth:state:" + state,
    shopDomain,
    5,
    TimeUnit.MINUTES
);

// 验证state
String storedShop = redisTemplate.opsForValue().get("oauth:state:" + state);
```

### 2. HTTPS配置

生产环境必须使用HTTPS：

```yaml
shopify:
  oauth:
    redirect-uri: https://yourdomain.com/api/v1/oauth/shopify/callback
    frontend-redirect: https://yourdomain.com/shops
```

### 3. 错误日志监控

建议添加错误监控和告警：

```java
@Slf4j
public class ShopifyOAuthController {

    @GetMapping("/callback")
    public void callback(...) {
        try {
            // OAuth处理逻辑
        } catch (Exception e) {
            log.error("OAuth callback failed for shop: {}", shop, e);
            // 发送告警通知
            alertService.sendAlert("Shopify OAuth失败", e);
            response.sendRedirect(frontendRedirect + "?oauth=error");
        }
    }
}
```

## 测试建议

### 1. 单元测试

```java
@Test
public void testValidateHmac() {
    Map<String, String> params = new HashMap<>();
    params.put("code", "test-code");
    params.put("shop", "test.myshopify.com");
    params.put("state", "test-state");
    params.put("hmac", "calculated-hmac");

    boolean valid = shopifyOAuthService.validateHmac(params);
    assertTrue(valid);
}
```

### 2. 集成测试

使用Shopify开发店铺测试完整OAuth流程。

### 3. 安全测试

- 测试state重放攻击防护
- 测试HMAC篡改检测
- 测试域名格式验证

## 参考文档

- [Shopify OAuth文档](https://shopify.dev/docs/apps/build/authentication-authorization/access-tokens/authorization-code-grant)
- [Shopify API权限范围](https://shopify.dev/docs/api/usage/access-scopes)
- [HMAC验证](https://shopify.dev/docs/apps/build/authentication-authorization/oauth/security)

## 文件清单

### 后端文件

```
src/main/java/com/logistics/track17/
├── entity/
│   └── Shop.java                        # 新增OAuth字段
├── service/
│   ├── ShopService.java                 # 新增OAuth相关方法
│   └── ShopifyOAuthService.java         # 新增
├── controller/
│   └── ShopifyOAuthController.java      # 新增
└── mapper/
    ├── ShopMapper.java                  # 新增selectByShopDomain
    └── ShopMapper.xml                   # 更新SQL映射

src/main/resources/
└── application.yml                      # 新增Shopify配置
```

### 前端文件

```
frontend-vue/src/views/
└── Shops.vue                            # 新增OAuth授权UI
```

### 文档

```
docs/
└── SHOPIFY_OAUTH_IMPLEMENTATION.md      # 本文档
```

## 总结

### 已完成功能

- ✅ 数据库shops表扩展OAuth字段
- ✅ Shop实体类添加OAuth相关属性
- ✅ ShopifyOAuthService实现OAuth核心逻辑
- ✅ ShopifyOAuthController处理OAuth HTTP请求
- ✅ 前端Shops页面支持OAuth授权
- ✅ CSRF和HMAC安全验证
- ✅ 编译测试通过

### 技术特点

- **安全**: State防CSRF + HMAC防篡改
- **自动化**: 无需手动输入API密钥
- **用户友好**: 一键授权，自动保存
- **符合规范**: 遵循Shopify OAuth 2.0标准

### 下一步优化

1. 将state存储从内存改为Redis（生产环境必须）
2. 添加Token刷新机制（如果使用短期token）
3. 添加店铺授权状态检查接口
4. 支持撤销授权功能
5. 添加OAuth流程监控和统计

---

**实施时间**: 2025-11-23
**状态**: ✅ 已完成并编译通过
**版本**: v1.0（OAuth基础实现）
