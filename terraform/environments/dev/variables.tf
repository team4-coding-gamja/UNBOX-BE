# =============================================================================
# UNBOX 백엔드 개발 환경 변수
# 팀원들이 로컬에서 인프라에 접속할 때 사용하는 설정값들
# =============================================================================

# 프로젝트 기본 이름
# - 모든 AWS 리소스 이름에 사용되는 접두사
# - 리소스 구분 및 관리를 위한 명명 규칙
variable "project_name" {
  description = "UNBOX 백엔드 프로젝트 이름"
  type        = string
  default     = "unbox-mvp"  # MVP 버전으로 설정
}

# SSH 접속용 공개키
# - EC2 인스턴스에 안전하게 접속하기 위한 SSH 키
# - 인프라 배포 전에 반드시 설정 필요
# - 사용법: ssh-keygen으로 키 생성 후 .pub 파일 내용 입력
variable "public_key" {
  description = "EC2 SSH 접속용 공개키 (필수)"
  type        = string
  # 기본값 없음 - 반드시 terraform.tfvars에서 설정 필요
}

# RDS 데이터베이스 사용 여부
# - true: AWS RDS PostgreSQL 사용 (비용 발생)
# - false: H2 인메모리 DB 사용 (무료, 개발용 추천)
variable "use_rds" {
  description = "RDS PostgreSQL 사용 여부 (false=H2 DB 사용)"
  type        = bool
  default     = false  # 기본적으로 H2 DB 사용으로 비용 절약
}