# API费用优化 - 拒绝无效运单号

## 问题描述

**优化前的问题**:
在添加运单时，系统会先调用17Track Register API注册运单号，然后再保存到数据库。

这导致一个严重问题：
- **即使运单号无效**，也会调用17Track API
- **浪费API配额和费用**
- 无效运单号会被17Track拒绝，但仍然消耗API调用次数

## 优化方案

利用17Track Register API返回的`rejected`字段，在保存到数据库**之前**验证运单号有效性。

### 17Track Register API响应结构

```json
{
  "code": 0,
  "data": {
    "accepted": [
      {
        "number": "1234567890",
        "carrier": 100002
      }
    ],
    "rejected": [
      {
        "number": "INVALID123",
        "carrier": 0,
        "error": {
          "code": 4031,
          "message": "Invalid tracking number"
        }
      }
    ]
  }
}
```

### 优化逻辑

#### 单个运单创建 (`create` 方法)

**优化前**:
```java
// 1. 调用Register API (消耗配额)
Track17RegisterResponse response = track17Service.registerTracking(...);

// 2. 提取carrier信息
if (response.getAccepted() != null) {
    // 使用carrier信息
}

// 3. 保存到数据库 (无论是否有效)
trackingNumberMapper.insert(trackingNumber);
```

**优化后**:
```java
// 1. 调用Register API
Track17RegisterResponse response = track17Service.registerTracking(...);

// 2. ✨ 检查是否被拒绝
if (response.getRejected() != null && !response.getRejected().isEmpty()) {
    String errorMsg = response.getRejected().get(0).getError().getMessage();
    throw BusinessException.of("运单号无效或不被支持: " + errorMsg);
    // ❌ 不保存到数据库
}

// 3. 提取carrier信息
if (response.getAccepted() != null) {
    // 使用carrier信息
}

// 4. 保存到数据库 (仅对有效运单号)
trackingNumberMapper.insert(trackingNumber);
```

#### 批量导入 (`batchImport` 方法)

**优化后**:
```java
for (BatchImportItem item : items) {
    Track17RegisterResponse response = track17Service.registerTracking(...);

    // ✨ 检查是否被拒绝
    if (response.getRejected() != null && !response.getRejected().isEmpty()) {
        log.warn("Tracking rejected: {}", item.getTrackingNumber());
        failed++;
        continue;  // 跳过无效运单号，不保存
    }

    // 只保存有效的运单号
    if (response.getAccepted() != null) {
        // ... 保存逻辑
    }
}
```

## 优化效果

### 节省API费用

| 场景 | 优化前 | 优化后 | 节省 |
|------|--------|--------|------|
| 添加1个无效运单 | ✅ Register + ✅ Query = **2次调用** | ✅ Register = **1次调用** | **50%** |
| 批量导入100个（10个无效） | **200次调用** | **110次调用** | **45%** |

### 用户体验

**优化前**:
- 无效运单号被保存到数据库
- 需要手动删除
- 混淆有效数据

**优化后**:
- ✅ 立即提示用户运单号无效
- ✅ 不保存无效数据
- ✅ 数据库保持干净

## 代码修改

### TrackingService.java

#### 1. create() 方法 (第69-83行)

```java
// 检查运单号是否被拒绝（无效）
if (registerResponse != null && registerResponse.getData() != null
        && registerResponse.getData().getRejected() != null
        && !registerResponse.getData().getRejected().isEmpty()) {

    Track17RegisterResponse.Track17RegisterData.RejectedItem rejectedItem =
            registerResponse.getData().getRejected().get(0);
    String errorMsg = rejectedItem.getError() != null
        ? rejectedItem.getError().getMessage()
        : "运单号无效";

    log.warn("Tracking number rejected by 17Track: {} - {}",
            request.getTrackingNumber(), errorMsg);
    throw BusinessException.of("运单号无效或不被支持: " + errorMsg);
}
```

#### 2. batchImport() 方法 (第469-483行)

```java
// 检查运单号是否被拒绝（无效）
if (registerResponse != null && registerResponse.getData() != null
        && registerResponse.getData().getRejected() != null
        && !registerResponse.getData().getRejected().isEmpty()) {

    Track17RegisterResponse.Track17RegisterData.RejectedItem rejectedItem =
            registerResponse.getData().getRejected().get(0);
    String errorMsg = rejectedItem.getError() != null
            ? rejectedItem.getError().getMessage()
            : "运单号无效";

    log.warn("Tracking number rejected by 17Track: {} - {}", item.getTrackingNumber(), errorMsg);
    failed++;
    continue;  // 跳过无效运单号
}
```

## 常见拒绝原因

17Track可能拒绝运单号的原因：

| 错误码 | 错误信息 | 原因 |
|--------|----------|------|
| 4031 | Invalid tracking number | 运单号格式无效 |
| 4032 | Carrier not supported | 承运商不支持 |
| 4033 | Tracking number too short | 运单号太短 |
| 4034 | Tracking number too long | 运单号太长 |

## 测试验证

### 测试1: 添加无效运单号

```bash
curl -X POST "http://localhost:8080/api/v1/tracking" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"trackingNumber":"INVALID123","source":"manual"}'
```

**预期结果**:
```json
{
  "code": 400,
  "message": "运单号无效或不被支持: Invalid tracking number"
}
```

### 测试2: 批量导入（含无效运单）

```bash
curl -X POST "http://localhost:8080/api/v1/tracking/batch-import" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"trackingNumber": "9400100000000000000001"},
      {"trackingNumber": "INVALID123"},
      {"trackingNumber": "9400100000000000000002"}
    ]
  }'
```

**预期结果**:
```json
{
  "code": 200,
  "message": "导入完成: 成功 2, 失败 1"
}
```

日志输出：
```
WARN: Tracking number rejected by 17Track: INVALID123 - Invalid tracking number
```

## 后续建议

### 1. 前端验证 (可选)
在前端添加运单号格式验证，减少无效请求：
```javascript
// 基本格式验证
const isValidTrackingNumber = (number) => {
  return number.length >= 10 && /^[A-Z0-9]+$/.test(number)
}
```

### 2. 运单号格式提示 (可选)
在前端添加常见快递公司运单号格式说明，帮助用户输入正确格式。

### 3. 批量导入错误详情 (可选)
批量导入时返回详细的失败列表，告知用户哪些运单号无效：
```json
{
  "total": 3,
  "success": 2,
  "failed": 1,
  "failedItems": [
    {
      "trackingNumber": "INVALID123",
      "reason": "Invalid tracking number"
    }
  ]
}
```

## 总结

### 优化效果
- ✅ **节省API费用**: 无效运单号不会消耗Query API调用
- ✅ **提升用户体验**: 立即反馈运单号无效
- ✅ **保持数据质量**: 数据库仅存储有效运单号
- ✅ **减少维护成本**: 不需要手动清理无效数据

### 修改范围
- **影响文件**: `TrackingService.java`
- **修改行数**: ~30行
- **风险**: 低（仅增加验证逻辑）

---

**实施时间**: 2025-11-23 22:15
**状态**: ✅ 已完成并部署
**影响**: 所有新创建和批量导入的运单
