output "alb_address" {
  description = "ALB DNS name"
  value       = module.alb.alb_dns_name
}

output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}

output "rds_endpoints" {
  description = "RDS endpoints for all services"
  value       = module.rds.db_endpoints
}

output "redis_primary_endpoint" {
  description = "Redis primary endpoint"
  value       = module.redis.redis_primary_endpoint
}

output "msk_bootstrap_brokers" {
  description = "MSK bootstrap brokers"
  value       = module.msk.bootstrap_brokers
}

output "kms_key_arn" {
  description = "KMS key ARN"
  value       = module.common.kms_key_arn
}

output "ecs_task_execution_role_arn" {
  description = "ECS task execution role ARN"
  value       = module.common.ecs_task_execution_role_arn
}

output "ecs_task_role_arn" {
  description = "ECS task role ARN"
  value       = module.common.ecs_task_role_arn
}

output "ecr_repository_urls" {
  description = "ECR repository URLs for all services"
  value       = module.ecs.ecr_repository_urls
}

# ECS 관련 추가 output (CD 워크플로우에서 사용)
output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = module.ecs.cluster_name
}

output "ecs_service_names" {
  description = "ECS service names"
  value = {
    for service in ["user", "product", "trade", "order", "payment"] :
    service => "${var.project_name}-${var.env}-${service}"
  }
}

output "github_actions_role_arn" {
  description = "GitHub Actions OIDC role ARN for CI/CD"
  value       = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/github-actions-ecr-role"
}
