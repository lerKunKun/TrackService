# 外键约束问题修复说明

## 问题描述

在执行 `database.sql` 时遇到以下错误：

```
3730 - Cannot drop table 'shops' referenced by a foreign key constraint 'fk_orders_shop' on table 'orders'.
```

## 问题原因

在删除数据表时，必须先删除包含外键的子表，再删除被引用的父表。原来的SQL脚本在每个CREATE TABLE之前都有DROP TABLE语句，但这些DROP语句的顺序不正确。

## 已修复

✅ 已将所有DROP TABLE语句移至文件开头，并按照正确的依赖关系逆序排列：

```sql
-- 正确的删除顺序（从子表到父表）
DROP TABLE IF EXISTS `tracking_events`;      -- 依赖 tracking_numbers
DROP TABLE IF EXISTS `tracking_numbers`;     -- 依赖 users, parcels
DROP TABLE IF EXISTS `parcels`;              -- 依赖 orders
DROP TABLE IF EXISTS `orders`;               -- 依赖 shops
DROP TABLE IF EXISTS `webhook_logs`;         -- 依赖 shops
DROP TABLE IF EXISTS `sync_jobs`;            -- 依赖 shops
DROP TABLE IF EXISTS `shops`;                -- 依赖 users
DROP TABLE IF EXISTS `users`;                -- 被多个表依赖
DROP TABLE IF EXISTS `carriers`;             -- 独立表
```

## 如何执行

### 方式1：直接执行（推荐）

```bash
# 1. 创建数据库和表
mysql -u root -p < docs/database.sql

# 2. 初始化管理员账号
mysql -u root -p < docs/init_admin.sql
```

### 方式2：使用测试脚本

```bash
# 1. 给脚本添加执行权限
chmod +x test_database.sh

# 2. 运行测试脚本
./test_database.sh
```

测试脚本会自动完成：
- ✓ 检查MySQL服务
- ✓ 测试MySQL连接
- ✓ 创建数据库和表
- ✓ 初始化管理员账号
- ✓ 验证表结构
- ✓ 显示外键约束

### 方式3：手动执行（如果需要）

```bash
# 1. 登录MySQL
mysql -u root -p

# 2. 在MySQL命令行中执行
source /path/to/docs/database.sql;
source /path/to/docs/init_admin.sql;

# 3. 验证表是否创建成功
USE logistics_system;
SHOW TABLES;

# 4. 验证管理员账号
SELECT id, username, email, role FROM users WHERE username = 'admin';
```

## 表依赖关系说明

```
users (顶层父表)
  ↓
shops (引用 users)
  ↓
├─ orders (引用 shops)
│    ↓
│  parcels (引用 orders)
│    ↓
│  tracking_numbers (引用 parcels, users)
│    ↓
│  tracking_events (引用 tracking_numbers)
│
├─ webhook_logs (引用 shops)
│
└─ sync_jobs (引用 shops)

carriers (独立表，无外键)
```

## 外键约束说明

修复后的数据库包含以下外键约束：

| 子表 | 外键字段 | 父表 | 约束名 | 删除策略 |
|------|---------|------|--------|---------|
| shops | user_id | users | fk_shops_user | CASCADE |
| orders | shop_id | shops | fk_orders_shop | CASCADE |
| parcels | order_id | orders | fk_parcels_order | CASCADE |
| tracking_numbers | user_id | users | fk_tracking_user | CASCADE |
| tracking_numbers | parcel_id | parcels | fk_tracking_parcel | SET NULL |
| tracking_events | tracking_id | tracking_numbers | fk_events_tracking | CASCADE |

### 删除策略说明

- **CASCADE**: 删除父表记录时，自动删除所有相关的子表记录
- **SET NULL**: 删除父表记录时，将子表中的外键字段设置为NULL

## 验证数据库

执行以下SQL验证数据库是否正确创建：

```sql
-- 1. 检查表数量（应该是9张表）
SELECT COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema = 'logistics_system';

-- 2. 列出所有表
SHOW TABLES;

-- 3. 检查外键约束
SELECT
    TABLE_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME
FROM
    information_schema.KEY_COLUMN_USAGE
WHERE
    TABLE_SCHEMA = 'logistics_system'
    AND REFERENCED_TABLE_NAME IS NOT NULL;

-- 4. 验证管理员账号
SELECT id, username, email, real_name, role, status
FROM users
WHERE username = 'admin';

-- 5. 检查承运商数据（应该是10条）
SELECT COUNT(*) AS carrier_count FROM carriers;
```

## 预期结果

✓ 9张数据表成功创建
✓ 6个外键约束正确建立
✓ 1个管理员账号（admin）
✓ 10条承运商数据

## 故障排除

### 问题1：仍然报外键约束错误

**解决方案**：先手动删除数据库，再重新创建

```sql
DROP DATABASE IF EXISTS logistics_system;
```

然后重新执行 `database.sql`

### 问题2：管理员账号已存在

**解决方案**：`init_admin.sql` 脚本会先删除旧的admin账号，可以安全重复执行

### 问题3：表结构需要修改

**解决方案**：

```sql
-- 1. 删除整个数据库
DROP DATABASE IF EXISTS logistics_system;

-- 2. 重新执行脚本
source docs/database.sql;
source docs/init_admin.sql;
```

## 数据库连接配置

修改 `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/logistics_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC
    username: root
    password: 你的密码    # 修改为实际密码
```

---

**修复完成时间**: 2025-11-23
**影响范围**: docs/database.sql
**修复方式**: 调整DROP TABLE语句顺序
