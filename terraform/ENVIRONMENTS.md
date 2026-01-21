# 환경별 구성 가이드

## 📋 환경 구조

```
terraform/environments/
├── dev/          # 개발 환경
├── staging/      # 스테이징 환경
└── production/   # 프로덕션 환경
```

## 🎯 각 환경의 목적

### Dev (개발)
- **용도**: 개발자들이 기능 개발 및 테스트
- **특징**: 
  - 가장 작은 리소스
  - 빠른 배포/삭제
  - 비용 최소화
- **배포 시점**: Feature 브랜치 머지 시

### Staging (스테이징)
- **용도**: 프로덕션 배포 전 최종 검증
- **특징**:
  - 프로덕션과 유사한 환경
  - QA 테스트 수행
  - 성능 테스트
- **배포 시점**: Develop 브랜치 머지 시

### Production (프로덕션)
- **용도**: 실제 서비스 운영
- **특징**:
  - 가장 큰 리소스
  - 고가용성 설정
  - 백업 및 모니터링 강화
- **배포 시점**: Main 브랜치 머지 시 (수동 승인)

## 📊 환경별 리소스 비교

| 리소스 | Dev | Staging | Production |
|--------|-----|---------|------------|
| **ECS CPU** | 256 | 256 | 512 |
| **ECS Memory** | 512MB | 512MB | 1024MB |
| **ECS Task 수** | 1 | 2 | 4 |
| **RDS Instance** | db.t3.micro | db.t3.micro | db.t3.small |
| **RDS Storage** | 20GB | 20GB | 50GB |
| **RDS Backup** | 3일 | 7일 | 30일 |
| **Redis Node** | cache.t3.micro | cache.t3.micro | cache.t3.small |
| **Log 보관** | 3일 | 7일 | 30일 |
| **NAT Gateway** | 1개 | 2개 | 2개 |

## 💰 예상 비용

### 월간 비용 (30일 기준)

| 환경 | ECS Fargate | RDS | Redis | NAT Gateway | ALB | 합계 |
|------|-------------|-----|-------|-------------|-----|------|
| **Dev** | ~$15 | ~$15 | ~$12 | ~$32 | - | **~$74/월** |
| **Staging** | ~$30 | ~$15 | ~$12 | ~$64 | - | **~$121/월** |
| **Production** | ~$120 | ~$50 | ~$25 | ~$64 | ~$20 | **~$279/월** |

**전체 합계 (30일): 약 $474/월**

---

### 실제 운영 비용 (Dev 3일 + Staging 2일 + Production 3일)

#### Dev (3일 운영)
- ECS Fargate: $15 × (3/30) = **$1.5**
- RDS: $15 × (3/30) = **$1.5**
- Redis: $12 × (3/30) = **$1.2**
- NAT Gateway: $32 × (3/30) = **$3.2**
- **Dev 3일 합계: ~$7.4**

#### Staging (2일 운영)
- ECS Fargate: $30 × (2/30) = **$2.0**
- RDS: $15 × (2/30) = **$1.0**
- Redis: $12 × (2/30) = **$0.8**
- NAT Gateway: $64 × (2/30) = **$4.3**
- **Staging 2일 합계: ~$8.1**

#### Production (3일 운영)
- ECS Fargate: $120 × (3/30) = **$12.0**
- RDS: $50 × (3/30) = **$5.0**
- Redis: $25 × (3/30) = **$2.5**
- NAT Gateway: $64 × (3/30) = **$6.4**
- ALB: $20 × (3/30) = **$2.0**
- **Production 3일 합계: ~$27.9**

---

### 📊 비용 요약

| 시나리오 | Dev | Staging | Production | 총 비용 |
|----------|-----|---------|------------|---------|
| **월간 (30일)** | $74 | $121 | $279 | **$474** |
| **실제 운영 (3+2+3일)** | $7.4 | $8.1 | $27.9 | **$43.4** |

**💡 Tip**: 
- Dev/Staging은 필요할 때만 켜고 끄면 비용 절감 가능
- Production만 24/7 운영 시: ~$279/월
- 테스트 기간에만 Dev/Staging 운영 시: 월 $300 이하로 관리 가능
- **자세한 비용 절감 방법은 `terraform/COST_OPTIMIZATION.md` 참고!** 💰

## 🔧 환경별 설정 차이

### Dev 환경 특징
```hcl
# terraform/environments/dev/variables.tf
user_service_cpu = 256
user_service_memory = 512
user_service_desired_count = 1

# RDS
instance_class = "db.t3.micro"
backup_retention_period = 3
skip_final_snapshot = true

# CloudWatch
retention_in_days = 3
```

