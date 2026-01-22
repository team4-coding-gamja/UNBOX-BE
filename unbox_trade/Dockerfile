# ============================================
# Multi-stage build for Spring Boot Application
# ============================================

# Stage 1: Build
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy Gradle wrapper and settings first (least frequently changed)
COPY settings.gradle ./settings.gradle
COPY unbox_trade/gradle ./gradle

# Copy build files (changes less frequently than source code)
COPY unbox_trade/build.gradle ./build.gradle

# Copy common module (shared dependency)
COPY unbox_common ./unbox_common

# Download dependencies (cached layer - only re-runs if build files change)
RUN gradle dependencies --no-daemon || true

# Copy source code (changes most frequently - separate layer)
COPY unbox_trade/src ./src

# Build application
RUN gradle bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Create logs directory and set ownership
RUN mkdir -p logs && chown -R spring:spring logs && chown -R spring:spring /app

USER spring:spring

# Copy JAR from builder
COPY --from=builder /app/build/libs/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

# Expose port (will be overridden by environment variable)
EXPOSE 8080

# JVM options for container
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
