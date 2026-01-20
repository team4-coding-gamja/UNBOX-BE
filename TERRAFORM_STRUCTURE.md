# 모노레포 환경에서의 Terraform 구조

## 📁 디렉토리 구조

```
UNBOX-BE/  (모노레포)
├── services/
│   ├── user-service/
│   │   ├── src/
│   │   ├── build.gradle
│   │   └── Dockerfile
│   ├── product-service/
│   ├── order-service/
│   ├── payment-service/
│   └── trade-service/
│
├── terraform/
│   ├── modules/  (낙균님이 제공하는 공통 모듈)
│   │   ├── ecs-service/
│   │   │   ├── main.tf
│   │   │   ├── variables.tf
│   │   │   ├── outputs.tf
│   │   │   └── README.md
│   │   ├── rds/
│   │   │   ├── main.tf
│   │   │   ├── variables.tf
│   │   │   └── outputs.tf
│   │   ├── redis/
│   │   └── alb-target-group/
│   │
│   ├── shared/  (모든 환경에서 공유하는 리소스)
│   │   ├── ecr/
│   │   │   ├── main.tf  (ECR 레포지토리들)
│   │   │   └── outputs.tf
│   │   └── iam/
│   │       └── main.tf  (공통 IAM 역할)
│   │
│   ├── environments/
│   │   ├── staging/
│   │   │   ├── main.tf  (Provider, Backend, 공통 리소스)
│   │   │   ├── vpc.tf  (VPC, Subnet, Security Group)
│   │   │   ├── alb.tf  (공유 ALB)
│   │   │   ├── ecs-cluster.tf  (ECS Cluster)
│   │   │   ├── user-service.tf
│   │   │   ├── product-service.tf
│   │   │   ├── order-service.tf
│   │   │   ├── payment-service.tf
│   │   │   ├── trade-service.tf
│   │   │   ├── variables.tf
│   │   │   ├── outputs.tf
│   │   │   └── terraform.tfvars
│   │   │
│   │   └── production/
│   │       ├── main.tf
│   │       ├── vpc.tf
│   │       ├── alb.tf
│   │       ├── ecs-cluster.tf
│   │       ├── user-service.tf
│   │       ├── product-service.tf
│   │       ├── order-service.tf
│   │       ├── payment-service.tf
│   │       ├── trade-service.tf
│   │       ├── variables.tf
│   │       ├── outputs.tf
│   │       └── terraform.tfvars
│   │
│   └── README.md
│
└── .github/
    └── workflows/
        ├── terraform-plan-staging.yml
        ├── terraform-apply-staging.yml
        ├── terraform-plan-production.yml
        └── terraform-apply-production.yml
```

## 🎯 핵심 개념

### 1. 공통 모듈 (terraform/modules/)
낙균님이 만드는 **재사용 가능한 뼈대**

**예시: ECS Service 모듈**
```hcl
# terraform/modules/ecs-service/main.tf
resource "aws_ecs_task_definition" "this" {
  family                   = var.service_name
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.cpu
  memory                   = var.memory
  
  container_definitions = jsonencode([{
    name  = var.service_name
    image = "${var.ecr_repository_url}:${var.image_tag}"
    portMappings = [{
      containerPort = var.container_port
      protocol      = "tcp"
    }]
    environment = var.environment_variables
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = "/ecs/${var.service_name}"
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }])
}

resource "aws_ecs_service" "this" {
  name            = var.service_name
  cluster         = var.ecs_cluster_id
  task_definition = aws_ecs_task_definition.this.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"
  
  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [var.security_group_id]
    assign_public_ip = false
  }
  
  load_balancer {
    target_group_arn = var.target_group_arn
    container_name   = var.service_name
    container_port   = var.container_port
  }
}
```

### 2. 환경별 설정 (terraform/environments/)
각 환경(staging, production)에서 **모듈을 가져다 사용**

**예시: Staging의 User Service**
```hcl
# terraform/environments/staging/user-service.tf

# User Service용 RDS
module "user_service_db" {
  source = "../../modules/rds"
  
  identifier     = "unbox-user-staging"
  engine         = "postgres"
  engine_version = "15.4"
  instance_class = "db.t3.micro"
  
  database_name = "unbox_user"
  username      = "unbox_user"
  password      = var.user_db_password  # tfvars에서 주입
  
  vpc_id             = aws_vpc.main.id
  subnet_ids         = aws_subnet.private[*].id
  security_group_ids = [aws_security_group.rds.id]
  
  backup_retention_period = 7
  
  tags = {
    Environment = "staging"
    Service     = "user-service"
  }
}

# User Service용 Redis
module "user_service_redis" {
  source = "../../modules/redis"
  
  cluster_id      = "unbox-user-staging"
  node_type       = "cache.t3.micro"
  num_cache_nodes = 1
  
  subnet_ids         = aws_subnet.private[*].id
  security_group_ids = [aws_security_group.redis.id]
  
  tags = {
    Environment = "staging"
    Service     = "user-service"
  }
}

# User Service용 ALB Target Group
resource "aws_lb_target_group" "user_service" {
  name        = "unbox-user-staging"
  port        = 8081
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"
  
  health_check {
    path                = "/actuator/health"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
  }
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
}

# User Service ECS
module "user_service" {
  source = "../../modules/ecs-service"
  
  service_name         = "user-service"
  ecs_cluster_id       = aws_ecs_cluster.main.id
  ecr_repository_url   = data.terraform_remote_state.shared.outputs.ecr_repositories["user-service"]
  image_tag            = var.user_service_image_tag
  
  cpu                  = 256
  memory               = 512
  desired_count        = 2
  container_port       = 8081
  
  private_subnet_ids   = aws_subnet.private[*].id
  security_group_id    = aws_security_group.ecs_service.id
  target_group_arn     = aws_lb_target_group.user_service.arn
  
  environment_variables = [
    {
      name  = "SPRING_PROFILES_ACTIVE"
      value = "staging"
    },
    {
      name  = "SPRING_DATASOURCE_URL"
      value = "jdbc:postgresql://${module.user_service_db.endpoint}/unbox_user"
    },
    {
      name  = "SPRING_DATASOURCE_USERNAME"
      value = "unbox_user"
    },
    {
      name  = "SPRING_REDIS_HOST"
      value = module.user_service_redis.endpoint
    },
    {
      name  = "SPRING_REDIS_PORT"
      value = "6379"
    }
  ]
  
  secrets = [
    {
      name      = "SPRING_DATASOURCE_PASSWORD"
      valueFrom = aws_secretsmanager_secret.user_db_password.arn
    },
    {
      name      = "JWT_SECRET"
      valueFrom = aws_secretsmanager_secret.jwt_secret.arn
    }
  ]
  
  aws_region = var.aws_region
  
  tags = {
    Environment = "staging"
    Service     = "user-service"
  }
}
```

