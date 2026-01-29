# ============================================
# CodeDeploy Outputs
# ============================================

output "codedeploy_app_name" {
  description = "CodeDeploy application name"
  value       = aws_codedeploy_app.main.name
}

output "codedeploy_app_id" {
  description = "CodeDeploy application ID"
  value       = aws_codedeploy_app.main.id
}

output "deployment_group_names" {
  description = "Map of deployment group names by service"
  value = {
    for service in var.service_names :
    service => aws_codedeploy_deployment_group.services[service].deployment_group_name
  }
}

output "deployment_group_ids" {
  description = "Map of deployment group IDs by service"
  value = {
    for service in var.service_names :
    service => aws_codedeploy_deployment_group.services[service].id
  }
}

output "codedeploy_role_arn" {
  description = "CodeDeploy IAM role ARN"
  value       = aws_iam_role.codedeploy.arn
}

output "alarm_arns_5xx" {
  description = "Map of 5XX error alarm ARNs by service"
  value = {
    for service in var.service_names :
    service => aws_cloudwatch_metric_alarm.target_5xx_count[service].arn
  }
}

output "alarm_arns_latency" {
  description = "Map of latency alarm ARNs by service"
  value = {
    for service in var.service_names :
    service => aws_cloudwatch_metric_alarm.target_response_time[service].arn
  }
}
