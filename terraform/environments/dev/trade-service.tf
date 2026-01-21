# ============================================
# Trade Service
# Port: 8083
# ============================================

locals {
  trade_service_name = "trade-service"
  trade_service_port = 8083
}

resource "aws_security_group" "trade_db" {
  name        = "${local.project}-${local.environment}-trade-db-sg"
  description = "Security group for Trade Service database"
  vpc_id      = aws_vpc.main.id
  
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.trade_service.id]
  }
  
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  tags = merge(local.common_tags, {
    Service = local.trade_service_name
  })
}

resource "aws_db_subnet_group" "trade" {
  name       = "${local.project}-${local.environment}-trade-db-subnet"
  subnet_ids = aws_subnet.private[*].id
  
  tags = merge(local.common_tags, {
    Service = local.trade_service_name
  })
}

resource "aws_db_instance" "trade" {
  identifier             = "${local.project}-${local.environment}-trade-db"
  engine                 = "postgres"
  engine_version         = "15.4"
  instance_class         = "db.t3.micro"
  allocated_storage      = 20
  max_allocated_storage  = 100
  storage_type           = "gp3"
  storage_encrypted      = true
  db_name                = "unbox_trade"
  username               = "unbox_user"
  password               = var.trade_db_password
  port                   = 5432
  db_subnet_group_name   = aws_db_subnet_group.trade.name
  vpc_security_group_ids = [aws_security_group.trade_db.id]
  backup_retention_period = 7
  skip_final_snapshot    = true
  
  tags = merge(local.common_tags, {
    Service = local.trade_service_name
  })
}

resource "aws_security_group" "trade_service" {
  name        = "${local.project}-${local.environment}-${local.trade_service_name}-sg"
  description = "Security group for Trade Service"
  vpc_id      = aws_vpc.main.id
  
  ingress {
    from_port       = local.trade_service_port
    to_port         = local.trade_service_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }
  
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  tags = merge(local.common_tags, {
    Service = local.trade_service_name
  })
}

resource "aws_cloudwatch_log_group" "trade_service" {
  name              = "/ecs/${local.project}-${local.environment}-${local.trade_service_name}"
  retention_in_days = 7
  
  tags = merge(local.common_tags, {
    Service = local.trade_service_name
  })
}

resource "aws_ecs_task_definition" "trade_service" {
  family                   = "${local.project}-${local.environment}-${local.trade_service_name}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.trade_service_cpu
  memory                   = var.trade_service_memory
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  
  container_definitions = jsonencode([{
    name  = local.trade_service_name
    image = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/${local.project}-${local.trade_service_name}:${var.trade_service_image_tag}"
    
    portMappings = [{
      containerPort = local.trade_service_port
      protocol      = "tcp"
    }]
    
    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = local.environment },
      { name = "SERVER_PORT", value = tostring(local.trade_service_port) },
      { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${aws_db_instance.trade.endpoint}/unbox_trade" },
      { name = "SPRING_DATASOURCE_USERNAME", value = "unbox_user" }
    ]
    
    secrets = [{
      name      = "SPRING_DATASOURCE_PASSWORD"
      valueFrom = var.trade_db_password_secret_arn
    }]
    
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.trade_service.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ecs"
      }
    }
    
    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:${local.trade_service_port}/actuator/health || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
    
    essential = true
  }])
  
  tags = merge(local.common_tags, {
    Service = local.trade_service_name
  })
}

resource "aws_lb_target_group" "trade_service" {
  name        = "${local.project}-${local.environment}-trade-tg"
  port        = local.trade_service_port
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"
  
  health_check {
    enabled             = true
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 5
    interval            = 30
    path                = "/actuator/health"
    matcher             = "200"
  }
  
  deregistration_delay = 30
  
  tags = merge(local.common_tags, {
    Service = local.trade_service_name
  })
}

resource "aws_lb_listener_rule" "trade_service" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 140
  
  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.trade_service.arn
  }
  
  condition {
    path_pattern {
      values = ["/api/trades/*", "/api/bids/*"]
    }
  }
  
  tags = merge(local.common_tags, {
    Service = local.trade_service_name
  })
}

resource "aws_ecs_service" "trade_service" {
  name            = "${local.project}-${local.environment}-${local.trade_service_name}"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.trade_service.arn
  desired_count   = var.trade_service_desired_count
  launch_type     = "FARGATE"
  
  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.trade_service.id]
    assign_public_ip = false
  }
  
  load_balancer {
    target_group_arn = aws_lb_target_group.trade_service.arn
    container_name   = local.trade_service_name
    container_port   = local.trade_service_port
  }
  
  depends_on = [
    aws_lb_listener_rule.trade_service,
    aws_db_instance.trade
  ]
  
  tags = merge(local.common_tags, {
    Service = local.trade_service_name
  })
}
