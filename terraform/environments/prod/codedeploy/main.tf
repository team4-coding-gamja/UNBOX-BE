# ============================================
# CodeDeploy Application
# ============================================

resource "aws_codedeploy_app" "main" {
  name             = "${var.project_name}-${var.env}-app"
  compute_platform = "ECS"

  tags = {
    Name        = "${var.project_name}-${var.env}-app"
    Environment = var.env
    Project     = var.project_name
  }
}

# ============================================
# CodeDeploy Deployment Groups (서비스별)
# ============================================

resource "aws_codedeploy_deployment_group" "services" {
  for_each = toset(var.service_names)

  app_name               = aws_codedeploy_app.main.name
  deployment_group_name  = "${var.project_name}-${var.env}-${each.key}-dg"
  service_role_arn       = aws_iam_role.codedeploy.arn
  deployment_config_name = "CodeDeployDefault.ECSCanary10Percent5Minutes"

  # ============================================
  # Auto Rollback 설정
  # ============================================
  auto_rollback_configuration {
    enabled = true
    events  = ["DEPLOYMENT_FAILURE", "DEPLOYMENT_STOP_ON_ALARM"]
  }

  # ============================================
  # CloudWatch Alarms 연결
  # ============================================
  alarm_configuration {
    enabled = true
    alarms = [
      aws_cloudwatch_metric_alarm.target_5xx_count[each.key].alarm_name,
      aws_cloudwatch_metric_alarm.target_response_time[each.key].alarm_name
    ]
  }

  # ============================================
  # Blue/Green 배포 설정
  # ============================================
  blue_green_deployment_config {
    # Termination Wait Time: Blue 환경 유지 시간 (30분)
    terminate_blue_instances_on_deployment_success {
      action                           = "TERMINATE"
      termination_wait_time_in_minutes = 30
    }

    # 배포 준비 완료 후 자동으로 트래픽 전환
    deployment_ready_option {
      action_on_timeout = "CONTINUE_DEPLOYMENT"
    }
  }

  # ============================================
  # ECS 서비스 설정
  # ============================================
  ecs_service {
    cluster_name = var.cluster_name
    service_name = var.ecs_service_names[each.key]
  }

  # ============================================
  # Load Balancer 설정
  # ============================================
  load_balancer_info {
    target_group_pair_info {
      prod_traffic_route {
        listener_arns = [var.alb_listener_arn]
      }

      target_group {
        name = split(":", var.target_group_arns[each.key])[5]
      }

      # Test Traffic Route는 선택사항 (Lambda Hook 테스트용)
      # test_traffic_route {
      #   listener_arns = [var.alb_test_listener_arn]
      # }
    }
  }

  # ============================================
  # Lambda Hook 설정 (AfterAllowTestTraffic)
  # ============================================
  trigger_configuration {
    trigger_events     = ["DeploymentFailure", "DeploymentStop"]
    trigger_name       = "${var.project_name}-${var.env}-${each.key}-trigger"
    trigger_target_arn = aws_lambda_function.deployment_validator.arn
  }

  # ============================================
  # 배포 스타일: Blue/Green
  # ============================================
  deployment_style {
    deployment_option = "WITH_TRAFFIC_CONTROL"
    deployment_type   = "BLUE_GREEN"
  }

  tags = {
    Name        = "${var.project_name}-${var.env}-${each.key}-dg"
    Environment = var.env
    Project     = var.project_name
    Service     = each.key
  }
}
