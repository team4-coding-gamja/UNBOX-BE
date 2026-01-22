#!/bin/bash

# ============================================
# Build and Check Image Sizes
# ============================================

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo "🚀 Building optimized Docker images..."
echo ""

# Services to build
SERVICES=("unbox_product" "unbox_user" "unbox_payment" "unbox_trade")

# Build each service
for SERVICE in "${SERVICES[@]}"; do
    IMAGE_NAME=$(echo $SERVICE | sed 's/_/-/g')
    
    echo -e "${BLUE}📦 Building $SERVICE...${NC}"
    
    START_TIME=$(date +%s)
    
    docker build \
        -t $IMAGE_NAME:latest \
        -f $SERVICE/Dockerfile \
        . > /dev/null 2>&1
    
    END_TIME=$(date +%s)
    BUILD_TIME=$((END_TIME - START_TIME))
    
    echo -e "${GREEN}✅ Built in ${BUILD_TIME}s${NC}"
    echo ""
done

echo ""
echo "─────────────────────────────────────"
echo ""

# Check sizes
./scripts/check-image-sizes.sh
