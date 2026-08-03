# MySQL

## 快速启动

```bash
cp .env.example .env
# 编辑 .env 填入密码
docker compose up -d
```

## 初始化

容器首次启动时会自动执行 `../../sql/` 下的初始化脚本。

现有远程 / 非空数据库不会自动重放初始化脚本；新增业务库或应用用户权限变更时，需要使用具备授权能力的账号手工执行对应脚本，例如：

```bash
mysql -h <host> -P <port> -u root -p < ../../sql/daydayup_reading.sql
```

`daydayup_reading.sql` 会创建 `daydayup_reading` 库（如不存在）并授予应用账号 `ddup` 对该库的 `SELECT/INSERT/UPDATE/DELETE` 权限。脚本不创建用户、不保存真实密码；如果环境中的应用账号或 Host 不同，请先调整脚本末尾的 `GRANT ... TO` 目标。

## 连接

- Host: `127.0.0.1`
- Port: `23336`（可在 `.env` 中修改）
- 用户名: `root`
- 密码: 见 `.env`
