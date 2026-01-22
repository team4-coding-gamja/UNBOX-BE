# ============================================
# Multi-stage build for Spring Boot Application
# Order Service
# ============================================

# Stage 1: Build
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy Gradle files from service directory
# (Assumes build context is root)
COPY unbox_order/build.gradle ./unbox_order/build.gradle
COPY settings.gradle ./settings.gradle

# Copy common module
COPY unbox_common ./unbox_common

# Download dependencies (cached layer)
# We need to copy the project specific build.gradle to root correctly or adjust settings.
# Actually, the previous user dockerfile copy logic was:
# COPY unbox_user/build.gradle ./build.gradle  <- This renames it to ./build.gradle in the container
# This implies it treats the container root as the PROJECT root, which is weird for a composite build.
# BUT, let's look at unbox_user configuration again.
# It copies settings.gradle.
#
# If I use composite build, I should replicate the structure:
# /app/settings.gradle
# /app/unbox_common/
# /app/unbox_order/build.gradle
#
# Let's try to keep the structure clean.

# Copy Root settings (essential for composite)
COPY settings.gradle ./

# Copy Common Module
COPY unbox_common ./unbox_common

# Copy Target Module
COPY unbox_order ./unbox_order

# Copy other modules definition if necessary (gradle needs to know they exist even if empty?)
# settings.gradle includes 'unbox_product', 'unbox_trade', etc.
# If these dirs are missing, gradle might complain.
# However, usually we can ignore if we only build :unbox_order.

# Build application
# Using -x test to speed up and avoid environment issues
RUN gradle :unbox_order:bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Create logs directory and set ownership
RUN mkdir -p logs && chown -R spring:spring logs && chown -R spring:spring /app

USER spring:spring

# Copy JAR from builder
COPY --from=builder /app/unbox_order/build/libs/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT:-8084}/actuator/health || exit 1

# Expose port (will be overridden by environment variable)
EXPOSE 8084

# JVM options for container
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
