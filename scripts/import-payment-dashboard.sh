#!/bin/bash

echo "📊 Payment Service API Key Analysis 대시보드 임포트 중..."

# 그라파나 API를 통해 대시보드 임포트
curl -X POST \
  http://admin:admin123@localhost:30300/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @monitoring/dashboards/payment-api-key-analysis.json

echo ""
echo "✅ Payment Service 대시보드 임포트 완료!"
echo "🔗 접속 URL: http://localhost:30300/d/payment-api-key-analysis/"
echo ""
echo "📈 확인할 수 있는 메트릭:"
echo "   - 💳 Request Rate & Success Rate"
echo "   - 🔑 API Key Authentication Errors (401/403)"
echo "   - ⏱️ Response Time (P50/P95/P99)"
echo "   - 🏗️ Pod Status & Rolling Update Progress"
echo "   - 💻 JVM Memory Usage"