FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8080

# 환경변수를 활용한 JVM 옵션 적용
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar app.jar"]