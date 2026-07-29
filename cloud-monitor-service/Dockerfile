# Bước 1: Build bằng Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy toàn bộ dự án (bao gồm cả common-lib và các pom.xml)
COPY . .

# Build common-lib trước và cài vào local repo của container
RUN mvn clean install -pl common-lib -am -DskipTests

# Build dịch vụ cloud-monitor-service
RUN mvn clean package -pl cloud-monitor-service -am -DskipTests

# Bước 2: Chạy ứng dụng bằng JDK nhẹ
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy file .jar đã build xong ở Bước 1 sang
COPY --from=builder /app/cloud-monitor-service/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]