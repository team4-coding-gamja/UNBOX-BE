# =============================================================================
# EC2 인스턴스 및 관련 리소스 구성
# 애플리케이션 서버 역할을 하는 컴퓨팅 리소스
# =============================================================================

# SSH 키 페어 생성
# - EC2 인스턴스에 안전하게 접속하기 위한 SSH 공개키 등록
# - 비밀번호 대신 키 기반 인증 사용으로 보안 강화
resource "aws_key_pair" "main" {
  key_name   = "${var.project_name}-key"  # AWS에서 사용할 키 이름
  public_key = var.public_key             # 사용자가 제공한 SSH 공개키
}

# EC2 인스턴스 생성
# - UNBOX 애플리케이션을 실행할 메인 서버
# - Docker 환경이 자동으로 설치되어 컴테이너 기반 배포 가능
resource "aws_instance" "web" {
  ami                    = "ami-0c2d3e23e757b5d84"  # Amazon Linux 2023 AMI (ap-northeast-2)
  instance_type          = "t3.micro"               # 프리티어 대상 인스턴스 타입
  key_name               = aws_key_pair.main.key_name
  subnet_id              = var.subnet_id            # Public 서브넷에 배치
  vpc_security_group_ids = [var.security_group_id]  # 보안 그룹 연결

  # User Data 스크립트
  # - 인스턴스 최초 시작 시 자동으로 실행되는 스크립트
  # - Docker, Docker Compose, Git 설치 및 환경 설정
  # - Redis 컴테이너 자동 시작으로 캐시 서버 준비
  user_data = base64encode(<<-EOF
    #!/bin/bash
    # 시스템 업데이트
    yum update -y
    
    # Docker 설치 및 시작
    yum install -y docker
    systemctl start docker      # Docker 서비스 시작
    systemctl enable docker     # 부팅 시 자동 시작 설정
    usermod -a -G docker ec2-user  # ec2-user를 docker 그룹에 추가
    
    # Docker Compose 설치
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose  # 실행 권한 부여
    
    # Git 설치 (소스 코드 다운로드용)
    yum install -y git
    
    # Redis 컨테이너 시작 (캐시 서버용)
    # - 애플리케이션에서 세션 및 캐시 데이터 저장용
    # - 6379 포트로 로컬에서 접속 가능
    docker run -d --name redis --restart unless-stopped -p 6379:6379 redis:7-alpine
    # curl 설치 (헬스체크용)
    yum install -y curl
    
    # 환경 변수 설정
    # - 애플리케이션 설정용 환경변수
    echo "export USE_H2_DATABASE=false" >> /home/ec2-user/.bashrc    # RDS 사용 설정
    echo "export REDIS_HOST=redis" >> /home/ec2-user/.bashrc         # Docker Compose 내 Redis 서비스명
    echo "export REDIS_PORT=6379" >> /home/ec2-user/.bashrc          # Redis 포트
    
    # Docker 그룹 권한 적용을 위한 재로그인 없이 바로 사용 가능하도록 설정
    newgrp docker
  EOF
  )

  tags = {
    Name = "${var.project_name}-web"
    Type = "Application-Server"
  }
}

