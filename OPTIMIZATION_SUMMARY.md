# 17Track数据优化实施总结

## 一、数据库结构优化

### 1. tracking_numbers表新增字段
```sql
- carrier_id INT              -- 17Track承运商ID
- sub_status VARCHAR(100)     -- 子状态(如Delivered_Other, InTransit_PickedUp)
- sub_status_descr VARCHAR(255) -- 子状态描述
- days_of_transit INT         -- 运输天数
- days_after_last_update INT  -- 距离最后更新天数
- latest_event_time DATETIME  -- 最新事件时间
- latest_event_desc VARCHAR(500) -- 最新事件描述
- latest_event_location VARCHAR(255) -- 最新事件地点
- pickup_time DATETIME        -- 揽收时间
- delivered_time DATETIME     -- 签收时间
- origin_country VARCHAR(10)  -- 始发国家
```

### 2. tracking_events表新增字段
```sql
- stage VARCHAR(50)           -- 阶段(PickedUp/InTransit/Delivered等)
- sub_status VARCHAR(100)     -- 子状态
- time_iso VARCHAR(50)        -- ISO时间字符串
- city VARCHAR(100)           -- 城市
- postal_code VARCHAR(20)     -- 邮编
- provider_key INT            -- 承运商ID
- provider_name VARCHAR(100)  -- 承运商名称
```

## 二、后端改动

### 1. 新增文件

#### Track17V2Response.java
- 完整支持17Track V2 API的JSON格式
- 包含shipping_info, latest_status, latest_event, time_metrics, milestone, tracking等所有字段
- 使用@JsonProperty注解映射下划线命名的JSON字段

#### Track17V2Parser.java
- 解析17Track V2返回数据的工具类
- `parseAndUpdateTracking()` - 解析并更新运单主信息
- `parseEvents()` - 解析物流轨迹事件
- `normalizeStatus()` - 统一状态格式

### 2. 修改文件

#### Entity层
- **TrackingNumber.java**: 新增所有优化字段
- **TrackingEvent.java**: 新增字段，并提供兼容旧字段名的getter/setter

#### Mapper XML
- **TrackingNumberMapper.xml**: 更新resultMap、insert、update语句以支持新字段
- **TrackingEventMapper.xml**: 完全重写以匹配新的字段名

#### DTO层
- **TrackingResponse.java**: 添加所有新字段用于API返回
- **TrackingEventResponse.java**: 添加新字段并保持向后兼容

## 三、17Track V2 API数据映射关系

### 运单主信息映射
```
17Track字段                          → 数据库字段
====================================================
carrier                            → carrier_id
track_info.latest_status.status    → track_status
track_info.latest_status.sub_status→ sub_status
track_info.time_metrics.days_of_transit → days_of_transit
track_info.latest_event.time_iso   → latest_event_time
track_info.latest_event.description→ latest_event_desc
track_info.latest_event.location   → latest_event_location
track_info.shipping_info.shipper_address.country → origin_country
track_info.shipping_info.recipient_address.country → destination_country
track_info.milestone[PickedUp].time_iso → pickup_time
track_info.milestone[Delivered].time_iso → delivered_time
```

### 物流事件映射
```
17Track字段                          → 数据库字段
====================================================
tracking.providers[].events[].time_iso → time_iso
tracking.providers[].events[].description → event_description
tracking.providers[].events[].location → event_location
tracking.providers[].events[].stage → stage
tracking.providers[].events[].sub_status → sub_status
tracking.providers[].events[].address.city → city
tracking.providers[].events[].address.postal_code → postal_code
tracking.providers[].provider.key → provider_key
tracking.providers[].provider.name → provider_name
```

## 四、状态分类说明

### 主状态(track_status)
- **InfoReceived**: 信息已录入，待揽收
- **InTransit**: 运输中(包括已揽收、运输途中、到达待取等)
- **Delivered**: 已送达
- **Exception**: 异常(疑难件、退件等)

### 子状态(sub_status)示例
- Delivered_Other
- InTransit_PickedUp
- InTransit_CustomsProcessing
- InTransit_CustomsReleased
- InTransit_Arrival
- OutForDelivery_Other
- AvailableForPickup_Other

### 阶段(stage)
来自17Track的milestone，包括：
- InfoReceived
- PickedUp
- Departure
- Arrival
- AvailableForPickup
- OutForDelivery
- Delivered
- Returning
- Returned

## 五、前端显示优化建议

### 1. 列表页新增显示
- 子状态标签
- 运输天数
- 最新事件简述
- 揽收/签收时间

### 2. 详情页优化
- 显示完整的shipping_info(始发/目的地址)
- 显示time_metrics(运输天数统计)
- 显示milestone时间轴
- 物流事件显示承运商名称、城市、邮编
- 区分不同provider的事件(用不同颜色/图标)

### 3. 状态显示优化
- 主状态用tag颜色区分
- 子状态用文字说明
- 里程碑用timeline展示

## 六、使用方式

### 后端Service调用示例
```java
// 在TrackingService中使用
Track17V2Response response = track17Service.queryTrackingV2(trackingNumber);
if (response.getData().getAccepted() != null) {
    Track17V2Response.AcceptedItem item = response.getData().getAccepted().get(0);

    // 更新运单主信息
    Track17V2Parser.parseAndUpdateTracking(item, trackingNumber);
    trackingNumberMapper.update(trackingNumber);

    // 更新物流事件
    trackingEventMapper.deleteByTrackingId(trackingId);
    List<TrackingEvent> events = Track17V2Parser.parseEvents(item, trackingId);
    if (!events.isEmpty()) {
        trackingEventMapper.insertBatch(events);
    }
}
```

## 七、向后兼容性

1. **Entity兼容**:
   - TrackingEvent提供了status/location/description的getter/setter映射到新字段
   - 旧代码仍可使用event.getStatus()等方法

2. **DTO兼容**:
   - TrackingEventResponse保留了status字段
   - 前端旧代码仍可正常访问

3. **数据库兼容**:
   - 所有新字段都允许NULL
   - 不影响现有数据

## 八、测试清单

- [ ] 编译通过
- [ ] 启动服务无错误
- [ ] 创建运单，检查carrier_id正确保存
- [ ] 同步运单，检查新字段正确保存
- [ ] 查询运单列表，检查新字段正确返回
- [ ] 查询运单详情，检查事件新字段正确显示
- [ ] 前端列表页显示正常
- [ ] 前端详情页显示正常

## 九、下一步工作

1. 更新Track17Service添加V2 API支持
2. 更新TrackingService的sync方法使用新的解析器
3. 前端Tracking.vue显示新字段
4. 添加子状态和运输天数的筛选功能
5. 优化详情页的milestone显示
