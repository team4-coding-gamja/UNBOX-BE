#!/bin/bash

echo "📊 Product Service Cache Performance 대시보드 임포트 중..."

# 그라파나 API를 통해 대시보드 임포트
curl -X POST \
  http://admin:admin123@localhost:30300/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @monitoring/dashboards/product-cache-performance.json

echo ""
echo "✅ 대시보드 임포트 완료!"
echo "🌐 그라파나 접속: http://localhost:30300"
echo "📋 대시보드명: Product Service - Cache Performance Analysis"
echo ""
echo "📈 확인할 수 있는 메트릭:"
echo "   1. Redis Cache Hit Rate (캐시 히트율)"
echo "   2. Product Service Response Time (응답시간)"
echo "   3. Request Rate (요청 처리율)"
echo "   4. Cache Operations Rate (캐시 작업 빈도)"
echo "   5. CPU Usage (CPU 사용률)"
echo "   6. Redis Connection & Key Count (Redis 상태)"