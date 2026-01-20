# Product Service Dockerfile
FROM openjdk:17-jre-slim

# 작업 디렉토리 설정
WORKDIR /app

# 애플리케이션 JAR 파일 복사
COPY build/libs/*.jar app.jar

# 로그 디렉토리 생성
RUN mkdir -p /app/logs

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]