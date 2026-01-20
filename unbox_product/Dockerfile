FROM bellsoft/liberica-openjdk-alpine:17

# 2. 컨테이너 내부 작업 디렉토리 설정
WORKDIR /app

# 3. 방금 성공적으로 만든 .jar 파일을 컨테이너 내부로 복사
# build/libs 폴더 안의 jar 파일을 app.jar라는 이름으로 가져옵니다.
COPY build/libs/*SNAPSHOT.jar app.jar

# 4. 앱 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]