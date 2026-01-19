# 멀티 스테이지 빌드 - 빌드 스테이지
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Gradle 설정 파일들 복사
COPY build.gradle settings.gradle ./
COPY core-business/build.gradle ./core-business/
COPY product-service/build.gradle ./product-service/

# 의존성 다운로드 (캐시 최적화)
RUN gradle dependencies --no-daemon

# 소스 코드 복사 (현재는 빈 상태)
COPY product-service/src ./product-service/src

# 애플리케이션 빌드
RUN gradle :product-service:bootJar --no-daemon

# 런타임 스테이지
FROM openjdk:17-jre-slim

WORKDIR /app

# 애플리케이션 사용자 생성
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 빌드된 JAR 파일 복사
COPY --from=builder /app/product-service/build/libs/*.jar app.jar

# 권한 설정
RUN chown appuser:appuser app.jar

# 사용자 변경
USER appuser

# 포트 노출
EXPOSE 8081

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]