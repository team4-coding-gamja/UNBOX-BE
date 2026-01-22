#!/bin/bash

# ============================================
# Test Image Size Optimization
# ============================================

set -e

echo "🚀 Testing Docker image optimization..."
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Build one service as test
SERVICE="unbox_product"
IMAGE_NAME="unbox-product-test"

echo -e "${BLUE}📦 Building $SERVICE...${NC}"
echo ""

# Build with timing
START_TIME=$(date +%s)

docker build \
  -t $IMAGE_NAME:latest \
  -f $SERVICE/Dockerfile \
  . 2>&1 | grep -E "(Step|Successfully|CACHED|#)"

END_TIME=$(date +%s)
BUILD_TIME=$((END_TIME - START_TIME))

echo ""
echo -e "${GREEN}✅ Build completed in ${BUILD_TIME}s${NC}"
echo ""

# Check image size
echo "📊 Image Size Analysis:"
echo "─────────────────────────────────────"

SIZE=$(docker images --format "{{.Repository}}:{{.Tag}} {{.Size}}" | grep "^$IMAGE_NAME:latest" | awk '{print $2}')
echo "   Image: $IMAGE_NAME:latest"
echo "   Size: $SIZE"

# Get size in MB for comparison
if [[ $SIZE == *"GB"* ]]; then
    SIZE_MB=$(echo $SIZE | sed 's/GB//' | awk '{printf "%.0f", $1 * 1024}')
elif [[ $SIZE == *"MB"* ]]; then
    SIZE_MB=$(echo $SIZE | sed 's/MB//' | awk '{printf "%.0f", $1}')
else
    SIZE_MB="0"
fi

echo "   Size (MB): ${SIZE_MB}MB"
echo ""

# Evaluation
if [ "$SIZE_MB" -le 300 ]; then
    echo -e "${GREEN}🎉 Excellent! Image is optimally sized (≤300MB)${NC}"
elif [ "$SIZE_MB" -le 500 ]; then
    echo -e "${YELLOW}👍 Good! Image is within target (≤500MB)${NC}"
else
    echo -e "${YELLOW}⚠️  Image could be smaller (current: ${SIZE_MB}MB, target: ≤500MB)${NC}"
fi

echo ""
echo "💡 Image layers:"
docker history $IMAGE_NAME:latest --human --format "table {{.CreatedBy}}\t{{.Size}}" | head -20

echo ""
echo "🧹 Cleanup test image? (y/n)"
read -r response
if [[ "$response" =~ ^[Yy]$ ]]; then
    docker rmi $IMAGE_NAME:latest
    echo -e "${GREEN}✅ Test image removed${NC}"
fi
