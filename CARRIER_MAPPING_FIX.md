# 承运商代码映射修复总结

## 问题描述

用户反馈：运单显示 `carrier-3011` 而不是 `中国邮政 (china-post)`

原因：
1. `convertCarrierIdToCode()` 方法在数据库没有映射时，返回 `carrier-{ID}` 作为fallback
2. 创建运单时如果没有正确查询到carrier信息，会保存fallback值
3. 已存在的数据库记录中有 `carrier-3011` 的错误值

## 修复方案

### 1. 数据库修复

修复已存在的错误数据：
```sql
UPDATE tracking_numbers
SET carrier_code = 'china-post'
WHERE carrier_code = 'carrier-3011';
```

验证映射表：
```sql
SELECT carrier_id, carrier_code, carrier_name, carrier_name_cn
FROM carriers
WHERE carrier_id = 3011;
```

结果：
```
carrier_id: 3011
carrier_code: china-post
carrier_name: China Post
carrier_name_cn: 中国邮政
```

### 2. 后端代码修复

#### TrackingService.java

在 `sync()` 方法中添加carrier_code自动更新逻辑：

```java
// 同步时更新carrier_code为正确的映射（如果carrier_id有值）
if (item.getCarrier() != null) {
    String carrierCode = convertCarrierIdToCode(item.getCarrier());
    trackingNumber.setCarrierCode(carrierCode);
    log.info("Updated carrier_code to: {} (from carrier_id: {})", carrierCode, item.getCarrier());
}
```

位置：src/main/java/com/logistics/track17/service/TrackingService.java:257-261

#### TrackingNumberMapper.xml

在 `update` 语句中添加 carrier_code 更新支持：

```xml
<if test="carrierCode != null">carrier_code = #{carrierCode},</if>
```

位置：src/main/resources/mapper/TrackingNumberMapper.xml:128

### 3. 测试验证

#### 测试1：查询运单详情
```bash
GET /api/v1/tracking/3
```

**响应：**
```json
{
  "id": 3,
  "trackingNumber": "EA571232338CN",
  "carrierCode": "china-post",
  "carrierId": 3011,
  "trackStatus": "Delivered",
  "events": [
    {
      "description": "【ID】已妥投",
      "providerName": "China Post"
    }
  ]
}
```

✅ carrierCode显示为 `china-post`
✅ carrierId保存为 `3011`
✅ providerName显示为 `China Post`

#### 测试2：同步运单
```bash
POST /api/v1/tracking/3/sync
```

**响应：**
```json
{
  "code": 200,
  "message": "同步成功",
  "data": {
    "carrierCode": "china-post",
    "carrierId": 3011,
    "lastSyncAt": "2025-11-23T19:47:25"
  }
}
```

✅ 同步后carrier_code保持正确
✅ 自动从17Track API获取carrier_id并更新映射

#### 测试3：数据库验证
```bash
mysql> SELECT id, tracking_number, carrier_code, carrier_id FROM tracking_numbers WHERE id = 3;
```

**结果：**
```
id: 3
tracking_number: EA571232338CN
carrier_code: china-post
carrier_id: 3011
```

✅ 数据库中carrier_code正确保存

## 承运商映射机制

### convertCarrierIdToCode() 工作流程

1. 接收17Track返回的 `carrier_id` (如 3011)
2. 查询数据库 `carriers` 表获取映射
3. 如果找到映射，返回 `carrier_code` (如 `china-post`)
4. 如果未找到，返回fallback: `carrier-{ID}` (如 `carrier-3011`)

### 数据来源

承运商映射表基于17Track官方数据：
- 文件：`/docs/apicarrier.all.json`
- 包含3000+承运商映射
- 格式：`{_id: 3011, _key: "3011", _name: "中国邮政", _name_en: "China Post"}`

### 映射示例

| Carrier ID | Carrier Code | Name (EN) | Name (CN) |
|------------|--------------|-----------|-----------|
| 3011 | china-post | China Post | 中国邮政 |
| 100002 | ups | UPS | UPS |
| 100003 | fedex | FedEx | FedEx |
| 21051 | usps | USPS | USPS |
| 100001 | dhl | DHL Express | DHL国际快递 |
| 190094 | 4px | 4PX | 递四方 |

## 修复效果

### 修复前
- 显示：`carrier-3011`
- 用户体验差，不直观

### 修复后
- 显示：`china-post`
- API响应包含完整信息：
  - `carrierCode`: "china-post"
  - `carrierId`: 3011
  - `providerName`: "China Post"

### 自动修复机制
- 每次同步运单时，自动检查并更新carrier_code
- 新创建的运单会自动使用正确的映射
- 历史数据通过手动SQL修复

## 前端显示建议

### 承运商名称显示优先级

1. **优先显示中文名称**（如果有）
   - 从 `carriers` 表查询 `carrier_name_cn`
   - 示例：`中国邮政`

2. **备选显示英文名称**
   - 从 `carriers` 表查询 `carrier_name`
   - 示例：`China Post`

3. **最后显示代码**
   - 使用 `carrier_code` 转换为可读格式
   - 示例：`china-post` → `China Post`

### 实现示例（Vue）

```javascript
// 获取承运商显示名称
function getCarrierDisplayName(carrierCode, carrierId) {
  // 1. 通过carrier_code或carrier_id查询carriers表
  const carrier = await getCarrierInfo(carrierCode, carrierId);

  // 2. 优先返回中文名称
  if (carrier && carrier.carrierNameCn) {
    return carrier.carrierNameCn;  // "中国邮政"
  }

  // 3. 备选返回英文名称
  if (carrier && carrier.carrierName) {
    return carrier.carrierName;  // "China Post"
  }

  // 4. 最后格式化代码
  return formatCarrierCode(carrierCode);  // "China Post"
}

// 格式化carrier code
function formatCarrierCode(code) {
  if (!code) return 'Unknown';
  // china-post → China Post
  return code.split('-')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}
```

## 总结

✅ 数据库历史数据已修复
✅ 后端代码支持自动更新carrier_code
✅ 同步功能正常工作
✅ API返回正确的承运商信息
✅ 承运商映射机制完善

系统现在可以正确显示承运商名称，不再出现 `carrier-3011` 这样的技术代码！
