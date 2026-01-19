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
echo "🔍 소스 디렉토리 내용: $TEMPLATE_DIR/docker/local/"
ls -la "$TEMPLATE_DIR/docker/local/" || echo "❌ 소스 디렉토리에 접근할 수 없습니다."

# 숨김 파일도 포함하여 복사
cp -r "$TEMPLATE_DIR/docker/local/." "$TARGET_DIR/docker/local/"
if [ $? -eq 0 ]; then
    echo "✅ docker/local/ 파일 복사 완료"
else
    echo "❌ docker/local/ 파일 복사 실패"
fi

echo "🔧 scripts/ 파일들 복사 중..."
cp -r "$TEMPLATE_DIR/scripts/." "$TARGET_DIR/scripts/"
if [ $? -eq 0 ]; then
    echo "✅ scripts/ 파일 복사 완료"
else
    echo "❌ scripts/ 파일 복사 실패"
fi

# 실행 권한 부여
chmod +x "$TARGET_DIR/scripts/"*.sh

# .env 파일 생성
echo "📝 환경 설정 파일 처리 중..."
echo "🔍 디버깅: $TARGET_DIR/docker/local/ 디렉토리 내용 확인..."
ls -la "$TARGET_DIR/docker/local/" || echo "❌ docker/local 디렉토리에 접근할 수 없습니다."

if [ -f "$TARGET_DIR/docker/local/.env.example" ]; then
    echo "✅ .env.example 파일을 찾았습니다."
    
    if [ -f "$TARGET_DIR/docker/local/.env" ]; then
        echo "⚠️  .env 파일이 이미 존재합니다. 백업을 생성합니다..."
        BACKUP_FILE="$TARGET_DIR/docker/local/.env.backup.$(date +%Y%m%d_%H%M%S)"
        cp "$TARGET_DIR/docker/local/.env" "$BACKUP_FILE"
        if [ $? -eq 0 ]; then
            echo "📋 기존 .env 파일을 $(basename $BACKUP_FILE)로 백업했습니다."
        else
            echo "❌ 백업 생성에 실패했습니다. 권한을 확인해주세요."
        fi
    fi
    
    echo "📝 .env.example에서 .env 파일을 생성합니다..."
    cp "$TARGET_DIR/docker/local/.env.example" "$TARGET_DIR/docker/local/.env"
    
    # 복사 결과 확인
    if [ $? -eq 0 ] && [ -f "$TARGET_DIR/docker/local/.env" ]; then
        echo "✅ .env 파일이 성공적으로 생성되었습니다."
        echo "📄 생성된 파일 정보:"
        ls -la "$TARGET_DIR/docker/local/.env"
        echo ""
        echo "🔧 반드시 $TARGET_DIR/docker/local/.env 파일을 열어서 다음 항목들을 수정하세요:"
        echo "   - POSTGRES_PASSWORD: 데이터베이스 비밀번호 (현재값: ChangeHERE!)"
        echo "   - JWT_SECRET: JWT 시크릿 키 (32자 이상 권장, 현재값: ChangeHERE!)"
    else
        echo "❌ .env 파일 생성에 실패했습니다."
        echo "🔍 문제 해결 방법:"
        echo "   1. 디렉토리 권한 확인: ls -la $TARGET_DIR/docker/local/"
        echo "   2. 수동으로 복사: cp $TARGET_DIR/docker/local/.env.example $TARGET_DIR/docker/local/.env"
        echo "   3. 현재 사용자 권한 확인: whoami && id"
    fi
else
    echo "❌ .env.example 파일을 찾을 수 없습니다."
    echo "🔍 예상 위치: $TARGET_DIR/docker/local/.env.example"
    echo "📁 현재 디렉토리 내용:"
    ls -la "$TARGET_DIR/docker/local/" 2>/dev/null || echo "   디렉토리에 접근할 수 없습니다."
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
echo "🚀 CI/CD 파이프라인도 설정하려면:"
echo "   ./scripts/deploy-workflows.sh $SERVICE_NAME $TARGET_DIR"
echo ""