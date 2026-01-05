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
    
    # 환경 변수 설정
    # H2 데이터베이스 사용 설정 (비용 절약)
    echo "export USE_H2_DATABASE=true" >> /home/ec2-user/.bashrc
    echo "export REDIS_HOST=localhost" >> /home/ec2-user/.bashrc
  EOF
  )

  tags = {
    Name = "${var.project_name}-web"
    Type = "Application-Server"
  }
}

# =============================================================================
# Elastic IP (EIP) - 고정 퍼블릭 IP 주소
# =============================================================================

# EC2 인스턴스에 고정 퍼블릭 IP를 할당하는 Elastic IP
# 용도: 
# 1. EC2 재시작/교체 시에도 동일한 IP 주소 유지
# 2. 프론트엔드에서 백엔드 API 호출 시 고정 엔드포인트 사용
# 3. DNS 설정 또는 외부 서비스 연동 시 IP 변경 걱정 없음
resource "aws_eip" "web" {
  instance = aws_instance.web.id  # EC2 인스턴스에 연결
  domain   = "vpc"                # VPC 내에서 사용
  
  tags = {
    Name = "${var.project_name}-web-eip"
    Type = "Web-Server-EIP"
  }
  
  # EC2 인스턴스가 완전히 시작된 후 EIP 연결
  # 이렇게 하지 않으면 EC2 생성 중에 EIP 연결을 시도하여 에러 발생 가능
  depends_on = [aws_instance.web]
}

# EIP 사용 시 주의사항:
# 1. EIP가 EC2에 연결되어 사용 중일 때: 무료
# 2. EIP가 생성되었지만 사용하지 않을 때: 시간당 과금
# 3. EC2 삭제 시 EIP도 함께 삭제해야 과금 방지