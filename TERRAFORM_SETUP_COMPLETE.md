# ✅ Terraform 구성 (3단계 환경)

## 🎉 완료된 작업

각 환경에 맞는 Terraform 구성을 완료했습니다!

### 핵심 특징
1. **지금 당장 사용 가능** - 공통 모듈 기다릴 필요 없음
2. **중앙 집중식 관리** - 한 곳에서 모든 서비스 인프라 관리
3. **3단계 환경** - Dev, Staging, Production 완벽 분리
4. **쉬운 마이그레이션** - 나중에 공통 모듈로 교체 가능

## 📁 생성된 파일 구조

```
terraform/
├── README.md                          # 전체 개요
├── GETTING_STARTED.md                 # 시작 가이드 ⭐
├── ENVIRONMENTS.md                    # 환경별 가이드 ⭐⭐
├── MIGRATION_GUIDE.md                 # 공통 모듈 마이그레이션 가이드
├── .gitignore                         # Git 제외 파일
│
├── environments/
│   ├── dev/                           # 개발 환경 (작은 리소스)
│   │   ├── main.tf
│   │   ├── shared-infra.tf
│   │   ├── user-service.tf
│   │   ├── product-service.tf
│   │   ├── trade-service.tf
│   │   ├── order-service.tf
│   │   ├── payment-service.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   └── terraform.tfvars.example
│   │
│   ├── staging/                       # 스테이징 환경 (중간 리소스)
│   │   └── (동일한 파일 구조)
│   │
│   └── production/                    # 프로덕션 환경 (큰 리소스)
│       └── (동일한 파일 구조)
│
└── modules/  (나중에 인프라팀이 제공)
    ├── ecs-service/
    ├── rds/
    └── redis/
```

## 🚀 다음 단계

### 1. 환경 선택

**추천 순서: Dev → Staging → Production**

```bash
# Dev 환경부터 시작 (가장 작고 저렴)
cd terraform/environments/dev
```

### 2. 즉시 할 일

#### A. 변수 파일 생성
```bash
cd terraform/environments/dev  # 또는 staging, production
cp terraform.tfvars.example terraform.tfvars
```

#### B. terraform.tfvars 수정
```hcl
# 최소한 이것들은 수정 필요
user_service_image_tag = "v1.0.0"
user_db_password = "강력한비밀번호123!"

# Secrets Manager ARN (인프라팀에게 요청)
user_db_password_secret_arn = "arn:aws:secretsmanager:..."
jwt_secret_arn = "arn:aws:secretsmanager:..."
```

#### C. 낙균님에게 요청할 것
```
1. S3 Backend 설정 (환경별)
   - Bucket: unbox-terraform-state
   - Keys: dev/, staging/, production/
   - DynamoDB: unbox-terraform-locks

2. Secrets Manager 생성 (환경별)
   Dev:
   - dev/user-db-password
   - dev/product-db-password
   - dev/order-db-password
   - dev/payment-db-password
   - dev/trade-db-password
   - dev/jwt-secret
   
   Staging:
   - staging/user-db-password
   - staging/product-db-password
   - staging/order-db-password
   - staging/payment-db-password
   - staging/trade-db-password
   - staging/jwt-secret
   
   Production:
   - production/user-db-password
   - production/product-db-password
   - production/order-db-password
   - production/payment-db-password
   - production/trade-db-password
   - production/jwt-secret

3. ECR 레포지토리 생성
   - unbox-user-service
   - unbox-product-service
   - unbox-order-service
   - unbox-payment-service
   - unbox-trade-service
```

### 2. 첫 배포 (Dev)
```bash
cd terraform/environments/dev

# 초기화
terraform init

# 계획 확인
terraform plan

# 배포
terraform apply
```

### 3. 서비스 확인
```bash
# ALB DNS 확인
terraform output alb_dns_name

# 서비스 테스트
curl http://ALB_DNS/api/users/health
curl http://ALB_DNS/api/products/health
```

## 🎯 환경별 특징 (낙균님 의견따라서 수정예정)

| 항목 | Dev | Staging | Production |
|------|-----|---------|------------|
| **용도** | 개발/테스트 | QA 검증 | 실제 서비스 |
| **ECS Task** | 1개 | 2개 | 4개 |
| **CPU/Memory** | 256/512 | 256/512 | 512/1024 |
| **RDS** | t3.micro | t3.micro | t3.small |
| **NAT Gateway** | 1개 | 2개 | 2개 |
| **백업 보관** | 3일 | 7일 | 30일 |
| **로그 보관** | 3일 | 7일 | 30일 |
| **예상 비용** | ~$74/월 | ~$121/월 | ~$279/월 |

