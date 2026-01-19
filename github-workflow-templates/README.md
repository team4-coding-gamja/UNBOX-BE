# GitHub Actions 워크플로우 템플릿

이 디렉토리는 각 MSA 서비스별 CI/CD 파이프라인 템플릿을 포함합니다.

## 📁 템플릿 구조

```
github-workflow-templates/
├── core-business.yml      # Core Business (API Gateway) 배포
├── user-service.yml       # User Service 배포
├── product-service.yml    # Product Service 배포
├── trade-service.yml      # Trade Service 배포
├── order-service.yml      # Order Service 배포
├── payment-service.yml    # Payment Service 배포
└── shared/               # 공통 설정 및 재사용 가능한 액션들
    ├── build-and-push.yml
    └── deploy-to-ecs.yml
```

## 🚀 사용 방법

### 1. 서비스 분리 완료 후
각 서비스 리포지토리에서 해당 워크플로우 템플릿을 복사합니다.

### 2. 워크플로우 배포
```bash
# 예시: User Service 리포지토리에서
mkdir -p .github/workflows
cp /path/to/templates/github-workflow-templates/user-service.yml .github/workflows/deploy.yml
```

### 3. 환경변수 설정
각 서비스 리포지토리의 GitHub Secrets에 다음 값들을 설정해야 합니다:

#### 필수 Secrets
- `AWS_ACCESS_KEY_ID`: AWS 액세스 키
- `AWS_SECRET_ACCESS_KEY`: AWS 시크릿 키
- `AWS_REGION`: AWS 리전 (예: ap-northeast-2)
- `ECR_REPOSITORY`: ECR 리포지토리 이름
- `ECS_CLUSTER`: ECS 클러스터 이름
- `ECS_SERVICE`: ECS 서비스 이름
- `ECS_TASK_DEFINITION`: 태스크 정의 이름

#### 선택적 Secrets
- `DISCORD_WEBHOOK_URL`: 배포 알림용 (선택사항)

## 🔧 워크플로우 특징

### 공통 기능
- **Docker 이미지 빌드 및 ECR 푸시**
- **ECS 서비스 배포**
- **롤백 기능**
- **배포 알림**
- **헬스 체크**

### 트리거 조건
- `main` 브랜치 푸시 시 자동 배포
- `develop` 브랜치 푸시 시 개발 환경 배포
- 수동 배포 (workflow_dispatch)
- Pull Request 시 빌드 테스트

## 📋 배포 환경별 설정

| 환경 | 브랜치 | ECS 클러스터 | 도메인 |
|------|--------|-------------|--------|
| **Production** | `main` | `unbox-prod-cluster` | `api.unbox.com` |
| **Staging** | `develop` | `unbox-staging-cluster` | `api-staging.unbox.com` |
| **Development** | `feature/*` | `unbox-dev-cluster` | `api-dev.unbox.com` |

## ⚠️ 주의사항

1. **ECR 리포지토리**: 각 서비스별로 미리 생성되어 있어야 함
2. **ECS 태스크 정의**: 초기 태스크 정의가 등록되어 있어야 함
3. **IAM 권한**: GitHub Actions에서 사용할 IAM 역할 권한 설정 필요
4. **보안**: Secrets는 절대 코드에 하드코딩하지 말 것

## 🔄 배포 플로우

```mermaid
graph LR
    A[코드 푸시] --> B[빌드 테스트]
    B --> C[Docker 이미지 빌드]
    C --> D[ECR 푸시]
    D --> E[ECS 태스크 정의 업데이트]
    E --> F[ECS 서비스 배포]
    F --> G[헬스 체크]
    G --> H[배포 완료 알림]
```