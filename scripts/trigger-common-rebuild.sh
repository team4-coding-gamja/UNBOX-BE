#!/bin/bash

# Common 모듈 변경 시 모든 서비스 재빌드 트리거 스크립트
# 사용법: ./scripts/trigger-common-rebuild.sh [dev|prod]

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 환경 파라미터 (기본값: dev)
ENV=${1:-dev}

if [[ "$ENV" != "dev" && "$ENV" != "prod" ]]; then
    echo -e "${RED}❌ 잘못된 환경입니다. 'dev' 또는 'prod'를 입력하세요.${NC}"
    echo "사용법: $0 [dev|prod]"
    exit 1
fi

# 브랜치 설정
if [[ "$ENV" == "dev" ]]; then
    BRANCH="develop"
else
    BRANCH="main"
fi

echo -e "${BLUE}🚀 Common 모듈 변경 감지 - 모든 서비스 재빌드 시작${NC}"
echo -e "${YELLOW}환경: $ENV${NC}"
echo -e "${YELLOW}브랜치: $BRANCH${NC}"
echo ""

# GitHub CLI 설치 확인
if ! command -v gh &> /dev/null; then
    echo -e "${RED}❌ GitHub CLI (gh)가 설치되어 있지 않습니다.${NC}"
    echo "설치 방법: https://cli.github.com/"
    exit 1
fi


# 서비스 목록
SERVICES=("user" "product" "trade" "payment" "order")

echo -e "${GREEN}📋 트리거할 워크플로우:${NC}"
for service in "${SERVICES[@]}"; do
    echo "  - ${service}-${ENV}-ci.yml"
done
echo ""

# 확인 메시지
read -p "$(echo -e ${YELLOW}계속하시겠습니까? [y/N]: ${NC})" -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${RED}❌ 취소되었습니다.${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}🔄 워크플로우 트리거 중...${NC}"
echo ""

# 각 서비스 워크플로우 트리거
SUCCESS_COUNT=0
FAIL_COUNT=0

for service in "${SERVICES[@]}"; do
    WORKFLOW="${service}-${ENV}-ci.yml"
    echo -e "${YELLOW}⏳ ${service} 서비스 트리거 중...${NC}"
    
    if gh workflow run "$WORKFLOW" --ref "$BRANCH"; then
        echo -e "${GREEN}✅ ${service} 서비스 트리거 성공${NC}"
        ((SUCCESS_COUNT++))
    else
        echo -e "${RED}❌ ${service} 서비스 트리거 실패${NC}"
        ((FAIL_COUNT++))
    fi
    echo ""
done


# 결과 요약
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${BLUE}📊 트리거 결과 요약${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${GREEN}✅ 성공: $SUCCESS_COUNT개${NC}"
echo -e "${RED}❌ 실패: $FAIL_COUNT개${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if [[ $FAIL_COUNT -eq 0 ]]; then
    echo -e "${GREEN}🎉 모든 워크플로우가 성공적으로 트리거되었습니다!${NC}"
    echo ""
    echo -e "${BLUE}📍 다음 단계:${NC}"
    echo "1. GitHub Actions 탭에서 빌드 진행 상황 확인"
    echo "2. Discord 알림으로 빌드 결과 확인"
    echo ""
    echo -e "${YELLOW}💡 빌드 상태 확인:${NC}"
    echo "   gh run list --workflow=${SERVICES[0]}-${ENV}-ci.yml --limit 1"
else
    echo -e "${RED}⚠️  일부 워크플로우 트리거에 실패했습니다.${NC}"
    echo "GitHub Actions 탭에서 수동으로 실행하세요."
    exit 1
fi
