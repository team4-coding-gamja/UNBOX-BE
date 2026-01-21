# 공통 모듈 마이그레이션 가이드

## 📋 개요

현재 Terraform 구성은 **공통 모듈 없이 바로 사용 가능**하도록 만들어놓고 낙균님이 공통 모듈을 제공하면 마이그레이션하겠습니다.

## 🔄 마이그레이션 전략

### Phase 1: 현재 상태 (공통 모듈 없음)
```
terraform/environments/staging/
├── user-service.tf          # AWS 리소스 직접 정의
├── product-service.tf       # AWS 리소스 직접 정의
└── ...
```

### Phase 1: 공통 모듈 사용
```
terraform/
├── modules/                 # 낙균님 제공
│   ├── ecs-service/
│   ├── rds/
│   └── redis/
└── environments/staging/
    ├── user-service.tf      # 모듈 호출만
    └── product-service.tf   # 모듈 호출만
```

**장점**:
- 코드 중복 제거
- 일관된 인프라 구성
- 서비스 추가가 간단

## 📝 마이그레이션 예시

### Before: 직접 정의 (현재)

```hcl
# user-service.tf
resource "aws_ecs_task_definition" "user_service" {
  family                   = "${local.project}-${local.environment}-user-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.user_service_cpu
  memory                   = var.user_service_memory
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  
  container_definitions = jsonencode([{
    name  = "user-service"
    image = "..."
    # ... 50줄 이상의 설정
  }])
}

resource "aws_ecs_service" "user_service" {
  name            = "${local.project}-${local.environment}-user-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.user_service.arn
  # ... 30줄 이상의 설정
}

resource "aws_lb_target_group" "user_service" {
  # ... 20줄 이상의 설정
}

resource "aws_db_instance" "user" {
  # ... 30줄 이상의 설정
}

# 총 150줄 이상
```

### After: 모듈 사용 (나중에)

```hcl
# user-service.tf
module "user_service" {
  source = "../../modules/ecs-service"
  
  service_name       = "user-service"
  port               = 8081
  cpu                = var.user_service_cpu
  memory             = var.user_service_memory
  desired_count      = var.user_service_desired_count
  image_tag          = var.user_service_image_tag
  
  vpc_id             = aws_vpc.main.id
  private_subnet_ids = aws_subnet.private[*].id
  alb_listener_arn   = aws_lb_listener.https.arn
  alb_sg_id          = aws_security_group.alb.id
  
  environment_variables = {
    SPRING_PROFILES_ACTIVE = "staging"
    SERVER_PORT            = "8081"
  }
  
  secrets = {
    SPRING_DATASOURCE_PASSWORD = var.user_db_password_secret_arn
    JWT_SECRET                 = var.jwt_secret_arn
  }
}

module "user_db" {
  source = "../../modules/rds"
  
  identifier     = "unbox-staging-user-db"
  database_name  = "unbox_user"
  username       = "unbox_user"
  password       = var.user_db_password
  
  vpc_id             = aws_vpc.main.id
  subnet_ids         = aws_subnet.private[*].id
  allowed_sg_ids     = [module.user_service.security_group_id]
}

module "user_redis" {
  source = "../../modules/redis"
  
  cluster_id     = "unbox-staging-user-redis"
  vpc_id         = aws_vpc.main.id
  subnet_ids     = aws_subnet.private[*].id
  allowed_sg_ids = [module.user_service.security_group_id]
}

# 총 50줄 정도로 축소!
```

## 🚀 마이그레이션 단계

### Step 1: 낙균님 모듈 완성
```
필요한 모듈:
1. ecs-service: ECS Fargate 서비스 생성
2. rds: PostgreSQL RDS 생성
3. redis: ElastiCache Redis 생성
4. alb-target-group: ALB Target Group 생성 (선택)
```

### Step 2: 모듈 인터페이스 확인
```bash
# 인프라팀이 제공한 모듈 확인
cd terraform/modules/ecs-service
cat README.md
cat variables.tf
```

### Step 3: 한 서비스씩 마이그레이션
```bash
# 1. User Service부터 시작
# 2. user-service.tf 백업
cp user-service.tf user-service.tf.backup

# 3. 모듈 방식으로 재작성
vim user-service.tf

# 4. Plan 확인 (변경사항이 없어야 함!)
terraform plan

# 5. 문제 없으면 다음 서비스로
```

### Step 4: 점진적 마이그레이션
```
Week 1: User Service
Week 2: Product Service
Week 3: Order Service
Week 4: Payment Service
Week 5: Trade Service
```

## ⚠️ 주의사항

### 1. State 파일 관리
```bash
# 마이그레이션 전 State 백업
terraform state pull > backup.tfstate

# 리소스 이동 (필요 할 때)
terraform state mv aws_ecs_service.user_service module.user_service.aws_ecs_service.this
```

### 2. 무중단 마이그레이션
```bash
# Plan에서 destroy가 나오면 안 됨!
terraform plan

# 출력 예시 (OK):
# Plan: 0 to add, 0 to change, 0 to destroy.

# 출력 예시 (위험!):
# Plan: 5 to add, 0 to change, 5 to destroy.
# -> 이 경우 리소스가 재생성됨 (서비스 중단!)
```

### 3. 모듈 버전 고정
```hcl
# 모듈 버전 명시 (Git 태그 사용)
module "user_service" {
  source = "git::https://github.com/your-org/terraform-modules.git//ecs-service?ref=v1.0.0"
  # ...
}
```

## 📊 마이그레이션 체크리스트

- [ ] 낙균님에게 모듈 요청
- [ ] 모듈 문서 확인
- [ ] State 파일 백업
- [ ] 한 서비스씩 마이그레이션
- [ ] `terraform plan`으로 변경사항 확인
- [ ] destroy가 없는지 확인
- [ ] 테스트 환경에서 먼저 시도
- [ ] Production 마이그레이션


### 유지보수성
- 모듈 업데이트 시 모든 서비스에 자동 반영
- 일관된 보안 설정
- 베스트 프랙티스 자동 적용


