# Terraform 설정
terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  
  # S3 Backend 설정 (인프라팀이 제공)
  backend "s3" {
    bucket         = "unbox-terraform-state"
    key            = "dev/terraform.tfstate"
    region         = "ap-northeast-2"
    encrypt        = true
    dynamodb_table = "unbox-terraform-locks"
  }
}

# AWS Provider
provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Environment = "dev"
      Project     = "UNBOX"
      ManagedBy   = "Terraform"
    }
  }
}

# 공통 로컬 변수
locals {
  environment = "dev"
  project     = "unbox"
  
  common_tags = {
    Environment = local.environment
    Project     = local.project
    ManagedBy   = "Terraform"
  }
}
