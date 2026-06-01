# 部署目录

按中间件 / 角色拆分子目录，每个子目录独立可移植（含自己的 compose + README）。

| 子目录 | 用途 | 落地阶段 |
|---|---|---|
| `nacos/` | 注册中心 + 配置中心 | P1 已落地 |
| `mysql/` | 业务库（admin / auth） | P1 已落地 |
| `redis/` | 缓存 + 分布式锁 | P1 已落地 |
| `rocketmq/` | 消息队列（事务消息、领域事件） | P3 |
| `xxljob/` | XXL-JOB Admin | P4 |
| `observability/` | Prometheus + Grafana + Zipkin | P4 |

> 各子目录自带 `.env.example`，复制为 `.env` 后填入密钥；`.env` 与数据卷 (`data/` `*-logs/`) 默认被仓库根 `.gitignore` 排除。

## 一键启动本地开发环境

项目根目录提供 `docker-compose-dev.yml`，可一键拉起 MySQL + Redis + Nacos：

```bash
# 基础服务
docker compose -f docker-compose-dev.yml up -d

# 含 RocketMQ + XXL-JOB
docker compose -f docker-compose-dev.yml --profile full up -d
```
