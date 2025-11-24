# 承运商映射更新总结

## 更新时间
2025-11-23

## 更新内容

根据17Track官方最新承运商列表（https://res.17track.net/asset/carrier/info/apicarrier.all.json），完整更新了系统的承运商ID映射关系。

## 主要变更

### 1. 修正的承运商ID

所有主流承运商的ID都已更新为17Track官方最新ID：

| 承运商 | 旧ID | 新ID | 状态 |
|--------|------|------|------|
| UPS | 1001 | **100002** | ✅ 已修正 |
| FedEx | 2003 | **100003** | ✅ 已修正 |
| USPS | 2071 | **21051** | ✅ 已修正 |
| DHL | 2002 | **100001** | ✅ 已修正 |
| 4PX | 10001 | **190094** | ✅ 已修正 |
| China Post | 1159 | **3011** | ✅ 已修正 |
| Yanwen | 1356 | **190012** | ✅ 已修正 |

### 2. 新增的承运商支持

添加了更多承运商的支持，特别是中国国内快递：

**国际快递扩展**:
- UPS Mail Innovations (100398)
- UPS Freight (100399)
- FedEx International Connect (100222)
- DHL Paket (7041)
- DHL eCommerce US (7047)
- DHL eCommerce Asia (7048)
- DHL eCommerce CN (100765)

**中国邮政**:
- China EMS (3013)

**中国快递**:
- SF Express / 顺丰速运 (100012)
- Cainiao / 菜鸟 (190271)
- ZTO International / 中通国际 (190175)

### 3. 前端更新

**添加运单表单** (`Tracking.vue`):
- 新增：China EMS
- 新增：顺丰速运 (SF Express)
- 新增：菜鸟 (Cainiao)
- 新增：中通 (ZTO)

**搜索筛选**:
- 同步更新所有新增承运商选项

## 技术实现

### 后端文件修改

1. **TrackingService.java**
   - 更新 `convertCarrierIdToCode()` 方法
   - 新增19个承运商ID映射
   - 添加详细注释和官方链接

2. **Track17RegisterRequest.java**
   - 更新 `convertCarrierCode()` 方法
   - 支持用户指定新的承运商代码
   - 添加 "sf" 别名映射到 "sf-express"

### 前端文件修改

3. **Tracking.vue**
   - 更新承运商下拉选项（添加运单）
   - 更新承运商筛选选项（搜索）
   - 显示中文名称（顺丰、菜鸟、中通等）

### 文档更新

4. **CARRIER_AUTO_DETECTION.md**
   - 更新承运商ID映射表
   - 添加官方链接引用

5. **docs/CARRIER_MAPPING.md** (新建)
   - 完整的承运商映射文档
   - 包含使用方法和示例
   - 添加新承运商的指南

## 测试验证

### 自动识别测试

已验证UPS自动识别功能正常工作：

```bash
# 请求
POST /api/v1/tracking
{
  "trackingNumber": "1Z9999A70123456789",
  "carrierCode": "",  # 留空自动识别
  "source": "manual"
}

# 17Track返回
{
  "carrier": 100002,  # UPS的正确ID
  "number": "1Z9999A70123456789"
}

# 系统转换
carrier_code: "ups"  # 正确转换
```

## 数据来源

- **官方承运商列表**: https://res.17track.net/asset/carrier/info/apicarrier.all.json
- **承运商总数**: 3017个（截至2025-11-23）
- **官方文档**: https://api.17track.net/en/doc
- **帮助文档**: https://help.17track.net/hc/en-us/articles/37467353177753

## 影响范围

### 向后兼容性

✅ **完全向后兼容**

- 旧的承运商代码（如 "ups", "fedex"）继续有效
- 现有数据库中的运单不受影响
- API接口保持不变

### 需要注意

1. **已注册的运单**: 如果之前使用错误ID注册的运单，可能需要重新同步
2. **自动识别**: 推荐使用自动识别（留空carrier），避免ID错误
3. **定期更新**: 17Track的承运商列表会定期更新，建议定期同步

## 下一步建议

1. **测试主要承运商**: 建议测试UPS、FedEx、USPS、DHL的自动识别功能
2. **测试中国快递**: 测试顺丰、菜鸟、中通的识别和查询
3. **监控日志**: 关注未知carrier ID的日志，及时添加支持
4. **数据迁移**: 如有需要，更新现有数据库中的carrier_code

## 相关文件

- `/CARRIER_AUTO_DETECTION.md` - 自动识别功能说明
- `/docs/CARRIER_MAPPING.md` - 完整承运商映射文档
- `src/main/java/com/logistics/track17/service/TrackingService.java`
- `src/main/java/com/logistics/track17/dto/Track17RegisterRequest.java`
- `frontend-vue/src/views/Tracking.vue`

## Sources

- [17TRACK Tracking API](https://www.17track.net/en/api)
- [17TRACK API Documentation](https://api.17track.net/en/doc)
- [Supported carriers and carrier codes - Help Center](https://help.17track.net/hc/en-us/articles/37467353177753-Supported-carriers-and-carrier-codes)
- [Tracking API Quick Guide](https://help.17track.net/hc/en-us/articles/30944262120729--Tracking-API-Quick-Guide)
- [Official Carrier List JSON](https://res.17track.net/asset/carrier/info/apicarrier.all.json)
