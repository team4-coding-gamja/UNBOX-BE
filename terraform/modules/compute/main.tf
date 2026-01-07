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
  ami                    = "ami-0c2acfcb2ac4d02a0"  # Amazon Linux 2023 AMI (ap-northeast-2)
  instance_type          = "t3.micro"               # 프리티어 대상 인스턴스 타입
  key_name               = aws_key_pair.main.key_name
  subnet_id              = var.subnet_id            # Public 서브넷에 배치
  vpc_security_group_ids = [var.security_group_id]  # 보안 그룹 연결

  # User Data 스크립트
  # - 인스턴스 최초 시작 시 자동으로 실행되는 스크립트
  # - Docker, Docker Compose 설치 및 환경 설정
  user_data = base64encode(templatefile("${path.module}/user_data.sh", {
    db_endpoint = var.db_endpoint
    db_name     = var.db_name
    db_username = var.db_username
    db_password = var.db_password
  }))

  tags = {
    Name = "${var.project_name}-web"
    Type = "Application-Server"
  }
}

