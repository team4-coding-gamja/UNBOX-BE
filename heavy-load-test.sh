#!/bin/bash
# Payment 서비스에 강력한 부하를 거는 스크립트 (병렬 요청)
# Blue-Green 배포 시 모든 Pod에 트래픽 전송

DURATION=300  # 5분간 요청 (AnalysisRun 전체 기간 커버)
PARALLEL=10   # 동시 10개 요청

echo "🚀 Payment 서비스 강력한 부하 테스트 시작"
echo "⏱️  지속 시간: ${DURATION}초"
echo "🔥 병렬 요청: ${PARALLEL}개"
echo ""

# 모든 payment-service Pod 가져오기
PODS=$(kubectl get pods -n unbox-app -l app=payment-service -o jsonpath='{.items[*].metadata.name}')
echo "📍 대상 Pods: ${PODS}"
echo ""

START_TIME=$(date +%s)

# 각 Pod에 대해 백그라운드로 여러 요청 프로세스 실행
for POD_NAME in $PODS; do
  for i in $(seq 1 $PARALLEL); do
    (
      while true; do
        CURRENT_TIME=$(date +%s)
        ELAPSED=$((CURRENT_TIME - START_TIME))
        
        if [ $ELAPSED -ge $DURATION ]; then
          break
        fi
        
        # 요청 보내기
        kubectl exec -n unbox-app $POD_NAME -c payment-service -- \
          curl -s -o /dev/null -w "%{http_code}\n" \
          "http://localhost:8085/payment/test/api/payment/version" 2>/dev/null
        
        sleep 0.1
      done
    ) &
  done
done

# 진행 상황 모니터링
while true; do
  CURRENT_TIME=$(date +%s)
  ELAPSED=$((CURRENT_TIME - START_TIME))
  
  if [ $ELAPSED -ge $DURATION ]; then
    break
  fi
  
  echo "⏱️  경과 시간: ${ELAPSED}초 / ${DURATION}초"
  sleep 10
done

# 모든 백그라운드 프로세스 종료 대기
wait

echo ""
echo "✅ 부하 테스트 완료"
