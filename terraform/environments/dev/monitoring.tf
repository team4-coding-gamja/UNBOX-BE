# DEV 환경 비용 모니터링 설정

module "cost_monitoring" {
  source = "../../modules/monitoring"
  
  environment = "dev"
  aws_region  = "ap-northeast-2"
  
  # DEV 환경 예산 설정
  monthly_budget_limit = 50   # 월 $50
  daily_budget_limit   = 5    # 일 $5
  anomaly_threshold    = 10   # $10 이상 이상 징후시 알림
  
  # 알림 받을 이메일 주소들
  alert_email_addresses = [
    "your.email@company.com",           # 당신 (Backend + CI/CD)
    "team.lead@company.com",            # 팀 리드
    "finance@company.com"               # 재무팀
  ]
  
  common_tags = {
    Project     = "unbox"
    Environment = "dev"
    ManagedBy   = "terraform"
    Team        = "backend"
    CostCenter  = "engineering"
  }
}

# 모니터링 정보 출력
output "dev_monitoring_info" {
  description = "DEV environment monitoring information"
  value = {
    dashboard_url = module.cost_monitoring.dashboard_url
    budget_name   = module.cost_monitoring.budget_name
    sns_topic_arn = module.cost_monitoring.sns_topic_arn
  }
}