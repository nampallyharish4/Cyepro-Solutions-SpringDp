# Multi-stage build for Spring Boot backend
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Cache dependencies first for faster rebuilds
COPY pom.xml ./
COPY .mvn ./.mvn
COPY mvnw ./mvnw
RUN chmod +x mvnw
RUN ./mvnw -DskipTests dependency:go-offline

# Build application
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/cyepro-stack2-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 5000
CMD ["java", "-jar", "/app/app.jar"]
