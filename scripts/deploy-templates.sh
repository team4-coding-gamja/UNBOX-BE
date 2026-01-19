#!/bin/bash

# MSA 서비스별 템플릿 배포 스크립트
# 개발자들이 서비스 분리 후 각 리포지토리에 로컬 환경을 설정할 때 사용

echo "🚀 UNBOX MSA 템플릿 배포 도구"
echo "================================"

# 사용법 출력
show_usage() {
    echo "사용법: $0 <서비스명> <대상_디렉토리>"
    echo ""
    echo "사용 가능한 서비스:"
    echo "  - core-business    (API Gateway, 포트 8080)"
    echo "  - user-service     (사용자 관리, 포트 8081)"
    echo "  - product-service  (상품 관리, 포트 8082)"
    echo "  - trade-service    (거래 관리, 포트 8083)"
    echo "  - order-service    (주문 관리, 포트 8084)"
    echo "  - payment-service  (결제 처리, 포트 8085)"
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

# 템플릿 디렉토리 확인
TEMPLATE_DIR="templates/$SERVICE_NAME"
if [ ! -d "$TEMPLATE_DIR" ]; then
    echo "❌ 템플릿 디렉토리를 찾을 수 없습니다: $TEMPLATE_DIR"
    echo "   이 스크립트를 UNBOX-BE 루트 디렉토리에서 실행해주세요."
    exit 1
fi

# 대상 디렉토리 확인
if [ ! -d "$TARGET_DIR" ]; then
    echo "❌ 대상 디렉토리를 찾을 수 없습니다: $TARGET_DIR"
    exit 1
fi

echo "📦 $SERVICE_NAME 템플릿을 $TARGET_DIR 에 배포합니다..."

# 디렉토리 생성
mkdir -p "$TARGET_DIR/docker/local"
mkdir -p "$TARGET_DIR/scripts"

# 파일 복사
echo "📁 docker/local/ 파일들 복사 중..."
cp -r "$TEMPLATE_DIR/docker/local/"* "$TARGET_DIR/docker/local/"

echo "🔧 scripts/ 파일들 복사 중..."
cp -r "$TEMPLATE_DIR/scripts/"* "$TARGET_DIR/scripts/"

# 실행 권한 부여
chmod +x "$TARGET_DIR/scripts/"*.sh

# .env 파일 생성 안내
if [ -f "$TARGET_DIR/docker/local/.env.example" ] && [ ! -f "$TARGET_DIR/docker/local/.env" ]; then
    echo "📝 .env 파일을 생성합니다..."
    cp "$TARGET_DIR/docker/local/.env.example" "$TARGET_DIR/docker/local/.env"
    echo "✅ .env 파일이 생성되었습니다."
fi

echo ""
echo "✅ $SERVICE_NAME 템플릿 배포가 완료되었습니다!"
echo ""
echo "📋 다음 단계:"
echo "1. $TARGET_DIR/docker/local/.env 파일을 열어서 환경변수를 설정하세요"
echo "2. 로컬 개발 환경을 시작하려면: cd $TARGET_DIR && ./scripts/local-setup.sh"
echo ""
echo "🔧 생성된 파일들:"
echo "   $TARGET_DIR/docker/local/docker-compose.yml"
echo "   $TARGET_DIR/docker/local/.env.example"
echo "   $TARGET_DIR/docker/local/.env"
echo "   $TARGET_DIR/scripts/local-setup.sh"
echo ""