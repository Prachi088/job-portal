FROM maven:3.9-eclipse-temurin-23-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Force rebuild v3
RUN mvn package -DskipTests

FROM eclipse-temurin:23-jdk-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]