# ============================================
# ECR 전용 변수
# ============================================

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "github_org" {
  description = "GitHub organization or username"
  type        = string
  default     = "team4-coding-gamja"
}

variable "github_repo" {
  description = "GitHub repository name"
  type        = string
  default     = "UNBOX-BE"
}

variable "ecr_image_retention_count" {
  description = "Number of images to retain in ECR"
  type        = number
  default     = 10
}
