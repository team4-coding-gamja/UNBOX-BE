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
      Environment = var.env
      Project     = var.project_name
      ManagedBy   = "Terraform"
    }
  }
}

data "aws_caller_identity" "current" {}
