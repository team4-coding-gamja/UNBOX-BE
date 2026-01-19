# AWS 비용 모니터링 및 알림 설정

# SNS 토픽 생성 (비용 알림용)
resource "aws_sns_topic" "cost_alerts" {
  name = "unbox-cost-alerts-${var.environment}"
  
  tags = merge(var.common_tags, {
    Name        = "unbox-cost-alerts-${var.environment}"
    Purpose     = "cost-monitoring"
    Environment = var.environment
  })
}

# SNS 토픽 구독 (이메일 알림)
resource "aws_sns_topic_subscription" "cost_email_alerts" {
  for_each = toset(var.alert_email_addresses)
  
  topic_arn = aws_sns_topic.cost_alerts.arn
  protocol  = "email"
  endpoint  = each.value
}

# 월 예산 설정
resource "aws_budgets_budget" "monthly_budget" {
  name         = "unbox-monthly-budget-${var.environment}"
  budget_type  = "COST"
  limit_amount = var.monthly_budget_limit
  limit_unit   = "USD"
  time_unit    = "MONTHLY"
  
  time_period_start = formatdate("YYYY-MM-01_00:00", timestamp())
  
  cost_filters = {
    Tag = [
      "Project:unbox",
      "Environment:${var.environment}"
    ]
  }
  
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                 = 80
    threshold_type            = "PERCENTAGE"
    notification_type         = "ACTUAL"
    subscriber_email_addresses = var.alert_email_addresses
    subscriber_sns_topic_arns  = [aws_sns_topic.cost_alerts.arn]
  }
  
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                 = 100
    threshold_type            = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = var.alert_email_addresses
    subscriber_sns_topic_arns  = [aws_sns_topic.cost_alerts.arn]
  }
}

# 일일 예산 설정 (DEV 환경용)
resource "aws_budgets_budget" "daily_budget" {
  count = var.environment == "dev" ? 1 : 0
  
  name         = "unbox-daily-budget-dev"
  budget_type  = "COST"
  limit_amount = var.daily_budget_limit
  limit_unit   = "USD"
  time_unit    = "DAILY"
  
  cost_filters = {
    Tag = [
      "Project:unbox",
      "Environment:dev"
    ]
  }
  
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                 = 100
    threshold_type            = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = var.alert_email_addresses
  }
}

# CloudWatch 대시보드 생성
resource "aws_cloudwatch_dashboard" "cost_dashboard" {
  dashboard_name = "unbox-cost-monitoring-${var.environment}"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6

        properties = {
          metrics = [
            ["AWS/Billing", "EstimatedCharges", "Currency", "USD"]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "Estimated Monthly Charges"
          period  = 86400
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 6
        height = 6

        properties = {
          metrics = [
            ["AWS/EC2", "CPUUtilization"],
            ["AWS/RDS", "CPUUtilization"],
            ["AWS/ElastiCache", "CPUUtilization"]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "Resource Utilization"
          period  = 300
        }
      },
      {
        type   = "metric"
        x      = 6
        y      = 6
        width  = 6
        height = 6

        properties = {
          metrics = [
            ["AWS/ApplicationELB", "RequestCount"],
            ["AWS/ApplicationELB", "TargetResponseTime"]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "Application Performance"
          period  = 300
        }
      }
    ]
  })
}

# 비용 이상 탐지 (Cost Anomaly Detection)
resource "aws_ce_anomaly_detector" "cost_anomaly" {
  name         = "unbox-cost-anomaly-${var.environment}"
  monitor_type = "DIMENSIONAL"

  specification = jsonencode({
    Dimension = "SERVICE"
    MatchOptions = ["EQUALS"]
    Values = ["Amazon Elastic Compute Cloud - Compute", "Amazon Relational Database Service"]
  })
}

# 이상 탐지 구독
resource "aws_ce_anomaly_subscription" "cost_anomaly_subscription" {
  name      = "unbox-anomaly-subscription-${var.environment}"
  frequency = "DAILY"
  
  monitor_arn_list = [
    aws_ce_anomaly_detector.cost_anomaly.arn
  ]
  
  subscriber {
    type    = "EMAIL"
    address = var.alert_email_addresses[0]  # 첫 번째 이메일로 전송
  }
  
  threshold_expression {
    and {
      dimension {
        key           = "ANOMALY_TOTAL_IMPACT_ABSOLUTE"
        values        = [tostring(var.anomaly_threshold)]
        match_options = ["GREATER_THAN_OR_EQUAL"]
      }
    }
  }
}