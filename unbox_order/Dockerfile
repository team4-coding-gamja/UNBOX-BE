# ============================================
# Multi-stage build for Spring Boot Application
# ============================================

# Stage 1: Build
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# 변경 빈도가 낮은 파일부터 복사 (캐시 최적화)
COPY settings.gradle ./settings.gradle
COPY unbox_order/build.gradle ./build.gradle

# 공통 모듈 복사
COPY unbox_common ./unbox_common

# 의존성 다운로드 (캐시 레이어)
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사 (가장 자주 변경됨)
COPY unbox_order/src ./src

# 빌드
RUN gradle bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
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
