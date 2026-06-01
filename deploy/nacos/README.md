# Nacos 单机部署（Docker / 云服务器）

> 适配 DayDayUP 项目 Spring Cloud Alibaba 客户端，外置 MySQL 持久化、强制鉴权、可一键起停。

## 1. 适用场景

* 单机部署（开发 / 小规模生产）
* Spring Cloud Alibaba 2023.x / 2025.x（gRPC 客户端连接走 9848 端口）
* 配置中心 + 注册中心一体

集群部署请另起 compose；本方案明确 `MODE: standalone`。

## 2. 前置

* Docker ≥ 20.10、docker compose v2
* 云服务器 ≥ 2C4G（推荐 4C8G）
* 安全组放行：`8848` `9848` `9849`，**源 IP 建议白名单**
* 当前目录写权限（用于挂载 `data/` 和 `nacos-logs/`）

## 3. 首次部署步骤

```bash
cd deploy/nacos

# 1) 下载 Nacos 官方建库 SQL（默认 2.4.3，可传版本号覆盖）
bash scripts/download-schema.sh
# 等价：bash scripts/download-schema.sh 2.4.3

# 2) 准备 .env
cp .env.example .env

# 3) 生成强随机密钥并替换 .env 中的占位
openssl rand -base64 32   # 复制输出，替换 NACOS_AUTH_TOKEN
openssl rand -base64 24   # 复制输出，替换 NACOS_AUTH_IDENTITY_VALUE
# 同时改 MYSQL_ROOT_PASSWORD / MYSQL_PASSWORD

# 4) 启动
docker compose up -d
docker compose logs -f nacos    # 出现 "Nacos started successfully" 即成功
```

浏览器访问 `http://<服务器IP>:8848/nacos`，默认 `nacos / nacos`，**首次登录立即在控制台修改密码**。

## 4. 客户端接入（Spring Boot）

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: <nacos-host>:8848
        username: nacos
        password: <你在控制台改后的密码>
        namespace: ${NACOS_NS:public}
      config:
        server-addr: <nacos-host>:8848
        username: nacos
        password: <同上>
        namespace: ${NACOS_NS:public}
        file-extension: yaml
```

部署到生产时建议把 `username/password/namespace` 用启动参数注入，不要硬编码到代码仓库。

## 5. 安全加固清单（**上线前逐项确认**）

| 项 | 必做 |
|---|---|
| `NACOS_AUTH_TOKEN` 用 `openssl rand -base64 32` 生成 | ✅ |
| `NACOS_AUTH_IDENTITY_VALUE` 随机化 | ✅ |
| 首次登录修改 `nacos` 用户密码 | ✅ |
| 安全组限制 `8848/9848/9849` 访问源 IP（业务服务白名单） | ✅ |
| `.env` 已在 `.gitignore` 中（仓库根 `.gitignore` 已配 `**/.env`） | ✅ |
| MySQL `root` 与 `nacos` 用户使用独立强密码 | ✅ |
| （可选）前置 Nginx 反代 + HTTPS（Let's Encrypt） | ⬜ |

## 6. 日常运维

```bash
docker compose ps                # 查看状态
docker compose logs -f nacos     # 跟踪日志
docker compose restart nacos     # 重启 nacos
docker compose down              # 停止（保留数据卷）
docker compose down -v           # 停止并清空数据（危险）
```

升级 Nacos 版本：

```bash
# 1) 修改 docker-compose.yml 的 image tag
# 2) 拉镜像 + 重启（数据兼容则无需迁移）
docker compose pull nacos
docker compose up -d nacos
```

## 7. 目录与持久化

```
deploy/nacos/
├── docker-compose.yml
├── .env.example
├── .env                  # ← 本地填，不入 git
├── nacos-mysql.sql       # ← 下载脚本生成
├── scripts/
│   └── download-schema.sh
├── data/                 # ← MySQL 持久化（自动生成，不入 git）
└── nacos-logs/           # ← Nacos 日志（自动生成，不入 git）
```

`data/` 与 `nacos-logs/` 由仓库根 `.gitignore` 排除。

## 8. 常见坑

| 现象 | 原因 / 解决 |
|---|---|
| 客户端连接超时但浏览器能开控制台 | 没开 `9848`，SCA 2.x 默认走 gRPC |
| Nacos 启动 30 秒后被 OOM kill | JVM_XMX 过大；2G 内存机器调小：`JVM_XMS=256m JVM_XMX=512m JVM_XMN=128m` |
| 控制台 502 | 启动慢，首次还在建表；等 60 秒 |
| 容器重启后数据全丢 | `./data` 不在持久卷上，或被 `down -v` 清掉 |
| 控制台登录后立即被踢 | `NACOS_AUTH_TOKEN` 不到 32 字节，按 base64 重新生成 |
