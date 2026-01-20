#!/bin/bash

# Product Service - Local Development Setup Script

echo "🚀 Product Service 로컬 개발 환경 설정을 시작합니다..."

# 현재 디렉토리 확인
if [ ! -f "docker/local/docker-compose.yml" ]; then
    echo "❌ docker/local/docker-compose.yml 파일을 찾을 수 없습니다."
    echo "   프로젝트 루트 디렉토리에서 실행해주세요."
    exit 1
fi

# .env 파일 생성 (없는 경우)
if [ ! -f "docker/local/.env" ]; then
    echo "📝 .env 파일을 생성합니다..."
    cp docker/local/.env.example docker/local/.env
    echo "✅ .env 파일이 생성되었습니다. 필요한 값들을 수정해주세요."
else
    echo "✅ .env 파일이 이미 존재합니다."
fi

# logs 디렉토리 생성
mkdir -p logs

# Docker 컨테이너 정리 (기존 컨테이너가 있는 경우)
echo "🧹 기존 컨테이너를 정리합니다..."
docker-compose -f docker/local/docker-compose.yml down -v

# Docker 이미지 빌드 및 컨테이너 시작
echo "🐳 Docker 컨테이너를 시작합니다..."
docker-compose -f docker/local/docker-compose.yml up -d --build

# 서비스 상태 확인
echo "⏳ 서비스 시작을 기다리는 중..."
sleep 10

# 헬스 체크
echo "🔍 서비스 상태를 확인합니다..."
if curl -f http://localhost:8082/actuator/health > /dev/null 2>&1; then
    echo "✅ Product Service가 정상적으로 시작되었습니다!"
    echo "🌐 Product Service: http://localhost:8082"
else
    echo "⚠️  서비스 시작 중이거나 문제가 있을 수 있습니다."
    echo "   로그를 확인해보세요: docker-compose -f docker/local/docker-compose.yml logs"
fi

echo ""
echo "📋 유용한 명령어들:"
echo "   로그 확인: docker-compose -f docker/local/docker-compose.yml logs -f"
echo "   서비스 중지: docker-compose -f docker/local/docker-compose.yml down"
echo "   데이터베이스 접속: docker exec -it unbox-product-postgres psql -U unbox_user -d unbox_product_local"
echo ""