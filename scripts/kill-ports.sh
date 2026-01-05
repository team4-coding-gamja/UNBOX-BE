#!/bin/bash

echo "🔍 포트 사용 상태 확인 중..."

# 8080 포트 확인
echo "📍 8080 포트:"
lsof -i :8080

# 6379 포트 확인  
echo "📍 6379 포트 (Redis):"
lsof -i :6379

echo ""
echo "🛑 포트 정리 옵션:"
echo "1. 8080 포트만 정리: sudo lsof -t -i:8080 | xargs kill -9"
echo "2. 6379 포트만 정리: sudo lsof -t -i:6379 | xargs kill -9"
echo "3. 둘 다 정리: sudo lsof -t -i:8080,6379 | xargs kill -9"
echo "4. Java 프로세스만: pkill -f UnboxBeApplication"

read -p "실행할 옵션 번호 (1-4): " choice

case $choice in
    1)
        sudo lsof -t -i:8080 | xargs kill -9
        echo "✅ 8080 포트 정리 완료"
        ;;
    2)
        sudo lsof -t -i:6379 | xargs kill -9
        echo "✅ 6379 포트 정리 완료"
        ;;
    3)
        sudo lsof -t -i:8080 | xargs kill -9
        sudo lsof -t -i:6379 | xargs kill -9
        echo "✅ 모든 포트 정리 완료"
        ;;
    4)
        pkill -f "UnboxBeApplication"
        echo "✅ Java 프로세스 정리 완료"
        ;;
    *)
        echo "❌ 잘못된 선택입니다"
        ;;
esac