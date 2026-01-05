#!/bin/bash
yum update -y

# Docker 설치
yum install -y docker
systemctl start docker
systemctl enable docker #서버 재부팅 시 docker 자동 실행
usermod -a -G docker ec2-user

# Docker Compose 설치
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Git 설치
yum install -y git

# 환경변수 설정 (RDS정보 자동설정)
echo "export DB_HOST=${db_endpoint}" >> /home/ec2-user/.bashrc
echo "export DB_NAME=${db_name}" >> /home/ec2-user/.bashrc

# 로그 디렉토리 생성, ec2-user가 로그파일 쓸 수 있게 설정
mkdir -p /var/log/unbox
chown ec2-user:ec2-user /var/log/unbox