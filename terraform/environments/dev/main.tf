# =============================================================================
# Terraform 설정 및 Provider 구성
# =============================================================================

# Terraform 버전 및 필수 Provider 정의
terraform {
  required_providers {
    # AWS Provider: AWS 리소스 관리
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"  # 5.x 버전 사용
    }
    # Random Provider: 랜덤 문자열 생성 (S3 버킷명 등에 사용)
    random = {
      source  = "hashicorp/random"
      version = "~> 3.1"
    }
  }
}

# AWS Provider 설정
provider "aws" {
  region = "ap-northeast-2"  # 서울 리전 사용
}

# =============================================================================
# 모듈 호출 - 각 인프라 컴포넌트를 모듈로 분리하여 관리
# =============================================================================

# VPC 모듈: 네트워크 인프라 구성
# - VPC, 서브넷, 인터넷 게이트웨이, 라우팅 테이블 생성
module "vpc" {
  source = "../../modules/vpc"
  
  project_name = var.project_name  # 프로젝트명을 모든 리소스 이름에 사용
}

# Security 모듈: 보안 그룹 설정
# - EC2용 보안 그룹 (HTTP, HTTPS, SSH 허용)
# - RDS용 보안 그룹 (EC2에서만 접근 허용)
module "security" {
  source = "../../modules/security"
  
  project_name = var.project_name
  vpc_id       = module.vpc.vpc_id  # VPC 모듈에서 생성된 VPC ID 참조
}

# Database 모듈: RDS PostgreSQL (조건부 생성)
# - use_rds 변수가 true일 때만 생성
# - 개발 환경에서는 H2 DB 사용으로 비용 절약 가능
module "database" {
  count  = var.use_rds ? 1 : 0  # 조건부 생성: use_rds가 true일 때만 생성
  source = "../../modules/database"
  
  project_name      = var.project_name
  subnet_ids        = module.vpc.private_subnet_ids  # Private 서브넷에 DB 배치
  security_group_id = module.security.rds_sg_id      # RDS 전용 보안 그룹 사용
  
  # DB 설정 (변수로 관리)
  db_name     = var.db_name
  db_username = var.db_username
  db_password = var.db_password
}

# Compute 모듈: EC2 인스턴스 및 관련 리소스
# - 애플리케이션 서버 역할
# - Docker 환경 자동 구성
# - Elastic IP로 고정 공인 IP 제공
module "compute" {
  source = "../../modules/compute"
  
  project_name      = var.project_name
  public_key        = var.public_key                # SSH 접속용 공개키
  subnet_id         = module.vpc.public_subnet_id   # Public 서브넷에 배치
  security_group_id = module.security.ec2_sg_id     # EC2 전용 보안 그룹 사용
  
  # 데이터베이스 정보 (user_data.sh에서 사용)
  db_endpoint = var.use_rds ? try(module.database[0].db_endpoint, "localhost") : "localhost"
  db_name     = var.db_name
  db_username = var.db_username
  db_password = var.db_password
}

# Storage 모듈: S3 버킷 (사용 안 함)
# - 로그는 EC2에서 직접 Docker 로그로 확인
module "storage" {
  source = "../../modules/storage"
  
  project_name = var.project_name
}