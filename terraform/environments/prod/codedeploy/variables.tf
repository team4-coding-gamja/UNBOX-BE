variable "project_name" {
  description = "Project name"
  type        = string
}

variable "env" {
  description = "Environment (production)"
  type        = string
}

variable "cluster_name" {
  description = "ECS cluster name"
  type        = string
}

variable "service_names" {
  description = "List of service names"
  type        = list(string)
}

variable "alb_arn" {
  description = "ALB ARN"
  type        = string
}

variable "alb_dns_name" {
  description = "ALB DNS name"
  type        = string
}

variable "alb_listener_arn" {
  description = "ALB listener ARN"
  type        = string
}

variable "target_group_arns" {
  description = "Map of target group ARNs by service"
  type        = map(string)
}

variable "ecs_service_names" {
  description = "Map of ECS service names"
  type        = map(string)
}

variable "app_subnet_ids" {
  description = "Private app subnet IDs"
  type        = list(string)
}

variable "ecs_sg_ids" {
  description = "Map of ECS security group IDs by service"
  type        = map(string)
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}
