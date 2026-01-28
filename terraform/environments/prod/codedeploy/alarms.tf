# ============================================
# CloudWatch Alarms for Auto Rollback
# ============================================

# 각 서비스별로 2개의 Alarm 생성:
# 1. 5XX Error Rate Alarm
# 2. High Latency Alarm

locals {
  services = ["user", "product", "order", "payment", "trade"]
}

# ============================================
# 5XX Error Rate Alarms
# ============================================

resource "aws_cloudwatch_metric_alarm" "target_5xx_count" {
  for_each = toset(local.services)

  alarm_name          = "${var.project_name}-${var.env}-${each.key}-5xx-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "HTTPCode_Target_5XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = 60
  statistic           = "Sum"
  threshold           = 10
  alarm_description   = "Triggers when ${each.key} service has more than 10 5XX errors in 2 minutes"
  treat_missing_data  = "notBreaching"

  dimensions = {
    TargetGroup  = split(":", var.target_group_arns[each.key])[5]
    LoadBalancer = split("/", var.alb_arn)[1]
  }

  tags = {
    Name        = "${var.project_name}-${var.env}-${each.key}-5xx-alarm"
    Environment = var.env
    Project     = var.project_name
    Service     = each.key
  }
}

# ============================================
# High Latency Alarms
# ============================================

resource "aws_cloudwatch_metric_alarm" "target_response_time" {
  for_each = toset(local.services)

  alarm_name          = "${var.project_name}-${var.env}-${each.key}-high-latency"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "TargetResponseTime"
  namespace           = "AWS/ApplicationELB"
  period              = 60
  statistic           = "Average"
  threshold           = 2.0
  alarm_description   = "Triggers when ${each.key} service response time exceeds 2 seconds for 2 minutes"
  treat_missing_data  = "notBreaching"

  dimensions = {
    TargetGroup  = split(":", var.target_group_arns[each.key])[5]
    LoadBalancer = split("/", var.alb_arn)[1]
  }

  tags = {
    Name        = "${var.project_name}-${var.env}-${each.key}-latency-alarm"
    Environment = var.env
    Project     = var.project_name
    Service     = each.key
  }
}
