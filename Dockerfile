FROM maven:3.9-eclipse-temurin-23-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:23-jdk-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 10000
ENV SERVER_PORT=10000
ENTRYPOINT ["java", "-Xmx256m", "-Dserver.port=10000", "-jar", "app.jar"]