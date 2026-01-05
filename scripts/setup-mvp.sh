#!/bin/bash

set -e
set -u

print_success() { echo -e "\033[32m✅ $1\033[0m"; }
print_error() { echo -e "\033[31m❌ $1\033[0m"; }
print_info() { echo -e "\033[34mℹ️  $1\033[0m"; }

print_info "🚀 MVP 환경 설정 시작..."

# Terraform 상태 확인
if ! command -v terraform &> /dev/null; then
    print_error "Terraform이 설치되지 않았습니다."
    exit 1
fi

# AWS 인프라 생성
print_info "AWS 인프라 생성 중..."
cd terraform/environments/dev
terraform init -upgrade
terraform apply -var="use_rds=true" -auto-approve

# 인프라 정보 확인
RDS_ENDPOINT=$(terraform output -raw rds_endpoint)
EC2_IP=$(terraform output -raw ec2_public_ip)

print_info "RDS Endpoint: $RDS_ENDPOINT"
print_info "EC2 Public IP: $EC2_IP"

# 프로젝트 루트로 이동
cd ../../../

# .env.mvp 파일 업데이트
print_info "환경변수 파일 업데이트 중..."
cp .env.mvp .env.mvp.backup  # 백업
sed -i "s/RDS_ENDPOINT_HERE/$RDS_ENDPOINT/g" .env.mvp
sed -i "s/EC2-PUBLIC-IP/$EC2_IP/g" .env.mvp

# 환경변수 로드
cp .env.mvp .env
source .env

print_info "Base URL: $BASE_URL"
print_info "Demo Email: $DEMO_EMAIL"

# 로컬에서 빌드
print_info "애플리케이션 빌드 중..."
./gradlew clean build

# EC2에 배포
print_info "EC2에 배포 중..."
scp -i ~/.ssh/unbox-key.pem .env ec2-user@$EC2_IP:~/UNBOX-BE/
scp -i ~/.ssh/unbox-key.pem build/libs/*.jar ec2-user@$EC2_IP:~/UNBOX-BE/build/libs/

# EC2에서 실행
print_info "EC2에서 애플리케이션 실행 중..."
ssh -i ~/.ssh/unbox-key.pem ec2-user@$EC2_IP << 'EOF'
cd UNBOX-BE
docker build -t unbox-app .
docker-compose -f docker-compose.mvp.yml up -d
EOF

# 헬스체크 대기
print_info "애플리케이션 시작 대기 중..."
sleep 20

# 헬스체크
if curl -s $BASE_URL/actuator/health > /dev/null 2>&1; then
    print_success "MVP 환경 준비 완료!"
    print_info "접속 URL: $BASE_URL"
    print_info "RDS Endpoint: $RDS_ENDPOINT"
else
    print_error "헬스체크 실패. EC2 로그를 확인하세요."
    ssh -i ~/.ssh/unbox-key.pem ec2-user@$EC2_IP "docker logs unbox-mvp-app"
    exit 1
fi
