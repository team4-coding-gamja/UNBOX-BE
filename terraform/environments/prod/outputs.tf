# ============================================
# VPC Outputs
# ============================================
output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}

output "public_subnet_ids" {
  description = "Public subnet IDs"
  value       = module.vpc.public_subnet_ids
}

output "private_app_subnet_ids" {
  description = "Private app subnet IDs"
  value       = module.vpc.private_app_subnet_ids
}

output "private_db_subnet_ids" {
  description = "Private DB subnet IDs"
  value       = module.vpc.private_db_subnet_ids
}

# ============================================
# ALB Outputs
# ============================================
output "alb_dns_name" {
  description = "ALB DNS name"
  value       = module.alb.alb_dns_name
}

output "alb_zone_id" {
  description = "ALB Zone ID"
  value       = module.alb.alb_zone_id
}

# ============================================
# ECS Outputs
# ============================================
output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = module.ecs.cluster_name
}

output "ecs_service_names" {
  description = "ECS service names"
  value       = {
    for service_name in ["user", "product", "order", "payment", "trade"] :
    service_name => "${var.project_name}-${var.env}-${service_name}"
  }
}

# ============================================
# RDS Outputs
# ============================================
output "rds_endpoints" {
  description = "RDS endpoints by service"
  value       = module.rds.db_endpoints
  sensitive   = true
}

# ============================================
# Redis Outputs
# ============================================
output "redis_endpoint" {
  description = "Redis primary endpoint"
  value       = module.redis.redis_primary_endpoint
  sensitive   = true
}

# ============================================
# MSK Outputs
# ============================================
output "msk_bootstrap_brokers" {
  description = "MSK bootstrap brokers"
  value       = module.msk.bootstrap_brokers
  sensitive   = true
}

# ============================================
# CloudMap Outputs
# ============================================
output "cloud_map_namespace_id" {
  description = "Cloud Map namespace ID"
  value       = module.common.cloud_map_namespace_id
}

output "cloud_map_namespace_arn" {
  description = "Cloud Map namespace ARN"
  value       = module.common.cloud_map_namespace_arn
}
