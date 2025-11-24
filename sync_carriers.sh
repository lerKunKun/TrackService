#!/bin/bash

# 承运商数据同步脚本
# 用法: ./sync_carriers.sh

echo "========================================="
echo "  承运商映射表同步工具"
echo "========================================="
echo ""

# 检查 JSON 文件是否存在
if [ ! -f "docs/apicarrier.all.json" ]; then
    echo "错误: 找不到 docs/apicarrier.all.json 文件"
    exit 1
fi

# 检查数据库连接
echo "检查数据库连接..."
mysql -u root -p123456 -e "USE logistics_system; SELECT 1;" 2>/dev/null
if [ $? -ne 0 ]; then
    echo "错误: 无法连接到数据库"
    exit 1
fi

echo "数据库连接正常"
echo ""

# 询问是否继续
read -p "警告: 此操作将清空并重新导入所有承运商数据。是否继续? (y/n): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "操作已取消"
    exit 0
fi

echo ""
echo "开始同步承运商数据..."
echo ""

# 运行同步工具
mvn exec:java -Dexec.mainClass="com.logistics.track17.util.CarrierSyncTool" -q

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================="
    echo "  同步完成！"
    echo "========================================="
    echo ""

    # 显示统计信息
    echo "数据统计:"
    mysql -u root -p123456 -e "
        USE logistics_system;
        SELECT
            COUNT(*) as '总承运商数',
            COUNT(DISTINCT country_iso) as '覆盖国家数',
            SUM(CASE WHEN url IS NOT NULL THEN 1 ELSE 0 END) as '有官网链接',
            SUM(CASE WHEN tel IS NOT NULL THEN 1 ELSE 0 END) as '有联系电话'
        FROM carriers;
    " 2>/dev/null

    echo ""
    echo "常用承运商示例:"
    mysql -u root -p123456 -e "
        USE logistics_system;
        SELECT carrier_code, carrier_name, carrier_name_cn, country_iso
        FROM carriers
        WHERE carrier_id IN (3011, 3013, 100001, 100002, 100003, 21051)
        ORDER BY carrier_id;
    " 2>/dev/null
else
    echo ""
    echo "同步失败！请检查错误信息"
    exit 1
fi
