# ============================================================
# 服装网店退换货处理系统 · 单镜像（前端 + 后端）
#
# 三阶段构建：
#   1) web  —— 用 Node 构建 Vue 前端
#   2) api  —— 用 Maven 打 Spring Boot 可执行 jar，并把前端产物塞进 static
#   3) run  —— 只带 JRE 的运行时镜像
#
# 构建：docker build -t aftersales-demo:1.0.0 .
# 运行：docker compose up -d          （推荐，连带 MySQL）
#      docker run -p 8082:8082 -e DB_HOST=host.docker.internal ... aftersales-demo:1.0.0
# ============================================================

# ---------- 1. 前端构建 ----------
FROM node:20-alpine AS web
WORKDIR /build/frontend

# 先只拷依赖清单，源码没改时这一层能命中缓存
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY frontend/ ./
# 构建到独立 dist，避免往镜像里写仓库中的 backend/static
RUN VITE_OUT_DIR=dist npm run build

# ---------- 2. 后端构建 ----------
FROM maven:3.9-eclipse-temurin-17 AS api
WORKDIR /build

# 先解析依赖，同样利用镜像层缓存
COPY backend/pom.xml backend/pom.xml
RUN mvn -B -q -f backend/pom.xml dependency:go-offline

COPY backend/ backend/
COPY --from=web /build/frontend/dist/ backend/src/main/resources/static/
RUN mvn -B -q -f backend/pom.xml -DskipTests package \
    && cp backend/target/aftersales-demo-*.jar /build/app.jar

# ---------- 3. 运行时 ----------
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="aftersales-demo" \
      org.opencontainers.image.description="服装网店退换货处理系统（Spring Boot + Vue3）" \
      org.opencontainers.image.version="1.0.0"

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=api /build/app.jar /app/app.jar
USER app

ENV SERVER_PORT=8082 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"

EXPOSE 8082

# 容器自身健康检查，compose 的 app 服务也会读它
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=6 \
    CMD wget -qO- "http://127.0.0.1:${SERVER_PORT}/api/health" >/dev/null 2>&1 || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
