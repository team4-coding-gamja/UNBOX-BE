# ============================================
# User Service
# Port: 8081
# ============================================

locals {
  user_service_name = "user-service"
  user_service_port = 8081
}

# ============================================
# RDS PostgreSQL (User Service)
# ============================================

# RDS Security Group
resource "aws_security_group" "user_db" {
  name        = "${local.project}-${local.environment}-user-db-sg"
  description = "Security group for User Service database"
  vpc_id      = aws_vpc.main.id
  
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.user_service.id]
    description     = "Allow PostgreSQL from User Service"
  }
  
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  tags = merge(local.common_tags, {
    Name    = "${local.project}-${local.environment}-user-db-sg"
    Service = local.user_service_name
  })
}

# RDS Subnet Group
resource "aws_db_subnet_group" "user" {
  name       = "${local.project}-${local.environment}-user-db-subnet"
  subnet_ids = aws_subnet.private[*].id
  
  tags = merge(local.common_tags, {
    Name    = "${local.project}-${local.environment}-user-db-subnet"
    Service = local.user_service_name
  })
}

# RDS Instance
resource "aws_db_instance" "user" {
  identifier = "${local.project}-${local.environment}-user-db"
  
  engine         = "postgres"
  engine_version = "15.4"
  instance_class = "db.t3.small"  # Production: small
  
  allocated_storage     = 50  # Production: 50GB
  max_allocated_storage = 200  # Production: 200GB
  storage_type          = "gp3"
  storage_encrypted     = true
  
  db_name  = "unbox_user"
  username = "unbox_user"
  password = var.user_db_password
  port     = 5432
  
  db_subnet_group_name   = aws_db_subnet_group.user.name
  vpc_security_group_ids = [aws_security_group.user_db.id]
  
  backup_retention_period = 30  # Production: 30일
  backup_window          = "03:00-04:00"
  maintenance_window     = "mon:04:00-mon:05:00"
  
  skip_final_snapshot       = false  # Production: 최종 스냅샷 생성
  final_snapshot_identifier = "${local.project}-${local.environment}-user-db-final-snapshot"
  
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
  
  tags = merge(local.common_tags, {
    Name    = "${local.project}-${local.environment}-user-db"
    Service = local.user_service_name
  })
}

# ============================================
# ElastiCache Redis (User Service)
# ============================================

# Redis Security Group
resource "aws_security_group" "user_redis" {
  name        = "${local.project}-${local.environment}-user-redis-sg"
  description = "Security group for User Service Redis"
  vpc_id      = aws_vpc.main.id
  
  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.user_service.id]
    description     = "Allow Redis from User Service"
  }
  
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  tags = merge(local.common_tags, {
    Name    = "${local.project}-${local.environment}-user-redis-sg"
    Service = local.user_service_name
  })
}

# Redis Subnet Group
resource "aws_elasticache_subnet_group" "user" {
  name       = "${local.project}-${local.environment}-user-redis-subnet"
  subnet_ids = aws_subnet.private[*].id
  
  tags = merge(local.common_tags, {
    Name    = "${local.project}-${local.environment}-user-redis-subnet"
    Service = local.user_service_name
  })
}

# Redis Cluster
resource "aws_elasticache_cluster" "user" {
  cluster_id           = "${local.project}-${local.environment}-user-redis"
  engine               = "redis"
  engine_version       = "7.0"
  node_type            = "cache.t3.micro"
  num_cache_nodes      = 1
  parameter_group_name = "default.redis7"
  port                 = 6379
  
  subnet_group_name  = aws_elasticache_subnet_group.user.name
  security_group_ids = [aws_security_group.user_redis.id]
  
  tags = merge(local.common_tags, {
    Name    = "${local.project}-${local.environment}-user-redis"
    Service = local.user_service_name
  })
}

# ============================================
# ECS Service (User Service)
# ============================================

