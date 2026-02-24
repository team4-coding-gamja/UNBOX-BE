#!/bin/bash
# Preview 서비스에만 부하를 거는 스크립트

PREVIEW_SERVICE="payment-service-preview.unbox-app.svc.cluster.local"
ENDPOINT="/payment/test/api/payment/version"
DURATION=240  # 4분간 요청 (분석 시간 3분 + 여유)

echo "🚀 Preview 서비스 부하 테스트 시작"
echo "📍 서비스: ${PREVIEW_SERVICE}${ENDPOINT}"
echo "⏱️  지속 시간: ${DURATION}초"
echo "📊 50% 에러율 예상 → 롤백 유도"
echo ""

# 먼저 preview 서비스가 존재하는지 확인
echo "⏳ Preview 서비스 대기 중..."
while true; do
    if kubectl get svc payment-service-preview -n unbox-app &>/dev/null; then
        echo "✅ Preview 서비스 발견!"
        break
    fi
    sleep 2
done

# Preview Pod가 Ready 상태가 될 때까지 대기
echo "⏳ Preview Pod Ready 대기 중..."
kubectl wait --for=condition=Ready pod -l app=payment-service -n unbox-app --timeout=120s 2>/dev/null

sleep 5
echo ""
echo "🔥 부하 테스트 시작!"
echo ""

START_TIME=$(date +%s)
SUCCESS_COUNT=0
ERROR_COUNT=0
REQUEST_COUNT=0

while true; do
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - START_TIME))
    
    if [ $ELAPSED -ge $DURATION ]; then
        break
    fi
    
    REQUEST_COUNT=$((REQUEST_COUNT + 1))
    
    # Preview 서비스로 직접 요청
    HTTP_CODE=$(kubectl run curl-test-${REQUEST_COUNT} --image=curlimages/curl:latest --rm -i --restart=Never -n unbox-app -- \
        curl -s -o /dev/null -w "%{http_code}" \
        "http://${PREVIEW_SERVICE}${ENDPOINT}" 2>/dev/null | tail -1)
    
    if [ "$HTTP_CODE" = "200" ]; then
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        echo "✅ [${ELAPSED}s] #${REQUEST_COUNT} Success (성공: ${SUCCESS_COUNT}, 에러: ${ERROR_COUNT})"
    else
        ERROR_COUNT=$((ERROR_COUNT + 1))
        echo "❌ [${ELAPSED}s] #${REQUEST_COUNT} Error ${HTTP_CODE} (성공: ${SUCCESS_COUNT}, 에러: ${ERROR_COUNT})"
    fi
    
    # 현재 에러율 계산
    if [ $REQUEST_COUNT -gt 0 ]; then
        ERROR_RATE=$(awk "BEGIN {printf \"%.2f\", ($ERROR_COUNT / $REQUEST_COUNT) * 100}")
        echo "   📊 현재 에러율: ${ERROR_RATE}%"
    fi
    
    # 1초 대기
    sleep 1
done

TOTAL=$((SUCCESS_COUNT + ERROR_COUNT))
FINAL_ERROR_RATE=$(awk "BEGIN {printf \"%.2f\", ($ERROR_COUNT / $TOTAL) * 100}")

echo ""
echo "📊 테스트 완료"
echo "   총 요청: ${TOTAL}"
echo "   성공: ${SUCCESS_COUNT}"
echo "   실패: ${ERROR_COUNT}"
echo "   최종 에러율: ${FINAL_ERROR_RATE}%"
