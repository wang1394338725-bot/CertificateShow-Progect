# 后端运行镜像：本地/CI 已构建好 app.jar，这里只负责 JRE 运行（服务器上无需 Maven/编译）
# 构建：docker compose build backend
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY app.jar app.jar

# 堆内存限制：2G 服务器与 MySQL 共存，512m 上限防止 JVM 抢占过多
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
