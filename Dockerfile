# AWS MVP 배포용 Dockerfile (간소화)
# 기본적인 프로덕션 설정

FROM bellsoft/liberica-openjdk-alpine:17

# 필수 패키지 설치
RUN apk add --no-cache curl tzdata

# 시간대 설정
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime

# 애플리케이션 디렉토리
WORKDIR /app

# 로그 디렉토리 생성
RUN mkdir -p /var/log/unbox

# JAR 파일 복사
COPY build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# JVM 옵션 설정
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# 헬스체크 스크립트
RUN echo '#!/bin/sh' > /app/healthcheck.sh && \
    echo 'curl -f http://localhost:8080/actuator/health || exit 1' >> /app/healthcheck.sh && \
    chmod +x /app/healthcheck.sh

# 헬스체크 설정
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD /app/healthcheck.sh

# 애플리케이션 실행
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]