## 🔄 작업 흐름

### 낙균님
1. **공통 모듈 개발** (`terraform/modules/`)
   - ECS Service 모듈
   - RDS 모듈
   - Redis 모듈
   - ALB Target Group 모듈
   
2. **공유 리소스 구성** (`terraform/shared/`)
   - ECR 레포지토리
   - 공통 IAM 역할

3. **환경 뼈대 제공** (`terraform/environments/staging/`)
   - VPC, Subnet, Security Group
   - ECS Cluster
   - 공유 ALB
   - `main.tf`, `variables.tf` 템플릿

### 가현
1. **서비스별 Terraform 파일 작성**
   - `terraform/environments/staging/user-service.tf`
   - 낙균님이 제공한 모듈을 가져다 사용
   - 서비스별 환경변수, 리소스 크기 설정

2. **변수 값 설정**
   - `terraform.tfvars`에 이미지 태그, DB 비밀번호 등 설정

3. **배포**
   - GitHub Actions로 자동화
   - 또는 수동으로 `terraform apply`

## 📝 예시: 새 서비스 추가하기

### Step 1: 서비스 파일 생성
```bash
# terraform/environments/staging/product-service.tf 생성
```

### Step 2: 모듈 사용
```hcl
# terraform/environments/staging/product-service.tf

# Product Service DB
module "product_service_db" {
  source = "../../modules/rds"
  
  identifier     = "unbox-product-staging"
  database_name  = "unbox_product"
  username       = "unbox_user"
  password       = var.product_db_password
  
  # ... 나머지 설정
}

# Product Service ECS
module "product_service" {
  source = "../../modules/ecs-service"
  
  service_name       = "product-service"
  ecs_cluster_id     = aws_ecs_cluster.main.id
  container_port     = 8082
  
  # ... 나머지 설정
}
```

### Step 3: 변수 추가
```hcl
# terraform/environments/staging/variables.tf
variable "product_service_image_tag" {
  description = "Product service Docker image tag"
  type        = string
  default     = "latest"
}

variable "product_db_password" {
  description = "Product service database password"
  type        = string
  sensitive   = true
}
```

### Step 4: 배포
```bash
cd terraform/environments/staging
terraform init
terraform plan
terraform apply
```

## 🚀 GitHub Actions 통합

```yaml
# .github/workflows/deploy-staging.yml
name: Deploy to Staging

on:
  push:
    branches: [develop]
    paths:
      - 'services/**'
      - 'terraform/environments/staging/**'

jobs:
  terraform:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Terraform
        uses: hashicorp/setup-terraform@v2
        
      - name: Terraform Init
        working-directory: terraform/environments/staging
        run: terraform init
        
      - name: Terraform Plan
        working-directory: terraform/environments/staging
        run: terraform plan -out=tfplan
        env:
          TF_VAR_user_service_image_tag: ${{ github.sha }}
          TF_VAR_user_db_password: ${{ secrets.USER_DB_PASSWORD }}
          
      - name: Terraform Apply
        if: github.ref == 'refs/heads/develop'
        working-directory: terraform/environments/staging
        run: terraform apply tfplan
```

## ✅ 장점 요약

1. **한 곳에서 전체 인프라 관리** - 모든 서비스의 인프라를 한눈에 파악
2. **공유 리소스 관리 용이** - VPC, ALB 등을 여러 서비스가 공유
3. **State 파일 단순화** - 환경당 하나의 State 파일
4. **의존성 관리 쉬움** - 서비스 간 참조가 간단
5. **배포 순서 제어** - 공유 리소스 먼저, 서비스는 나중에
6. **코드 중복 최소화** - 모듈 재사용으로 DRY 원칙 준수

## ⚠️ 주의사항

1. **State 파일 잠금** - 낙균님과 동시에 apply하지 않도록 S3 Backend + DynamoDB Lock 사용
2. **서비스별 변경 영향도** - 한 서비스 변경이 다른 서비스에 영향 없도록 모듈화
3. **Terraform Workspace 고려** - 필요시 workspace로 환경 분리 가능
