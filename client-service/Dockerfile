FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

COPY . .

RUN mvn clean install -pl common-lib -am -DskipTests

RUN mvn clean package -pl client-service -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /app/client-service/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
