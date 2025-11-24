# 17Track轨迹功能完善总结

## 一、功能完成情况 ✅

### 1. 数据库优化
- ✅ tracking_numbers表新增11个字段支持17Track V2数据
- ✅ tracking_events表新增7个字段支持详细轨迹信息
- ✅ 所有字段支持NULL，保持向后兼容

### 2. 后端API集成
- ✅ Track17Service新增queryTrackingV2方法
- ✅ 支持17Track v2.4 API格式
- ✅ 完整解析track_info结构

### 3. 数据解析器
- ✅ Track17V2Response DTO完整映射JSON格式
- ✅ Track17V2Parser工具类解析所有字段
- ✅ TrackingService使用V2解析器更新数据

### 4. 测试验证
- ✅ 编译通过无错误
- ✅ 服务器正常启动
- ✅ 同步功能正常工作
- ✅ 成功获取30个详细轨迹事件

## 二、实际测试数据

### 测试运单：EA571232338CN
**同步后获取的数据：**

#### 运单主信息
```json
{
  "id": 3,
  "trackingNumber": "EA571232338CN",
  "carrierCode": "carrier-3011",
  "carrierId": 3011,
  "trackStatus": "Delivered",
  "subStatus": "Delivered_Other",
  "daysOfTransit": 12,
  "daysAfterLastUpdate": 0,
  "originCountry": "CN",
  "destinationCountry": "ID",
  "pickupTime": "2025-09-22T17:47:31",
  "deliveredTime": "2025-10-04T10:24:00",
  "latestEventTime": "2025-10-04T10:24:00",
  "latestEventDesc": "【ID】已妥投"
}
```

#### 物流轨迹（30个事件）
包含完整的物流链路：
1. 揽收 (PickedUp) - 武汉市 2025-09-22 17:47:31
2. 分拣处理 (InTransit) - 多个处理中心
3. 海关清关 (CustomsProcessing/Released)
4. 航空运输 (InTransit)
5. 到达目的地 (Arrival) - 印度尼西亚
6. 派送安排 (OutForDelivery)
7. 签收 (Delivered) - 2025-10-04 10:24:00

每个事件包含：
- ✅ 时间 (eventTime, timeIso)
- ✅ 描述 (description)
- ✅ 阶段 (stage)
- ✅ 子状态 (subStatus)
- ✅ 位置 (location, city, postalCode)
- ✅ 承运商 (providerName: China Post)

## 三、核心改动文件

### 后端（8个文件）

#### 新增文件
1. **Track17V2Response.java** - 17Track V2 API响应DTO
   - 支持track_info所有字段
   - 使用@JsonProperty映射下划线字段

2. **Track17V2Parser.java** - V2数据解析工具类
   - parseAndUpdateTracking() - 解析运单主信息
   - parseEvents() - 解析物流事件
   - normalizeStatus() - 统一状态格式

#### 修改文件
3. **Track17Service.java**
   - 新增queryTrackingV2()方法
   - 新增queryTrackingV2Batch()方法

4. **TrackingService.java**
   - 更新sync()方法使用V2 API
   - 使用Track17V2Parser解析数据
   - 导入Track17V2Parser

5. **TrackingNumber.java** (Entity)
   - 新增11个字段

6. **TrackingEvent.java** (Entity)
   - 新增7个字段
   - 兼容旧字段名的getter/setter

7. **TrackingNumberMapper.xml**
   - 更新resultMap
   - 更新insert/update语句

8. **TrackingEventMapper.xml**
   - 完全重写匹配新字段

### 响应DTO（2个文件）
9. **TrackingResponse.java** - 新增15个字段
10. **TrackingEventResponse.java** - 新增7个字段

## 四、17Track V2 数据映射

### 主状态映射
```
17Track返回              → 系统存储
===========================================
latest_status.status     → trackStatus
  - Delivered            → Delivered
  - InTransit            → InTransit
  - InfoReceived         → InfoReceived
  - Exception            → Exception

latest_status.sub_status → subStatus
  - Delivered_Other
  - InTransit_PickedUp
  - InTransit_CustomsProcessing
  - InTransit_CustomsReleased
  - OutForDelivery_Other
  等30+种子状态
```

