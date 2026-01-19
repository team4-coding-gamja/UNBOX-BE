# 모니터링 모듈 변수

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "monthly_budget_limit" {
  description = "Monthly budget limit in USD"
  type        = number
  default     = 100
}

variable "daily_budget_limit" {
  description = "Daily budget limit in USD (for dev environment)"
  type        = number
  default     = 10
}

variable "alert_email_addresses" {
  description = "List of email addresses for cost alerts"
  type        = list(string)
  default     = []
}

variable "anomaly_threshold" {
  description = "Cost anomaly detection threshold in USD"
  type        = number
  default     = 20
}

variable "common_tags" {
  description = "Common tags to apply to all resources"
  type        = map(string)
  default     = {}
}