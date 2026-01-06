#!/bin/bash

# UNBOX MVP AWS 배포 스크립트
# EC2 + RDS + Redis 구성으로 Swagger UI까지 배포

set -e
set -u

# 색상 출력 함수
print_success() { echo -e "\033[32m✅ $1\033[0m"; }
print_error() { echo -e "\033[31m❌ $1\033[0m"; }
print_info() { echo -e "\033[34mℹ️  $1\033[0m"; }
print_warning() { echo -e "\033[33m⚠️  $1\033[0m"; }

print_info "🚀 UNBOX MVP AWS 배포 시작..."

# 필수 도구 확인
if ! command -v terraform &> /dev/null; then
    print_error "Terraform이 설치되지 않았습니다."
    exit 1
fi

if ! command -v aws &> /dev/null; then
    print_error "AWS CLI가 설치되지 않았습니다."
    exit 1
fi

# SSH 키 확인
if [ ! -f ~/.ssh/unbox_key ]; then
    print_error "SSH 키가 없습니다. ssh-keygen -t rsa -b 4096 -f ~/.ssh/unbox_key 실행하세요."
    exit 1
fi

# terraform.tfvars 확인
if [ ! -f terraform/environments/dev/terraform.tfvars ]; then
    print_error "terraform.tfvars 파일이 없습니다. terraform.tfvars.example을 복사하여 설정하세요."
    exit 1
fi

# AWS 인프라 배포
print_info "AWS 인프라 배포 중..."
cd terraform/environments/dev

# Terraform 초기화 및 배포
terraform init -upgrade
terraform plan -var="use_rds=true"
terraform apply -var="use_rds=true" -auto-approve

# 인프라 정보 추출
EC2_PUBLIC_IP=$(terraform output -raw ec2_public_ip)
if [ "$?" -ne 0 ]; then
    print_error "EC2 Public IP를 가져올 수 없습니다."
    exit 1
fi

# RDS 엔드포인트 추출 (조건부)
RDS_ENDPOINT=""
if terraform output database_info &> /dev/null; then
    RDS_ENDPOINT=$(terraform output -json database_info | jq -r '.endpoint // empty')
fi

print_success "인프라 배포 완료!"
print_info "EC2 Public IP: $EC2_PUBLIC_IP"
if [ -n "$RDS_ENDPOINT" ]; then
    print_info "RDS Endpoint: $RDS_ENDPOINT"
else
    print_warning "RDS를 사용하지 않습니다 (H2 DB 사용)"
fi

cd ../../../

# .env.mvp 파일 업데이트
print_info "환경변수 파일 업데이트 중..."
cp .env.mvp .env.mvp.backup

# RDS 엔드포인트 업데이트 (있는 경우만)
if [ -n "$RDS_ENDPOINT" ]; then
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s|RDS_ENDPOINT_HERE|$RDS_ENDPOINT|g" .env.mvp
    else
        sed -i "s|RDS_ENDPOINT_HERE|$RDS_ENDPOINT|g" .env.mvp
    fi
    print_info "RDS 엔드포인트 업데이트 완료"
fi

# 애플리케이션 빌드
print_info "Spring Boot 애플리케이션 빌드 중..."
./gradlew clean build -x test

if [ ! -f build/libs/*.jar ]; then
    print_error "JAR 파일 빌드에 실패했습니다."
    exit 1
fi

print_success "애플리케이션 빌드 완료!"

# EC2 접속 대기
print_info "EC2 인스턴스 준비 대기 중... (30초)"
sleep 30

# SSH 키 권한 설정
chmod 600 ~/.ssh/unbox_key

# EC2에 파일 전송
print_info "EC2에 파일 전송 중..."

# 디렉토리 생성
ssh -i ~/.ssh/unbox_key -o StrictHostKeyChecking=no ec2-user@$EC2_PUBLIC_IP "mkdir -p ~/UNBOX-BE/{build/libs,logs}"

# 필요한 파일들 전송
scp -i ~/.ssh/unbox_key .env.mvp ec2-user@$EC2_PUBLIC_IP:~/UNBOX-BE/.env
scp -i ~/.ssh/unbox_key docker-compose-mvp.yml ec2-user@$EC2_PUBLIC_IP:~/UNBOX-BE/
scp -i ~/.ssh/unbox_key Dockerfile ec2-user@$EC2_PUBLIC_IP:~/UNBOX-BE/
scp -i ~/.ssh/unbox_key build/libs/*.jar ec2-user@$EC2_PUBLIC_IP:~/UNBOX-BE/build/libs/

print_success "파일 전송 완료!"

# EC2에서 Docker 애플리케이션 실행
print_info "EC2에서 Docker 애플리케이션 시작 중..."
ssh -i ~/.ssh/unbox_key ec2-user@$EC2_PUBLIC_IP << 'EOF'
cd ~/UNBOX-BE

# Docker 서비스 시작
sudo systemctl start docker
sudo systemctl enable docker

# 기존 컨테이너 정리
docker-compose -f docker-compose-mvp.yml down 2>/dev/null || true
docker system prune -f

# 환경변수 로드
source .env

# Docker 이미지 빌드
echo "Docker 이미지 빌드 중..."
docker build -t unbox-app .

# Docker Compose로 애플리케이션 시작
echo "애플리케이션 시작 중..."
docker-compose -f docker-compose-mvp.yml up -d

# 컨테이너 상태 확인
echo "컨테이너 상태:"
docker ps
EOF

# 애플리케이션 시작 대기
print_info "애플리케이션 시작 대기 중... (90초)"
sleep 90

# 헬스체크 및 Swagger UI 확인
HEALTH_URL="http://$EC2_PUBLIC_IP:8080/actuator/health"
SWAGGER_URL="http://$EC2_PUBLIC_IP:8080/swagger-ui/index.html"

print_info "헬스체크 시작..."
for i in {1..5}; do
    if curl -s --connect-timeout 10 "$HEALTH_URL" | grep -q '"status":"UP"'; then
        print_success "🎉 MVP 배포 완료!"
        echo ""
        print_info "📋 접속 정보:"
        print_info "🌐 Swagger UI: $SWAGGER_URL"
        print_info "📊 헬스체크: $HEALTH_URL"
        print_info "🔗 SSH 접속: ssh -i ~/.ssh/unbox_key ec2-user@$EC2_PUBLIC_IP"
        echo ""
        print_info "📝 로그 확인 방법:"
        print_info "   docker logs -f unbox-mvp-app"
        print_info "   docker logs -f unbox-mvp-redis"
        echo ""
        exit 0
    fi
    print_warning "헬스체크 재시도 $i/5..."
    sleep 30
done

# 헬스체크 실패 시 로그 확인
print_error "헬스체크 실패. 로그를 확인합니다..."
ssh -i ~/.ssh/unbox_key ec2-user@$EC2_PUBLIC_IP "cd ~/UNBOX-BE && docker-compose -f docker-compose-mvp.yml logs --tail=50"

print_error "배포에 실패했습니다. 위 로그를 확인하세요."
exit 1