### 时效数据映射
```
time_metrics.days_of_transit        → daysOfTransit
time_metrics.days_after_last_update → daysAfterLastUpdate
```

### 地址信息映射
```
shipping_info.shipper_address.country   → originCountry
shipping_info.recipient_address.country → destinationCountry
```

### 里程碑时间映射
```
milestone[PickedUp].time_iso   → pickupTime
milestone[Delivered].time_iso  → deliveredTime
```

### 物流事件映射
```
tracking.providers[].events[] → tracking_events表
  - time_iso              → timeIso, eventTime
  - description           → eventDescription
  - location              → eventLocation
  - stage                 → stage
  - sub_status            → subStatus
  - address.city          → city
  - address.postal_code   → postalCode
  - provider.key          → providerKey
  - provider.name         → providerName
```

## 五、API使用示例

### 同步运单
```bash
POST /api/v1/tracking/{id}/sync
Authorization: Bearer <token>
```

**响应示例：**
```json
{
  "code": 200,
  "message": "同步成功",
  "data": {
    "id": 3,
    "trackingNumber": "EA571232338CN",
    "trackStatus": "Delivered",
    "subStatus": "Delivered_Other",
    "daysOfTransit": 12,
    "originCountry": "CN",
    "destinationCountry": "ID",
    "pickupTime": "2025-09-22T17:47:31",
    "deliveredTime": "2025-10-04T10:24:00",
    "events": [...]
  }
}
```

### 查询运单详情
```bash
GET /api/v1/tracking/{id}
Authorization: Bearer <token>
```

## 六、前端显示建议

### 1. 列表页新增列
- 子状态标签 (subStatus)
- 运输天数 (daysOfTransit + "天")
- 最新事件 (latestEventDesc)
- 目的国 (destinationCountry)

### 2. 详情页优化
#### 运单信息卡片
```
- 运单号: EA571232338CN
- 状态: 已送达 (Delivered)
- 子状态: Delivered_Other
- 运输天数: 12天
- 始发地: CN → 目的地: ID
- 揽收时间: 2025-09-22 17:47:31
- 签收时间: 2025-10-04 10:24:00
```

#### 物流轨迹Timeline
按时间倒序显示，每个事件显示：
```
[图标] 2025-10-04 10:24:00
      【ID】已妥投
      阶段: Delivered
      承运商: China Post
```

不同stage使用不同颜色：
- PickedUp: 蓝色
- InTransit: 橙色
- Arrival: 紫色
- OutForDelivery: 绿色
- Delivered: 绿色（完成）
- Exception: 红色

包含城市和邮编的事件显示位置：
```
广州市 (51040034)
```

## 七、向后兼容性

1. **数据库字段**：所有新字段允许NULL
2. **Entity兼容**：TrackingEvent提供旧字段getter/setter
3. **DTO兼容**：保留status等旧字段
4. **API兼容**：旧的查询方法仍可用

## 八、性能优化

1. **批量插入**：物流事件支持批量插入（已实现）
2. **索引优化**：tracking_id已有索引
3. **日志控制**：V2响应超过500字符自动截断

## 九、已知限制

1. **API限制**：
   - 批量查询最多40个运单
   - 需要有效的17Track API token

2. **数据质量**：
   - 部分物流事件可能没有location/city信息
   - milestone时间可能为null（未发生的阶段）

## 十、后续优化建议

1. **定时同步**：添加定时任务自动同步运单
2. **Webhook**：接入17Track Webhook实时推送
3. **异常告警**：运输时间过长自动告警
4. **数据分析**：统计平均运输天数、各阶段耗时
5. **前端可视化**：地图显示物流轨迹

## 总结

✅ 完整支持17Track V2.4 API的track_info格式
✅ 数据库、后端、DTO全链路优化
✅ 实测同步成功，获取30个详细事件
✅ 保持向后兼容
✅ 代码编译通过，服务运行正常

系统现在可以完整展示物流包裹的全生命周期轨迹！
