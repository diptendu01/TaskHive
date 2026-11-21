# Build stage (Maven + Eclipse Temurin Java 21 LTS)
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage (Eclipse Temurin Java 21 LTS JRE)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache tini
ENTRYPOINT ["tini", "--", "java", "-jar", "/app/app.jar"]

COPY --from=builder /app/target/taskhive-backend-0.1.0.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
