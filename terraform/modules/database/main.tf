# =============================================================================
# RDS (Relational Database Service) 구성
# PostgreSQL 데이터베이스 서비스 (조건부 생성)
# =============================================================================

# DB 서브넷 그룹 생성
# - RDS 인스턴스를 배치할 서브넷들을 그룹화
# - AWS RDS 요구사항: 최소 2개 AZ에 서브넷 필요
# - 실제 데이터베이스는 AZ-A에만 배치됨
resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet-group"  # 서브넷 그룹 이름
  subnet_ids = var.subnet_ids                        # Private 서브넷 ID 목록

  tags = {
    Name = "${var.project_name}-db-subnet-group"
    Type = "Database-Subnet-Group"
  }
}

# RDS PostgreSQL 인스턴스 생성
# - UNBOX 애플리케이션의 메인 데이터베이스
# - AZ-A의 Private 서브넷에만 배치 (비용 절약)
# - Single-AZ 구성으로 개발용 최적화
resource "aws_db_instance" "postgres" {
  # 스토리지 설정
  allocated_storage      = 20                    # 20GB SSD 스토리지 (프리티어 범위)
  storage_type          = "gp2"                  # 범용 SSD
  
  # 데이터베이스 엔진 설정
  engine                 = "postgres"            # PostgreSQL 사용
  engine_version         = "15.4"                # 최신 안정 버전으로 변경
  instance_class         = "db.t3.micro"         # 프리티어 대상 인스턴스 타입
  
  # 데이터베이스 설정
  db_name                = var.db_name           # 데이터베이스 이름
  username               = var.db_username       # 마스터 사용자명
  password               = var.db_password       # 마스터 비밀번호 (변수로 관리)
  
  # 네트워크 설정
  db_subnet_group_name   = aws_db_subnet_group.main.name  # 위에서 생성한 서브넷 그룹 사용
  vpc_security_group_ids = [var.security_group_id]        # RDS 전용 보안 그룹 연결
  publicly_accessible    = false                # 퍼블릭 접근 차단
  
  # 백업 설정 (최소화)
  backup_retention_period = 0                   # 백업 비활성화 (개발용)
  skip_final_snapshot    = true                 # 인스턴스 삭제 시 최종 스냅샷 생성 안 함
  delete_automated_backups = true              # 자동 백업 삭제
  
  # Single-AZ 구성 (비용 절약)
  multi_az               = false                # Multi-AZ 비활성화로 비용 절약
  
  # 성능 설정
  auto_minor_version_upgrade = false           # 자동 마이너 버전 업그레이드 비활성화
  
  tags = {
    Name = "${var.project_name}-postgres"
    Type = "Primary-Database"
    Engine = "PostgreSQL"
  }
}

# 주의사항:
# 1. 비밀번호는 프로덕션에서 더 복잡하게 설정해야 함
# 2. skip_final_snapshot=false로 설정하여 데이터 보호 강화 가능
# 3. Multi-AZ 배포를 위해 여러 서브넷에 배치
# 4. 비용 절약을 위해 개발 시 H2 DB 사용 권장