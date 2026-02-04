#!/bin/bash

# 개발자용 Grafana 대시보드 자동 임포트 스크립트
# 사용법: ./import-dashboard.sh [grafana_url] [username] [password]

echo "📊 UNBOX 개발자 대시보드 임포트"
echo "==============================="
echo ""

# 기본값 설정
GRAFANA_URL="${1:-http://localhost:3000}"
USERNAME="${2:-admin}"
PASSWORD="${3:-admin}"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔗 Grafana URL: ${GRAFANA_URL}${NC}"
echo -e "${BLUE}👤 Username: ${USERNAME}${NC}"
echo ""

# Grafana 연결 테스트
echo -e "${YELLOW}🔍 Grafana 연결 테스트 중...${NC}"
if ! curl -s -f "${GRAFANA_URL}/api/health" > /dev/null; then
    echo -e "${RED}❌ Grafana에 연결할 수 없습니다.${NC}"
    echo "다음을 확인하세요:"
    echo "1. Grafana가 실행 중인지 확인"
    echo "2. URL이 올바른지 확인: ${GRAFANA_URL}"
    echo "3. Docker Compose가 실행 중인지 확인"
    exit 1
fi

echo -e "${GREEN}✅ Grafana 연결 성공${NC}"
echo ""

# 대시보드 임포트
echo -e "${YELLOW}📊 대시보드 임포트 중...${NC}"

RESPONSE=$(curl -s -X POST \
    "${GRAFANA_URL}/api/dashboards/db" \
    -H "Content-Type: application/json" \
    -u "${USERNAME}:${PASSWORD}" \
    -d @complete-with-logs-dashboard.json)

# 응답 확인
if echo "$RESPONSE" | grep -q '"status":"success"'; then
    DASHBOARD_URL=$(echo "$RESPONSE" | grep -o '"url":"[^"]*"' | cut -d'"' -f4)
    echo -e "${GREEN}✅ 대시보드 임포트 성공!${NC}"
    echo ""
    echo -e "${BLUE}📊 대시보드 접속 URL:${NC}"
    echo "${GRAFANA_URL}${DASHBOARD_URL}"
    echo ""
    echo -e "${YELLOW}💡 브라우저에서 위 URL로 접속하세요!${NC}"
    
    # macOS에서 자동으로 브라우저 열기
    if [[ "$OSTYPE" == "darwin"* ]]; then
        read -p "브라우저에서 대시보드를 열까요? (y/N): " -n 1 -r
        echo ""
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            open "${GRAFANA_URL}${DASHBOARD_URL}"
        fi
    fi
    
elif echo "$RESPONSE" | grep -q '"message":"Dashboard with the same uid already exists"'; then
    echo -e "${YELLOW}⚠️  대시보드가 이미 존재합니다.${NC}"
    echo "기존 대시보드를 덮어쓰시겠습니까? (y/N): "
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        # 덮어쓰기로 재시도
        RESPONSE=$(curl -s -X POST \
            "${GRAFANA_URL}/api/dashboards/db" \
            -H "Content-Type: application/json" \
            -u "${USERNAME}:${PASSWORD}" \
            -d '{"dashboard": '$(cat complete-with-logs-dashboard.json | jq '.dashboard')', "overwrite": true}')
        
        if echo "$RESPONSE" | grep -q '"status":"success"'; then
            echo -e "${GREEN}✅ 대시보드 업데이트 성공!${NC}"
        else
            echo -e "${RED}❌ 대시보드 업데이트 실패${NC}"
            echo "응답: $RESPONSE"
        fi
    fi
else
    echo -e "${RED}❌ 대시보드 임포트 실패${NC}"
    echo "응답: $RESPONSE"
    echo ""
    echo -e "${YELLOW}💡 수동 임포트 방법:${NC}"
    echo "1. Grafana 접속: ${GRAFANA_URL}"
    echo "2. 왼쪽 메뉴 '+' → 'Import' 클릭"
    echo "3. 'Upload JSON file' → complete-with-logs-dashboard.json 선택"
    echo "4. 'Import' 버튼 클릭"
fi

echo ""
echo -e "${BLUE}📖 상세 가이드: README_FOR_DEVELOPERS.md${NC}"
echo -e "${GREEN}Happy Monitoring! 🚀${NC}"