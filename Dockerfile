# Multi-stage Dockerfile for FreightFox Transporter Optimizer
# Stage 1: Build application with Maven and Eclipse Temurin JDK 21
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Copy pom.xml and source code
COPY pom.xml .
COPY src src

# Package executable jar without running tests (tests are verified in CI pipeline)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime image using lightweight JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /workspace/target/transporter-optimizer-*.jar app.jar

ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
