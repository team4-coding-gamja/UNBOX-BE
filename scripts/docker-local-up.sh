#!/bin/bash

# 로컬 개발 환경 Docker 실행 스크립트

echo "🚀 Starting local development environment..."

# 프로젝트 루트로 이동
cd "$(dirname "$0")/.."

# 기존 컨테이너 정리
echo "🧹 Cleaning up existing containers..."
docker-compose -f docker/local/docker-compose.yml down

# 이미지 빌드 및 컨테이너 실행
echo "🔨 Building and starting services..."
docker-compose -f docker/local/docker-compose.yml up --build -d

# 서비스 상태 확인
echo "⏳ Waiting for services to be ready..."
sleep 10

echo "📊 Service status:"
docker-compose -f docker/local/docker-compose.yml ps

echo ""
echo "✅ Local development environment is ready!"
echo ""
echo "🌐 Services:"
echo "  - Core Business API: http://localhost:8080"
echo "  - Product Service API: http://localhost:8081"
echo "  - PostgreSQL: localhost:5432"
echo "  - Redis: localhost:6379"
echo ""
echo "📝 To view logs:"
echo "  docker-compose -f docker/local/docker-compose.yml logs -f"
echo ""
echo "🛑 To stop:"
echo "  docker-compose -f docker/local/docker-compose.yml down"