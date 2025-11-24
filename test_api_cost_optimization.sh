#!/bin/bash

echo "=========================================="
echo "API费用优化验证测试"
echo "=========================================="
echo ""

# 获取Token
echo "1. 登录获取Token..."
TOKEN=$(curl -s "http://localhost:8080/api/v1/auth/login" \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

if [ -z "$TOKEN" ]; then
    echo "❌ 登录失败"
    exit 1
fi
echo "✅ 登录成功"
echo ""

# 测试1: 添加无效运单号
echo "=========================================="
echo "测试1: 添加无效运单号"
echo "=========================================="
echo "运单号: INVALID123 (格式无效)"
echo ""

RESPONSE=$(curl -s "http://localhost:8080/api/v1/tracking" \
  -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"trackingNumber":"INVALID123","source":"manual"}')

echo "$RESPONSE" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(f'状态码: {data[\"code\"]}')
    print(f'消息: {data[\"message\"]}')
    if data['code'] == 200:
        print('❌ 测试失败: 无效运单号被接受了')
    else:
        print('✅ 测试通过: 无效运单号被正确拒绝')
except Exception as e:
    print(f'解析失败: {e}')
"

echo ""
echo "=========================================="
echo "测试2: 批量导入（含无效运单）"
echo "=========================================="
echo "运单1: 9400100000000000000001 (USPS格式)"
echo "运单2: INVALID999 (无效)"
echo "运单3: 9400100000000000000002 (USPS格式)"
echo ""

RESPONSE=$(curl -s "http://localhost:8080/api/v1/tracking/batch-import" \
  -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"trackingNumber":"9400100000000000000001","remarks":"测试1"},
      {"trackingNumber":"INVALID999","remarks":"无效运单"},
      {"trackingNumber":"9400100000000000000002","remarks":"测试2"}
    ]
  }')

echo "$RESPONSE" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    result = data.get('data', {})
    print(f'状态码: {data[\"code\"]}')
    print(f'消息: {data.get(\"message\", \"\")}')
    print(f'总数: {result.get(\"total\", 0)}')
    print(f'成功: {result.get(\"success\", 0)}')
    print(f'失败: {result.get(\"failed\", 0)}')

    if result.get('failed', 0) > 0:
        print('✅ 测试通过: 无效运单号被正确过滤')
    else:
        print('⚠️  警告: 没有失败的运单号')
except Exception as e:
    print(f'解析失败: {e}')
"

echo ""
echo "=========================================="
echo "测试3: 验证数据库中不存在无效运单"
echo "=========================================="

# 查询数据库中是否存在无效运单号
EXISTS=$(mysql -u root -p123456 -D logistics_system -se "
SELECT COUNT(*) FROM tracking_numbers
WHERE tracking_number IN ('INVALID123', 'INVALID999');
" 2>/dev/null)

if [ "$EXISTS" = "0" ]; then
    echo "✅ 测试通过: 数据库中不存在无效运单号"
else
    echo "❌ 测试失败: 数据库中存在 $EXISTS 个无效运单号"
fi

echo ""
echo "=========================================="
echo "测试完成"
echo "=========================================="
echo ""
echo "预期结果:"
echo "1. 添加无效运单时，应返回错误提示"
echo "2. 批量导入时，无效运单应被标记为失败"
echo "3. 数据库中不应存在无效运单号"
echo ""
echo "优化效果:"
echo "- 节省API调用费用"
echo "- 保持数据库干净"
echo "- 提升用户体验"
