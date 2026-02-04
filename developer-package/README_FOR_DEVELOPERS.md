# 📊 개발자용 Grafana 대시보드 가이드

## 🎯 **개발자들이 사용할 대시보드**

### **추천 대시보드: `complete-with-logs-dashboard.json`**
- **모든 메트릭 포함**: API 성능, 에러율, DB, Redis, JVM, 로그 가이드
- **실시간 모니터링**: 5초 자동 새로고침
- **개발자 친화적**: 색상 코딩, 임계값 설정

---

## 🚀 **Grafana에 대시보드 임포트 방법**

### **방법 1: JSON 파일 업로드**
1. Grafana 접속 (보통 http://localhost:3000)
2. 왼쪽 메뉴 **"+"** → **"Import"** 클릭
3. **"Upload JSON file"** 클릭
4. `complete-with-logs-dashboard.json` 파일 선택
5. **"Import"** 버튼 클릭

### **방법 2: JSON 내용 복사/붙여넣기**
1. Grafana 접속 → **"+"** → **"Import"**
2. **"Import via panel json"** 선택
3. `complete-with-logs-dashboard.json` 파일 내용 복사
4. 텍스트 박스에 붙여넣기
5. **"Load"** → **"Import"** 클릭

### **방법 3: API로 자동 임포트**
```bash
# Grafana가 localhost:3000에서 실행 중일 때
curl -X POST \
  http://admin:admin@localhost:3000/api/dashboards/db \
  -H 'Content-Type: application/json' \
  -d @complete-with-logs-dashboard.json
```

---

## 📈 **대시보드에서 볼 수 있는 메트릭**

### **🚀 API 성능**
- **Request Rate**: 서비스별 초당 요청 수
- **Response Time**: P95 응답시간 (500ms 이하 권장)
- **Success Rate**: 전체 성공률 (99% 이상이면 녹색)

### **🚨 에러 모니터링**
- **4xx Error Rate**: 클라이언트 에러 (주황색)
- **5xx Error Rate**: 서버 에러 (빨간색)
- **HTTP Status Distribution**: 상태코드별 분포

### **🔥 인프라 성능**
- **Redis Cache Hit Rate**: 캐시 효율성
- **Database Connection Pool**: DB 커넥션 상태
- **JVM Memory Usage**: 힙 메모리 사용량

### **📋 로그 모니터링**
- 대시보드 하단에 실시간 로그 확인 방법 안내
- kubectl 명령어 및 스크립트 사용법

---

## 🔧 **Docker Compose 환경에서 메트릭 수집**

### **Prometheus 설정 확인**
Docker Compose의 `prometheus.yml`에 다음 설정이 있는지 확인:

```yaml
scrape_configs:
  - job_name: 'unbox-user'
    static_configs:
      - targets: ['unbox-user:8081']
    metrics_path: '/user/actuator/prometheus'
    
  - job_name: 'unbox-product'
    static_configs:
      - targets: ['unbox-product:8082']
    metrics_path: '/product/actuator/prometheus'
    
  - job_name: 'unbox-order'
    static_configs:
      - targets: ['unbox-order:8083']
    metrics_path: '/order/actuator/prometheus'
    
  - job_name: 'unbox-trade'
    static_configs:
      - targets: ['unbox-trade:8084']
    metrics_path: '/trade/actuator/prometheus'
    
  - job_name: 'unbox-payment'
    static_configs:
      - targets: ['unbox-payment:8085']
    metrics_path: '/payment/actuator/prometheus'
    
  - job_name: 'redis'
    static_configs:
      - targets: ['redis:6379']
```

### **Spring Boot Actuator 설정**
각 서비스의 `application.yml`에 다음 설정 확인:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
```

---

## 🎮 **개발 중 모니터링 팁**

### **실시간 개발 모니터링**
1. **대시보드를 듀얼 모니터에 고정**
2. **자동 새로고침 5초로 설정**
3. **코드 변경 후 즉시 메트릭 확인**

### **성능 최적화 포인트**
- **응답시간 > 500ms** → 쿼리 최적화 필요
- **에러율 > 5%** → 예외 처리 점검
- **캐시 히트율 < 80%** → 캐시 전략 재검토
- **메모리 사용량 > 80%** → GC 튜닝 필요

### **문제 발생 시 체크리스트**
1. **대시보드에서 이상 징후 발견**
2. **Docker 로그 확인**: `docker logs unbox-user`
3. **헬스체크 API 호출**: `curl http://localhost:8081/user/actuator/health`
4. **Prometheus 메트릭 확인**: `curl http://localhost:8081/user/actuator/prometheus`

---

## 📁 **파일 목록**

### **추천 대시보드 (개발자용)**
- **`complete-with-logs-dashboard.json`** ⭐ **메인 대시보드**
- **`complete-developer-dashboard.json`** - 상세 메트릭 버전

### **특수 목적 대시보드**
- **`rolling-update-problems.json`** - Rolling Update 문제 분석용
- **`unbox-services-overview.json`** - 전체 서비스 개요
- **`service-specific-metrics.json`** - 서비스별 상세 메트릭

### **CI/CD 관련**
- **`argo-rollouts-comparison.json`** - 배포 전략 비교용

---

## 🚨 **문제 해결**

### **메트릭이 안 나올 때**
1. **Prometheus 타겟 확인**: http://localhost:9090/targets
2. **Spring Boot Actuator 확인**: http://localhost:8081/user/actuator/prometheus
3. **Docker 네트워크 확인**: 서비스 간 통신 가능한지 확인

### **대시보드가 비어있을 때**
1. **데이터소스 확인**: Grafana → Configuration → Data Sources
2. **Prometheus URL 확인**: http://prometheus:9090 (Docker 내부) 또는 http://localhost:9090
3. **시간 범위 확인**: 대시보드 우상단 시간 선택기

### **성능이 느릴 때**
1. **쿼리 최적화**: 복잡한 Prometheus 쿼리 단순화
2. **새로고침 간격 조정**: 5초 → 10초 또는 30초
3. **시간 범위 축소**: Last 1 hour → Last 15 minutes

---
