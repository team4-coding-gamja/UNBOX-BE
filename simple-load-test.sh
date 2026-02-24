#!/bin/bash
# Payment 서비스에 간단하게 부하를 거는 스크립트

DURATION=180  # 3분간 요청

echo "🚀 Payment 서비스 부하 테스트 시작"
echo "⏱️  지속 시간: ${DURATION}초"
echo ""

START_TIME=$(date +%s)
SUCCESS_COUNT=0
ERROR_COUNT=0

while true; do
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - START_TIME))
    
    if [ $ELAPSED -ge $DURATION ]; then
        break
    fi
    
    # payment 서비스에 직접 요청
    HTTP_CODE=$(kubectl exec -n unbox-app deploy/payment-service -- \
        curl -s -o /dev/null -w "%{http_code}" \
        "http://localhost:8085/payment/test/api/payment/version" 2>/dev/null)
    
    if [ "$HTTP_CODE" = "200" ]; then
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        echo "✅ [${ELAPSED}s] Success (성공: ${SUCCESS_COUNT}, 에러: ${ERROR_COUNT})"
    else
        ERROR_COUNT=$((ERROR_COUNT + 1))
        echo "❌ [${ELAPSED}s] Error ${HTTP_CODE} (성공: ${SUCCESS_COUNT}, 에러: ${ERROR_COUNT})"
    fi
    
    # 현재 에러율 계산
    TOTAL=$((SUCCESS_COUNT + ERROR_COUNT))
    if [ $TOTAL -gt 0 ]; then
        ERROR_RATE=$(awk "BEGIN {printf \"%.2f\", ($ERROR_COUNT / $TOTAL) * 100}")
        echo "   📊 현재 에러율: ${ERROR_RATE}%"
    fi
    
    # 0.5초 대기
    sleep 0.5
done

TOTAL=$((SUCCESS_COUNT + ERROR_COUNT))
FINAL_ERROR_RATE=$(awk "BEGIN {printf \"%.2f\", ($ERROR_COUNT / $TOTAL) * 100}")

echo ""
echo "📊 테스트 완료"
echo "   총 요청: ${TOTAL}"
echo "   성공: ${SUCCESS_COUNT}"
echo "   실패: ${ERROR_COUNT}"
echo "   최종 에러율: ${FINAL_ERROR_RATE}%"
