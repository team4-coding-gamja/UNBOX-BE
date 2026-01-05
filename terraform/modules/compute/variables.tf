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