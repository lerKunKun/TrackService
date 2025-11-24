#!/bin/bash

# 显示名称功能测试脚本

echo "=========================================="
echo "测试用户显示名称功能"
echo "=========================================="
echo ""

echo "1. 测试后端登录API..."
RESPONSE=$(curl -s "http://localhost:8080/api/v1/auth/login" \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

echo "$RESPONSE" | python3 -c "
import sys, json
data = json.load(sys.stdin)
if data['code'] == 200:
    print('✅ 登录成功')
    print(f\"   用户名: {data['data']['username']}\")
    print(f\"   真实姓名: {data['data'].get('realName', '未找到')}\")
    print(f\"   Token: {data['data']['token'][:20]}...\")
else:
    print('❌ 登录失败')
    print(data)
"

echo ""
echo "=========================================="
echo "前端测试步骤"
echo "=========================================="
echo ""
echo "请按以下步骤测试："
echo ""
echo "1. 打开浏览器开发者工具 (F12)"
echo "2. 清除localStorage数据："
echo "   - 进入 Application/存储 标签"
echo "   - 清除 localStorage"
echo "   或在Console中执行: localStorage.clear()"
echo ""
echo "3. 刷新页面，重新登录"
echo "   - 用户名: admin"
echo "   - 密码: admin123"
echo ""
echo "4. 登录后查看右上角"
echo "   - 应该显示: 系统管理员"
echo "   - 而不是: admin"
echo ""
echo "5. 验证localStorage存储："
echo "   在Console中执行:"
echo "   localStorage.getItem('displayName')"
echo "   应该返回: '系统管理员'"
echo ""
echo "=========================================="
