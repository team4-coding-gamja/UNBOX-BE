#!/bin/bash

# ============================================
# 빌드 시간 비교 스크립트
# GitHub Actions 워크플로우 실행 기록 비교
# ============================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}📊 빌드 시간 비교 분석${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# GitHub CLI 설치 확인
if ! command -v gh &> /dev/null; then
    echo -e "${RED}❌ GitHub CLI (gh)가 설치되어 있지 않습니다.${NC}"
    echo "설치 방법: https://cli.github.com/"
    exit 1
fi

# 워크플로우 이름
WORKFLOW="product-dev-ci.yml"
LIMIT=10

echo -e "${YELLOW}📋 최근 ${LIMIT}개 빌드 기록 조회 중...${NC}"
echo ""

# 워크플로우 실행 기록 조회
gh run list \
  --workflow="$WORKFLOW" \
  --limit="$LIMIT" \
  --json databaseId,conclusion,createdAt,displayTitle,headSha \
  --jq '.[] | "\(.databaseId)|\(.conclusion)|\(.createdAt)|\(.displayTitle)|\(.headSha[0:7])"' | \
while IFS='|' read -r run_id conclusion created_at title sha; do
  
  # 상태 아이콘
  if [ "$conclusion" = "success" ]; then
    STATUS_ICON="✅"
    STATUS_COLOR="${GREEN}"
  elif [ "$conclusion" = "failure" ]; then
    STATUS_ICON="❌"
    STATUS_COLOR="${RED}"
  else
    STATUS_ICON="⏸️"
    STATUS_COLOR="${YELLOW}"
  fi
  
  # 날짜 포맷팅
  DATE=$(date -d "$created_at" "+%Y-%m-%d %H:%M" 2>/dev/null || date -j -f "%Y-%m-%dT%H:%M:%SZ" "$created_at" "+%Y-%m-%d %H:%M" 2>/dev/null || echo "$created_at")
  
  echo -e "${STATUS_COLOR}${STATUS_ICON} Run #${run_id}${NC}"
  echo "   📅 $DATE"
  echo "   📝 $title"
  echo "   🔖 $sha"
  echo ""
done

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${YELLOW}💡 상세 정보 확인:${NC}"
echo "   gh run view <run-id>"
echo ""
echo -e "${YELLOW}💡 로그 확인:${NC}"
echo "   gh run view <run-id> --log"
echo ""
echo -e "${YELLOW}💡 특정 워크플로우 재실행:${NC}"
echo "   gh run rerun <run-id>"
echo ""

# 성공/실패 통계
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}📈 빌드 통계 (최근 ${LIMIT}개)${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

STATS=$(gh run list \
  --workflow="$WORKFLOW" \
  --limit="$LIMIT" \
  --json conclusion \
  --jq 'group_by(.conclusion) | map({conclusion: .[0].conclusion, count: length}) | .[]')

echo "$STATS" | while read -r line; do
  CONCLUSION=$(echo "$line" | jq -r '.conclusion')
  COUNT=$(echo "$line" | jq -r '.count')
  
  case "$CONCLUSION" in
    "success")
      echo -e "${GREEN}✅ 성공: ${COUNT}개${NC}"
      ;;
    "failure")
      echo -e "${RED}❌ 실패: ${COUNT}개${NC}"
      ;;
    *)
      echo -e "${YELLOW}⏸️  기타 ($CONCLUSION): ${COUNT}개${NC}"
      ;;
  esac
done

echo ""
echo -e "${GREEN}✅ 분석 완료${NC}"
