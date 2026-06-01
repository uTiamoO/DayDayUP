# MySQL

## 快速启动

```bash
cp .env.example .env
# 编辑 .env 填入密码
docker compose up -d
```

## 初始化

容器首次启动时会自动执行 `../../sql/` 下的初始化脚本。

## 连接

- Host: `127.0.0.1`
- Port: `23336`（可在 `.env` 中修改）
- 用户名: `root`
- 密码: 见 `.env`
