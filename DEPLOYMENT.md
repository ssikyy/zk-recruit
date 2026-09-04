# Docker 上线说明

## 部署概览

- 目标环境：Ubuntu 22.04（或同类发行版），建议至少 2 vCPU / 4 GiB。
- Docker Compose 运行 MySQL、Spring Boot 和 Caddy。
- 正式入口为 `.env` 中的 `SITE_DOMAIN`（HTTPS）。HTTP 自动跳转 HTTPS，证书由 Caddy 通过 ACME 签发并自动续期。
- 演示数据（可选）由 `DataInitializer` 幂等补齐，重复启动不会产生重复数据。
- 备份目录为 `backups/`（已被 Git 忽略），包含数据库与简历附件，请保存在版本库之外。
- 管理员账号与密码、数据库密码只写在服务器本地的 `.env` 中，本文不保存真实密码。

## 1. 服务器准备

Ubuntu 22.04 只需安装 Docker Engine 与 Docker Compose 插件。开放公网端口 80、443；MySQL 3306 和后端 8080 仅在 Compose 内部网络使用。

## 2. 生产配置

```bash
cp .env.example .env
chmod 600 .env
```

填写域名、公网 IP、两个不同的数据库随机密码，以及 8-20 位管理员密码。`.env` 已被 Git 忽略，不得提交。

`ADMIN_EMAIL` 同时用于证书签发联系邮箱，正式使用时应改成自己可收信的地址。更改初始化密码不会自动修改数据库里已存在的管理员密码。

## 3. 启动

```bash
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

首次启动时，MySQL 只会在空数据卷中执行 `db/schema.sql`。后端会幂等创建管理员、字典和可选演示数据。Caddy 会在域名解析正确且 80/443 可访问后自动签发并续期 HTTPS 证书。

为减少不同服务器网络环境造成的构建差异，基础镜像使用 AWS Public ECR 的 Docker Official Images，Maven 使用阿里云公共镜像，npm 使用 npmmirror，证书使用 ZeroSSL ACME。前后端仍在 Docker 多阶段构建中编译。

## 4. 验证

```bash
curl -I https://你的域名/
curl -s https://你的域名/api/public/job-categories
docker compose logs --tail=100 backend web db
```

每次发布后应在浏览器验证：首页、职位列表、登录、投递、简历上传和 HR 后台。执行 `docker compose restart` 后需要重新登录，再确认历史数据仍然存在。

## 5. 备份

```bash
bash scripts/backup-server.sh
```

备份目录保存在 `backups/zk_recruit_时间/`，包含 `database.sql.gz` 和 `resumes.tar.gz`，权限仅当前用户可读。建议把整个备份目录再复制到服务器之外。本脚本需要手动执行，尚未设置定时备份。

数据库卷是 `zk-recruit_mysql_data`，简历卷是 `zk-recruit_resume_data`。数据库与附件分步备份；真实业务应在低流量时备份或使用一致性快照方案。

更新应用前先备份，然后执行：

```bash
docker compose up -d --build
```

不要对已有数据的环境执行 `docker compose down -v`，该命令会删除数据库、简历和证书卷。
