#!/bin/bash

# Multi-Repository MSA Development Environment Setup Script

echo "🚀 UNBOX MSA 멀티 리포지토리 개발 환경을 설정합니다..."

# 서비스 목록 및 포트 정의
declare -A SERVICES=(
    ["core-business"]="8080"
    ["user-service"]="8081"
    ["product-service"]="8082"
    ["trade-service"]="8083"
    ["order-service"]="8084"
    ["payment-service"]="8085"
)

# 리포지토리 URL 정의 (실제 URL로 변경 필요)
declare -A REPO_URLS=(
    ["core-business"]="https://github.com/your-org/unbox-core-business.git"
    ["user-service"]="https://github.com/your-org/unbox-user-service.git"
    ["product-service"]="https://github.com/your-org/unbox-product-service.git"
    ["order-service"]="https://github.com/your-org/unbox-order-service.git"
    ["payment-service"]="https://github.com/your-org/unbox-payment-service.git"
    ["trade-service"]="https://github.com/your-org/unbox-trade-service.git"
)

# 작업 디렉토리 생성
WORKSPACE_DIR="unbox-msa-workspace"
mkdir -p $WORKSPACE_DIR
cd $WORKSPACE_DIR

echo "📁 작업 디렉토리: $(pwd)"

# 각 서비스 리포지토리 클론
for service in "${!SERVICES[@]}"; do
    echo ""
    echo "📦 $service 설정 중..."
    
    if [ -d "$service" ]; then
        echo "   ✅ $service 디렉토리가 이미 존재합니다. 업데이트합니다..."
        cd $service
        git pull origin main
        cd ..
    else
        echo "   📥 $service 리포지토리를 클론합니다..."
        # git clone ${REPO_URLS[$service]} $service
        echo "   ⚠️  리포지토리 URL을 설정한 후 주석을 해제하세요: ${REPO_URLS[$service]}"
        mkdir -p $service
    fi
done

echo ""
echo "🔧 개발 환경 설정 스크립트를 생성합니다..."

# 전체 서비스 시작 스크립트 생성
cat > start-all-services.sh << 'EOF'
#!/bin/bash

echo "🚀 모든 UNBOX MSA 서비스를 시작합니다..."

# 서비스 시작 순서 (의존성 고려)
SERVICES=("user-service" "product-service" "trade-service" "payment-service" "order-service" "core-business")

for service in "${SERVICES[@]}"; do
    if [ -d "$service" ]; then
        echo "🔄 $service 시작 중..."
        cd $service
        if [ -f "scripts/local-setup.sh" ]; then
            chmod +x scripts/local-setup.sh
            ./scripts/local-setup.sh
        else
            echo "⚠️  $service/scripts/local-setup.sh 파일이 없습니다."
        fi
        cd ..
        sleep 5
    else
        echo "⚠️  $service 디렉토리가 없습니다."
    fi
done

echo "✅ 모든 서비스 시작 완료!"
EOF

# 전체 서비스 중지 스크립트 생성
cat > stop-all-services.sh << 'EOF'
#!/bin/bash

echo "🛑 모든 UNBOX MSA 서비스를 중지합니다..."

SERVICES=("core-business" "user-service" "product-service" "trade-service" "order-service" "payment-service")

for service in "${SERVICES[@]}"; do
    if [ -d "$service" ]; then
        echo "🔄 $service 중지 중..."
        cd $service
        if [ -f "docker/local/docker-compose.yml" ]; then
            docker-compose -f docker/local/docker-compose.yml down
        fi
        cd ..
    fi
done

echo "✅ 모든 서비스 중지 완료!"
EOF

# 서비스 상태 확인 스크립트 생성
cat > check-services.sh << 'EOF'
#!/bin/bash

echo "🔍 UNBOX MSA 서비스 상태를 확인합니다..."

declare -A SERVICES=(
    ["Core Business (API Gateway)"]="8080"
    ["User Service"]="8081"
    ["Product Service"]="8082"
    ["Trade Service"]="8083"
    ["Order Service"]="8084"
    ["Payment Service"]="8085"
)

for service in "${!SERVICES[@]}"; do
    port=${SERVICES[$service]}
    if curl -f http://localhost:$port/actuator/health > /dev/null 2>&1; then
        echo "✅ $service (포트 $port): 정상"
    else
        echo "❌ $service (포트 $port): 비정상 또는 중지됨"
    fi
done
EOF

# 실행 권한 부여
chmod +x start-all-services.sh
chmod +x stop-all-services.sh
chmod +x check-services.sh

echo ""
echo "✅ 멀티 리포지토리 개발 환경 설정이 완료되었습니다!"
echo ""
echo "📋 사용 가능한 명령어들:"
echo "   전체 서비스 시작: ./start-all-services.sh"
echo "   전체 서비스 중지: ./stop-all-services.sh"
echo "   서비스 상태 확인: ./check-services.sh"
echo ""
echo "🌐 서비스 포트 정보:"
for service in "${!SERVICES[@]}"; do
    port=${SERVICES[$service]}
    echo "   $service: http://localhost:$port"
done
echo ""