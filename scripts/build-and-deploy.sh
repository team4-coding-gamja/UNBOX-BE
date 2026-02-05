#!/bin/bash

# UNBOX 서비스 빌드 및 배포 스크립트 (Git SHA 기반)

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 변수 설정
SERVICE_NAME=${1:-"unbox-user"}
GIT_SHA=$(git rev-parse --short HEAD)
IMAGE_NAME="${SERVICE_NAME}:${GIT_SHA}"
CLUSTER_NAME="unbox-local"

echo -e "${BLUE}🚀 ${SERVICE_NAME} 빌드 및 배포 시작...${NC}"
echo "Git SHA: ${GIT_SHA}"
echo "Image: ${IMAGE_NAME}"

# 1. Docker 이미지 빌드
echo -e "${YELLOW}📦 Docker 이미지 빌드 중...${NC}"
if [[ "$SERVICE_NAME" == "unbox-user" ]]; then
    SERVICE_DIR="unbox_user"
elif [[ "$SERVICE_NAME" == "unbox-product" ]]; then
    SERVICE_DIR="unbox_product"
elif [[ "$SERVICE_NAME" == "unbox-order" ]]; then
    SERVICE_DIR="unbox_order"
elif [[ "$SERVICE_NAME" == "unbox-trade" ]]; then
    SERVICE_DIR="unbox_trade"
elif [[ "$SERVICE_NAME" == "unbox-payment" ]]; then
    SERVICE_DIR="unbox_payment"
else
    echo -e "${RED}❌ 지원하지 않는 서비스: ${SERVICE_NAME}${NC}"
    exit 1
fi

# ARM64 Mac에서 linux/amd64 이미지 빌드
docker build --platform linux/amd64 -t ${IMAGE_NAME} -f ${SERVICE_DIR}/Dockerfile .

# 2. k3d 클러스터에 이미지 로드
echo -e "${YELLOW}📥 k3d 클러스터에 이미지 로드 중...${NC}"
k3d image import ${IMAGE_NAME} -c ${CLUSTER_NAME}

# 3. Helm values 파일 생성
echo -e "${YELLOW}⚙️  Helm values 파일 생성 중...${NC}"
VALUES_FILE="helm/values-${SERVICE_NAME##*-}-local.yaml"

# 서비스별 포트 설정
case $SERVICE_NAME in
    "unbox-user")
        SERVICE_PORT=8081
        ;;
    "unbox-product")
        SERVICE_PORT=8082
        ;;
    "unbox-order")
        SERVICE_PORT=8083
        ;;
    "unbox-trade")
        SERVICE_PORT=8084
        ;;
    "unbox-payment")
        SERVICE_PORT=8085
        ;;
esac

cat > ${VALUES_FILE} <<EOF
# ${SERVICE_NAME} 로컬 배포용 설정

service:
  name: ${SERVICE_NAME}
  port: ${SERVICE_PORT}
  targetPort: ${SERVICE_PORT}

image:
  repository: ${SERVICE_NAME}
  tag: ${GIT_SHA}
  pullPolicy: Never

env:
  - name: SPRING_PROFILES_ACTIVE
    value: "local"
  - name: SPRING_APPLICATION_NAME
    value: "${SERVICE_NAME}"
  - name: SERVER_PORT
    value: "${SERVICE_PORT}"
  - name: MANAGEMENT_ZIPKIN_TRACING_ENDPOINT
    value: "http://zipkin.monitoring.svc.cluster.local:9411/api/v2/spans"
  - name: MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE
    value: "health,info,metrics,prometheus"
  - name: MANAGEMENT_TRACING_SAMPLING_PROBABILITY
    value: "1.0"

ingress:
  enabled: true
  className: "nginx"
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /\$2
  hosts:
    - host: localhost
      paths:
        - path: /api/v1/${SERVICE_NAME##*-}(/|$)(.*)
          pathType: Prefix
EOF

# 4. Helm 배포
echo -e "${YELLOW}🚀 Helm으로 배포 중...${NC}"
if helm list | grep -q ${SERVICE_NAME}; then
    echo "기존 배포 업그레이드 중..."
    helm upgrade ${SERVICE_NAME} helm/unbox-service/ -f ${VALUES_FILE} --namespace default
else
    echo "새로운 배포 설치 중..."
    helm install ${SERVICE_NAME} helm/unbox-service/ -f ${VALUES_FILE} --namespace default
fi

# 5. 배포 상태 확인
echo -e "${YELLOW}⏳ 배포 상태 확인 중...${NC}"
kubectl rollout status deployment/${SERVICE_NAME} --timeout=120s

# 6. 서비스 정보 출력
echo -e "${GREEN}✅ ${SERVICE_NAME} 배포 완료!${NC}"
echo
echo -e "${BLUE}📋 서비스 정보:${NC}"
echo "이미지: ${IMAGE_NAME}"
echo "포트: ${SERVICE_PORT}"
echo "엔드포인트: http://localhost/api/v1/${SERVICE_NAME##*-}/"
echo "헬스체크: http://localhost/api/v1/${SERVICE_NAME##*-}/actuator/health"
echo
echo -e "${BLUE}📊 모니터링:${NC}"
echo "Grafana: http://localhost:30300"
echo "Zipkin: http://localhost:9411"
echo "ArgoCD: https://localhost:8080"
echo
kubectl get pods -l app=${SERVICE_NAME}
kubectl get svc ${SERVICE_NAME}