#!/bin/bash

# ==========================================
# k3d 로컬 개발용 원터치 빌드 스크립트
# ==========================================

# 1. 설정 변수
CLUSTER_NAME="unbox-cluster"
TAG="v1"

# 🚨 수정됨: 실제 폴더 이름으로 변경했습니다!
APPS=("unbox_user" "unbox_product" "unbox_trade" "unbox_order" "unbox_payment")

echo "🚀 [Start] k3d 로컬 배포 준비를 시작합니다..."

# 2. 반복문으로 각 앱 처리
for app in "${APPS[@]}"; do
  echo ""
  echo "------------------------------------------------"
  echo "🛠  Target: $app"
  echo "------------------------------------------------"

  # 폴더가 실제로 있는지 확인 (안전장치)
  if [ ! -d "./$app" ]; then
    echo "❌ Error: 폴더를 찾을 수 없습니다: ./$app"
    echo "   (현재 위치에서 ls를 쳤을 때 이 폴더가 보여야 합니다)"
    exit 1
  fi

  echo "   Step 1. Docker Build..."
  # 도커 이미지 빌드 (이미지 이름도 폴더명과 똑같이 unbox_user:v1 로 만들어집니다)
  docker build -t "$app:$TAG" -f "./$app/Dockerfile" .
  
  if [ $? -ne 0 ]; then
    echo "❌ Docker build failed for $app"
    exit 1
  fi

  echo "   Step 2. Import into k3d..."
  k3d image import "$app:$TAG" -c "$CLUSTER_NAME"

  echo "✅ $app 준비 완료!"
done

echo ""
echo "================================================"
echo "🎉 모든 이미지가 k3d($CLUSTER_NAME)로 전송되었습니다."
echo "================================================"