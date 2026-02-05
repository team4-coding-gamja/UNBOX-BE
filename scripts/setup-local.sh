#!/bin/bash

# UNBOX 로컬 K8s 환경 자동 구축 스크립트 (k3d 사용)
# 완전한 관측성 스택 포함 (Traces, Metrics, Logs)

set -e  # 에러 발생시 스크립트 중단

echo "🚀 UNBOX 로컬 K8s 환경 + 관측성 스택 구축을 시작합니다 (k3d 사용)..."

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# 클러스터 이름 설정
CLUSTER_NAME="unbox-local"

# 시스템 정보 확인
echo -e "${BLUE}📋 시스템 정보 확인 중...${NC}"
echo "OS: $(uname -s)"
echo "Architecture: $(uname -m)"

# 필수 도구 설치 확인
check_tool() {
    if command -v $1 &> /dev/null; then
        echo -e "${GREEN}✅ $1 설치됨${NC}"
    else
        echo -e "${RED}❌ $1 설치 필요${NC}"
        return 1
    fi
}

echo -e "${BLUE}🔍 필수 도구 확인 중...${NC}"
check_tool docker || { echo "Docker를 먼저 설치해주세요"; exit 1; }
check_tool kubectl || { echo "kubectl을 먼저 설치해주세요"; exit 1; }
check_tool helm || { echo "Helm을 먼저 설치해주세요"; exit 1; }

# k3d 설치 (macOS)
if ! command -v k3d &> /dev/null; then
    echo -e "${YELLOW}📦 k3d 설치 중...${NC}"
    if command -v brew &> /dev/null; then
        brew install k3d
    else
        echo "Homebrew가 없습니다. 수동으로 k3d를 설치해주세요."
        echo "curl -s https://raw.githubusercontent.com/k3d-io/k3d/main/install.sh | bash"
        exit 1
    fi
else
    echo -e "${GREEN}✅ k3d 이미 설치됨${NC}"
fi

# 기존 k3d 클러스터 확인 및 정리
if k3d cluster list | grep -q "$CLUSTER_NAME"; then
    echo -e "${YELLOW}⚠️  기존 k3d 클러스터 '$CLUSTER_NAME'가 존재합니다.${NC}"
    read -p "기존 클러스터를 삭제하고 새로 시작하시겠습니까? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${YELLOW}🗑️  기존 클러스터 삭제 중...${NC}"
        k3d cluster delete $CLUSTER_NAME
    fi
fi

# k3d 클러스터 생성
echo -e "${BLUE}🏗️  k3d 클러스터 생성 중...${NC}"
echo "클러스터 이름: $CLUSTER_NAME"
echo "포트 매핑: 80:80, 443:443, 30300:30300, 30411:30411"
k3d cluster create $CLUSTER_NAME \
    --port "80:80@loadbalancer" \
    --port "443:443@loadbalancer" \
    --port "30300:30300@loadbalancer" \
    --port "30411:30411@loadbalancer" \
    --port "8080:8080@loadbalancer" \
    --agents 2 \
    --k3s-arg "--disable=traefik@server:0" \
    --wait

# kubectl 컨텍스트 설정
echo -e "${BLUE}🔧 kubectl 컨텍스트 설정 중...${NC}"
kubectl config use-context k3d-$CLUSTER_NAME

# 클러스터 상태 확인
echo -e "${BLUE}🔍 클러스터 상태 확인 중...${NC}"
kubectl cluster-info
kubectl get nodes

# NGINX Ingress Controller 설치
echo -e "${BLUE}🌐 NGINX Ingress Controller 설치 중...${NC}"
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml

# Metrics Server 설치
echo -e "${BLUE}📊 Metrics Server 설치 중...${NC}"
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
kubectl patch deployment metrics-server -n kube-system --type='json' -p='[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value": "--kubelet-insecure-tls"}]'

# monitoring 네임스페이스 생성
echo -e "${BLUE}📁 monitoring 네임스페이스 생성 중...${NC}"
kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -

# ArgoCD 설치
echo -e "${BLUE}🔄 ArgoCD 설치 중...${NC}"
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Argo Rollouts 설치
echo -e "${BLUE}🎯 Argo Rollouts 설치 중...${NC}"
kubectl create namespace argo-rollouts --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -n argo-rollouts -f https://github.com/argoproj/argo-rollouts/releases/latest/download/install.yaml

# Helm 레포지토리 추가
echo -e "${BLUE}📦 Helm 레포지토리 추가 중...${NC}"
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo add fluent https://fluent.github.io/helm-charts
helm repo update

# Prometheus Stack 설치 (Grafana 포함)
echo -e "${BLUE}📊 Prometheus + Grafana 설치 중...${NC}"
helm install prometheus prometheus-community/kube-prometheus-stack \
    --namespace monitoring \
    --set grafana.adminPassword=admin123 \
    --set grafana.service.type=LoadBalancer \
    --set grafana.service.port=30300 \
    --wait

