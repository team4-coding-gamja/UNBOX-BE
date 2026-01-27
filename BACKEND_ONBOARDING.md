# 🚀 백엔드 개발자 온보딩 가이드

> **Unbox MSA Unbox** 백엔드 개발을 위한 필수 정보 및 환경 설정 가이드

---

## 📋 목차

1. [개발 환경 정보](#-개발-환경-정보)
2. [AWS 리소스 접근 정보](#-aws-리소스-접근-정보)
3. [로컬 개발 환경 설정](#-로컬-개발-환경-설정)
4. [데이터베이스 접근](#-데이터베이스-접근)
5. [CI/CD 파이프라인](#-cicd-파이프라인)
6. [개발 워크플로우](#-개발-워크플로우)
7. [트러블슈팅](#-트러블슈팅)

---

## 🌐 개발 환경 정보

### Dev 환경 엔드포인트

```bash
# ALB 주소 (모든 서비스 접근)
ALB_URL="http://unbox-dev-alb-<id>.ap-northeast-2.elb.amazonaws.com"

# 서비스별 경로
User Service:    ${ALB_URL}/user/*
Product Service: ${ALB_URL}/product/*
Trade Service:   ${ALB_URL}/trade/*
Order Service:   ${ALB_URL}/order/*
Payment Service: ${ALB_URL}/payment/*
```

**ALB 주소 확인 방법**:
```bash
cd terraform/environments/dev
terraform output alb_address
```

### 서비스 포트 (로컬 개발)

| 서비스 | 포트 | 디렉토리 |
|--------|------|----------|
| User | 8081 | `unbox_user` |
| Product | 8082 | `unbox_product` |
| Trade | 8083 | `unbox_trade` |
| Order | 8084 | `unbox_order` |
| Payment | 8085 | `unbox_payment` |

---

## ☁️ AWS 리소스 접근 정보

### 1. ECR (Docker 이미지 저장소)

```bash
# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin \
  632941626317.dkr.ecr.ap-northeast-2.amazonaws.com

# 이미지 푸시 (예시: user 서비스)
docker tag unbox-user:latest 632941626317.dkr.ecr.ap-northeast-2.amazonaws.com/unbox-dev-user-repo:latest
docker push 632941626317.dkr.ecr.ap-northeast-2.amazonaws.com/unbox-dev-user-repo:latest
```

**ECR Repository URLs**:
```bash
terraform output ecr_repository_urls
```

### 2. ECS (컨테이너 실행 환경)

```bash
# 클러스터 이름
ECS_CLUSTER="unbox-dev-cluster"

# 서비스 목록 확인
aws ecs list-services --cluster unbox-dev-cluster --region ap-northeast-2

# 특정 서비스 상태 확인
aws ecs describe-services \
  --cluster unbox-dev-cluster \
  --services unbox-dev-user \
  --region ap-northeast-2

# 서비스 로그 확인
aws logs tail /ecs/unbox-dev/user --follow --region ap-northeast-2
```

### 3. RDS (PostgreSQL 데이터베이스)

```bash
# RDS 엔드포인트
RDS_ENDPOINT="unbox-dev-common-db.crmw2cqokxc4.ap-northeast-2.rds.amazonaws.com"
RDS_PORT="5432"
MASTER_USER="unbox_admin"
```

**데이터베이스 구조** (Database-per-Service):
- `unbox_user` - User 서비스 전용
- `unbox_product` - Product 서비스 전용
- `unbox_trade` - Trade 서비스 전용
- `unbox_order` - Order 서비스 전용
- `unbox_payment` - Payment 서비스 전용

### 4. Redis (캐시 & 분산 락)

```bash
# Redis 엔드포인트
REDIS_ENDPOINT="unbox-dev-redis.xxxxx.0001.apn2.cache.amazonaws.com:6379"

# SSL 연결 필수
REDIS_SSL_ENABLED="true"
```

### 5. MSK (Kafka)

```bash
# Kafka Bootstrap Servers
KAFKA_BOOTSTRAP_SERVERS="b-1.unbox-dev-msk.xxxxx.kafka.ap-northeast-2.amazonaws.com:9092,..."
```

---

## 💻 로컬 개발 환경 설정

### 사전 요구사항

- **Java 17** (LTS)
- **Docker & Docker Compose**
- **Gradle** (프로젝트에 포함된 Gradle Wrapper 사용)
- **IntelliJ IDEA** (권장) 또는 Eclipse

### 1. 프로젝트 클론

```bash
git clone https://github.com/team4-coding-gamja/UNBOX-BE.git
cd UNBOX-BE
```

### 2. 로컬 전체 실행 (Docker Compose)

```bash
# 모든 서비스 + DB + Redis 실행
docker-compose up -d --build

# 로그 확인
docker-compose logs -f

# 특정 서비스만 재시작
docker-compose restart unbox-user

# 전체 종료
docker-compose down
```


### 3. 공통 모듈 (unbox_common) 빌드

**중요**: 다른 서비스를 실행하기 전에 `unbox_common`을 먼저 빌드해야 합니다.

```bash
# Common 모듈 빌드 및 로컬 Maven 저장소에 설치
cd unbox_common
./gradlew clean build publishToMavenLocal

# 또는 루트에서
./gradlew :unbox_common:publishToMavenLocal
```

**Common 모듈 변경 시**:
- 로컬: 위 명령어 재실행 후 다른 서비스 재시작
- Dev/Prod: `./scripts/trigger-common-rebuild.sh dev` 실행

---

## 🗄 데이터베이스 접근

```

### Dev 환경 (AWS RDS)

**방법 1: Bastion Host 사용** (권장)

```bash
# 1. Bastion Host 생성 (필요시)
cd terraform/environments/dev
terraform apply -target=aws_instance.bastion -auto-approve
sleep 30  # PostgreSQL 클라이언트 설치 대기

# 2. RDS 접속
./scripts/connect_to_rds.sh

# 3. 특정 데이터베이스 접속
./scripts/connect_to_rds.sh unbox_user
```

**방법 : SQL 파일 실행**

```bash
# SQL 파일 작성
cat > my_script.sql << 'EOF'
SELECT * FROM users LIMIT 10;
EOF

# 실행
./scripts/run_sql_on_rds.sh my_script.sql
```

**데이터베이스 비밀번호 확인**:
```bash
# SSM Parameter Store에서 가져오기
aws ssm get-parameter \
  --name /unbox/dev/user/DB_PASSWORD \
  --with-decryption \
  --query 'Parameter.Value' \
  --output text \
  --region ap-northeast-2
```

---

## 🔄 CI/CD 파이프라인

### GitHub Actions 워크플로우

각 서비스는 **CI (빌드/테스트)** 와 **CD (배포)** 워크플로우가 분리되어 있습니다.

#### Dev 환경 배포 플로우

```
코드 푸시 (develop 브랜치)
    ↓
CI: 빌드 & 테스트 & ECR 푸시
    ↓
CD: ECS 서비스 업데이트
    ↓
Discord 알림
```

#### 워크플로우 파일 위치

```
.github/workflows/
├── user-dev-ci.yml      # User 서비스 Dev CI
├── user-dev-cd.yml      # User 서비스 Dev CD
├── user-prod-ci.yml     # User 서비스 Prod CI
├── user-prod-cd.yml     # User 서비스 Prod CD
├── product-dev-ci.yml   # Product 서비스 Dev CI
└── ...
```

### 수동 배포 트리거

```bash
# GitHub CLI 설치 (macOS)
brew install gh
gh auth login

# 특정 서비스 수동 배포
gh workflow run user-dev-ci.yml --ref develop

# 전체 서비스 재빌드 (Common 모듈 변경 시)
./scripts/trigger-common-rebuild.sh dev
```

### GitHub Secrets 설정 (관리자가 설정 완료)

필요한 Secrets:
- `DEV_IAM_ROLE_ARN` - AWS OIDC Role ARN
- `PROD_IAM_ROLE_ARN` - AWS OIDC Role ARN (Prod)
- `DISCORD_WEBHOOK_URL` - Discord 알림 Webhook

---

## 🛠 개발 워크플로우

### 1. 새 기능 개발

```bash
# 1. 최신 코드 가져오기
git checkout develop
git pull origin develop

# 2. 기능 브랜치 생성
git checkout -b feature/user-profile-update

# 3. 코드 작성 및 테스트
# ... 개발 ...

# 4. 로컬 테스트
./gradlew :unbox_user:test

# 5. 커밋 & 푸시
git add .
git commit -m "feat(user): add profile update API"
git push origin feature/user-profile-update

# 6. Pull Request 생성
# GitHub에서 develop 브랜치로 PR 생성
```

### 2. 브랜치 전략

```
main (Prod 환경)
  ↑
develop (Dev 환경)
  ↑
feature/* (기능 개발)
hotfix/* (긴급 수정)
```

---

## 🔍 트러블슈팅

### 문제 1: 로컬에서 서비스가 시작되지 않음

**증상**: `Connection refused` 또는 `Database connection failed`

**해결**:
```bash
# Docker Compose 상태 확인
docker-compose ps

# PostgreSQL 컨테이너 확인
docker logs unbox-postgres

# Redis 컨테이너 확인
docker logs unbox-redis

# 전체 재시작
docker-compose down
docker-compose up -d
```

### 문제 2: Common 모듈 변경이 반영되지 않음

**해결**:
```bash
# Common 모듈 재빌드
cd unbox_common
./gradlew clean build publishToMavenLocal

# 다른 서비스 재빌드
cd ../unbox_user
./gradlew clean build

# IntelliJ에서 Gradle 새로고침
```

### 문제 3: Dev 환경 배포 실패

**확인 사항**:
1. GitHub Actions 로그 확인
2. ECS 서비스 이벤트 확인:
   ```bash
   aws ecs describe-services \
     --cluster unbox-dev-cluster \
     --services unbox-dev-user \
     --query 'services[0].events[0:5]' \
     --region ap-northeast-2
   ```
3. CloudWatch 로그 확인:
   ```bash
   aws logs tail /ecs/unbox-dev/user --follow --region ap-northeast-2
   ```

### 문제 4: 데이터베이스 연결 실패

**Dev 환경**:
- Bastion Host가 생성되어 있는지 확인
- 보안 그룹 규칙 확인
- 비밀번호가 SSM Parameter Store와 일치하는지 확인

**로컬 환경**:
- Docker Compose가 실행 중인지 확인
- `application-local.yml`의 DB 설정 확인

---

## 📚 추가 문서

### 필수 문서
- [README.md](./README.md) - 프로젝트 개요
- [DATABASE_RECOVERY_GUIDE.md](./DATABASE_RECOVERY_GUIDE.md) - DB 복구 가이드
- [scripts/README.md](./scripts/README.md) - 유틸리티 스크립트 사용법

### 트러블슈팅 문서
- [트러블슈팅 모음](./트러블슈팅_모음_2026-01-26.md) - 주요 이슈 해결 사례

### API 문서
- Swagger UI (로컬): `http://localhost:8081/swagger-ui.html`
- Swagger UI (Dev): `http://<ALB_URL>/user/swagger-ui.html`

---

**#ci-cd-alerts** - 배포 알림 (자동)

---

## 🆘 도움이 필요하면?

**문서 먼저 확인**: 위 문서들을 참고


