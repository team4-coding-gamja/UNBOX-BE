# 📊 UNBOX 개발자 모니터링 대시보드

## 🚀 **빠른 시작**

### **1. 원클릭 설치**
```bash
./import-dashboard.sh
```

### **2. 수동 설치**
1. Grafana 접속 → **"+"** → **"Import"**
2. **"Upload JSON file"** → `complete-with-logs-dashboard.json` 선택
3. **"Import"** 클릭

---

## 📁 **파일 설명**

| 파일 | 설명 |
|------|------|
| `complete-with-logs-dashboard.json` | 📊 메인 대시보드 (모든 메트릭 포함) |
| `import-dashboard.sh` | 🚀 자동 임포트 스크립트 |
| `README_FOR_DEVELOPERS.md` | 📖 상세 사용 가이드 |

---

## 📈 **포함된 메트릭**

- 🚀 **API Request Rate** - 서비스별 초당 요청 수
- ⚡ **Response Time** - P95 응답시간
- ✅ **Success Rate** - 전체 성공률
- 🚨 **Error Rate** - 4xx/5xx 에러율
- 🔥 **Redis Cache** - 캐시 히트율
- 💾 **JVM Memory** - 힙 메모리 사용량
- 🗄️ **Database** - 커넥션 풀 상태
- 📋 **Log Guide** - 실시간 로그 확인 방법

---

## 🔧 **필수 설정**

### **Docker Compose 환경**
Prometheus가 다음 엔드포인트를 수집하도록 설정:

```yaml
scrape_configs:
  - job_name: 'unbox-user'
    static_configs:
      - targets: ['unbox-user:8081']
    metrics_path: '/user/actuator/prometheus'
  # ... 다른 서비스들
```

### **Spring Boot Actuator**
각 서비스에서 Prometheus 메트릭 활성화:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

---

## 🎯 **사용 시나리오**

1. **개발 중 실시간 모니터링**
2. **성능 최적화 작업**
3. **에러 디버깅**
4. **부하 테스트 결과 분석**

---

