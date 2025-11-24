# 性能优化快速实施指南

## 概述
本指南提供针对 10万~100万级运单的性能优化快速实施步骤。

## 实施时间表

### 第一阶段：立即实施（1天）
优先级：P0 - 影响线上稳定性

#### 步骤1: 添加缺失索引（30分钟）
```bash
# 1. 备份数据库
mysqldump -u root -p123456 logistics_system > backup_before_optimization.sql

# 2. 执行索引优化脚本
mysql -u root -p123456 < sql/optimization/001_add_indexes.sql

# 3. 更新统计信息
mysql -u root -p123456 -e "USE logistics_system; ANALYZE TABLE tracking_numbers;"

# 4. 验证索引
mysql -u root -p123456 -e "USE logistics_system; SHOW INDEX FROM tracking_numbers;"
```

**预期效果**:
- ✅ 状态筛选查询：3秒 → 50ms
- ✅ 列表排序：不再使用 filesort
- ✅ 日期范围查询性能提升 10-50倍

#### 步骤2: 优化列表查询（2小时）
修改文件：`src/main/resources/mapper/TrackingNumberMapper.xml`

**优化点**:
1. 条件化 JOIN（只在需要时才执行）
2. SELECT 指定字段而不是 `*`
3. LIKE 改为前缀匹配

#### 步骤3: 添加并发安全控制（1小时）
修改文件：`src/main/java/com/logistics/track17/service/TrackingService.java`

**实施方案**:
```java
// 方案1: 利用数据库约束（简单有效）
try {
    trackingNumberMapper.insert(trackingNumber);
} catch (DuplicateKeyException e) {
    throw BusinessException.of("运单号已存在");
}
```

### 第二阶段：近期实施（3天）
优先级：P1 - 性能优化

#### 步骤4: 批量导入优化（4小时）
1. 添加批量插入 Mapper 方法
2. 实现批量检查重复
3. 优化 17Track API 调用

**预期效果**:
- ✅ 1000条导入：30分钟 → 2分钟
- ✅ 支持 10000+ 条批量导入

#### 步骤5: 添加乐观锁（2小时）
```bash
# 1. 添加版本字段
mysql -u root -p123456 < sql/optimization/002_add_version.sql

# 2. 修改 Entity 和 Mapper
# 3. 修改 Service 更新逻辑
```

#### 步骤6: 添加 Redis 缓存（1天）
1. 配置 Redis 连接
2. 添加缓存注解
3. 实现缓存更新策略

**预期效果**:
- ✅ 热点数据查询：50ms → 2ms
- ✅ 数据库负载降低 60-80%

### 第三阶段：中期实施（1周）
优先级：P2 - 长期优化

#### 步骤7: 数据归档（2天）
```bash
# 1. 创建归档表
mysql -u root -p123456 < sql/optimization/003_create_archive.sql

# 2. 首次归档（谨慎操作）
mysql -u root -p123456 -e "USE logistics_system; CALL archive_completed_trackings(90);"

# 3. 设置定时任务
# 添加到 crontab: 每月1号凌晨2点执行
0 2 1 * * mysql -u root -p123456 -e "CALL logistics_system.archive_completed_trackings(90);"
```

#### 步骤8: 分页深度限制（1小时）
修改 Service 层，添加最大页码限制。

---

## 验证测试

### 性能测试脚本
```bash
# 1. 测试列表查询性能
time mysql -u root -p123456 -e "
USE logistics_system;
SELECT * FROM tracking_numbers
WHERE track_status = 'InTransit'
ORDER BY updated_at DESC
LIMIT 20;
"

# 2. 测试状态筛选
time mysql -u root -p123456 -e "
USE logistics_system;
SELECT COUNT(*) FROM tracking_numbers
WHERE track_status = 'Delivered'
  AND created_at >= '2025-01-01';
"

# 3. 查看执行计划
mysql -u root -p123456 -e "
USE logistics_system;
EXPLAIN SELECT * FROM tracking_numbers
WHERE track_status = 'InTransit'
  AND carrier_code = 'ups'
ORDER BY updated_at DESC
LIMIT 20;
"
```

### 预期 EXPLAIN 结果
优化后应该看到：
- ✅ `type`: ref 或 range（不是 ALL）
- ✅ `key`: 使用了索引（不是 NULL）
- ✅ `Extra`: 使用索引，没有 "Using filesort"

---

## 监控指标

### 关键指标
| 指标 | 优化前 | 优化后 | 监控方法 |
|------|--------|--------|----------|
| 列表查询响应时间 | 2-5秒 | 50-200ms | Slow Query Log |
| QPS | 10 | 100+ | MySQL STATUS |
| 批量导入1000条 | 20-30分钟 | 1-2分钟 | 应用日志 |
| 数据库CPU | 60-80% | 20-40% | top/htop |

### 监控命令
```bash
# 1. 查看慢查询
mysql -u root -p123456 -e "SHOW VARIABLES LIKE 'slow_query%';"

# 2. 查看 QPS
mysql -u root -p123456 -e "SHOW GLOBAL STATUS LIKE 'Questions';"

# 3. 查看连接数
mysql -u root -p123456 -e "SHOW PROCESSLIST;"

# 4. 查看表大小
mysql -u root -p123456 -e "
SELECT
    TABLE_NAME,
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS 'Size (MB)'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'logistics_system'
ORDER BY (DATA_LENGTH + INDEX_LENGTH) DESC;
"
```

---

## 回滚方案

### 如果优化后出现问题

#### 回滚索引
```sql
-- 删除添加的索引
ALTER TABLE tracking_numbers DROP INDEX idx_track_status;
ALTER TABLE tracking_numbers DROP INDEX idx_created_at;
ALTER TABLE tracking_numbers DROP INDEX idx_updated_at;
ALTER TABLE tracking_numbers DROP INDEX idx_status_updated;
ALTER TABLE tracking_numbers DROP INDEX idx_carrier_updated;
ALTER TABLE tracking_numbers DROP INDEX idx_user_status_updated;
ALTER TABLE tracking_numbers DROP INDEX idx_next_sync;
ALTER TABLE tracking_numbers DROP INDEX idx_source;
```

#### 回滚版本字段
```sql
ALTER TABLE tracking_numbers DROP COLUMN version;
```

#### 恢复数据库
```bash
mysql -u root -p123456 logistics_system < backup_before_optimization.sql
```

---

## 注意事项

### ⚠️ 执行前检查
1. ✅ 已备份数据库
2. ✅ 在测试环境验证
3. ✅ 选择低峰期执行
4. ✅ 通知相关人员

### ⚠️ 执行中监控
1. 观察 MySQL CPU 使用率
2. 监控慢查询日志
3. 检查错误日志
4. 验证应用功能

### ⚠️ 执行后验证
1. 运行性能测试
2. 检查查询执行计划
3. 验证核心功能
4. 对比性能指标

---

## 联系支持

如遇问题，请提供：
1. 错误日志
2. EXPLAIN 结果
3. 数据库版本
4. 数据量信息

---

**文档版本**: 1.0
**最后更新**: 2025-11-23
**适用范围**: 10万~100万级运单
