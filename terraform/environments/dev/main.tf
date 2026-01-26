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

module "vpc" {
  source = "git::https://github.com/team4-coding-gamja/UNBOX-INFRA.git//modules/vpc?ref=main"
  
  env                = var.env
  vpc_cidr           = var.vpc_cidr
  project_name       = var.project_name
  nat_sg_id          = module.security_group.nat_sg_id
  availability_zones = var.availability_zones
}

module "security_group" {
  source = "git::https://github.com/team4-coding-gamja/UNBOX-INFRA.git//modules/security_group?ref=main"
  
  env          = var.env
  project_name = var.project_name
  vpc_id       = module.vpc.vpc_id
}

module "s3" {
  source = "git::https://github.com/team4-coding-gamja/UNBOX-INFRA.git//modules/s3?ref=main"
  
  env          = var.env
  project_name = var.project_name
  kms_key_arn  = data.aws_kms_alias.infra_key.target_key_arn
}

module "alb" {
  source = "git::https://github.com/team4-coding-gamja/UNBOX-INFRA.git//modules/alb?ref=main"
  
  env               = var.env
  project_name      = var.project_name
  vpc_id            = module.vpc.vpc_id
  public_subnet_ids = module.vpc.public_subnet_ids
  alb_sg_id         = module.security_group.alb_sg_id
  service_config    = local.service_config
}

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

module "redis" {
  source = "git::https://github.com/team4-coding-gamja/UNBOX-INFRA.git//modules/redis?ref=main"
  
  project_name       = var.project_name
  env                = var.env
  private_subnet_ids = module.vpc.private_db_subnet_ids
  redis_sg_id        = module.security_group.redis_sg_id
  kms_key_arn        = module.common.kms_key_arn
}

module "msk" {
  source = "git::https://github.com/team4-coding-gamja/UNBOX-INFRA.git//modules/msk?ref=main"
  
  project_name          = var.project_name
  env                   = var.env
  private_db_subnet_ids = module.vpc.private_db_subnet_ids
  msk_sg_id             = module.security_group.msk_sg_id
  kms_key_arn           = module.common.kms_key_arn
}

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
  
  # RDS/Redis 연결 및 Secrets 설정
  rds_endpoints = {
    common = module.rds.db_endpoints["common"]  # dev는 공유 RDS 1개
  }
  redis_endpoint = "${module.redis.redis_primary_endpoint}:6379"
  
  # Dev 환경: SSM만 사용 (jwt_secret_arn은 prod에서만 필요)
  
  container_name_suffix = false
  health_check_path = "/actuator/health"
}

# ============================================
# GitHub Actions OIDC 추가 권한
# ============================================

# GitHub Actions가 ECS 서비스를 배포할 수 있도록 권한 추가
resource "aws_iam_role_policy" "github_actions_ecs_cd" {
  name = "${var.project_name}-${var.env}-github-ecs-cd-policy"
  role = "github-actions-ecr-role"  # common 모듈에서 생성한 role

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ecs:DescribeServices",        # 현재 서비스 상태 조회
          "ecs:DescribeTaskDefinition",  # 현재 Task Definition 조회
          "ecs:RegisterTaskDefinition",  # 새 Task Definition 등록
          "ecs:UpdateService",           # 서비스 업데이트
          "ecs:DescribeClusters"         # 클러스터 정보 조회
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

