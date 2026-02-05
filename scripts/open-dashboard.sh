#!/bin/bash

# 개발자용 대시보드 원클릭 오픈 스크립트
# 사용법: ./scripts/open-dashboard.sh

echo "🚀 Opening UNBOX Developer Dashboard..."

# 운영체제 감지
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    open "http://localhost:30300/d/complete-developer-with-logs/f09f94a5-complete-developer-dashboard-with-logs"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    if command -v xdg-open > /dev/null; then
        xdg-open "http://localhost:30300/d/complete-developer-with-logs/f09f94a5-complete-developer-dashboard-with-logs"
    elif command -v gnome-open > /dev/null; then
        gnome-open "http://localhost:30300/d/complete-developer-with-logs/f09f94a5-complete-developer-dashboard-with-logs"
    else
        echo "Please open this URL manually:"
        echo "http://localhost:30300/d/complete-developer-with-logs/f09f94a5-complete-developer-dashboard-with-logs"
    fi
elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
    # Windows
    start "http://localhost:30300/d/complete-developer-with-logs/f09f94a5-complete-developer-dashboard-with-logs"
else
    echo "Please open this URL manually:"
    echo "http://localhost:30300/d/complete-developer-with-logs/f09f94a5-complete-developer-dashboard-with-logs"
fi

echo ""
echo "🔑 Login credentials:"
echo "Username: admin"
echo "Password: admin123"
echo ""
echo "📖 For detailed guide, see: docs/DEVELOPER_QUICK_START.md"