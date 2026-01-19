# IAM 모듈 변수

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "ecr_repository_arns" {
  description = "List of ECR repository ARNs"
  type        = list(string)
  default     = []
}

variable "enable_additional_permissions" {
  description = "Enable additional permissions for ECS, etc."
  type        = bool
  default     = false
}

variable "common_tags" {
  description = "Common tags to apply to all resources"
  type        = map(string)
  default     = {}
}

# 팀원 관리 변수
variable "team_members" {
  description = "Map of team members with their roles and settings"
  type = map(object({
    role              = string  # cicd-admin, developer
    team              = string  # backend, infrastructure, monitoring
    create_access_key = bool    # Access Key 생성 여부
  }))
  default = {}
}

variable "enable_team_access" {
  description = "Enable team member access management"
  type        = bool
  default     = false
}