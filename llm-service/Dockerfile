# Bước 1: Build bằng Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy toàn bộ dự án (bao gồm cả common-lib và các pom.xml)
COPY . .

# Build common-lib trước và cài vào local repo của container
RUN mvn clean install -pl common-lib -am -DskipTests

# Build dịch vụ llm-service
RUN mvn clean package -pl llm-service -am -DskipTests

# Bước 2: Chạy ứng dụng bằng JDK nhẹ
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy file .jar đã build xong ở Bước 1 sang
COPY --from=builder /app/llm-service/target/*.jar app.jar

EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]