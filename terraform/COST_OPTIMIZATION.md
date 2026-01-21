# 💰 비용 최적화 가이드

## 🎯 핵심 아이디어

**Dev/Staging 환경은 24/7 운영할 필요가 없습니다!**
- 개발자들이 일하는 시간에만 켜기
- 주말/야간에는 끄기
- 필요할 때만 켜서 테스트하고 끄기

## 💡 비용 절감 효과

### 시나리오 1: 24/7 운영 (현재)
```
Dev:     $74/월 (30일)
Staging: $121/월 (30일)
합계:    $195/월
```

### 시나리오 2: 업무 시간만 운영 (평일 9-6시)
```
Dev:     $74 × (9시간/24시간) × (5일/7일) = $22/월
Staging: $121 × (9시간/24시간) × (5일/7일) = $36/월
합계:    $58/월

절감액: $137/월 (70% 절감!)
```

### 시나리오 3: 필요할 때만 운영 (주 2-3일)
```
Dev:     $74 × (3일/30일) = $7.4/월
Staging: $121 × (2일/30일) = $8.1/월
합계:    $15.5/월

절감액: $179.5/월 (92% 절감!)
```

## 🔧 환경 끄고 켜는 방법

### 방법 1: ECS Task 수를 0으로 변경

가장 간단하고 빠른 방법. RDS/Redis는 유지하고 ECS만 중지.

#### 끄기
```bash
# Dev 환경 중지
cd terraform/environments/dev

# terraform.tfvars 수정
user_service_desired_count = 0
product_service_desired_count = 0
order_service_desired_count = 0
payment_service_desired_count = 0
trade_service_desired_count = 0

# 적용
terraform apply

# 또는 AWS CLI로 직접
aws ecs update-service \
  --cluster unbox-dev-cluster \
  --service unbox-dev-user-service \
  --desired-count 0
```

#### 켜기
```bash
# terraform.tfvars 수정
user_service_desired_count = 1
product_service_desired_count = 1
# ...

# 적용
terraform apply
```

**절감 효과**: ECS 비용만 절감 (~50% 절감)
- Dev: $74 → $37/월
- Staging: $121 → $61/월

---

### 방법 2: RDS/Redis도 중지 

RDS와 Redis도 중지하면 더 많이 절감.

#### RDS 중지 (최대 7일)
```bash
# AWS CLI로 RDS 중지
aws rds stop-db-instance \
  --db-instance-identifier unbox-dev-user-db

# 7일 후 자동으로 다시 시작됨
```

#### RDS 재시작
```bash
aws rds start-db-instance \
  --db-instance-identifier unbox-dev-user-db
```

**⚠️ 주의**: RDS는 최대 7일만 중지 가능. 7일 후 자동으로 재시작됨.

**절감 효과**: ~70% 절감
- Dev: $74 → $22/월
- Staging: $121 → $36/월

---

### 방법 3: 완전 삭제 후 재생성

주말이나 긴 휴가 기간에 완전히 삭제.

#### 삭제
```bash
cd terraform/environments/dev

# ⚠️ 데이터 백업 먼저!
# RDS 스냅샷 생성
aws rds create-db-snapshot \
  --db-instance-identifier unbox-dev-user-db \
  --db-snapshot-identifier dev-backup-$(date +%Y%m%d)

# 인프라 삭제
terraform destroy
```

#### 재생성
```bash
# 인프라 재생성
terraform apply

# 필요시 스냅샷에서 복원
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier unbox-dev-user-db \
  --db-snapshot-identifier dev-backup-20260120
```

**절감 효과**: ~95% 절감
- Dev: $74 → $3.7/월 (스냅샷 저장 비용만)
- Staging: $121 → $6/월

---

## 🤖 자동화 방법

### 1. Terraform으로 자동화

#### 스케줄 변수 추가
```hcl
# terraform/environments/dev/variables.tf
variable "enable_services" {
  description = "Enable or disable services"
  type        = bool
  default     = true
}

variable "enable_databases" {
  description = "Enable or disable databases"
  type        = bool
  default     = true
}
```

#### 조건부 리소스 생성
```hcl
# terraform/environments/dev/user-service.tf
resource "aws_ecs_service" "user_service" {
  count = var.enable_services ? 1 : 0
  # ...
}

resource "aws_db_instance" "user" {
  count = var.enable_databases ? 1 : 0
  # ...
}
```

#### 사용법
```bash
# 끄기
terraform apply -var="enable_services=false"

# 켜기
terraform apply -var="enable_services=true"
```

---

### 2. AWS Lambda + EventBridge로 스케줄링

#### 평일 9시 켜기, 18시 끄기
```python
# lambda_function.py
import boto3

ecs = boto3.client('ecs')
rds = boto3.client('rds')

def lambda_handler(event, context):
    action = event['action']  # 'start' or 'stop'
    
    if action == 'start':
        # ECS 서비스 시작
        ecs.update_service(
            cluster='unbox-dev-cluster',
            service='unbox-dev-user-service',
            desiredCount=1
        )
        
        # RDS 시작
        rds.start_db_instance(
            DBInstanceIdentifier='unbox-dev-user-db'
        )
    
    elif action == 'stop':
        # ECS 서비스 중지
        ecs.update_service(
            cluster='unbox-dev-cluster',
            service='unbox-dev-user-service',
            desiredCount=0
        )
        
        # RDS 중지
        rds.stop_db_instance(
            DBInstanceIdentifier='unbox-dev-user-db'
        )
    
    return {'statusCode': 200}
```

