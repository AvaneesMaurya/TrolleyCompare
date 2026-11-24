
# Stage 1: Build Spring Boot application
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy dependency definition first
COPY pom.xml .

# Download dependencies separately.
# Docker can reuse this layer if pom.xml hasn't changed.
RUN mvn -B -ntp dependency:go-offline

# Copy application source
COPY src ./src

# Build application without running tests again.
# Tests are already handled by GitHub Actions CI.
RUN mvn -B -ntp -DskipTests package

# Stage 2: Lightweight runtime
FROM eclipse-temurin:17-jre

WORKDIR /app

# Run as non-root user
RUN useradd --system --uid 10001 appuser

# Copy only the executable JAR
COPY --from=build --chown=10001:10001 \
    /app/target/*.jar app.jar

USER 10001

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-jar", "app.jar"]
