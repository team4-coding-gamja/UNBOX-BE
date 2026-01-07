#!/bin/bash
# Amazon Linux 2023용 초기화 스크립트

# 패키지 업데이트
yum update -y

# Docker 설치
yum install -y docker
systemctl start docker
systemctl enable docker
usermod -a -G docker ec2-user

# Docker Compose 설치
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Git 및 기본 도구 설치
yum install -y git curl

# AWS CLI 설치 (배포 시 필요)
yum install -y aws-cli

# 환경변수 설정 (RDS정보 자동설정)
echo "export DB_HOST=${db_endpoint}" >> /home/ec2-user/.bashrc
echo "export DB_NAME=${db_name}" >> /home/ec2-user/.bashrc
echo "export DB_USERNAME=${db_username}" >> /home/ec2-user/.bashrc
echo "export DB_PASSWORD=${db_password}" >> /home/ec2-user/.bashrc

# 로그 디렉토리 생성
mkdir -p /var/log/unbox
chown ec2-user:ec2-user /var/log/unbox

# Docker 그룹 권한 적용
newgrp docker