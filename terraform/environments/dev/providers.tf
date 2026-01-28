# terraform/environments/dev/providers.tf
terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "ap-northeast-2"
  
  default_tags {
    tags = {
      Environment = "dev"
      Project     = "UNBOX"
      ManagedBy   = "Terraform"
    }
  }
}

# 현재 AWS 계정 정보
data "aws_caller_identity" "current" {}

# 사용 가능한 AZ 정보
data "aws_availability_zones" "available" {
  state = "available"
}
