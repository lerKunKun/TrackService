# 承运商映射表同步工具使用文档

## 概述

`CarrierSyncTool` 是一个用于从 `docs/apicarrier.all.json` 同步承运商数据到数据库的工具。该工具会读取 JSON 文件中的所有承运商信息，并批量导入到 MySQL 数据库的 `carriers` 表中。

## 数据库表结构

### carriers 表字段

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| carrier_id | INT | 17Track承运商ID（来自JSON的key字段） |
| carrier_code | VARCHAR(50) | 系统承运商代码（自动生成） |
| carrier_name | VARCHAR(100) | 承运商英文名称 |
| carrier_name_cn | VARCHAR(100) | 承运商中文名称 |
| country_id | INT | 国家ID |
| country_iso | VARCHAR(10) | 国家ISO代码 |
| email | VARCHAR(255) | 联系邮箱 |
| tel | VARCHAR(100) | 联系电话 |
| url | VARCHAR(500) | 官网地址 |
| is_active | TINYINT | 是否启用（默认1） |
| sort_order | INT | 排序（默认0） |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### JSON 文件格式

```json
{
  "key": 3011,                          // carrier_id
  "_country": 301,                      // country_id
  "_country_iso": "CN",                 // country_iso
  "_email": null,                       // email
  "_tel": null,                         // tel
  "_url": "https://yjcx.ems.com.cn/",  // url
  "_name": "China Post",                // carrier_name
  "_name_zh-cn": "中国邮政",            // carrier_name_cn
  "_name_zh-hk": "中國郵政"             // 未使用
}
```

## 使用方法

### 1. 运行同步工具

```bash
mvn exec:java -Dexec.mainClass="com.logistics.track17.util.CarrierSyncTool"
```

### 2. 同步过程

工具执行以下步骤：

1. 读取 `docs/apicarrier.all.json` 文件
2. 解析所有承运商数据（3017条）
3. 清空 `carriers` 表中的现有数据
4. 批量插入新数据（每1000条提交一次）
5. 输出同步结果

### 3. 预期输出

```
19:58:55 INFO - 开始同步承运商数据...
19:58:55 INFO - 从JSON文件读取到 3017 条承运商数据
19:58:55 INFO - 清空现有数据，删除 X 条记录
19:58:55 INFO - 已插入 1000 条记录
19:58:56 INFO - 已插入 2000 条记录
19:58:56 INFO - 已插入 3000 条记录
19:58:56 INFO - 批量插入完成，共 3017 条记录
19:58:56 INFO - 成功同步 3017 条承运商数据到数据库
19:58:56 INFO - 承运商数据同步完成！
```

## carrier_code 生成规则

`carrier_code` 是从 `carrier_name` 自动生成的，规则如下：

1. 将承运商名称转换为小写
2. 移除特殊字符（仅保留字母、数字、空格和短横线）
3. 将空格替换为短横线
4. 移除多余的短横线
5. 限制长度在50字符以内
6. 如果处理后为空，使用 `carrier-{carrier_id}` 格式

### 示例

| carrier_name | carrier_code |
|--------------|--------------|
| China Post | china-post |
| DHL Express | dhl-express |
| UPS | ups |
| FedEx | fedex |
| USPS | usps |

## 验证同步结果

### 检查总数

```sql
SELECT COUNT(*) FROM carriers;
-- 预期结果: 3017
```

### 查看样例数据

```sql
SELECT carrier_id, carrier_code, carrier_name, carrier_name_cn, country_iso
FROM carriers
LIMIT 10;
```

### 查询特定承运商

```sql
-- 按 carrier_id 查询
SELECT * FROM carriers WHERE carrier_id = 3011;

-- 按 carrier_code 查询
SELECT * FROM carriers WHERE carrier_code = 'china-post';

-- 按国家查询
SELECT * FROM carriers WHERE country_iso = 'CN';
```

## 配置说明

### 数据库连接配置

在 `CarrierSyncTool.java` 中修改以下常量：

```java
private static final String DB_URL = "jdbc:mysql://localhost:3306/logistics_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
private static final String DB_USER = "root";
private static final String DB_PASSWORD = "123456";
```

### JSON 文件路径

```java
private static final String JSON_FILE_PATH = "docs/apicarrier.all.json";
```

## 注意事项

1. **数据备份**：运行工具前建议备份 `carriers` 表数据，因为工具会清空现有数据
2. **数据库权限**：确保数据库用户有 DELETE 和 INSERT 权限
3. **文件路径**：确保 `docs/apicarrier.all.json` 文件存在且路径正确
4. **事务处理**：工具使用事务批量提交，如遇错误会自动回滚
5. **性能优化**：采用批处理方式，每1000条提交一次，提高插入效率

## 常见问题

### Q: 同步失败怎么办？

A: 检查以下几点：
- 数据库连接是否正常
- JSON 文件是否存在
- 数据库用户权限是否足够
- 查看错误日志获取详细信息

### Q: 如何部分更新数据？

A: 当前工具采用全量替换方式。如需部分更新，需要修改工具代码，使用 INSERT ON DUPLICATE KEY UPDATE 语法。

### Q: carrier_code 重复怎么办？

A: 数据库中 `carrier_code` 有唯一索引。如果自动生成的 code 重复，需要手动调整生成规则。

## 集成说明

### 在应用中使用

可以将同步功能集成到应用中作为管理功能：

```java
@Service
public class CarrierSyncService {

    @Autowired
    private CarrierMapper carrierMapper;

    public void syncFromJson() throws IOException {
        CarrierSyncTool tool = new CarrierSyncTool();
        tool.syncCarriers();
    }
}
```

### 定时同步

可以配置定时任务定期同步：

```java
@Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
public void scheduledSync() {
    try {
        carrierSyncService.syncFromJson();
        log.info("定时同步承运商数据成功");
    } catch (Exception e) {
        log.error("定时同步失败", e);
    }
}
```

## 版本历史

- v1.0 (2025-01-23): 初始版本，支持从 JSON 文件全量同步承运商数据
