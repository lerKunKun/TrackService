#!/bin/bash

# Track17 数据库初始化测试脚本
#
# 使用方法:
#   chmod +x test_database.sh
#   ./test_database.sh

echo "=========================================="
echo "Track17 数据库初始化测试"
echo "=========================================="
echo ""

# 提示输入MySQL密码
read -sp "请输入MySQL root密码: " MYSQL_PASSWORD
echo ""
echo ""

# 检查MySQL是否运行
echo "1. 检查MySQL服务..."
if command -v mysql &> /dev/null; then
    echo "✓ MySQL已安装"
else
    echo "✗ MySQL未安装或不在PATH中"
    exit 1
fi

# 测试MySQL连接
echo ""
echo "2. 测试MySQL连接..."
if mysql -u root -p"$MYSQL_PASSWORD" -e "SELECT 1;" &> /dev/null; then
    echo "✓ MySQL连接成功"
else
    echo "✗ MySQL连接失败，请检查密码"
    exit 1
fi

# 执行database.sql
echo ""
echo "3. 创建数据库和表..."
if mysql -u root -p"$MYSQL_PASSWORD" < docs/database.sql; then
    echo "✓ 数据库创建成功"
else
    echo "✗ 数据库创建失败"
    exit 1
fi

# 初始化管理员账号
echo ""
echo "4. 初始化管理员账号..."
if mysql -u root -p"$MYSQL_PASSWORD" < docs/init_admin.sql; then
    echo "✓ 管理员账号初始化成功"
else
    echo "✗ 管理员账号初始化失败"
    exit 1
fi

# 验证表结构
echo ""
echo "5. 验证表结构..."
TABLE_COUNT=$(mysql -u root -p"$MYSQL_PASSWORD" -D logistics_system -sN -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'logistics_system';")

if [ "$TABLE_COUNT" -eq 9 ]; then
    echo "✓ 表创建成功（共 $TABLE_COUNT 张表）"
else
    echo "✗ 表数量不正确（预期9张，实际 $TABLE_COUNT 张）"
    exit 1
fi

# 列出所有表
echo ""
echo "6. 数据表列表:"
mysql -u root -p"$MYSQL_PASSWORD" -D logistics_system -e "SHOW TABLES;"

# 验证管理员账号
echo ""
echo "7. 验证管理员账号..."
USER_COUNT=$(mysql -u root -p"$MYSQL_PASSWORD" -D logistics_system -sN -e "SELECT COUNT(*) FROM users WHERE username = 'admin';")

if [ "$USER_COUNT" -eq 1 ]; then
    echo "✓ 管理员账号验证成功"
    echo ""
    echo "管理员信息:"
    mysql -u root -p"$MYSQL_PASSWORD" -D logistics_system -e "SELECT id, username, email, real_name, role, status FROM users WHERE username = 'admin';"
else
    echo "✗ 管理员账号验证失败"
    exit 1
fi

# 验证承运商数据
echo ""
echo "8. 验证承运商数据..."
CARRIER_COUNT=$(mysql -u root -p"$MYSQL_PASSWORD" -D logistics_system -sN -e "SELECT COUNT(*) FROM carriers;")

if [ "$CARRIER_COUNT" -eq 10 ]; then
    echo "✓ 承运商数据初始化成功（共 $CARRIER_COUNT 条）"
else
    echo "⚠ 承运商数量异常（预期10条，实际 $CARRIER_COUNT 条）"
fi

# 显示外键约束
echo ""
echo "9. 外键约束列表:"
mysql -u root -p"$MYSQL_PASSWORD" -D logistics_system -e "
SELECT
    TABLE_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME
FROM
    information_schema.KEY_COLUMN_USAGE
WHERE
    TABLE_SCHEMA = 'logistics_system'
    AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY
    TABLE_NAME;
"

echo ""
echo "=========================================="
echo "✓ 数据库初始化完成！"
echo "=========================================="
echo ""
echo "默认管理员账号信息:"
echo "  用户名: admin"
echo "  密码: admin123"
echo ""
echo "⚠️  请在首次登录后立即修改默认密码！"
echo ""
