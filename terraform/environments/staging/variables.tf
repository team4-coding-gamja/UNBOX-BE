# ============================================
# 공통 변수
# ============================================

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

# ============================================
# User Service 변수
# ============================================

variable "user_service_image_tag" {
  description = "User service Docker image tag"
  type        = string
  default     = "latest"
}

variable "user_service_cpu" {
  description = "CPU units for user service (256, 512, 1024, etc.)"
  type        = number
  default     = 256
}

variable "user_service_memory" {
  description = "Memory in MB for user service"
  type        = number
  default     = 512
}

variable "user_service_desired_count" {
  description = "Desired number of user service tasks"
  type        = number
  default     = 2
}

variable "user_db_password" {
  description = "User service database password"
  type        = string
  sensitive   = true
}

variable "user_db_password_secret_arn" {
  description = "ARN of AWS Secrets Manager secret for user DB password"
  type        = string
}

variable "jwt_secret_arn" {
  description = "ARN of AWS Secrets Manager secret for JWT secret"
  type        = string
}

# ============================================
# Product Service 변수
# ============================================

variable "product_service_image_tag" {
  description = "Product service Docker image tag"
  type        = string
  default     = "latest"
}

variable "product_service_cpu" {
  description = "CPU units for product service"
  type        = number
  default     = 256
}

variable "product_service_memory" {
  description = "Memory in MB for product service"
  type        = number
  default     = 512
}

variable "product_service_desired_count" {
  description = "Desired number of product service tasks"
  type        = number
  default     = 2
}

variable "product_db_password" {
  description = "Product service database password"
  type        = string
  sensitive   = true
}

variable "product_db_password_secret_arn" {
  description = "ARN of AWS Secrets Manager secret for product DB password"
  type        = string
}

# ============================================
# Order Service 변수
# ============================================

variable "order_service_image_tag" {
  description = "Order service Docker image tag"
  type        = string
  default     = "latest"
}

variable "order_service_cpu" {
  description = "CPU units for order service"
  type        = number
  default     = 256
}

variable "order_service_memory" {
  description = "Memory in MB for order service"
  type        = number
  default     = 512
}

variable "order_service_desired_count" {
  description = "Desired number of order service tasks"
  type        = number
  default     = 2
}

variable "order_db_password" {
  description = "Order service database password"
  type        = string
  sensitive   = true
}

variable "order_db_password_secret_arn" {
  description = "ARN of AWS Secrets Manager secret for order DB password"
  type        = string
}

# ============================================
# Payment Service 변수
# ============================================

variable "payment_service_image_tag" {
  description = "Payment service Docker image tag"
  type        = string
  default     = "latest"
}

variable "payment_service_cpu" {
  description = "CPU units for payment service"
  type        = number
  default     = 256
}

variable "payment_service_memory" {
  description = "Memory in MB for payment service"
  type        = number
  default     = 512
}

variable "payment_service_desired_count" {
  description = "Desired number of payment service tasks"
  type        = number
  default     = 2
}

variable "payment_db_password" {
  description = "Payment service database password"
  type        = string
  sensitive   = true
}

variable "payment_db_password_secret_arn" {
  description = "ARN of AWS Secrets Manager secret for payment DB password"
  type        = string
}

# ============================================
# Trade Service 변수
# ============================================

variable "trade_service_image_tag" {
  description = "Trade service Docker image tag"
  type        = string
  default     = "latest"
}

variable "trade_service_cpu" {
  description = "CPU units for trade service"
  type        = number
  default     = 256
}

variable "trade_service_memory" {
  description = "Memory in MB for trade service"
  type        = number
  default     = 512
}

variable "trade_service_desired_count" {
  description = "Desired number of trade service tasks"
  type        = number
  default     = 2
}

variable "trade_db_password" {
  description = "Trade service database password"
  type        = string
  sensitive   = true
}

variable "trade_db_password_secret_arn" {
  description = "ARN of AWS Secrets Manager secret for trade DB password"
  type        = string
}
