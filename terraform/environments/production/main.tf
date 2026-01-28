locals {
  service_config = {
    "user"    = 8080
    "product" = 8080
    "trade"   = 8080
    "order"   = 8080
    "payment" = 8080
  }
}

data "aws_region" "current" {}

data "aws_kms_alias" "infra_key" {
  name = "alias/${var.project_name}/${var.env}/main-key"
}

# ============================================
# VPC 및 네트워크
# ============================================
module "vpc" {
  source = "git::https://github.com/team4-coding-gamja/UNBOX-INFRA.git//modules/vpc?ref=main"
  
  env                = var.env
  vpc_cidr           = var.vpc_cidr
  project_name       = var.project_name
  nat_sg_id          = module.security_group.nat_sg_id
  availability_zones = var.availability_zones
}

# ============================================
# Security Groups
# ============================================
module "security_group" {
  source = "git::https://github.com/team4-coding-gamja/UNBOX-INFRA.git//modules/security_group?ref=main"
  
  env          = var.env
  project_name = var.project_name
  vpc_id       = module.vpc.vpc_id
}

# ============================================
# S3 (CloudTrail, ALB Logs 등)
# ============================================
module "s3" {
  source = "git::https://github.com/team4-coding-gamja/UNBOX-INFRA.git//modules/s3?ref=main"
  
  env          = var.env
  project_name = var.project_name
  kms_key_arn  = data.aws_kms_alias.infra_key.target_key_arn
}

# ============================================
# ALB (Application Load Balancer)
# ============================================
module "alb" {
  source = "git::https://github.com/team4-coding-gamja/UNBOX-INFRA.git//modules/alb?ref=main"
  
  env               = var.env
  project_name      = var.project_name
  vpc_id            = module.vpc.vpc_id
  public_subnet_ids = module.vpc.public_subnet_ids
  alb_sg_id         = module.security_group.alb_sg_id
  service_config    = local.service_config
}

# ============================================
# Common 리소스 (IAM, KMS, CloudMap, Secrets 등)
# ============================================
module "common" {
  source = "git::https://github.com/team4-coding-gamja/UNBOX-INFRA.git//modules/common?ref=main"
  
  env                  = var.env
  project_name         = var.project_name
  service_config       = local.service_config
  vpc_id               = module.vpc.vpc_id
  cloudtrail_bucket_id = module.s3.cloudtrail_bucket_id
  users                = var.users
  kms_key_arn          = data.aws_kms_alias.infra_key.target_key_arn
}

# ============================================
# RDS (Prod: 서비스별 별도 인스턴스)
# ============================================
data "aws_ssm_parameter" "db_password" {
  name       = "/${var.project_name}/${var.env}/user/DB_PASSWORD"
  depends_on = [module.common]
}

module "rds" {
  source = "git::https://github.com/team4-coding-gamja/UNBOX-INFRA.git//modules/rds?ref=main"
  
  project_name       = var.project_name
  env                = var.env
  private_subnet_ids = module.vpc.private_db_subnet_ids
  availability_zones = var.availability_zones
  kms_key_arn        = module.common.kms_key_arn
  service_config     = local.service_config
  rds_sg_ids         = module.security_group.rds_sg_ids
  db_password        = data.aws_ssm_parameter.db_password.value
}

# ============================================
# ElastiCache Redis (Prod: cache.t4g.small)
# ============================================
module "redis" {
  source = "git::https://github.com/team4-coding-gamja/UNBOX-INFRA.git//modules/redis?ref=main"
  
  project_name       = var.project_name
  env                = var.env
  private_subnet_ids = module.vpc.private_db_subnet_ids
  redis_sg_id        = module.security_group.redis_sg_id
  kms_key_arn        = module.common.kms_key_arn
}

# ============================================
# MSK (Prod: 3-broker cluster)
# ============================================
module "msk" {
  source = "git::https://github.com/team4-coding-gamja/UNBOX-INFRA.git//modules/msk?ref=main"
  
  project_name          = var.project_name
  env                   = var.env
  private_db_subnet_ids = module.vpc.private_db_subnet_ids
  msk_sg_id             = module.security_group.msk_sg_id
  kms_key_arn           = module.common.kms_key_arn
}

# ============================================
# ECS (Fargate)
# ============================================
module "ecs" {
  source = "git::https://github.com/team4-coding-gamja/UNBOX-INFRA.git//modules/ecs?ref=main"
  
  project_name                = var.project_name
  env                         = var.env
  app_subnet_ids              = module.vpc.private_app_subnet_ids
  aws_region                  = data.aws_region.current.name
  account_id                  = data.aws_caller_identity.current.account_id
  msk_bootstrap_brokers       = module.msk.bootstrap_brokers
  service_config              = local.service_config
  service_names               = ["user", "product", "trade", "order", "payment"]
  target_group_arns           = module.alb.target_group_arns
  ecs_sg_ids                  = module.security_group.app_sg_ids
  ecs_task_execution_role_arn = module.common.ecs_task_execution_role_arn
  ecs_task_role_arn           = module.common.ecs_task_role_arn
  cloud_map_namespace_arn     = module.common.cloud_map_namespace_arn
  kms_key_arn                 = module.common.kms_key_arn
  
  # Prod 환경: 서비스별 별도 RDS 엔드포인트
  rds_endpoints = {
    user    = module.rds.db_endpoints["user"]
    product = module.rds.db_endpoints["product"]
    trade   = module.rds.db_endpoints["trade"]
    order   = module.rds.db_endpoints["order"]
    payment = module.rds.db_endpoints["payment"]
  }
  redis_endpoint = "${module.redis.redis_primary_endpoint}:6379"
  
  # Prod 환경: Secrets Manager 사용
  # jwt_secret_arn = var.jwt_secret_arn
  
  container_name_suffix = false
  health_check_path     = "/actuator/health"
}

# ============================================
# CloudWatch Logs 모니터링
# ============================================
module "monitoring" {
  source = "git::https://github.com/team4-coding-gamja/UNBOX-INFRA.git//modules/monitoring?ref=main"
  
  project_name        = var.project_name
  env                 = var.env
  aws_region          = data.aws_region.current.name
  account_id          = data.aws_caller_identity.current.account_id
  service_names       = ["user", "product", "trade", "order", "payment"]
  discord_webhook_url = data.aws_ssm_parameter.discord_webhook.value
  kms_key_arn         = module.common.kms_key_arn
  
  depends_on = [module.ecs]
}

data "aws_ssm_parameter" "discord_webhook" {
  name = "/${var.project_name}/${var.env}/common/DISCORD_WEBHOOK_URL"
}

# ============================================
# GitHub Actions OIDC 권한 (Prod CD용)
# ============================================
resource "aws_iam_role_policy" "github_actions_prod_cd" {
  name = "${var.project_name}-${var.env}-github-cd-policy"
  role = "github-actions-ecr-role"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ecs:DescribeServices",
          "ecs:DescribeTaskDefinition",
          "ecs:RegisterTaskDefinition",
          "ecs:UpdateService",
          "ecs:DescribeClusters",
          "codedeploy:CreateDeployment",
          "codedeploy:GetDeployment",
          "codedeploy:GetDeploymentConfig",
          "codedeploy:GetApplicationRevision",
          "codedeploy:RegisterApplicationRevision"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = ["iam:PassRole"]
        Resource = [
          module.common.ecs_task_execution_role_arn,
          module.common.ecs_task_role_arn
        ]
      }
    ]
  })
  
  depends_on = [module.common]
}
