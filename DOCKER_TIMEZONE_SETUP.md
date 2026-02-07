# Docker 容器时区配置说明

## 使用 Docker Compose 管理容器

### 1. 停止当前运行的容器

```bash
# 停止并删除旧容器（数据会保留在 volume 中）
docker stop mysql redis
docker rm mysql redis
```

### 2. 使用 Docker Compose 启动服务

```bash
# 在项目根目录下启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f mysql
docker-compose logs -f redis
```

### 3. 验证时区配置

```bash
# 验证 MySQL 时区
docker exec -it mysql mysql -uroot -p123456 -e "SELECT @@global.time_zone, @@session.time_zone, NOW();"

# 验证容器系统时区
docker exec -it mysql date
docker exec -it redis date
```

**预期输出：**
```
+--------------------+---------------------+---------------------+
| @@global.time_zone | @@session.time_zone | NOW()               |
+--------------------+---------------------+---------------------+
| +08:00             | +08:00              | 2025-12-22 14:36:00 |
+--------------------+---------------------+---------------------+
```

---

## Docker Compose 配置说明

### MySQL 时区配置

```yaml
environment:
  TZ: Asia/Shanghai  # 容器系统时区
command:
  - --default-time-zone=+08:00  # MySQL 服务器时区
```

**两层时区设置：**
1. **TZ 环境变量**: 设置容器操作系统的时区
2. **--default-time-zone**: 设置 MySQL 服务器的默认时区

这样配置后，容器重启时会自动应用 UTC+8 时区设置。

---

## 数据持久化

Docker Compose 使用命名卷 (named volumes) 来持久化数据：

- **mysql-data**: MySQL 数据库文件
- **redis-data**: Redis 持久化文件

即使删除容器，数据也不会丢失。如需完全清理数据：

```bash
# 停止并删除容器和卷
docker-compose down -v
```

---

## 常用命令

```bash
# 启动服务
docker-compose up -d

# 停止服务
docker-compose stop

# 重启服务
docker-compose restart

# 查看日志
docker-compose logs -f

# 停止并删除容器（保留数据）
docker-compose down

# 停止并删除容器和数据卷
docker-compose down -v

# 查看服务状态
docker-compose ps
```

---

## 自定义 MySQL 配置（可选）

如果需要更多自定义配置，可以创建 MySQL 配置文件：

### 1. 创建配置目录

```bash
mkdir -p mysql/conf.d
```

### 2. 创建配置文件

**mysql/conf.d/custom.cnf:**
```ini
[mysqld]
# 时区配置
default-time-zone='+08:00'

# 字符集配置
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci

# 性能优化
max_connections=200
innodb_buffer_pool_size=256M
```

### 3. 在 docker-compose.yml 中启用配置挂载

取消注释以下行：
```yaml
volumes:
  - ./mysql/conf.d:/etc/mysql/conf.d
```

### 4. 重启容器应用配置

```bash
docker-compose restart mysql
```

---

## 迁移现有数据

如果需要从旧容器迁移数据到新容器：

### 方法1: 备份和恢复

```bash
# 1. 从旧容器导出数据
docker exec mysql mysqldump -uroot -p123456 logistics_system > backup.sql

# 2. 启动新容器
docker-compose up -d

# 3. 导入数据到新容器
docker exec -i mysql mysql -uroot -p123456 logistics_system < backup.sql
```

### 方法2: 直接使用现有数据卷

如果旧容器使用了命名卷，可以在 docker-compose.yml 中使用相同的卷名，数据会自动迁移。
