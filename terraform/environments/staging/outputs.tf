# ============================================
# 공통 인프라 출력
# ============================================

output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "alb_dns_name" {
  description = "ALB DNS name"
  value       = aws_lb.main.dns_name
}

output "alb_arn" {
  description = "ALB ARN"
  value       = aws_lb.main.arn
}

output "ecs_cluster_name" {
  description = "ECS Cluster name"
  value       = aws_ecs_cluster.main.name
}

output "ecs_cluster_arn" {
  description = "ECS Cluster ARN"
  value       = aws_ecs_cluster.main.arn
}

# ============================================
# User Service 출력
# ============================================

output "user_service_endpoint" {
  description = "User service endpoint"
  value       = "http://${aws_lb.main.dns_name}/api/users"
}

output "user_db_endpoint" {
  description = "User service database endpoint"
  value       = aws_db_instance.user.endpoint
  sensitive   = true
}

output "user_redis_endpoint" {
  description = "User service Redis endpoint"
  value       = aws_elasticache_cluster.user.cache_nodes[0].address
  sensitive   = true
}

# ============================================
# Product Service 출력
# ============================================

output "product_service_endpoint" {
  description = "Product service endpoint"
  value       = "http://${aws_lb.main.dns_name}/api/products"
}

output "product_db_endpoint" {
  description = "Product service database endpoint"
  value       = aws_db_instance.product.endpoint
  sensitive   = true
}

# ============================================
# Order Service 출력
# ============================================

output "order_service_endpoint" {
  description = "Order service endpoint"
  value       = "http://${aws_lb.main.dns_name}/api/orders"
}

output "order_db_endpoint" {
  description = "Order service database endpoint"
  value       = aws_db_instance.order.endpoint
  sensitive   = true
}

# ============================================
# Payment Service 출력
# ============================================

output "payment_service_endpoint" {
  description = "Payment service endpoint"
  value       = "http://${aws_lb.main.dns_name}/api/payments"
}

output "payment_db_endpoint" {
  description = "Payment service database endpoint"
  value       = aws_db_instance.payment.endpoint
  sensitive   = true
}

# ============================================
# Trade Service 출력
# ============================================

output "trade_service_endpoint" {
  description = "Trade service endpoint"
  value       = "http://${aws_lb.main.dns_name}/api/trades"
}

output "trade_db_endpoint" {
  description = "Trade service database endpoint"
  value       = aws_db_instance.trade.endpoint
  sensitive   = true
}