#### EventBridge 규칙
```bash
# 평일 오전 9시 시작 (KST = UTC+9)
aws events put-rule \
  --name dev-start-schedule \
  --schedule-expression "cron(0 0 ? * MON-FRI *)"

# 평일 오후 6시 중지
aws events put-rule \
  --name dev-stop-schedule \
  --schedule-expression "cron(0 9 ? * MON-FRI *)"
```

---

### 3. GitHub Actions로 수동 제어

```yaml
# .github/workflows/control-dev-environment.yml
name: Control Dev Environment

on:
  workflow_dispatch:
    inputs:
      action:
        description: 'Action to perform'
        required: true
        type: choice
        options:
          - start
          - stop

jobs:
  control:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ap-northeast-2
      
      - name: Start/Stop ECS Services
        run: |
          if [ "${{ inputs.action }}" == "start" ]; then
            DESIRED_COUNT=1
          else
            DESIRED_COUNT=0
          fi
          
          aws ecs update-service \
            --cluster unbox-dev-cluster \
            --service unbox-dev-user-service \
            --desired-count $DESIRED_COUNT
          
          aws ecs update-service \
            --cluster unbox-dev-cluster \
            --service unbox-dev-product-service \
            --desired-count $DESIRED_COUNT
      
      - name: Start/Stop RDS
        run: |
          if [ "${{ inputs.action }}" == "start" ]; then
            aws rds start-db-instance \
              --db-instance-identifier unbox-dev-user-db
          else
            aws rds stop-db-instance \
              --db-instance-identifier unbox-dev-user-db
          fi
```

**사용법**: GitHub Actions 탭에서 버튼 클릭으로 켜고 끄기!

---

## 📊 비용 절감 전략 비교

| 방법 | 절감률 | 복잡도 | 데이터 유지 | 재시작 시간 |
|------|--------|--------|-------------|-------------|
| **ECS만 중지** | ~50% | 낮음 | ✅ | 1-2분 |
| **RDS도 중지** | ~70% | 중간 | ✅ | 5-10분 |
| **완전 삭제** | ~95% | 높음 | ❌ (스냅샷 필요) | 15-20분 |
| **자동 스케줄링** | ~70% | 중간 | ✅ | 자동 |

## 🎯 추천 전략

### Dev 환경
```
방법: ECS만 중지 + 수동 제어
이유: 
- 개발자들이 필요할 때 빠르게 켜고 끌 수 있음
- 데이터 유지로 테스트 연속성 보장
- GitHub Actions로 버튼 클릭만으로 제어

절감 효과: $74 → $37/월 (50% 절감)
```

### Staging 환경
```
방법: RDS도 중지 + 스케줄링
이유:
- QA 테스트는 정해진 시간에만 진행
- 평일 업무 시간에만 자동으로 켜기
- 주말/야간 자동 중지

절감 효과: $121 → $36/월 (70% 절감)
```

### Production 환경
```
방법: 24/7 운영 (중지 안 함)
이유:
- 실제 서비스는 항상 가용해야 함
- 비용보다 안정성이 우선

비용: $279/월 (변동 없음)
```

## 💡 실전 팁

### 1. 점심시간에도 끄기
```bash
# 12:00-13:00 중지
# 추가 10% 절감 가능
```

### 2. 금요일 저녁에 끄고 월요일 아침에 켜기
```bash
# 주말 48시간 중지
# 추가 30% 절감 가능
```

### 3. 휴가 기간에는 완전 삭제
```bash
# 1주일 이상 사용 안 할 때
terraform destroy
# 90% 이상 절감
```

### 4. Spot Instance 사용 (추가 절감)
```hcl
# ECS Task에서 Fargate Spot 사용
capacity_provider_strategy {
  capacity_provider = "FARGATE_SPOT"
  weight           = 100
}

# 추가 70% 절감 가능 (Dev/Staging에만 권장)
```

## 📈 예상 절감 효과

### 현재 (24/7 운영)
```
Dev:        $74/월
Staging:    $121/월
Production: $279/월
합계:       $474/월
```

### 최적화 후 (추천 전략)
```
Dev:        $37/월  (ECS만 중지)
Staging:    $36/월  (업무시간만 운영)
Production: $279/월 (24/7 운영)
합계:       $352/월

월간 절감: $122 (26% 절감)
연간 절감: $1,464 💰
```

### 적극 최적화 (주말 완전 중지)
```
Dev:        $22/월  (평일만 운영)
Staging:    $24/월  (평일만 운영)
Production: $279/월 (24/7 운영)
합계:       $325/월

월간 절감: $149 (31% 절감)
연간 절감: $1,788 💰
```

## 🚀 시작하기

### Step 1: GitHub Actions 워크플로우 추가
```bash
# .github/workflows/control-dev-environment.yml 생성
# 위의 예시 코드 복사
```

### Step 2: 테스트
```bash
# GitHub Actions 탭에서 "Control Dev Environment" 실행
# Action: stop 선택
# 5분 후 확인: ECS Task가 0개인지 확인
```

### Step 3: 일상적으로 사용
```
퇴근 전: GitHub Actions에서 "stop" 클릭
출근 후: GitHub Actions에서 "start" 클릭
```

### Step 4: 비용 모니터링
```bash
# AWS Cost Explorer에서 비용 추이 확인
# 1주일 후 절감 효과 확인
```

---

**💡 핵심 요약**: Dev/Staging은 필요할 때만 켜고, 사용 안 할 때는 끄면 **월 $100-150 절감** 가능!
