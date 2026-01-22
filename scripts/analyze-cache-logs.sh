#!/bin/bash

# ============================================
# Docker 캐시 로그 상세 분석 스크립트
# ============================================

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}🔍 Docker 캐시 레이어 상세 분석${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

analyze_log() {
  local log_file=$1
  local build_name=$2
  
  echo -e "${YELLOW}📋 ${build_name}${NC}"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  
  if [ ! -f "$log_file" ]; then
    echo -e "${RED}❌ 로그 파일을 찾을 수 없습니다: $log_file${NC}"
    echo ""
    return
  fi
  
  # 각 Step별 캐시 상태 확인
  echo ""
  echo "레이어별 캐시 상태:"
  grep -E "^#[0-9]+ \[" "$log_file" | while read -r line; do
    if echo "$line" | grep -q "CACHED"; then
      echo -e "  ${GREEN}✓ CACHED${NC} - $line"
    else
      echo -e "  ${YELLOW}⚡ BUILD${NC}  - $line"
    fi
  done
  
  echo ""
  
  # 통계
  total_steps=$(grep -c "^#[0-9]+ \[" "$log_file" || echo "0")
  cached_steps=$(grep -c "CACHED" "$log_file" || echo "0")
  built_steps=$((total_steps - cached_steps))
  
  if [ $total_steps -gt 0 ]; then
    cache_rate=$((cached_steps * 100 / total_steps))
    echo -e "${BLUE}통계:${NC}"
    echo "  전체 레이어: ${total_steps}개"
    echo -e "  ${GREEN}캐시 사용: ${cached_steps}개${NC}"
    echo -e "  ${YELLOW}새로 빌드: ${built_steps}개${NC}"
    echo "  캐시 적중률: ${cache_rate}%"
  fi
  
  echo ""
  echo ""
}

# 각 빌드 로그 분석
analyze_log "/tmp/docker-build-1.log" "1️⃣  첫 번째 빌드 (캐시 없음)"
analyze_log "/tmp/docker-build-2.log" "2️⃣  두 번째 빌드 (전체 캐시)"
analyze_log "/tmp/docker-build-3.log" "3️⃣  소스 변경 후 빌드 (부분 캐시)"

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}✅ 분석 완료${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${YELLOW}💡 주요 확인 포인트:${NC}"
echo "  1. 두 번째 빌드에서 대부분의 레이어가 CACHED로 표시되는지"
echo "  2. 소스 변경 후에도 의존성 다운로드 레이어는 CACHED인지"
echo "  3. 전체 빌드 시간이 50% 이상 단축되었는지"
echo ""
