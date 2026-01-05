variable "project_name" {
  description = "unboxBe"
  type        = string
  default     = "unbox-mvp"
}

variable "public_key" {
  description = "SSH 공개키"
  type        = string
}

variable "use_rds" {
  description = "RDS 사용 여부 (false면 H2 사용)"
  type        = bool
  default     = false
}