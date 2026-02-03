#!/bin/bash

# 기존에 돌던 포트포워딩 다 죽이기 (청소)
pkill -f "kubectl port-forward"
echo "🧹 기존 연결을 정리했습니다."

# 백그라운드(&)로 실행하고 로그는 안 보이게(> /dev/null) 숨김
echo "🚀 포트 포워딩 시작..."

# 1. User Service (8081)
kubectl port-forward svc/user-service -n unbox-app 8081:80 > /dev/null 2>&1 &

# 2. Product Service (8082)
kubectl port-forward svc/product-service -n unbox-app 8082:80 > /dev/null 2>&1 &

# 3. Order Service (8083)
kubectl port-forward svc/order-service -n unbox-app 8084:80 > /dev/null 2>&1 &

# 4. Trade Service (8084)
kubectl port-forward svc/trade-service -n unbox-app 8083:80 > /dev/null 2>&1 &

# 5. Payment Service (8085)
kubectl port-forward svc/payment-service -n unbox-app 8085:80 > /dev/null 2>&1 &

# 6. DB (5433 -> 5432)
kubectl port-forward svc/postgres-postgresql -n unbox-db 5433:5432 > /dev/null 2>&1 &

# 7. Zipkin (9411)
kubectl port-forward svc/zipkin -n unbox-infra 9411:9411 > /dev/null 2>&1 &

echo "✅ 모든 서비스 연결 완료! (종료하려면 터미널 끄거나 'pkill -f kubectl' 입력)"