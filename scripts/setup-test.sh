#!/bin/bash

# 스크립트 설정
set -e  # 에러 발생 시 스크립트 중단
set -u  # 정의되지 않은 변수 사용 시 에러

# 색상 출력 함수
print_success() { echo -e "\033[32m✅ $1\033[0m"; }
print_error() { echo -e "\033[31m❌ $1\033[0m"; }
print_info() { echo -e "\033[34mℹ️  $1\033[0m"; }

print_info "🧪 테스트 환경 설정 시작..."

# 기존 컨테이너 정리
print_info "기존 컨테이너 정리 중..."
docker-compose down 2>/dev/null || true

# 환경변수 설정
print_info "환경변수 설정 중..."
cp .env.test .env
source .env

print_info "Base URL: $BASE_URL"
print_info "Test Email: $TEST_EMAIL"

# 애플리케이션 빌드
print_info "애플리케이션 빌드 중..."
./gradlew clean build

# Docker 이미지 빌드
print_info "Docker 이미지 빌드 중..."
docker build -t unbox-app .

# 테스트 환경 실행
print_info "테스트 환경 실행 중..."
docker-compose -f docker-compose.test.yml up -d

# 헬스체크 대기
print_info "애플리케이션 시작 대기 중..."
sleep 10

# 헬스체크
if curl -s $BASE_URL/actuator/health > /dev/null 2>&1; then
    print_success "테스트 환경 준비 완료!"
    print_info "접속 URL: $BASE_URL"
    print_info "H2 콘솔: $BASE_URL/h2-console"
else
    print_error "헬스체크 실패. 로그를 확인하세요."
    docker logs unbox-test-app
    exit 1
fi
