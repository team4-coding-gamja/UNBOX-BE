# PROD 환경 ECR 설정

module "ecr" {
  source = "../../modules/ecr"
  
  repository_names = [
    "unbox-core-business",
    "unbox-product-service"
  ]
  
  environment = "prod"
  
  # PROD 환경 설정 (보안 강화)
  image_tag_mutability = "IMMUTABLE"  # PROD는 태그 변경 불가
  scan_on_push         = true
  encryption_type      = "KMS"        # KMS 암호화 사용
  kms_key_id          = aws_kms_key.ecr.arn
  max_image_count      = 20           # PROD는 많이 보관
  untagged_image_days  = 1
  
  # GitHub Actions에서 접근할 수 있도록 권한 설정
  allowed_principals = [
    "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
  ]
  
  common_tags = {
    Project     = "unbox"
    Environment = "prod"
    ManagedBy   = "terraform"
    Team        = "backend"
    Backup      = "required"
  }
}

# PROD 환경용 KMS 키
resource "aws_kms_key" "ecr" {
  description             = "KMS key for ECR encryption in PROD"
  deletion_window_in_days = 7
  
  tags = {
    Name        = "unbox-ecr-prod"
    Environment = "prod"
    ManagedBy   = "terraform"
  }
}

resource "aws_kms_alias" "ecr" {
  name          = "alias/unbox-ecr-prod"
  target_key_id = aws_kms_key.ecr.key_id
}

# 현재 AWS 계정 정보
data "aws_caller_identity" "current" {}

# ECR 정보를 다른 리소스에서 사용할 수 있도록 출력
output "ecr_repository_urls" {
  description = "ECR repository URLs for PROD environment"
  value       = module.ecr.repository_urls
}

output "ecr_registry_id" {
  description = "ECR registry ID"
  value       = module.ecr.registry_id
}