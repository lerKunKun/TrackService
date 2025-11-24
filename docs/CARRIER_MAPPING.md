# 17Track 承运商映射表

本文档记录了系统支持的承运商及其与17Track API的映射关系。

## 数据来源

- **17Track官方承运商列表**: https://res.17track.net/asset/carrier/info/apicarrier.all.json
- **17Track支持的承运商总数**: 3000+ 个
- **17Track官方文档**: https://api.17track.net/en/doc
- **承运商列表帮助文档**: https://help.17track.net/hc/en-us/articles/37467353177753-Supported-carriers-and-carrier-codes

## 系统支持的主要承运商

### 国际快递

| 承运商 | 17Track ID | 系统代码 | 备注 |
|--------|-----------|---------|------|
| **UPS** | 100002 | `ups` | 主要UPS服务 |
| UPS Mail Innovations | 100398 | `ups-mail-innovations` | UPS邮件创新 |
| TForce Freight (UPS Freight) | 100399 | `ups-freight` | UPS货运 |
| **FedEx** | 100003 | `fedex` | 主要FedEx服务 |
| FedEx International Connect | 100222 | `fedex-intl` | FedEx国际快递 |
| **USPS** | 21051 | `usps` | 美国邮政 |
| **DHL Express** | 100001 | `dhl` | 主要DHL服务（推荐） |
| DHL Paket | 7041 | `dhl-paket` | DHL包裹 |
| DHL eCommerce US | 7047 | `dhl-ecommerce-us` | DHL美国电商 |
| DHL eCommerce Asia | 7048 | `dhl-ecommerce-asia` | DHL亚洲电商 |
| DHL eCommerce CN | 100765 | `dhl-ecommerce-cn` | DHL中国电商 |

### 跨境电商物流

| 承运商 | 17Track ID | 系统代码 | 备注 |
|--------|-----------|---------|------|
| **4PX** | 190094 | `4px` | 递四方 |
| **Yanwen** | 190012 | `yanwen` | 燕文物流 |

### 中国邮政

| 承运商 | 17Track ID | 系统代码 | 备注 |
|--------|-----------|---------|------|
| **China Post** | 3011 | `china-post` | 中国邮政 |
| **China EMS** | 3013 | `china-ems` | 中国EMS |

### 中国快递

| 承运商 | 17Track ID | 系统代码 | 备注 |
|--------|-----------|---------|------|
| **SF Express** | 100012 | `sf-express` 或 `sf` | 顺丰速运 |
| **Cainiao** | 190271 | `cainiao` | 菜鸟物流 |
| **ZTO International** | 190175 | `zto` | 中通国际 |

## 使用方法

### 1. 前端添加运单

用户可以选择以下方式：

```javascript
// 方式1: 自动识别（推荐）
{
  "trackingNumber": "1Z999AA10123456784",
  "carrierCode": ""  // 留空，系统自动识别
}

// 方式2: 手动指定
{
  "trackingNumber": "1Z999AA10123456784",
  "carrierCode": "ups"  // 明确指定为UPS
}
```

### 2. 批量导入CSV

CSV格式示例：

```csv
运单号,承运商代码,备注
1Z999AA10123456784,ups,UPS包裹
123456789012,,自动识别承运商
9400100000000000000000,usps,美国邮政
SF1234567890,sf-express,顺丰包裹
```

### 3. API调用

```bash
# 自动识别承运商
curl -X POST http://localhost:8080/api/v1/tracking \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "trackingNumber": "1Z999AA10123456784",
    "carrierCode": "",
    "source": "manual"
  }'

# 指定承运商
curl -X POST http://localhost:8080/api/v1/tracking \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "trackingNumber": "SF1234567890",
    "carrierCode": "sf-express",
    "source": "manual"
  }'
```

## 承运商代码映射逻辑

### 前端到17Track（注册）

系统代码 → 17Track Carrier ID

实现位置：`Track17RegisterRequest.java` 中的 `convertCarrierCode()` 方法

```java
// 示例
"ups" → 100002
"fedex" → 100003
"usps" → 21051
"dhl" → 100001
"" → 0 (自动识别)
```

### 17Track到系统（响应）

17Track Carrier ID → 系统代码

实现位置：`TrackingService.java` 中的 `convertCarrierIdToCode()` 方法

```java
// 示例
100002 → "ups"
100003 → "fedex"
21051 → "usps"
100001 → "dhl"
未知ID → "carrier-{ID}"
```

## 自动识别流程

当用户添加运单时留空承运商代码：

1. **系统发送注册请求**到17Track，carrier=0
2. **17Track自动识别**运单号的承运商
3. **17Track返回**识别的carrier ID（如 100002）
4. **系统转换** carrier ID为系统代码（如 "ups"）
5. **系统保存**运单时携带识别的承运商信息

## 添加新承运商

如需支持新的承运商，需要在以下位置添加映射：

### 1. 后端映射

**文件**: `src/main/java/com/logistics/track17/service/TrackingService.java`

```java
private String convertCarrierIdToCode(Integer carrierId) {
    switch (carrierId) {
        // 添加新承运商
        case YOUR_CARRIER_ID: return "your-carrier-code";
        // ...
    }
}
```

**文件**: `src/main/java/com/logistics/track17/dto/Track17RegisterRequest.java`

```java
private Integer convertCarrierCode(String carrierCode) {
    switch (carrierCode.toLowerCase()) {
        // 添加新承运商
        case "your-carrier-code": return YOUR_CARRIER_ID;
        // ...
    }
}
```

### 2. 前端选项

**文件**: `frontend-vue/src/views/Tracking.vue`

```vue
<!-- 添加运单表单 -->
<a-select-option value="your-carrier-code">Your Carrier Name</a-select-option>

<!-- 搜索筛选 -->
<a-select-option value="your-carrier-code">Your Carrier</a-select-option>
```

## 查找承运商ID

### 方法1: 查询JSON文件

```bash
curl -s https://res.17track.net/asset/carrier/info/apicarrier.all.json | \
  jq '.[] | select(._name | test("DHL"; "i")) | {id: .key, name: ._name}'
```

### 方法2: 17Track网站

访问 https://www.17track.net/en/carriers 搜索承运商

### 方法3: 测试API

使用测试接口注册运单，17Track会返回识别的carrier ID：

```bash
curl -X GET "http://localhost:8080/api/v1/test/17track/register?number=YOUR_TRACKING&carrier="
```

## 注意事项

1. **承运商ID会变化**: 17Track可能更新承运商ID，建议定期同步官方列表
2. **自动识别优先**: 对于不确定的承运商，推荐使用自动识别（carrier=0）
3. **未知承运商处理**: 系统会将未知carrier ID转换为 `carrier-{ID}` 格式
4. **同一承运商多个ID**: 某些大型承运商（如DHL、UPS）有多个服务分支，每个分支有独立ID

## 更新日志

- **2025-11-23**: 更新为17Track官方最新承运商列表（3000+承运商）
- **2025-11-23**: 修正UPS ID (1001 → 100002)
- **2025-11-23**: 修正FedEx ID (2003 → 100003)
- **2025-11-23**: 修正USPS ID (2071 → 21051)
- **2025-11-23**: 修正DHL ID (2002 → 100001)
- **2025-11-23**: 修正4PX ID (10001 → 190094)
- **2025-11-23**: 添加中国快递支持（顺丰、菜鸟、中通）
