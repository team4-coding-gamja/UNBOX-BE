# 모니터링 모듈 출력값

output "sns_topic_arn" {
  description = "SNS topic ARN for cost alerts"
  value       = aws_sns_topic.cost_alerts.arn
}

output "budget_name" {
  description = "Budget name"
  value       = aws_budgets_budget.monthly_budget.name
}

output "dashboard_url" {
  description = "CloudWatch dashboard URL"
  value       = "https://${var.aws_region}.console.aws.amazon.com/cloudwatch/home?region=${var.aws_region}#dashboards:name=${aws_cloudwatch_dashboard.cost_dashboard.dashboard_name}"
}

output "cost_anomaly_detector_arn" {
  description = "Cost anomaly detector ARN"
  value       = aws_ce_anomaly_detector.cost_anomaly.arn
}