# Loki Stack 설치
echo -e "${BLUE}📝 Loki Stack 설치 중...${NC}"
helm install loki grafana/loki-stack \
    --namespace monitoring \
    --set grafana.enabled=false \
    --set loki.service.type=ClusterIP \
    --wait

# Zipkin 설치 (분산 추적)
echo -e "${PURPLE}🔍 Zipkin 설치 중...${NC}"
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: zipkin
  namespace: monitoring
  labels:
    app: zipkin
spec:
  replicas: 1
  selector:
    matchLabels:
      app: zipkin
  template:
    metadata:
      labels:
        app: zipkin
    spec:
      containers:
      - name: zipkin
        image: openzipkin/zipkin:latest
        ports:
        - containerPort: 9411
        env:
        - name: STORAGE_TYPE
          value: mem
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: zipkin
  namespace: monitoring
  labels:
    app: zipkin
spec:
  selector:
    app: zipkin
  ports:
  - port: 9411
    targetPort: 9411
  type: LoadBalancer
EOF

# Fluent-bit 설치 (로그 수집)
echo -e "${PURPLE}📋 Fluent-bit 설치 중...${NC}"
cat > /tmp/fluent-bit-values.yaml <<EOF
config:
  outputs: |
    [OUTPUT]
        Name loki
        Match *
        Host loki
        Port 3100
        Labels job=fluentbit
        Auto_Kubernetes_Labels on
        Label_keys \$kubernetes['pod_name'],\$kubernetes['namespace_name'],\$kubernetes['container_name']
  
  filters: |
    [FILTER]
        Name kubernetes
        Match kube.*
        Kube_URL https://kubernetes.default.svc:443
        Kube_CA_File /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
        Kube_Token_File /var/run/secrets/kubernetes.io/serviceaccount/token
        Merge_Log On
        K8S-Logging.Parser On
        K8S-Logging.Exclude Off
EOF

helm install fluent-bit fluent/fluent-bit \
    --namespace monitoring \
    --values /tmp/fluent-bit-values.yaml \
    --wait

# 설치 완료 대기
echo -e "${YELLOW}⏳ 모든 컴포넌트 설치 완료 대기 중...${NC}"
kubectl wait --for=condition=available --timeout=300s deployment/argocd-server -n argocd
kubectl wait --for=condition=available --timeout=300s deployment/prometheus-grafana -n monitoring
kubectl wait --for=condition=available --timeout=300s deployment/zipkin -n monitoring

# Grafana에 Zipkin 데이터소스 추가를 위한 설정
echo -e "${PURPLE}🔗 Grafana 데이터소스 설정 중...${NC}"
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: grafana-datasources
  namespace: monitoring
  labels:
    grafana_datasource: "1"
data:
  zipkin.yaml: |
    apiVersion: 1
    datasources:
    - name: Zipkin
      type: zipkin
      access: proxy
      url: http://zipkin:9411
      isDefault: false
      editable: true
EOF

# 설치 완료 확인
echo -e "${GREEN}✅ 모든 컴포넌트 설치 완료!${NC}"
echo
echo -e "${BLUE}📋 설치된 관측성 스택:${NC}"
echo "🔍 Traces: Zipkin (분산 추적)"
echo "📊 Metrics: Prometheus (메트릭 수집)"
echo "📝 Logs: Loki + Fluent-bit (로그 수집)"
echo "📈 Visualization: Grafana (통합 대시보드)"
echo
echo -e "${BLUE}📋 설치된 CICD 스택:${NC}"
echo "🔄 GitOps: ArgoCD"
echo "🎯 Advanced Deployment: Argo Rollouts"
echo
echo -e "${YELLOW}🌐 접속 정보:${NC}"
echo "Grafana: http://localhost:30300 (admin/admin123)"
echo "Zipkin: http://localhost:30411"
echo "ArgoCD: kubectl port-forward svc/argocd-server -n argocd 8080:443"
echo
echo -e "${YELLOW}📋 유용한 명령어:${NC}"
echo "ArgoCD 초기 비밀번호: kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d"
echo "모든 Pod 상태: kubectl get pods -A"
echo "클러스터 정보: k3d cluster list"
echo "클러스터 삭제: k3d cluster delete $CLUSTER_NAME"
echo
echo -e "${GREEN}🎉 완전한 로컬 관측성 + CICD 환경 구축 완료!${NC}"
echo -e "${BLUE}📱 Spring Boot 애플리케이션 설정:${NC}"
echo "1. Micrometer Tracing → Zipkin: http://zipkin:9411"
echo "2. Actuator Metrics → Prometheus: 자동 수집됨"
echo "3. 로그 파일 → Fluent-bit → Loki: 자동 수집됨"
echo
echo -e "${BLUE}다음 단계: ./scripts/deploy-local.sh 실행하여 서비스 배포${NC}"