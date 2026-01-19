#!/bin/bash

# GitHub Actions 워크플로우 배포 스크립트
# 각 서비스 리포지토리에 CI/CD 파이프라인을 설정할 때 사용

echo "🚀 UNBOX MSA GitHub Actions 워크플로우 배포 도구"
echo "================================================"

# 사용법 출력
show_usage() {
    echo "사용법: $0 <서비스명> <대상_디렉토리>"
    echo ""
    echo "사용 가능한 서비스:"
    echo "  - core-business    (API Gateway)"
    echo "  - user-service     (사용자 관리)"
    echo "  - product-service  (상품 관리)"
    echo "  - trade-service    (거래 관리)"
    echo "  - order-service    (주문 관리)"
    echo "  - payment-service  (결제 처리)"
    echo ""
    echo "예시:"
    echo "  $0 user-service /path/to/user-service-repo"
    echo "  $0 product-service ."
    echo ""
}

# 파라미터 확인
if [ $# -ne 2 ]; then
    show_usage
    exit 1
fi

SERVICE_NAME=$1
TARGET_DIR=$2

# 서비스 유효성 검사
VALID_SERVICES=("core-business" "user-service" "product-service" "trade-service" "order-service" "payment-service")
if [[ ! " ${VALID_SERVICES[@]} " =~ " ${SERVICE_NAME} " ]]; then
    echo "❌ 잘못된 서비스명: $SERVICE_NAME"
    show_usage
    exit 1
fi

# 워크플로우 템플릿 확인
WORKFLOW_FILE="github-workflow-templates/${SERVICE_NAME}.yml"
if [ ! -f "$WORKFLOW_FILE" ]; then
    echo "❌ 워크플로우 템플릿을 찾을 수 없습니다: $WORKFLOW_FILE"
    echo "   이 스크립트를 UNBOX-BE 루트 디렉토리에서 실행해주세요."
    exit 1
fi

# 대상 디렉토리 확인
if [ ! -d "$TARGET_DIR" ]; then
    echo "❌ 대상 디렉토리를 찾을 수 없습니다: $TARGET_DIR"
    exit 1
fi

echo "📦 $SERVICE_NAME 워크플로우를 $TARGET_DIR 에 배포합니다..."

# .github/workflows 디렉토리 생성
mkdir -p "$TARGET_DIR/.github/workflows"

# 워크플로우 파일 복사
echo "📁 GitHub Actions 워크플로우 복사 중..."
cp "$WORKFLOW_FILE" "$TARGET_DIR/.github/workflows/deploy.yml"

echo ""
echo "✅ $SERVICE_NAME 워크플로우 배포가 완료되었습니다!"
echo ""
echo "📋 다음 단계:"
echo "1. GitHub 리포지토리의 Settings > Secrets and variables > Actions 에서 다음 시크릿을 설정하세요:"
echo ""
echo "   🔑 필수 Secrets:"
echo "   - AWS_ACCESS_KEY_ID"
echo "   - AWS_SECRET_ACCESS_KEY"
echo ""
echo "2. AWS 리소스가 준비되었는지 확인하세요:"
echo "   - ECR 리포지토리: unbox-${SERVICE_NAME}"
echo "   - ECS 클러스터: unbox-staging-cluster, unbox-prod-cluster"
echo "   - ECS 서비스: unbox-${SERVICE_NAME}-staging, unbox-${SERVICE_NAME}-prod"
echo ""
echo "3. 코드를 푸시하면 자동으로 CI/CD 파이프라인이 실행됩니다!"
echo ""
echo "🔧 생성된 파일:"
echo "   $TARGET_DIR/.github/workflows/deploy.yml"
echo ""

# GitHub Secrets 설정 가이드 생성
cat > "$TARGET_DIR/GITHUB_SECRETS_SETUP.md" << 'EOF'
# GitHub Secrets 설정 가이드

## 🔑 필수 Secrets 설정

GitHub 리포지토리의 **Settings > Secrets and variables > Actions** 에서 다음 시크릿들을 설정해주세요:

### AWS 관련
```
AWS_ACCESS_KEY_ID=your_aws_access_key_id
AWS_SECRET_ACCESS_KEY=your_aws_secret_access_key
```

### 선택적 Secrets (알림용)
```
SLACK_WEBHOOK_URL=your_slack_webhook_url
DISCORD_WEBHOOK_URL=your_discord_webhook_url
```

## 🏗️ AWS 리소스 준비사항

### ECR 리포지토리
- 리포지토리명: `unbox-{service-name}`
- 예시: `unbox-user-service`, `unbox-product-service`

### ECS 클러스터
- Staging: `unbox-staging-cluster`
- Production: `unbox-prod-cluster`

### ECS 서비스
- Staging: `unbox-{service-name}-staging`
- Production: `unbox-{service-name}-prod`

## 🚀 배포 트리거

### 자동 배포
- `main` 브랜치 푸시 → Production 환경 배포
- `develop` 브랜치 푸시 → Staging 환경 배포

### 수동 배포
- Actions 탭에서 "Run workflow" 버튼 클릭
- 환경 선택 후 실행

## 📊 모니터링

배포 후 다음 사항들을 확인하세요:
- ECS 서비스 상태
- ALB 헬스 체크
- CloudWatch 로그
- 애플리케이션 메트릭
EOF

echo "📖 GitHub Secrets 설정 가이드도 생성되었습니다:"
echo "   $TARGET_DIR/GITHUB_SECRETS_SETUP.md"
echo ""