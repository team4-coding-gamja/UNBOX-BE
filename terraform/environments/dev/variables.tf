variable "env" {
  description = "Environment (dev/prod)"
  type        = string
  default     = "dev"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "unbox"
}

variable "vpc_cidr" {
  description = "VPC CIDR block"
  type        = string
  default     = "10.1.0.0/16"
}

variable "availability_zones" {
  description = "List of availability zones"
  type        = list(string)
  default     = ["ap-northeast-2a", "ap-northeast-2c"]
}

variable "users" {
  description = "IAM Identity Center users"
  type = map(object({
    user_name   = string
    display     = string
    given_name  = string
    family_name = string
  }))
}

# Secrets Manager ARNs (낙균님 모듈 업데이트 후 사용)
variable "jwt_secret_arn" {
  description = "JWT Secret ARN in Secrets Manager"
  type        = string
  default     = ""
}

variable "user_db_password_secret_arn" {
  description = "User DB Password Secret ARN"
  type        = string
  default     = ""
}

variable "product_db_password_secret_arn" {
  description = "Product DB Password Secret ARN"
  type        = string
  default     = ""
}

variable "order_db_password_secret_arn" {
  description = "Order DB Password Secret ARN"
  type        = string
  default     = ""
}

variable "payment_db_password_secret_arn" {
  description = "Payment DB Password Secret ARN"
  type        = string
  default     = ""
}

variable "trade_db_password_secret_arn" {
  description = "Trade DB Password Secret ARN"
  type        = string
  default     = ""
}