### Staging 환경 특징
```hcl
# terraform/environments/staging/variables.tf
user_service_cpu = 256
user_service_memory = 512
user_service_desired_count = 2

# RDS
instance_class = "db.t3.micro"
backup_retention_period = 7
skip_final_snapshot = true

# CloudWatch
retention_in_days = 7
```

### Production 환경 특징
```hcl
# terraform/environments/production/variables.tf
user_service_cpu = 512
user_service_memory = 1024
user_service_desired_count = 4

# RDS
instance_class = "db.t3.small"
backup_retention_period = 30
skip_final_snapshot = false  # 최종 스냅샷 생성!

# CloudWatch
retention_in_days = 30
```

## 🚀 환경별 배포 방법

### Dev 배포
```bash
cd terraform/environments/dev
terraform init
terraform plan
terraform apply
```

### Staging 배포
```bash
cd terraform/environments/staging
terraform init
terraform plan
terraform apply
```

### Production 배포
```bash
cd terraform/environments/production
terraform init
terraform plan

# ⚠️ Production은 신중하게!
# Plan 결과를 팀원들과 리뷰 후 진행
terraform apply
```

## 📝 환경별 변수 파일

각 환경마다 `terraform.tfvars` 파일을 별도로 관리:

```bash
# Dev
terraform/environments/dev/terraform.tfvars
user_service_image_tag = "dev-latest"
user_db_password = "dev_password_123"

# Staging
terraform/environments/staging/terraform.tfvars
user_service_image_tag = "v1.2.3-rc1"
user_db_password = "staging_password_456"

# Production
terraform/environments/production/terraform.tfvars
user_service_image_tag = "v1.2.3"
user_db_password = "prod_strong_password_789"
```

## 🔐 보안 고려사항

### Dev
- 간단한 비밀번호 허용 (개발 편의성)
- Public Subnet 사용 가능 (디버깅 편의)
- 보안 그룹 규칙 느슨

### Staging
- 프로덕션과 동일한 보안 수준
- Private Subnet 사용
- 보안 그룹 규칙 엄격

### Production
- 강력한 비밀번호 필수
- Secrets Manager 사용 필수
- Private Subnet만 사용
- 보안 그룹 최소 권한 원칙
- 암호화 필수 (RDS, S3 등)

## 🔄 환경 간 데이터 동기화

### Dev → Staging
```bash
# RDS 스냅샷 복원
aws rds create-db-snapshot \
  --db-instance-identifier unbox-dev-user-db \
  --db-snapshot-identifier dev-to-staging-snapshot

aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier unbox-staging-user-db \
  --db-snapshot-identifier dev-to-staging-snapshot
```

### Staging → Production
```bash
# ⚠️ 주의: Production 데이터 덮어쓰기 전 백업 필수!
# 일반적으로 Staging → Production 동기화는 하지 않음
# Production → Staging 동기화가 일반적
```

## 📈 환경별 모니터링

### Dev
- 기본 CloudWatch 메트릭
- 에러 로그만 알림

### Staging
- CloudWatch 대시보드
- 주요 메트릭 알림
- 성능 테스트 결과 수집

### Production
- CloudWatch 대시보드
- 주요 메트릭 알림
- 성능 테스트 결과 수집

## ⚠️ 주의사항

### 1. 환경 격리
- 각 환경은 완전히 독립적인 AWS 리소스 사용
- VPC, Subnet, Security Group 모두 분리
- State 파일도 분리 (dev/, staging/, production/)

### 2. 비용 관리
- Dev 환경은 업무 시간 외 중지 고려
- Staging은 필요시에만 실행
- Production은 24/7 운영
- **상세 가이드**: `terraform/COST_OPTIMIZATION.md` 참고

### 3. 배포 순서
```
1. Dev 배포 → 테스트
2. Staging 배포 → QA 검증
3. Production 배포 → 모니터링
```

### 4. 롤백 전략
- Dev: 즉시 롤백 가능
- Staging: 이전 버전으로 롤백
- Production: Blue/Green 배포 권장

## 🎓 다음 단계

1. **Dev 환경 먼저 구축**
   ```bash
   cd terraform/environments/dev
   terraform apply
   ```

2. **Dev에서 충분히 테스트**
   - 모든 서비스 정상 작동 확인
   - 서비스 간 통신 확인

3. **Staging 구축**
   ```bash
   cd terraform/environments/staging
   terraform apply
   ```

4. **Staging에서 QA 진행**
   - 통합 테스트
   - 성능 테스트
   - 보안 테스트

5. **Production 배포**
   ```bash
   cd terraform/environments/production
   terraform apply
   ```

6. **Production 모니터링**
   - 메트릭 확인
   - 로그 모니터링
   - 알림 설정 확인