**전체 예상 비용: 약 $474/월**

자세한 내용은 `terraform/ENVIRONMENTS.md` 참고!

## 🔧 일상적인 작업

### 새 버전 배포
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

### 새 서비스 추가
```bash
# 1. 기존 서비스 파일 복사
cp user-service.tf notification-service.tf

# 2. 수정 (포트, 이름 등)
vim notification-service.tf

# 3. variables.tf에 변수 추가
vim variables.tf

# 4. terraform.tfvars에 값 추가
vim terraform.tfvars

# 5. 배포
terraform apply
```

## 🎓 학습 자료

### 필수 읽기
1. `terraform/ENVIRONMENTS.md` - **환경별 차이 및 배포 전략** ⭐⭐
2. `terraform/GETTING_STARTED.md` - 시작 가이드
3. `terraform/README.md` - 전체 개요

### 나중에 읽기
4. `terraform/MIGRATION_GUIDE.md` - 공통 모듈 마이그레이션

## 💡 핵심 개념

### 1. 중앙 집중식 관리
```
✅ 장점:
- 한 곳에서 전체 인프라 파악
- 공유 리소스(VPC, ALB) 관리 용이
- State 파일 단순 (환경당 1개)

⚠️ 주의:
- 여러 명이 동시에 apply하면 충돌
- S3 Backend + DynamoDB Lock 필수
```

### 2. 서비스별 파일 분리
```
user-service.tf
product-service.tf
order-service.tf
...

✅ 장점:
- 서비스별로 독립적으로 관리
- 코드 가독성 향상
- 충돌 최소화
```

### 3. 공유 인프라
```
shared-infra.tf:
- VPC, Subnet, NAT Gateway
- ALB (모든 서비스가 공유)
- ECS Cluster (모든 서비스가 공유)
- IAM Role (모든 서비스가 공유)

✅ 장점:
- 비용 절감 (ALB 하나만 사용)
- 관리 포인트 감소
```

## 🔄 공통 모듈 마이그레이션

인프라팀이 공통 모듈을 제공하면:

### Before (현재)
```hcl
# user-service.tf 
resource "aws_ecs_task_definition" "user_service" {
  # 모든 설정 직접 작성
}
resource "aws_ecs_service" "user_service" {
  # 모든 설정 직접 작성
}
resource "aws_db_instance" "user" {
  # 모든 설정 직접 작성
}
```

### After (나중에)
```hcl
# user-service.tf 
module "user_service" {
  source = "../../modules/ecs-service"
  service_name = "user-service"
  port = 8081
  # 간단한 변수만 전달
}

module "user_db" {
  source = "../../modules/rds"
  identifier = "unbox-staging-user-db"
  # 간단한 변수만 전달
}
```

**70% 코드 감소!**

## 📞 도움이 필요하면

### 인프라팀에게 문의
- S3 Backend 설정
- Secrets Manager 생성
- ECR 레포지토리 생성
- 공통 모듈 제공 시기

### 문서 참고
- `terraform/GETTING_STARTED.md` - 상세 가이드
- `terraform/MIGRATION_GUIDE.md` - 마이그레이션 가이드

## ✨ 요약

```
✅ 지금 당장 사용 가능한 Terraform 구성 완료
✅ 3단계 환경 (Dev, Staging, Production) 완벽 분리
✅ 공통 모듈 없이도 작동
✅ 나중에 공통 모듈로 쉽게 마이그레이션 가능
✅ 모노레포 구조에 최적화
✅ 5개 서비스 모두 구성 완료

환경별 특징:
- Dev: 작은 리소스, 빠른 테스트 (~$74/월)
- Staging: 중간 리소스, QA 검증 (~$121/월)
- Production: 큰 리소스, 실제 서비스 (~$279/월)

다음 단계:
1. terraform/ENVIRONMENTS.md 읽기 ⭐
2. Dev 환경부터 terraform.tfvars 설정
3. 인프라팀에게 Backend/Secrets 요청
4. terraform init && terraform apply
5. Dev 테스트 → Staging 배포 → Production 배포
6. 배포 완료! 🎉
```
