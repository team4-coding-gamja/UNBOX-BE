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