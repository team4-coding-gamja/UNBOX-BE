#!/bin/bash

# ============================================
# Docker 레이어 캐시 테스트 스크립트
# ============================================

set -e

SERVICE_NAME="unbox_product"
DOCKERFILE_PATH="$SERVICE_NAME/Dockerfile"
IMAGE_NAME="test-product-cache"
TEST_TAG="cache-test"

echo "🚀 Docker 레이어 캐시 테스트 시작"
echo "=================================="
echo ""

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 기존 이미지 정리
echo -e "${YELLOW}📦 기존 테스트 이미지 정리...${NC}"
docker rmi -f $(docker images -q "$IMAGE_NAME:$TEST_TAG" 2>/dev/null) 2>/dev/null || true
echo ""

# ============================================
# 테스트 1: 첫 번째 빌드 (캐시 없음)
# ============================================
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}테스트 1: 첫 번째 빌드 (캐시 없음)${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

START_TIME=$(date +%s)

docker build \
  --no-cache \
  --progress=plain \
  -t "$IMAGE_NAME:$TEST_TAG" \
  -f "$DOCKERFILE_PATH" \
  . 2>&1 | tee /tmp/docker-build-1.log

END_TIME=$(date +%s)
FIRST_BUILD_TIME=$((END_TIME - START_TIME))

echo ""
echo -e "${GREEN}✅ 첫 번째 빌드 완료${NC}"
echo -e "${GREEN}⏱️  소요 시간: ${FIRST_BUILD_TIME}초${NC}"
echo ""

# ============================================
# 테스트 2: 두 번째 빌드 (캐시 활용)
# ============================================
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}테스트 2: 두 번째 빌드 (캐시 활용)${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

START_TIME=$(date +%s)

docker build \
  --progress=plain \
  -t "$IMAGE_NAME:$TEST_TAG-cached" \
  -f "$DOCKERFILE_PATH" \
  . 2>&1 | tee /tmp/docker-build-2.log

END_TIME=$(date +%s)
SECOND_BUILD_TIME=$((END_TIME - START_TIME))

echo ""
echo -e "${GREEN}✅ 두 번째 빌드 완료${NC}"
echo -e "${GREEN}⏱️  소요 시간: ${SECOND_BUILD_TIME}초${NC}"
echo ""

# ============================================
# 테스트 3: 소스 코드만 변경 후 빌드
# ============================================
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}테스트 3: 소스 코드 변경 후 빌드${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 임시 파일 생성 (소스 코드 변경 시뮬레이션)
TEMP_FILE="$SERVICE_NAME/src/main/java/com/example/CacheTest.java"
mkdir -p "$(dirname "$TEMP_FILE")"
echo "// Cache test - $(date)" > "$TEMP_FILE"

START_TIME=$(date +%s)

docker build \
  --progress=plain \
  -t "$IMAGE_NAME:$TEST_TAG-source-change" \
  -f "$DOCKERFILE_PATH" \
  . 2>&1 | tee /tmp/docker-build-3.log

END_TIME=$(date +%s)
THIRD_BUILD_TIME=$((END_TIME - START_TIME))

# 임시 파일 삭제
rm -f "$TEMP_FILE"

echo ""
echo -e "${GREEN}✅ 소스 변경 후 빌드 완료${NC}"
echo -e "${GREEN}⏱️  소요 시간: ${THIRD_BUILD_TIME}초${NC}"
echo ""

# ============================================
# 결과 분석
# ============================================
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}📊 테스트 결과 요약${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${YELLOW}빌드 시간 비교:${NC}"
echo "  1️⃣  첫 번째 빌드 (캐시 없음):     ${FIRST_BUILD_TIME}초"
echo "  2️⃣  두 번째 빌드 (전체 캐시):     ${SECOND_BUILD_TIME}초"
echo "  3️⃣  소스 변경 후 빌드 (부분 캐시): ${THIRD_BUILD_TIME}초"
echo ""

# 캐시 효율 계산
if [ $FIRST_BUILD_TIME -gt 0 ]; then
  CACHE_EFFICIENCY=$(( (FIRST_BUILD_TIME - SECOND_BUILD_TIME) * 100 / FIRST_BUILD_TIME ))
  SOURCE_CACHE_EFFICIENCY=$(( (FIRST_BUILD_TIME - THIRD_BUILD_TIME) * 100 / FIRST_BUILD_TIME ))
  
  echo -e "${GREEN}캐시 효율:${NC}"
  echo "  📈 전체 캐시 효율: ${CACHE_EFFICIENCY}% 단축"
  echo "  📈 소스 변경 시 효율: ${SOURCE_CACHE_EFFICIENCY}% 단축"
  echo ""
fi

# ============================================
# 캐시 레이어 분석
# ============================================
echo -e "${YELLOW}🔍 캐시 레이어 분석:${NC}"
echo ""

echo "첫 번째 빌드 (캐시 없음):"
grep -c "CACHED" /tmp/docker-build-1.log || echo "  캐시된 레이어: 0개"

echo ""
echo "두 번째 빌드 (전체 캐시):"
CACHED_COUNT=$(grep -c "CACHED" /tmp/docker-build-2.log || echo "0")
echo "  캐시된 레이어: ${CACHED_COUNT}개"

echo ""
echo "소스 변경 후 빌드 (부분 캐시):"
CACHED_COUNT=$(grep -c "CACHED" /tmp/docker-build-3.log || echo "0")
echo "  캐시된 레이어: ${CACHED_COUNT}개"

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}✅ 테스트 완료!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${YELLOW}📝 상세 로그 파일:${NC}"
echo "  - /tmp/docker-build-1.log (첫 번째 빌드)"
echo "  - /tmp/docker-build-2.log (두 번째 빌드)"
echo "  - /tmp/docker-build-3.log (소스 변경 후 빌드)"
echo ""

echo -e "${YELLOW}💡 로그 확인 명령어:${NC}"
echo "  cat /tmp/docker-build-1.log | grep -E 'CACHED|Step'"
echo "  cat /tmp/docker-build-2.log | grep -E 'CACHED|Step'"
echo "  cat /tmp/docker-build-3.log | grep -E 'CACHED|Step'"
echo ""

# 정리
echo -e "${YELLOW}🧹 테스트 이미지 정리 중...${NC}"
docker rmi -f "$IMAGE_NAME:$TEST_TAG" 2>/dev/null || true
docker rmi -f "$IMAGE_NAME:$TEST_TAG-cached" 2>/dev/null || true
docker rmi -f "$IMAGE_NAME:$TEST_TAG-source-change" 2>/dev/null || true

echo -e "${GREEN}✨ 완료!${NC}"
