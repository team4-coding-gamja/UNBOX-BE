#!/bin/bash

# ============================================
# Docker Image Size Checker
# ============================================

set -e

echo "🔍 Checking Docker image sizes..."
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Target size in MB
TARGET_SIZE=500
IDEAL_SIZE=300

# Function to get image size in MB
get_size_mb() {
    local image=$1
    if docker images --format "{{.Repository}}:{{.Tag}} {{.Size}}" | grep -q "^$image"; then
        local size=$(docker images --format "{{.Repository}}:{{.Tag}} {{.Size}}" | grep "^$image" | awk '{print $2}')
        
        # Convert to MB
        if [[ $size == *"GB"* ]]; then
            size=$(echo $size | sed 's/GB//' | awk '{printf "%.0f", $1 * 1024}')
        elif [[ $size == *"MB"* ]]; then
            size=$(echo $size | sed 's/MB//' | awk '{printf "%.0f", $1}')
        else
            size="0"
        fi
        echo $size
    else
        echo "0"
    fi
}

# Function to print size with color
print_size() {
    local service=$1
    local size=$2
    
    if [ "$size" -eq 0 ]; then
        echo -e "${RED}❌ $service: Image not found${NC}"
    elif [ "$size" -le "$IDEAL_SIZE" ]; then
        echo -e "${GREEN}✅ $service: ${size}MB (Excellent!)${NC}"
    elif [ "$size" -le "$TARGET_SIZE" ]; then
        echo -e "${YELLOW}⚠️  $service: ${size}MB (Good, but can be better)${NC}"
    else
        echo -e "${RED}❌ $service: ${size}MB (Too large!)${NC}"
    fi
}

# Check services
echo "📦 Service Images:"
echo "─────────────────────────────────────"

services=("unbox-product" "unbox-user" "unbox-payment" "unbox-trade")
total_size=0
found_count=0

for service in "${services[@]}"; do
    size=$(get_size_mb "$service:latest")
    print_size "$service" "$size"
    
    if [ "$size" -gt 0 ]; then
        total_size=$((total_size + size))
        found_count=$((found_count + 1))
    fi
done

echo ""
echo "─────────────────────────────────────"

if [ "$found_count" -gt 0 ]; then
    avg_size=$((total_size / found_count))
    echo "📊 Statistics:"
    echo "   Total: ${total_size}MB"
    echo "   Average: ${avg_size}MB"
    echo "   Target: ≤${TARGET_SIZE}MB"
    echo "   Ideal: ≤${IDEAL_SIZE}MB"
    echo ""
    
    if [ "$avg_size" -le "$IDEAL_SIZE" ]; then
        echo -e "${GREEN}🎉 All images are optimally sized!${NC}"
    elif [ "$avg_size" -le "$TARGET_SIZE" ]; then
        echo -e "${YELLOW}👍 Images are within target, but can be improved${NC}"
    else
        echo -e "${RED}⚠️  Images need optimization${NC}"
    fi
else
    echo -e "${RED}No images found. Build images first:${NC}"
    echo "  docker build -t unbox-product:latest -f unbox_product/Dockerfile ."
fi

echo ""
echo "💡 Tips for optimization:"
echo "   • Use alpine-based images (eclipse-temurin:17-jre-alpine)"
echo "   • Use multi-stage builds"
echo "   • Remove unnecessary files from final image"
echo "   • Use .dockerignore to exclude build artifacts"
echo ""

# Compare base images
echo "📋 Base Image Sizes (for reference):"
echo "─────────────────────────────────────"
echo "   eclipse-temurin:17-jre        : ~470MB"
echo "   eclipse-temurin:17-jre-alpine : ~170MB"
echo "   openjdk:17-jre-alpine         : ~170MB"
echo ""
