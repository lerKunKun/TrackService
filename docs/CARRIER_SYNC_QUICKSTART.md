# 承运商同步工具 - 快速开始

## 一键同步

运行以下脚本一键同步承运商数据：

```bash
./sync_carriers.sh
```

该脚本会：
1. 检查数据库连接
2. 确认是否继续（会清空现有数据）
3. 从 `docs/apicarrier.all.json` 同步数据
4. 显示同步统计信息

## 手动同步

也可以直接运行 Java 工具：

```bash
mvn exec:java -Dexec.mainClass="com.logistics.track17.util.CarrierSyncTool"
```

## 同步结果

成功同步后，`carriers` 表将包含 **3017条** 承运商数据，包括：

- 承运商ID和代码映射
- 多语言名称（英文、中文）
- 国家信息
- 联系方式（邮箱、电话、官网）

## 数据查询示例

```sql
-- 查询中国邮政
SELECT * FROM carriers WHERE carrier_code = 'china-post';

-- 查询所有中国承运商
SELECT * FROM carriers WHERE country_iso = 'CN';

-- 查询所有激活的承运商
SELECT * FROM carriers WHERE is_active = 1;
```

## 详细文档

查看完整文档：[CARRIER_SYNC_TOOL.md](./CARRIER_SYNC_TOOL.md)

## 注意事项

⚠️ 同步工具会**清空并重新导入**所有承运商数据，请谨慎操作！

## 数据库配置

- 主机: localhost
- 端口: 3306
- 数据库: logistics_system
- 用户: root
- 密码: 123456

如需修改配置，请编辑 `src/main/java/com/logistics/track17/util/CarrierSyncTool.java`
