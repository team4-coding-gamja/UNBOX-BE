# STAGING 환경 ECR 설정

module "ecr" {
  source = "../../modules/ecr"
  
  repository_names = [
    "unbox-core-business",
    "unbox-product-service"
  ]
  
  environment = "staging"
  
  # STAGING 환경 설정
  image_tag_mutability = "MUTABLE"
  scan_on_push         = true
  encryption_type      = "AES256"
  max_image_count      = 10  # STAGING은 더 많이 보관
  untagged_image_days  = 1
  
  # GitHub Actions에서 접근할 수 있도록 권한 설정
  allowed_principals = [
    "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
  ]
  
  common_tags = {
    Project     = "unbox"
    Environment = "staging"
    ManagedBy   = "terraform"
    Team        = "backend"
  }
}

# 현재 AWS 계정 정보
data "aws_caller_identity" "current" {}

# ECR 정보를 다른 리소스에서 사용할 수 있도록 출력
output "ecr_repository_urls" {
  description = "ECR repository URLs for STAGING environment"
  value       = module.ecr.repository_urls
}

output "ecr_registry_id" {
  description = "ECR registry ID"
  value       = module.ecr.registry_id
}