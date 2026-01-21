# UNBOX Terraform 구성 (모노레포)

## 📁 디렉토리 구조

```
terraform/
├── environments/
│   ├── staging/
│   │   ├── main.tf                    # Provider, Backend 설정
│   │   ├── shared-infra.tf            # VPC, ALB, ECS Cluster (공통)
│   │   ├── user-service.tf            # User Service 리소스
│   │   ├── product-service.tf         # Product Service 리소스
│   │   ├── order-service.tf           # Order Service 리소스
│   │   ├── payment-service.tf         # Payment Service 리소스
│   │   ├── trade-service.tf           # Trade Service 리소스
│   │   ├── variables.tf               # 공통 변수
│   │   ├── outputs.tf                 # 출력값
│   │   └── terraform.tfvars.example   # 변수 예시
│   │
│   └── production/
│       ├── main.tf
│       ├── shared-infra.tf
│       ├── user-service.tf
│       ├── product-service.tf
│       ├── order-service.tf
│       ├── payment-service.tf
│       ├── trade-service.tf
│       ├── variables.tf
│       ├── outputs.tf
│       └── terraform.tfvars.example
│
└── modules/  (나중에 인프라팀이 제공)
    ├── ecs-service/
    ├── rds/
    └── redis/
```

## 🚀 사용 방법

### 1. 초기 설정

```bash
cd terraform/environments/staging
cp terraform.tfvars.example terraform.tfvars
# terraform.tfvars 파일 수정 (이미지 태그, 비밀번호 등)
```

### 2. Terraform 초기화

```bash
terraform init
```

### 3. 계획 확인

```bash
terraform plan
```

### 4. 배포

```bash
terraform apply
```

## 📝 새 서비스 추가하기

1. `{service-name}.tf` 파일 생성
2. 기존 서비스 파일을 복사해서 수정
3. `variables.tf`에 필요한 변수 추가
4. `terraform.tfvars`에 변수 값 설정
5. `terraform apply`

## 🔄 공통 모듈 마이그레이션 (나중에)

낙균님이 공통 모듈을 제공하면:

1. `terraform/modules/` 디렉토리에 모듈 추가
2. 각 서비스 파일에서 리소스를 모듈 호출로 교체
3. 점진적으로 마이그레이션

## ⚠️ 주의사항

- **State 파일**: S3 Backend 사용 (동시 작업 방지)
- **서비스별 우선순위**: ALB Listener Rule의 priority 값이 겹치지 않도록 주의
- **환경 분리**: staging과 production은 완전히 독립적인 인프라
