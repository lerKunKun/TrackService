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
# 检查是否存在对应容器的镜像，如果存在则打上带有时间戳的 tag 以备份
if [[ "$(docker images -q track-service-git-app 2> /dev/null)" != "" ]]; then
  docker tag track-service-git-app:latest track-service-git-app:backup-${TIMESTAMP}
  echo "后端应用镜像已备份为 track-service-git-app:backup-${TIMESTAMP}"
fi

if [[ "$(docker images -q track-service-git-web 2> /dev/null)" != "" ]]; then
  docker tag track-service-git-web:latest track-service-git-web:backup-${TIMESTAMP}
  echo "前端页面镜像已备份为 track-service-git-web:backup-${TIMESTAMP}"
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
