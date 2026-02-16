# Python 3.11 Slim 이미지 사용
FROM python:3.11-slim

WORKDIR /app

# 시스템 의존성 설치
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 라이브러리 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Playwright 브라우저 설치 (Chromium)
RUN playwright install chromium
RUN playwright install-deps

# 소스 복사
COPY . .

# 실행 (FastAPI)
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]