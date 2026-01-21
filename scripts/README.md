# 🚀 UNBOX CI/CD Scripts

UNBOX 모노레포에서 서비스별 CI/CD 파이프라인을 자동으로 생성하는 스크립트 모음입니다.

## 📋 목차

- [개요](#개요)
- [사용 가능한 스크립트](#사용-가능한-스크립트)
- [사용법](#사용법)
- [생성되는 파일](#생성되는-파일)
- [예시](#예시)
- [문제 해결](#문제-해결)

---

## 개요

UNBOX는 모노레포 구조로 여러 마이크로서비스를 관리합니다. 각 서비스마다 독립적인 CI/CD 파이프라인이 필요하며, 이 스크립트는 템플릿 기반으로 자동 생성을 지원합니다.

### 지원 서비스

| 서비스명 | 포트 | 담당 도메인 |
|----------|------|-------------|
| product  | 8082 | 상품 관리 (템플릿) |
| user     | 8081 | 사용자 관리, 인증/인가 |
| order    | 8084 | 주문 처리, 주문 이력 |
| payment  | 8085 | 결제 처리, 정산 |
| trade    | 8083 | 거래 관리, 중고거래 |

---

## 사용 가능한 스크립트

### `generate-service-cicd.sh`

**목적**: Product 서비스를 템플릿으로 사용하여 다른 서비스의 CI/CD 파일을 자동 생성

**사용법**:
```bash
./scripts/generate-service-cicd.sh <service-name> <port>
```

**파라미터**:
- `service-name`: 생성할 서비스 이름 (예: user, order, payment, trade)
- `port`: 서비스 포트 번호 (예: 8081, 8084, 8085, 8083)

---

## 사용법

### 1. 스크립트 실행 권한 확인

```bash
# 실행 권한이 없다면
chmod +x scripts/generate-service-cicd.sh
```

### 2. 서비스 CI/CD 생성

```bash
# User 서비스 생성 (포트 8081)
./scripts/generate-service-cicd.sh user 8081

# Order 서비스 생성 (포트 8084)
./scripts/generate-service-cicd.sh order 8084

# Payment 서비스 생성 (포트 8085)
./scripts/generate-service-cicd.sh payment 8085

# Trade 서비스 생성 (포트 8083)
./scripts/generate-service-cicd.sh trade 8083
```

### 3. 생성된 파일 확인

```bash
# 생성된 파일 목록 확인
git status
```

### 4. 커밋 및 푸시

```bash
git add .
git commit -m "feat(cicd): add user service CI/CD pipeline"
git push origin feat/cicd-pipeline
```

---

## 생성되는 파일

각 서비스당 **7개의 파일**이 자동 생성됩니다:

### 1. Task Definitions (2개)

ECS Fargate에서 실행될 컨테이너 정의 파일

- `task-definitions/dev-{service}-service.json`
  - Dev 환경용 (CPU: 256, Memory: 512)
  - Fargate Spot 사용 (비용 절감)
  
- `task-definitions/prod-{service}-service.json`
  - Prod 환경용 (CPU: 512, Memory: 1024)
  - 일반 Fargate 사용 (안정성)

### 2. AppSpec (1개)

CodeDeploy Blue/Green 배포 설정 파일

- `appspecs/prod-{service}-service.yaml`
  - Prod 환경 Blue/Green 배포 설정
  - Lambda Hook 연결
  - 네트워크 설정

### 3. GitHub Actions Workflows (4개)

CI/CD 자동화 워크플로우

#### Dev 환경
- `.github/workflows/{service}-dev-ci.yml`
  - develop 브랜치 푸시 시 자동 실행
  - 테스트 → 빌드 → ECR 푸시
  - 승인 불필요

- `.github/workflows/{service}-dev-cd.yml`
  - Dev CI 성공 시 자동 실행
  - ECS Rolling Update 배포
  - 승인 불필요

#### Prod 환경
- `.github/workflows/{service}-prod-ci.yml`
  - main 브랜치 푸시 시 실행
  - **수동 승인 필요**
  - 테스트 → 빌드 → ECR 푸시

- `.github/workflows/{service}-prod-cd.yml`
  - Prod CI 성공 시 실행
  - **수동 승인 필요**
  - CodeDeploy Blue/Green 배포
  - Canary 10% (5분 대기)
  - 3단계 Guardrail (Health Check → Lambda Hook → CloudWatch Alarm)

---

## 예시

### User 서비스 생성 예시

```bash
$ ./scripts/generate-service-cicd.sh user 8081

🚀 user 서비스 CI/CD 파일 생성 시작...

📝 1/7: Dev Task Definition 생성 중...
✅ task-definitions/dev-user-service.json
📝 2/7: Prod Task Definition 생성 중...
✅ task-definitions/prod-user-service.json
📝 3/7: Prod AppSpec 생성 중...
✅ appspecs/prod-user-service.yaml
📝 4/7: Dev CI Workflow 생성 중...
✅ .github/workflows/user-dev-ci.yml
📝 5/7: Dev CD Workflow 생성 중...
✅ .github/workflows/user-dev-cd.yml
📝 6/7: Prod CI Workflow 생성 중...
✅ .github/workflows/user-prod-ci.yml
📝 7/7: Prod CD Workflow 생성 중...
✅ .github/workflows/user-prod-cd.yml

🎉 user 서비스 CI/CD 파일 생성 완료!

생성된 파일 목록:
  - task-definitions/dev-user-service.json
  - task-definitions/prod-user-service.json
  - appspecs/prod-user-service.yaml
  - .github/workflows/user-dev-ci.yml
  - .github/workflows/user-dev-cd.yml
  - .github/workflows/user-prod-ci.yml
  - .github/workflows/user-prod-cd.yml

💡 다음 단계:
  1. 생성된 파일들을 확인하세요
  2. 필요시 추가 수정하세요
  3. git add . && git commit -m "feat(cicd): add user service CI/CD"
```

---

## 디렉토리 구조

```
UNBOX-BE/
├── .github/
│   └── workflows/
│       ├── product-dev-ci.yml      # Product 서비스 (템플릿)
│       ├── product-dev-cd.yml
│       ├── product-prod-ci.yml
│       ├── product-prod-cd.yml
│       ├── user-dev-ci.yml         # 생성된 파일들
│       ├── user-dev-cd.yml
│       ├── user-prod-ci.yml
│       └── user-prod-cd.yml
├── task-definitions/
│   ├── dev-product-service.json    # Product 서비스 (템플릿)
│   ├── prod-product-service.json
│   ├── dev-user-service.json       # 생성된 파일들
│   └── prod-user-service.json
├── appspecs/
│   ├── prod-product-service.yaml   # Product 서비스 (템플릿)
│   └── prod-user-service.yaml      # 생성된 파일
├── scripts/
│   ├── generate-service-cicd.sh    # 이 스크립트
│   └── README.md                   # 이 문서
└── unbox_product/                  # 서비스 코드 (개발자가 작성)
    ├── src/
    ├── build.gradle
    └── Dockerfile
```

---

## 변경 감지 및 배포 플로우

### Dev 환경 (develop 브랜치)

```
개발자가 unbox_user/ 수정
    ↓
git push origin develop
    ↓
user-dev-ci.yml 자동 실행 (paths 필터)
    ↓
테스트 → 빌드 → ECR 푸시
    ↓
user-dev-cd.yml 자동 실행
    ↓
ECS Rolling Update 배포
    ↓
Discord 알림
```

### Prod 환경 (main 브랜치)

```
develop → main PR & Merge
    ↓
user-prod-ci.yml 실행 대기
    ↓
수동 승인 ✋
    ↓
테스트 → 빌드 → ECR 푸시
    ↓
user-prod-cd.yml 실행 대기
    ↓
수동 승인 ✋
    ↓
CodeDeploy Blue/Green 배포
    ↓
[Guardrail 1] Health Check
    ↓
[Guardrail 2] Lambda Hook
    ↓
[Guardrail 3] Canary 10% + CloudWatch (5분)
    ↓
100% 트래픽 전환
    ↓
Blue 환경 30분 유지 후 종료
    ↓
Discord 알림
```

---

## 문제 해결

### Q1. 스크립트 실행 권한 오류

```bash
# 오류: Permission denied
$ ./scripts/generate-service-cicd.sh user 8081
-bash: ./scripts/generate-service-cicd.sh: Permission denied

# 해결: 실행 권한 부여
$ chmod +x scripts/generate-service-cicd.sh
```

### Q2. Product 서비스 파일이 없다는 오류

```bash
# 오류: No such file or directory
sed: .github/workflows/product-dev-ci.yml: No such file or directory

# 해결: Product 서비스 파일이 템플릿으로 필요합니다
# UNBOX-BE 루트 디렉토리에서 실행하세요
```

### Q3. 생성된 파일 확인

```bash
# 생성된 파일 목록 확인
git status

# 특정 파일 내용 확인
cat .github/workflows/user-dev-ci.yml
```

### Q4. 잘못 생성된 파일 삭제

```bash
# 특정 서비스 파일 삭제
rm task-definitions/dev-user-service.json
rm task-definitions/prod-user-service.json
rm appspecs/prod-user-service.yaml
rm .github/workflows/user-*.yml

# 다시 생성
./scripts/generate-service-cicd.sh user 8081
```

---

## 주의사항

1. **Product 서비스는 템플릿**입니다. 삭제하지 마세요!
2. **포트 번호**는 각 서비스마다 고유해야 합니다
3. **서비스 이름**은 소문자로 작성하세요 (예: user, order)
4. 생성 후 **반드시 파일 내용을 확인**하세요
5. **인프라 팀**이 AWS 리소스를 먼저 생성해야 배포가 가능합니다

---

## 관련 문서

- [CI/CD Pipeline Design](.kiro/specs/cicd-pipeline/design.md)
- [CI/CD Pipeline Requirements](.kiro/specs/cicd-pipeline/requirements.md)
- [Terraform Setup](../terraform/README.md)

---

## 문의

문제가 발생하면 다음을 확인하세요:
1. 이 README의 문제 해결 섹션
2. `.kiro/specs/cicd-pipeline/` 디렉토리의 설계 문서
3. GitHub Actions 워크플로우 실행 로그
