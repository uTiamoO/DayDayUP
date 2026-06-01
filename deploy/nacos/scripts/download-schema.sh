#!/usr/bin/env bash
# 下载 Nacos 官方 MySQL 建库脚本到当前 nacos/ 目录
# Usage: bash scripts/download-schema.sh [version]
set -euo pipefail

VERSION="${1:-2.4.3}"
TARGET_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT="${TARGET_DIR}/nacos-mysql.sql"

PRIMARY="https://raw.githubusercontent.com/alibaba/nacos/${VERSION}/distribution/conf/mysql-schema.sql"
FALLBACK="https://cdn.jsdelivr.net/gh/alibaba/nacos@${VERSION}/distribution/conf/mysql-schema.sql"

echo "==> 下载 Nacos ${VERSION} mysql-schema.sql"
if curl -fsSL "$PRIMARY" -o "$OUTPUT"; then
  echo "==> OK: $OUTPUT (from github)"
elif curl -fsSL "$FALLBACK" -o "$OUTPUT"; then
  echo "==> OK: $OUTPUT (from jsdelivr fallback)"
else
  echo "!! 下载失败，请手动从 https://github.com/alibaba/nacos/blob/${VERSION}/distribution/conf/mysql-schema.sql 获取" >&2
  exit 1
fi
wc -l "$OUTPUT"