# ECS Service Security Group
resource "aws_security_group" "user_service" {
  name        = "${local.project}-${local.environment}-${local.user_service_name}-sg"
  description = "Security group for User Service"
  vpc_id      = aws_vpc.main.id
  
  ingress {
    from_port       = local.user_service_port
    to_port         = local.user_service_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
    description     = "Allow traffic from ALB"
  }
  
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all outbound traffic"
  }
  
  tags = merge(local.common_tags, {
    Name    = "${local.project}-${local.environment}-${local.user_service_name}-sg"
    Service = local.user_service_name
  })
}

# CloudWatch Log Group
resource "aws_cloudwatch_log_group" "user_service" {
  name              = "/ecs/${local.project}-${local.environment}-${local.user_service_name}"
  retention_in_days = 30  # Production: 30일
  
  tags = merge(local.common_tags, {
    Service = local.user_service_name
  })
}

# ECS Task Definition
resource "aws_ecs_task_definition" "user_service" {
  family                   = "${local.project}-${local.environment}-${local.user_service_name}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.user_service_cpu
  memory                   = var.user_service_memory
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  
  container_definitions = jsonencode([
    {
      name  = local.user_service_name
      image = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/${local.project}-${local.user_service_name}:${var.user_service_image_tag}"
      
      portMappings = [
        {
          containerPort = local.user_service_port
          protocol      = "tcp"
        }
      ]
      
      environment = [
        {
          name  = "SPRING_PROFILES_ACTIVE"
          value = local.environment
        },
        {
          name  = "SERVER_PORT"
          value = tostring(local.user_service_port)
        },
        {
          name  = "SPRING_DATASOURCE_URL"
          value = "jdbc:postgresql://${aws_db_instance.user.endpoint}/unbox_user"
        },
        {
          name  = "SPRING_DATASOURCE_USERNAME"
          value = "unbox_user"
        },
        {
          name  = "SPRING_REDIS_HOST"
          value = aws_elasticache_cluster.user.cache_nodes[0].address
        },
        {
          name  = "SPRING_REDIS_PORT"
          value = "6379"
        }
      ]
      
      secrets = [
        {
          name      = "SPRING_DATASOURCE_PASSWORD"
          valueFrom = var.user_db_password_secret_arn
        },
        {
          name      = "JWT_SECRET"
          valueFrom = var.jwt_secret_arn
        }
      ]
      
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.user_service.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
      
      healthCheck = {
        command = [
          "CMD-SHELL",
          "curl -f http://localhost:${local.user_service_port}/actuator/health || exit 1"
        ]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
      
      essential = true
    }
  ])
  
  tags = merge(local.common_tags, {
    Service = local.user_service_name
  })
}

# ALB Target Group
resource "aws_lb_target_group" "user_service" {
  name        = "${local.project}-${local.environment}-user-tg"
  port        = local.user_service_port
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
    protocol            = "HTTP"
  }
  
  deregistration_delay = 30
  
  tags = merge(local.common_tags, {
    Name    = "${local.project}-${local.environment}-user-tg"
    Service = local.user_service_name
  })
}

# ALB Listener Rule
resource "aws_lb_listener_rule" "user_service" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 100
  
  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.user_service.arn
  }
  
  condition {
    path_pattern {
      values = ["/api/users/*", "/api/auth/*"]
    }
  }
  
  tags = merge(local.common_tags, {
    Service = local.user_service_name
  })
}

# ECS Service
resource "aws_ecs_service" "user_service" {
  name            = "${local.project}-${local.environment}-${local.user_service_name}"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.user_service.arn
  desired_count   = var.user_service_desired_count
  launch_type     = "FARGATE"
  
  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.user_service.id]
    assign_public_ip = false
  }
  
  load_balancer {
    target_group_arn = aws_lb_target_group.user_service.arn
    container_name   = local.user_service_name
    container_port   = local.user_service_port
  }
  
  depends_on = [
    aws_lb_listener_rule.user_service,
    aws_db_instance.user,
    aws_elasticache_cluster.user
  ]
  
  tags = merge(local.common_tags, {
    Service = local.user_service_name
  })
}
