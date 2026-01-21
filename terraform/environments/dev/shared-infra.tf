# ============================================
# 공유 인프라 리소스
# (인프라팀이 나중에 별도 관리할 수 있음)
# ============================================

# VPC
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true
  
  tags = merge(local.common_tags, {
    Name = "${local.project}-${local.environment}-vpc"
  })
}

# Internet Gateway
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
  
  tags = merge(local.common_tags, {
    Name = "${local.project}-${local.environment}-igw"
  })
}

# Public Subnets (ALB용)
resource "aws_subnet" "public" {
  count = 2
  
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.${count.index}.0/24"
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true
  
  tags = merge(local.common_tags, {
    Name = "${local.project}-${local.environment}-public-${count.index + 1}"
    Type = "public"
  })
}

# Private Subnets (ECS, RDS용)
resource "aws_subnet" "private" {
  count = 2
  
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.${count.index + 10}.0/24"
  availability_zone = data.aws_availability_zones.available.names[count.index]
  
  tags = merge(local.common_tags, {
    Name = "${local.project}-${local.environment}-private-${count.index + 1}"
    Type = "private"
  })
}

# NAT Gateway용 Elastic IP (Dev: 1개만)
resource "aws_eip" "nat" {
  count  = 1
  domain = "vpc"
  
  tags = merge(local.common_tags, {
    Name = "${local.project}-${local.environment}-nat-eip-${count.index + 1}"
  })
  
  depends_on = [aws_internet_gateway.main]
}

# NAT Gateway (Dev: 1개만)
resource "aws_nat_gateway" "main" {
  count = 1
  
  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id
  
  tags = merge(local.common_tags, {
    Name = "${local.project}-${local.environment}-nat-${count.index + 1}"
  })
}

# Public Route Table
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id
  
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }
  
  tags = merge(local.common_tags, {
    Name = "${local.project}-${local.environment}-public-rt"
  })
}

# Private Route Tables (Dev: 모두 첫 번째 NAT Gateway 사용)
resource "aws_route_table" "private" {
  count = 2
  
  vpc_id = aws_vpc.main.id
  
  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main[0].id  # Dev: 모두 첫 번째 NAT 사용
  }
  
  tags = merge(local.common_tags, {
    Name = "${local.project}-${local.environment}-private-rt-${count.index + 1}"
  })
}

# Route Table Associations
resource "aws_route_table_association" "public" {
  count = 2
  
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private" {
  count = 2
  
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private[count.index].id
}

# ============================================
# Application Load Balancer (공유)
# ============================================

# ALB Security Group
resource "aws_security_group" "alb" {
  name        = "${local.project}-${local.environment}-alb-sg"
  description = "Security group for Application Load Balancer"
  vpc_id      = aws_vpc.main.id
  
  # HTTP
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow HTTP from anywhere"
  }
  
  # HTTPS
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow HTTPS from anywhere"
  }
  
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all outbound traffic"
  }
  
  tags = merge(local.common_tags, {
    Name = "${local.project}-${local.environment}-alb-sg"
  })
}

# Application Load Balancer
resource "aws_lb" "main" {
  name               = "${local.project}-${local.environment}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id
  
  enable_deletion_protection = false
  enable_http2              = true
  
  tags = merge(local.common_tags, {
    Name = "${local.project}-${local.environment}-alb"
  })
}

# Default Target Group (404 응답용)
resource "aws_lb_target_group" "default" {
  name        = "${local.project}-${local.environment}-default-tg"
  port        = 80
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"
  
  health_check {
    enabled = false
  }
  
  tags = merge(local.common_tags, {
    Name = "${local.project}-${local.environment}-default-tg"
  })
}

# HTTP Listener (HTTPS로 리다이렉트)
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"
  
  default_action {
    type = "redirect"
    
    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

# HTTPS Listener (나중에 ACM 인증서 추가)
# 지금은 HTTP만 사용
resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.main.arn
  port              = 443
  protocol          = "HTTP"  # 나중에 HTTPS로 변경
  
  # certificate_arn = var.acm_certificate_arn  # 나중에 추가
  
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.default.arn
  }
}

# ============================================
# ECS Cluster (공유)
# ============================================

resource "aws_ecs_cluster" "main" {
  name = "${local.project}-${local.environment}-cluster"
  
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
  
  tags = merge(local.common_tags, {
    Name = "${local.project}-${local.environment}-cluster"
  })
}

# ECS Task Execution Role (공유)
resource "aws_iam_role" "ecs_task_execution" {
  name = "${local.project}-${local.environment}-ecs-task-execution-role"
  
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })
  
  tags = local.common_tags
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution" {
  role       = aws_iam_role.ecs_task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# ECR 접근 권한 추가
resource "aws_iam_role_policy" "ecs_task_execution_ecr" {
  name = "ecr-access"
  role = aws_iam_role.ecs_task_execution.id
  
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken",
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "*"
      }
    ]
  })
}

# ============================================
# Data Sources
# ============================================

data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_caller_identity" "current" {}
