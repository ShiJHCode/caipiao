# ============ 构建阶段 ============
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 阿里云 Maven 镜像，加速依赖下载
COPY settings.xml /root/.m2/settings.xml

# 先复制 pom.xml 下载依赖（利用 Docker 层缓存）
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

# 复制源码并打包
COPY src ./src
RUN mvn -B -q -DskipTests package

# ============ 运行阶段 ============
FROM eclipse-temurin:17-jre
WORKDIR /app

# 复制打包好的可执行 jar
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
