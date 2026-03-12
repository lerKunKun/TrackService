#!/bin/bash
# 简单的镜像回退脚本 (rollback.sh)

echo "=================================="
echo "      开始回退 Track-17 项目      "
echo "=================================="

# 1. 列出所有带 backup 前缀的历史版本镜像供选择
echo "可选的后端备份版本:"
docker images --format "{{.Repository}}:{{.Tag}}" | grep "track-service-git-app:backup" | sort -r | head -n 5

echo "----------------------------------"
echo "可选的前端备份版本:"
docker images --format "{{.Repository}}:{{.Tag}}" | grep "track-service-git-web:backup" | sort -r | head -n 5
echo "----------------------------------"

# 2. 提示用户输入要回退的版本号 (提供默认值)
read -p "请输入要回退的时间戳 (例如 20231025_153000): " TARGET_TIMESTAMP

if [ -z "$TARGET_TIMESTAMP" ]; then
    echo "❌ 错误: 未输入时间戳，回退已取消。"
    exit 1
fi

echo "=> [1/2] 正在将备份镜像恢复为 latest..."

# 后端镜像恢复
if [[ "$(docker images -q track-service-git-app:backup-${TARGET_TIMESTAMP} 2> /dev/null)" != "" ]]; then
    docker tag track-service-git-app:backup-${TARGET_TIMESTAMP} track-service-git-app:latest
    echo "✅ 后端已恢复至 backup-${TARGET_TIMESTAMP}"
else
    echo "⚠️ 警告: 找不到后端镜像 track-service-git-app:backup-${TARGET_TIMESTAMP}"
fi

# 前端镜像恢复
if [[ "$(docker images -q track-service-git-web:backup-${TARGET_TIMESTAMP} 2> /dev/null)" != "" ]]; then
    docker tag track-service-git-web:backup-${TARGET_TIMESTAMP} track-service-git-web:latest
    echo "✅ 前端已恢复至 backup-${TARGET_TIMESTAMP}"
else
    echo "⚠️ 警告: 找不到前端镜像 track-service-git-web:backup-${TARGET_TIMESTAMP}"
fi

# 3. 重新启动容器以应用最新(回退后)的 latest 镜像
echo "=> [2/2] 正在重启容器运行恢复的镜像..."
# 由于我们没有修改代码只是改了镜像指向，这里直接 down 然后 up，去掉 --build 确保它使用本地的 latest
docker-compose down
docker-compose up -d

echo "=================================="
echo "          ✅ 回退成功完成!          "
echo "=================================="
