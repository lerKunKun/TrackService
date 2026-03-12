#!/bin/bash
# 自动化部署与备份脚本 (deploy.sh)

echo "=================================="
echo "      开始自动部署 Track-17 项目     "
echo "=================================="

# 生成当前时间戳作为备份版本号
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# 1. 获取最新代码 (强制覆盖本地修改)
echo "=> [1/4] 从 Git 仓库拉取最新代码..."
git fetch --all
BRANCH=$(git symbolic-ref refs/remotes/origin/HEAD 2>/dev/null | sed 's@^refs/remotes/origin/@@')
git reset --hard origin/${BRANCH:-main}

# 2. 备份当前正在运行的镜像 (如果存在)
echo "=> [2/4] 备份当前镜像版本: $TIMESTAMP"
BACKUP_DIR="/opt/track-service-backups"
mkdir -p "$BACKUP_DIR"

if docker image inspect track-17-server:latest > /dev/null 2>&1; then
    docker tag track-17-server:latest track-17-server:backup-$TIMESTAMP
    echo "  - 已备份后端镜像为 track-17-server:backup-$TIMESTAMP"
    echo "  - 正在打包导出后端镜像到压缩包..."
    docker save track-17-server:backup-$TIMESTAMP | gzip > "$BACKUP_DIR/track-17-server-backup-${TIMESTAMP}.tar.gz"
fi

if docker image inspect track-17-web:latest > /dev/null 2>&1; then
    docker tag track-17-web:latest track-17-web:backup-$TIMESTAMP
    echo "  - 已备份前端镜像为 track-17-web:backup-$TIMESTAMP"
    echo "  - 正在打包导出前端镜像到压缩包..."
    docker save track-17-web:backup-$TIMESTAMP | gzip > "$BACKUP_DIR/track-17-web-backup-${TIMESTAMP}.tar.gz"
fi

# 3. 重新构建并启动容器
echo "=> [3/4] 使用 docker compose 重新构建并部署新服务..."
docker compose up -d --build

# 4. 清理废弃的无用镜像 (不影响上一步打标签备份好的历史版本镜像)
echo "=> [4/4] 清理本次构建产生的临时悬空(dangling)镜像..."
docker image prune -f

echo "=================================="
echo "          ✅ 部署成功完成!          "
echo "    若需回退，请使用 ./rollback.sh   "
echo "=================================="
