terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.1"
    }
  }
}

provider "aws" {
  region = "ap-northeast-2"
}

module "vpc" {
  source = "../../modules/vpc"
  
  project_name = var.project_name
}

module "security" {
  source = "../../modules/security"
  
  project_name = var.project_name
  vpc_id       = module.vpc.vpc_id
}

# 조건부 RDS 모듈 (비용 절약 시 H2 사용)
module "database" {
  count  = var.use_rds ? 1 : 0
  source = "../../modules/database"
  
  project_name      = var.project_name
  subnet_ids        = module.vpc.private_subnet_ids
  security_group_id = module.security.rds_sg_id
}

module "compute" {
  source = "../../modules/compute"
  
  project_name      = var.project_name
  public_key        = var.public_key
  subnet_id         = module.vpc.public_subnet_id
  security_group_id = module.security.ec2_sg_id
}

module "storage" {
  source = "../../modules/storage"
  
  project_name = var.project_name
}