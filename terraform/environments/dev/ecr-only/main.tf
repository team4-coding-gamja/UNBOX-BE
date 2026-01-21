# ============================================
# ECR만 독립적으로 배포하기 위한 설정
# ============================================

terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  
  # 로컬 state 사용 (나중에 S3로 마이그레이션 가능)
  # backend "s3" {
  #   bucket         = "unbox-terraform-state"
  #   key            = "dev/ecr/terraform.tfstate"
  #   region         = "ap-northeast-2"
  #   encrypt        = true
  #   dynamodb_table = "unbox-terraform-locks"
  # }
}

# AWS Provider
provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Environment = "dev"
      Project     = "UNBOX"
      ManagedBy   = "Terraform"
      Component   = "ECR"
    }
  }
}

# 로컬 변수
locals {
  environment = "dev"
  project     = "unbox"
  
  common_tags = {
    Environment = local.environment
    Project     = local.project
    ManagedBy   = "Terraform"
  }
  
  # 서비스 이름 정의
  services = {
    product = "product-service"
    order   = "order-service"
    payment = "payment-service"
    trade   = "trade-service"
    user    = "user-service"
  }
}
