#!/bin/bash

# ArgoCD GitOps 설정 스크립트
# UNBOX 플랫폼용 ArgoCD 애플리케이션 배포

set -e

echo "🚀 ArgoCD GitOps 설정 시작..."

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

# 환경 변수
CLUSTER_NAME="unbox-local"
ARGOCD_NAMESPACE="argocd"
GIT_REPO_URL="https://github.com/your-org/UNBOX-BE.git"  # 실제 Git 저장소 URL로 변경 필요

echo -e "${BLUE}📋 ArgoCD 설정 정보:${NC}"
echo -e "  • Cluster: ${CLUSTER_NAME}"
echo -e "  • Namespace: ${ARGOCD_NAMESPACE}"
echo -e "  • Git Repository: ${GIT_REPO_URL}"
echo ""

# 1. 사전 검사
echo -e "${PURPLE}🔍 1. 사전 환경 검사...${NC}"

# k3d 클러스터 확인
if ! k3d cluster list | grep -q "$CLUSTER_NAME"; then
    echo -e "${RED}❌ k3d 클러스터 '$CLUSTER_NAME'를 찾을 수 없습니다${NC}"
    echo -e "${YELLOW}다음 명령어로 클러스터를 생성하세요: ./scripts/setup-local.sh${NC}"
    exit 1
fi
echo -e "${GREEN}✅ k3d 클러스터 확인됨${NC}"

# kubectl 컨텍스트 설정
kubectl config use-context k3d-$CLUSTER_NAME
echo -e "${GREEN}✅ kubectl 컨텍스트 설정됨${NC}"

# ArgoCD 설치 확인
if ! kubectl get namespace $ARGOCD_NAMESPACE >/dev/null 2>&1; then
    echo -e "${RED}❌ ArgoCD가 설치되지 않았습니다${NC}"
    echo -e "${YELLOW}다음 명령어로 ArgoCD를 설치하세요: ./scripts/setup-local.sh${NC}"
    exit 1
fi
echo -e "${GREEN}✅ ArgoCD 네임스페이스 확인됨${NC}"

# ArgoCD 서버 상태 확인
if ! kubectl get deployment argocd-server -n $ARGOCD_NAMESPACE >/dev/null 2>&1; then
    echo -e "${RED}❌ ArgoCD 서버가 실행되지 않았습니다${NC}"
    exit 1
fi
echo -e "${GREEN}✅ ArgoCD 서버 확인됨${NC}"

# 2. Git 저장소 URL 업데이트
echo -e "${PURPLE}📝 2. Git 저장소 URL 업데이트...${NC}"

# 현재 Git 원격 저장소 URL 가져오기
CURRENT_GIT_URL=$(git remote get-url origin 2>/dev/null || echo "")
if [ -n "$CURRENT_GIT_URL" ]; then
    echo -e "${BLUE}  • 현재 Git URL: ${CURRENT_GIT_URL}${NC}"
    
    # ArgoCD Application 파일들의 Git URL 업데이트
    find argocd/applications/ -name "*.yaml" -exec sed -i.bak "s|https://github.com/your-org/UNBOX-BE.git|${CURRENT_GIT_URL}|g" {} \;
    sed -i.bak "s|https://github.com/your-org/UNBOX-BE.git|${CURRENT_GIT_URL}|g" argocd/app-of-apps.yaml
    
    # 백업 파일 삭제
    find argocd/ -name "*.bak" -delete
    
    echo -e "${GREEN}✅ Git URL 업데이트 완료${NC}"
else
    echo -e "${YELLOW}⚠️  Git 원격 저장소 URL을 찾을 수 없습니다. 수동으로 설정해주세요.${NC}"
fi

# 3. AnalysisTemplate 배포
echo -e "${PURPLE}📊 3. AnalysisTemplate 배포...${NC}"
if kubectl apply -f argocd/analysis-templates/ >/dev/null 2>&1; then
    echo -e "${GREEN}✅ AnalysisTemplate 배포 완료${NC}"
