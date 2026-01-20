# Terraform 서비스 파일


이 Terraform 구성은 **공통 모듈 없이 바로 사용 가능**하도록 만들었는데 낙균님이 공통 모듈을 제공하면 나중에 쉽게 마이그레이션 예정입니다.

## 📋 사전 준비

### 1. Terraform 설치
```bash
# macOS
brew install terraform

# 버전 확인
terraform version  # 1.0 이상 필요
```

### 2. AWS CLI 설정
```bash
aws configure
# AWS Access Key ID, Secret Access Key 입력
```

### 3. S3 Backend 준비 (낙균님이 제공)
```bash
# S3 버킷 생성 (낙균님이 만들어서 제공 예정)
aws s3 ls s3://unbox-terraform-state

# DynamoDB 테이블 확인 (State Lock용)
aws dynamodb describe-table --table-name unbox-terraform-locks
```

## 🚀 첫 배포 (Staging)

### Step 1: 변수 파일 생성
```bash
cd terraform/environments/staging
cp terraform.tfvars.example terraform.tfvars
```

### Step 2: terraform.tfvars 수정
```hcl
# 이것들은 수정
user_service_image_tag = "v1.0.0"  # 실제 이미지 태그
user_db_password = "강력한비밀번호123!"

# Secrets Manager ARN (낙균님에게 받기)
user_db_password_secret_arn = "arn:aws:secretsmanager:..."
jwt_secret_arn = "arn:aws:secretsmanager:..."
```

### Step 3: Terraform 초기화
```bash
terraform init
```

출력 예시:
```
Initializing the backend...
Successfully configured the backend "s3"!
Terraform has been successfully initialized!
```

### Step 4: 계획 확인
```bash
terraform plan
```

**실제로 생성될 리소스**확인 :
- VPC, Subnet, NAT Gateway
- ALB, ECS Cluster
- 각 서비스별 RDS, ECS Service, Target Group 등

### Step 5: 배포
```bash
terraform apply
```

확인 메시지가 나오면 `yes` 입력.

⏱️ **소요 시간**: 약 15-20분 (RDS 생성이 가장 오래 걸림)

### Step 6: 배포 확인
```bash
# ALB DNS 확인
terraform output alb_dns_name

# 서비스 엔드포인트 확인
terraform output user_service_endpoint
terraform output product_service_endpoint
```

## 📝 새 서비스 추가하기

### 예시: Notification Service 추가

#### 1. 서비스 파일 생성
```bash
cd terraform/environments/staging
cp user-service.tf notification-service.tf
```

#### 2. notification-service.tf 수정
```hcl
locals {
  notification_service_name = "notification-service"
  notification_service_port = 8086 
}

# 나머지는 user-service.tf와 동일한 구조
# 단, 모든 "user"를 "notification"으로 변경
```

#### 3. variables.tf에 변수 추가
```hcl
variable "notification_service_image_tag" {
  description = "Notification service Docker image tag"
  type        = string
  default     = "latest"
}

variable "notification_service_cpu" {
  type    = number
  default = 256
}

# ... 나머지 변수들
```

#### 4. ALB Listener Rule priority 조정
```hcl
# notification-service.tf
resource "aws_lb_listener_rule" "notification_service" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 150  # 다른 서비스와 겹치지 않게!
  
  # ...
}
```

#### 5. 배포
```bash
terraform plan   # 새 서비스만 추가되는지 확인
terraform apply
```

## 🔄 일상적인 작업

### 이미지 태그 업데이트 (배포)
```bash
# terraform.tfvars 수정
user_service_image_tag = "v1.0.1"

# 배포
terraform apply
```

### 리소스 크기 조정
```bash
# terraform.tfvars 수정
user_service_cpu = 512
user_service_memory = 1024
user_service_desired_count = 4

# 적용
terraform apply
```

### 특정 서비스만 업데이트
```bash
# User Service만 업데이트
terraform apply -target=aws_ecs_service.user_service

# ⚠️ 주의: 의존성 문제가 있을 수 있으니 가급적 전체 apply 권장
```

### 인프라 삭제
```bash
# Staging 전체 삭제
terraform destroy

# 특정 서비스만 삭제
terraform destroy -target=aws_ecs_service.user_service
```

## 🔧 문제 해결

### 1. State Lock 에러
```
Error: Error acquiring the state lock
```

**원인**: 다른 사람이 동시에 terraform apply 실행 중

**해결**:
```bash
# 작업이 끝날 때까지 기다리거나
# 정말 필요하면 강제 unlock (위험!)
terraform force-unlock LOCK_ID
```

### 2. RDS 생성 실패
```
Error: Error creating DB Instance: DBInstanceAlreadyExists
```

**원인**: 같은 이름의 RDS가 이미 존재

**해결**:
```bash
# main.tf에서 identifier 변경
identifier = "${local.project}-${local.environment}-user-db-v2"
```

### 3. ECR 이미지 없음
```
Error: CannotPullContainerError
```

**원인**: ECR에 이미지가 없음

**해결**:
```bash
# ECR 레포지토리 확인
aws ecr describe-repositories

# 이미지 푸시 (CI/CD에서 자동으로 해야 함)
docker push ACCOUNT_ID.dkr.ecr.ap-northeast-2.amazonaws.com/unbox-user-service:latest
```

### 4. ALB Listener Rule priority 충돌
```
Error: DuplicateListener
```

**원인**: 같은 priority 값 사용

**해결**: 각 서비스의 priority 값을 다르게 설정
- User Service: 100
- Product Service: 110
- Order Service: 120
- Payment Service: 130
- Trade Service: 140

## 🎓 다음 단계

### 1. 공통 모듈 마이그레이션 (나중에)
낙균님이 `terraform/modules/` 제공하면:

```hcl
# Before (현재)
resource "aws_ecs_service" "user_service" {
  # 모든 설정 직접 작성
}

# After (모듈 사용)
module "user_service" {
  source = "../../modules/ecs-service"
  
  service_name = "user-service"
  port         = 8081
  # 간단한 변수만 전달
}
```

### 2. Production 환경 구성
```bash
# Staging 복사
cp -r terraform/environments/staging terraform/environments/production

# production/main.tf 수정
backend "s3" {
  key = "production/terraform.tfstate"  # staging -> production
}

# 리소스 크기 조정 (Production은 더 크게)
user_service_cpu = 512
user_service_memory = 1024
user_service_desired_count = 4
```

### 3. GitHub Actions 통합
```yaml
# .github/workflows/terraform-apply-staging.yml
- name: Terraform Apply
  run: |
    cd terraform/environments/staging
    terraform apply -auto-approve \
      -var="user_service_image_tag=${{ github.sha }}"
```

## 📚 참고 자료

- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [ECS Fargate 가이드](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/AWS_Fargate.html)
- [ALB 라우팅](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-listeners.html)
