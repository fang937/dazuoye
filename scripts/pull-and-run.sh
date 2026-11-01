#!/usr/bin/env bash
# 拉取镜像并运行（macOS / Linux / Git Bash）
#
# 用法：
#   ./scripts/pull-and-run.sh
#   IMAGE=ghcr.io/you/aftersales-demo:1.0.0 ./scripts/pull-and-run.sh
#   DB_HOST=192.168.1.10 DB_PASSWORD=你的密码 ./scripts/pull-and-run.sh
#   BUILD=1 ./scripts/pull-and-run.sh          # 不拉镜像，改用本地源码构建
#
# 前提：目标 MySQL 已执行过 backend/database/init.sql。
# 想一条命令连数据库一起起，请用仓库根目录的 docker compose。

set -euo pipefail

IMAGE="${IMAGE:-aftersales-demo:1.0.0}"
NAME="${NAME:-aftersales-app}"
PORT="${PORT:-8082}"
DB_HOST="${DB_HOST:-host.docker.internal}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-aftersales_demo}"
DB_USERNAME="${DB_USERNAME:-root}"
DB_PASSWORD="${DB_PASSWORD:-123456}"

cd "$(dirname "$0")/.."

if [ "${BUILD:-0}" = "1" ]; then
  echo "[1/4] 从源码构建镜像 ${IMAGE} ..."
  docker build -t "${IMAGE}" .
else
  echo "[1/4] 拉取镜像 ${IMAGE} ..."
  docker pull "${IMAGE}"
fi

echo "[2/4] 清理同名容器（如果存在）..."
docker rm -f "${NAME}" >/dev/null 2>&1 || true

echo "[3/4] 启动容器 ${NAME} ..."
docker run -d --name "${NAME}" -p "${PORT}:8082" \
  -e "DB_HOST=${DB_HOST}" -e "DB_PORT=${DB_PORT}" -e "DB_NAME=${DB_NAME}" \
  -e "DB_USERNAME=${DB_USERNAME}" -e "DB_PASSWORD=${DB_PASSWORD}" \
  --restart unless-stopped \
  "${IMAGE}" >/dev/null

echo "[4/4] 等待健康检查通过..."
for _ in $(seq 1 30); do
  sleep 2
  status=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${NAME}" 2>/dev/null || echo starting)
  if [ "${status}" = "healthy" ] || [ "${status}" = "running" ]; then
    echo ""
    echo "启动成功： http://localhost:${PORT}/"
    echo "演示账号： lin / hong / kefu_wang / wh_li ，密码统一 123456"
    echo "查看日志： docker logs -f ${NAME}"
    echo "停止容器： docker rm -f ${NAME}"
    exit 0
  fi
  echo "  ... ${status}"
done

echo "容器未在预期时间内就绪，请查看： docker logs ${NAME}"
exit 1
