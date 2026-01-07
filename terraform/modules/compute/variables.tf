variable "project_name" {
  description = "unboxBe"
  type        = string
}

variable "public_key" {
  description = "SSH 공개키"
  type        = string
}

variable "subnet_id" {
  description = "서브넷 ID"
  type        = string
}

variable "security_group_id" {
  description = "보안그룹 ID"
  type        = string
}

variable "db_endpoint" {
  description = "RDS 엔드포인트"
  type        = string
}

variable "db_name" {
  description = "데이터베이스 이름"
  type        = string
}

variable "db_username" {
  description = "데이터베이스 사용자명"
  type        = string
}

variable "db_password" {
  description = "데이터베이스 비밀번호"
  type        = string
  sensitive   = true
}