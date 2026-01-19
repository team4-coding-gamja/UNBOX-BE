# DEV 환경 ECR 설정

module "ecr" {
  source = "../../modules/ecr"
  
  repository_names = [
    "unbox-core-business",
    "unbox-product-service"
  ]
  
  environment = "dev"
  
  # DEV 환경 설정
  image_tag_mutability = "MUTABLE"
  scan_on_push         = true
  encryption_type      = "AES256"
  max_image_count      = 5   # DEV는 적게 보관
  untagged_image_days  = 1
  
  # GitHub Actions에서 접근할 수 있도록 권한 설정
  allowed_principals = [
    module.github_actions_iam.github_actions_user_arn
  ]
  
  common_tags = {
    Project     = "unbox"
    Environment = "dev"
    ManagedBy   = "terraform"
    Team        = "backend"
  }
}

# GitHub Actions용 IAM 설정
module "github_actions_iam" {
  source = "../../modules/iam"
  
  environment           = "dev"
  ecr_repository_arns   = values(module.ecr.repository_arns)
  aws_region           = "ap-northeast-2"
  
  # DEV 환경에서는 추가 권한 비활성화
  enable_additional_permissions = false
  
  common_tags = {
    Project     = "unbox"
    Environment = "dev"
    ManagedBy   = "terraform"
    Team        = "devops"
  }
}

# 현재 AWS 계정 정보
data "aws_caller_identity" "current" {}

# ECR 정보를 다른 리소스에서 사용할 수 있도록 출력
output "ecr_repository_urls" {
  description = "ECR repository URLs for DEV environment"
  value       = module.ecr.repository_urls
}

output "ecr_registry_id" {
  description = "ECR registry ID"
  value       = module.ecr.registry_id
}

# GitHub Actions 자격증명 (민감 정보)
output "github_actions_credentials" {
  description = "GitHub Actions AWS credentials"
  value = {
    access_key_id     = module.github_actions_iam.github_actions_access_key_id
    secret_access_key = module.github_actions_iam.github_actions_secret_access_key
  }
  sensitive = true
}