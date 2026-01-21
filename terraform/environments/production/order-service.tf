# ============================================
# Order Service
# Port: 8084
# ============================================

locals {
  order_service_name = "order-service"
  order_service_port = 8084
}

# RDS
resource "aws_security_group" "order_db" {
  name        = "${local.project}-${local.environment}-order-db-sg"
  description = "Security group for Order Service database"
  vpc_id      = aws_vpc.main.id
  
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.order_service.id]
  }
  
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  tags = merge(local.common_tags, {
    Name    = "${local.project}-${local.environment}-order-db-sg"
    Service = local.order_service_name
  })
}

resource "aws_db_subnet_group" "order" {
  name       = "${local.project}-${local.environment}-order-db-subnet"
  subnet_ids = aws_subnet.private[*].id
  
  tags = merge(local.common_tags, {
    Service = local.order_service_name
  })
}

resource "aws_db_instance" "order" {
  identifier             = "${local.project}-${local.environment}-order-db"
  engine                 = "postgres"
  engine_version         = "15.4"
  instance_class         = "db.t3.micro"
  allocated_storage      = 20
  max_allocated_storage  = 100
  storage_type           = "gp3"
  storage_encrypted      = true
  db_name                = "unbox_order"
  username               = "unbox_user"
  password               = var.order_db_password
  port                   = 5432
  db_subnet_group_name   = aws_db_subnet_group.order.name
  vpc_security_group_ids = [aws_security_group.order_db.id]
  backup_retention_period = 7
  skip_final_snapshot    = true
  
  tags = merge(local.common_tags, {
    Service = local.order_service_name
  })
}

# ECS
resource "aws_security_group" "order_service" {
  name        = "${local.project}-${local.environment}-${local.order_service_name}-sg"
  description = "Security group for Order Service"
  vpc_id      = aws_vpc.main.id
  
  ingress {
    from_port       = local.order_service_port
    to_port         = local.order_service_port
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
    Service = local.order_service_name
  })
}

resource "aws_cloudwatch_log_group" "order_service" {
  name              = "/ecs/${local.project}-${local.environment}-${local.order_service_name}"
  retention_in_days = 7
  
  tags = merge(local.common_tags, {
    Service = local.order_service_name
  })
}

resource "aws_ecs_task_definition" "order_service" {
  family                   = "${local.project}-${local.environment}-${local.order_service_name}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.order_service_cpu
  memory                   = var.order_service_memory
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  
  container_definitions = jsonencode([{
    name  = local.order_service_name
    image = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/${local.project}-${local.order_service_name}:${var.order_service_image_tag}"
    
    portMappings = [{
      containerPort = local.order_service_port
      protocol      = "tcp"
    }]
    
    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = local.environment },
      { name = "SERVER_PORT", value = tostring(local.order_service_port) },
      { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${aws_db_instance.order.endpoint}/unbox_order" },
      { name = "SPRING_DATASOURCE_USERNAME", value = "unbox_user" }
    ]
    
    secrets = [{
      name      = "SPRING_DATASOURCE_PASSWORD"
      valueFrom = var.order_db_password_secret_arn
    }]
    
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.order_service.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ecs"
      }
    }
    
    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:${local.order_service_port}/actuator/health || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
    
    essential = true
  }])
  
  tags = merge(local.common_tags, {
    Service = local.order_service_name
  })
}

resource "aws_lb_target_group" "order_service" {
  name        = "${local.project}-${local.environment}-order-tg"
  port        = local.order_service_port
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
    Service = local.order_service_name
  })
}

resource "aws_lb_listener_rule" "order_service" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 120
  
  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.order_service.arn
  }
  
  condition {
    path_pattern {
      values = ["/api/orders/*"]
    }
  }
  
  tags = merge(local.common_tags, {
    Service = local.order_service_name
  })
}

resource "aws_ecs_service" "order_service" {
  name            = "${local.project}-${local.environment}-${local.order_service_name}"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.order_service.arn
  desired_count   = var.order_service_desired_count
  launch_type     = "FARGATE"
  
  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.order_service.id]
    assign_public_ip = false
  }
  
  load_balancer {
    target_group_arn = aws_lb_target_group.order_service.arn
    container_name   = local.order_service_name
    container_port   = local.order_service_port
  }
  
  depends_on = [
    aws_lb_listener_rule.order_service,
    aws_db_instance.order
  ]
  
  tags = merge(local.common_tags, {
    Service = local.order_service_name
  })
}
