#!/bin/bash

print_success() { echo -e "\033[32m✅ $1\033[0m"; }
print_info() { echo -e "\033[34mℹ️  $1\033[0m"; }

print_info "🧹 환경 정리 시작..."

# Docker 컨테이너 정리
print_info "Docker 컨테이너 정리 중..."
docker-compose down 2>/dev/null || true
docker-compose -f docker-compose.test.yml down 2>/dev/null || true
docker-compose -f docker-compose.local.yml down 2>/dev/null || true
docker-compose -f docker-compose.mvp.yml down 2>/dev/null || true

# Docker 볼륨 정리 (선택사항)
read -p "Docker 볼륨도 삭제하시겠습니까? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    docker volume prune -f
    print_info "Docker 볼륨 정리 완료"
fi

# AWS 인프라 정리 (선택사항)
read -p "AWS 인프라도 삭제하시겠습니까? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    cd terraform/environments/dev
    terraform destroy -auto-approve
    cd ../../../
    print_info "AWS 인프라 정리 완료"
fi

print_success "환경 정리 완료!"
