# =============================================================================
# Security Groups - 네트워크 보안 규칙 정의
# AWS의 가상 방화벽 역할을 하는 보안 그룹 설정
# =============================================================================

# EC2 인스턴스용 보안 그룹
# - 웹 서버로 사용되는 EC2에 적용되는 보안 규칙
# - 외부에서 HTTP, HTTPS, SSH 접근 허용
resource "aws_security_group" "ec2" {
  name   = "${var.project_name}-ec2-sg"  # 보안 그룹 이름
  vpc_id = var.vpc_id                    # 어떤 VPC에 속하는지 지정

  # 인바운드 규칙 (Ingress) - 외부에서 들어오는 트래픽 제어
  
  # HTTP 트래픽 허용 (80번 포트)
  # - 웹 브라우저에서 HTTP로 접근 가능
  # - 모든 IP에서 접근 허용 (0.0.0.0/0)
  ingress {
    from_port   = 80                # 시작 포트
    to_port     = 80                # 끝 포트
    protocol    = "tcp"             # TCP 프로토콜
    cidr_blocks = ["0.0.0.0/0"]     # 모든 IP 주소에서 접근 허용
  }

  # Spring Boot 애플리케이션 포트 (8080번 포트)
  # - Swagger UI 및 REST API 엔드포인트 접근용
  # - 팀원들이 로컬에서 http://{EC2_IP}:8080/swagger-ui/index.html 접근
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]     # 팀원 로컬 접속용
  }

  # SSH 트래픽 허용 (22번 포트)
  # - 서버 관리를 위한 SSH 접속 허용
  # - 보안상 특정 IP만 허용하는 것이 좋음
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]     # 개발용: 모든 IP 허용 (프로덕션에서는 제한 필요)
  }

  # HTTPS 트래픽 허용 (443번 포트)
  # - SSL/TLS 암호화된 웹 트래픽 허용
  # - CloudFront나 로드밸런서에서 접근 시 사용
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]     # CloudFront에서 접근
  }

  # 아웃바운드 규칙 (Egress) - 서버에서 나가는 트래픽 제어
  # - 모든 아웃바운드 트래픽 허용 (인터넷 접근, API 호출 등)
  egress {
    from_port   = 0                 # 모든 포트
    to_port     = 0                 # 모든 포트
    protocol    = "-1"              # 모든 프로토콜
    cidr_blocks = ["0.0.0.0/0"]     # 모든 대상지
  }

  tags = {
    Name = "${var.project_name}-ec2-sg"
    Type = "Web-Server-Security-Group"
  }
}

# RDS 데이터베이스용 보안 그룹
# - 데이터베이스 접근을 EC2 인스턴스로만 제한
# - 외부에서 직접 DB 접근 차단으로 보안 강화
resource "aws_security_group" "rds" {
  name   = "${var.project_name}-rds-sg"
  vpc_id = var.vpc_id

  # PostgreSQL 포트 (5432) 접근 허용
  # - EC2 보안 그룹에서만 접근 허용 (security_groups 사용)
  # - IP 대신 보안 그룹 ID를 사용하여 더 안전한 접근 제어
  ingress {
    from_port       = 5432                        # PostgreSQL 기본 포트
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2.id] # EC2 보안 그룹에서만 접근 허용
  }

  # 아웃바운드 규칙은 기본적으로 모든 트래픽 차단
  # RDS는 일반적으로 아웃바운드 연결이 필요 없음

  tags = {
    Name = "${var.project_name}-rds-sg"
    Type = "Database-Security-Group"
  }
}

# 보안 모범 사례:
# 1. EC2 SSH 접근은 특정 IP만 허용 (예: 사무실 IP)
# 2. RDS는 EC2에서만 접근 가능하도록 설정
# 3. 불필요한 포트는 모두 차단
# 4. 정기적으로 보안 그룹 규칙 검토 및 업데이트