else
    echo -e "${RED}❌ AnalysisTemplate 배포 실패${NC}"
    exit 1
fi

# 4. App of Apps 배포
echo -e "${PURPLE}🎯 4. App of Apps 배포...${NC}"
if kubectl apply -f argocd/app-of-apps.yaml >/dev/null 2>&1; then
    echo -e "${GREEN}✅ App of Apps 배포 완료${NC}"
else
    echo -e "${RED}❌ App of Apps 배포 실패${NC}"
    exit 1
fi

# 5. ArgoCD 애플리케이션 상태 확인
echo -e "${PURPLE}📋 5. ArgoCD 애플리케이션 상태 확인...${NC}"
echo -e "${BLUE}  • 애플리케이션 목록:${NC}"

# 잠시 대기 후 상태 확인
sleep 10

kubectl get applications -n $ARGOCD_NAMESPACE || echo "아직 애플리케이션이 생성되지 않았습니다"

# 6. ArgoCD CLI 설정 (선택사항)
echo -e "${PURPLE}🔧 6. ArgoCD CLI 설정...${NC}"

# ArgoCD 초기 비밀번호 가져오기
ARGOCD_PASSWORD=$(kubectl -n $ARGOCD_NAMESPACE get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d 2>/dev/null || echo "")

if [ -n "$ARGOCD_PASSWORD" ]; then
    echo -e "${GREEN}✅ ArgoCD 초기 비밀번호 확인됨${NC}"
    echo -e "${BLUE}  • Username: admin${NC}"
    echo -e "${BLUE}  • Password: ${ARGOCD_PASSWORD}${NC}"
else
    echo -e "${YELLOW}⚠️  ArgoCD 초기 비밀번호를 가져올 수 없습니다${NC}"
fi

# 7. 포트포워딩 설정 안내
echo -e "${PURPLE}🌐 7. ArgoCD 접속 안내...${NC}"
echo -e "${BLUE}ArgoCD UI에 접속하려면 다음 명령어를 실행하세요:${NC}"
echo -e "${YELLOW}kubectl port-forward svc/argocd-server -n argocd 8080:443${NC}"
echo -e "${BLUE}그 후 브라우저에서 https://localhost:8080 으로 접속${NC}"
echo ""

# 8. 배포 전략 안내
echo -e "${PURPLE}📈 8. 배포 전략 안내...${NC}"
echo -e "${BLUE}각 서비스별 배포 전략:${NC}"
echo -e "  • unbox-user: Rolling Update (자동 배포)"
echo -e "  • unbox-product: Blue-Green (수동 승인)"
echo -e "  • unbox-order: Blue-Green (수동 승인)"
echo -e "  • unbox-trade: Canary (자동 진행)"
echo -e "  • unbox-payment: Canary (자동 진행)"
echo ""

# 9. 최종 결과 요약
echo -e "${PURPLE}📋 ArgoCD 설정 완료!${NC}"
echo -e "${BLUE}===========================================${NC}"
echo -e "🎯 App of Apps 패턴으로 5개 서비스 관리"
echo -e "📊 Prometheus 기반 분석 템플릿 설정"
echo -e "🚀 다양한 배포 전략 적용"
echo ""

echo -e "${GREEN}🎉 ArgoCD GitOps 설정 완료!${NC}"
echo ""
echo -e "${BLUE}📊 접속 정보:${NC}"
echo -e "  • ArgoCD UI: https://localhost:8080 (admin/${ARGOCD_PASSWORD})"
echo -e "  • Grafana: http://localhost:30300 (admin/admin123)"
echo -e "  • Zipkin: http://localhost:9411"
echo ""

echo -e "${BLUE}🔄 다음 단계:${NC}"
echo -e "  1. ArgoCD UI에서 애플리케이션 동기화 상태 확인"
echo -e "  2. CI 파이프라인으로 이미지 빌드 및 배포 테스트"
echo -e "  3. 각 배포 전략별 동작 확인"
echo ""

echo -e "${GREEN}✨ ArgoCD 설정 완료!${NC}"