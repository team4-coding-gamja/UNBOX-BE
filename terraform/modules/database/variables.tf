variable "project_name" {
  description = "unboxBe"
  type        = string
}

variable "subnet_ids" {
  description = "DB 서브넷 ID 리스트"
  type        = list(string)
}

variable "security_group_id" {
  description = "RDS 보안그룹 ID"
  type        = string
}

variable "db_name" {
  description = "데이터베이스 이름"
  type        = string
  default     = "unboxdb"
}

variable "db_username" {
  description = "데이터베이스 마스터 사용자명"
  type        = string
  default     = "admin"
}

variable "db_password" {
  description = "데이터베이스 마스터 비밀번호"
  type        = string
  sensitive   = true
}