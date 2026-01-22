#!/bin/bash

# ============================================
# CI/CD 빌드 메트릭 분석 스크립트
# GitHub Actions 워크플로우에서 사용
# ============================================

set -e

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}📊 빌드 메트릭 분석${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 환경 변수에서 값 가져오기
BUILD_TIME=${BUILD_TIME_SECONDS:-0}
IMAGE_TAG=${IMAGE_TAG:-"unknown"}
SERVICE_NAME=${SERVICE_NAME:-"unknown"}

# 빌드 시간 포맷팅
if [ $BUILD_TIME -gt 0 ]; then
  BUILD_TIME_MIN=$((BUILD_TIME / 60))
  BUILD_TIME_SEC=$((BUILD_TIME % 60))
  echo -e "${GREEN}⏱️  빌드 시간: ${BUILD_TIME_MIN}분 ${BUILD_TIME_SEC}초 (${BUILD_TIME}초)${NC}"
else
  echo -e "${YELLOW}⏱️  빌드 시간: 측정 불가${NC}"
fi

# 캐시 효율 분석 (이전 빌드와 비교)
METRICS_FILE="/tmp/build-metrics-${SERVICE_NAME}.json"

# 현재 빌드 메트릭 저장
cat > "$METRICS_FILE" <<EOF
{
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "service": "$SERVICE_NAME",
  "image_tag": "$IMAGE_TAG",
  "build_time_seconds": $BUILD_TIME,
  "commit_sha": "${GITHUB_SHA:-unknown}",
  "workflow_run": "${GITHUB_RUN_NUMBER:-0}"
}
EOF

echo ""
echo -e "${BLUE}📝 메트릭 저장 완료: $METRICS_FILE${NC}"
cat "$METRICS_FILE"

echo ""
echo -e "${GREEN}✅ 분석 완료${NC}"
