# 테스트 목적

테스트의 목적은 **결제 완료(Confirm) API를 진입점으로**, 동기 구조와 비동기 구조가 **하위 서비스 지연(slow dependency)** 상황에서 보이는 구조적 차이(장애 전파 vs 격리)를 **정량 지표로 비교**하는 것이다.

- **전파성(Propagation)**: 하위 서비스(Order) 지연이 **결제 응답 실패/지연으로 전파되는가?**
- **안정성(Stability)**: 트래픽 증가(ramping) 시 **결제 진입점이 어디서부터 무너지는가(변곡점)?**

> **목표:** 동일한 부하(ramping)와 동일한 지연(Order 3s timeout) 조건에서,
> **동기 구조는 어떻게 실패하는지(전파/블로킹)**,
> **비동기 구조는 무엇을 지켜내는지(응답 안정/격리)** 를 수치로 비교한다.

---

# 테스트 시나리오

> 💡 **🏁 진입점 API**: `/test/api/payment/confirm/sync` 또는 `/confirm/async`
> **💉 장애/지연 주입**: Order 서비스 **헤더 기반 Thread.sleep** (MockOrderController)
> **🚨 부하 형태**: ramping-vus (10 → 30 → 50 → 100 VU 단계별 증가)
> **🆚 비교 축**: sync vs async / 구간별 TPS·실패율·p95 기록

## 시나리오 상황

- 결제 Confirm 요청이 점진적으로 증가(ramping)하는 상황
- 주문 서비스(Order)만 **고정 지연** (MockOrderController의 `Thread.sleep`)
- 다른 도메인(Trade/Settlement 등) 호출 없음 — 완전히 격리된 테스트
- **오류(5xx) 주입이 아니라 지연(slow dependency) 주입**으로 현실성 확보

> 핵심 질문: **"Order가 느려졌을 때, 결제 진입점까지 같이 무너지는가?"**

---

# 테스트 케이스 구성 (2×2)

## Case A — 전파를 확실히 증명 (5s, 100% 실패 유도)

- **조건**: Order 지연 5,000ms / Payment→Order Feign readTimeout 3,000ms
- **비교**: SYNC-A vs ASYNC-A
- **기대**:
  - SYNC-A: Feign timeout(3s)이 먼저 터지면서 `PAYMENT_CONFIRM_FAILED(5xx)` → **실패율 100% 수렴**
  - ASYNC-A: Order 호출 자체가 없으므로 `processSuccessfulPayment` 후 즉시 반환 → **성공 100% 유지**

## Case B — 부분 실패/변곡점 관찰 (2s)

- **조건**: Order 지연 2,000ms / Payment→Order Feign readTimeout 3,000ms
- **비교**: SYNC-B vs ASYNC-B
- **기대**:
  - SYNC-B: 지연 < timeout이라 바로 실패는 없지만, VU 증가 시 **p95/p99 급등 + 스레드 고갈** → 실패율 증가
  - ASYNC-B: VU와 무관하게 p95/p99 안정, 실패율 0% 유지

---

# 테스트 통제 조건

## 1. 부하 모델 통제 (k6: ramping-vus)

스파이크가 아니라 변곡점(무너지는 지점)을 관찰하기 위해 ramping-vus 사용.  
sync/async 모두 동일한 stages 구성을 적용하여 **부하 패턴을 단일 변수로 통제**한다.

```js
stages: [
    { duration: "30s", target: 10 },  // warm-up
    { duration: "10s", target: 30 },  // ramp
    { duration: "30s", target: 30 },  // sustain
    { duration: "10s", target: 50 },  // ramp
    { duration: "30s", target: 50 },  // sustain
    { duration: "10s", target: 100 }, // ramp
    { duration: "60s", target: 100 }, // sustain
    { duration: "30s", target: 0 },   // ramp-down
]
```

## 2. 장애/지연 주입 통제 (MockOrderController)

장애 원인을 단일화하여 **구조 차이만 비교**한다.

`MockOrderController` (`@Profile("loadtest")`) 는 Payment 서비스 내부에 탑재된 In-Process Mock 컨트롤러다.  
Order 서비스 URL이 `http://localhost:${port}/payment/mock/order/...` 로 라우팅되어, **실제 Order 서비스 없이** 지연을 주입한다.

```java
// MockOrderController.pendingShipmentOrder
if ("order".equalsIgnoreCase(faultTarget) && delayMs > 0) {
    Thread.sleep(delayMs); // ← 여기서 지연 주입
}
```

| 구분 | 설정 | 통제 의도 |
|---|---|---|
| 주입 대상 | **Order 서비스 단일** | 원인 혼입 방지 |
| 방식 | K6 헤더 `X-Fault-Target: order`, `X-Fault-Delay-MS` → MockOrderController Thread.sleep | slow dependency 재현 |
| 오류 | 5xx 미사용 (정상 지연만) | 현실적인 "느린 의존성" |
| 적용 구간 | 전체 테스트 구간 | 누적 효과 관찰 |
| 동기/비동기 격차 | **sync만 실제 호출**, async는 Order 호출 자체 없음 | 구조 외 변수 제거 |

| 케이스 | Order 지연 | Payment→Order readTimeout | 기대 효과 |
|---|---|---|---|
| **Case A** | 5,000ms | 3,000ms | timeout 충돌로 전파 100% 확증 |
| **Case B** | 2,000ms | 3,000ms | 부분 실패/변곡점 관찰 |

## 3. Feign 보호 장치 최소화 (`application-loadtest.yml`)

이번 실험은 "장애를 막는 실험"이 아닌 **장애 전파를 관찰하는 실험**이다.  
서킷브레이커/재시도를 모두 OFF하여 전파가 어디까지 도달하는지 있는 그대로 측정한다.

```yaml
spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: false   # 서킷 OFF
  kafka:
    listener:
      auto-startup: false  # 불필요한 Consumer 구동 방지

order-service:
  url: http://localhost:${server.port:8080}/payment/mock  # MockOrderController로 라우팅
```

## 4. 내부 timeout

Case A(지연 > timeout)와 Case B(지연 < timeout)를 나눠 완전 전파와 부분 실패를 구분 관찰한다.

```yaml
# application-loadtest.yml
spring:
  cloud:
    openfeign:
      client:
        config:
          order-service:
            readTimeout: 3000  # Order Feign readTimeout 3s 고정
```

> **주의:** Feign 설정의 `contextId` 우선순위에 의해, YAML 키는 FeignClient의 `name`이 아닌 `contextId`와 일치해야 실제 적용된다. → `TestOrderClient`의 `contextId: "testOrderClient"` 기준으로 설정.

## 5. 서버/DB 자원 통제 — "공정 비교" 중심

실험 결과가 "풀 설정 차이"로 왜곡되지 않도록 동기/비동기 동일 설정 유지.

```yaml
server:
  tomcat:
    threads:
      max: 200
      min-spare: 10

spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 3000
```

> 포인트: "최적화"가 아니라 **비교를 위한 통제**  
> (풀을 바꾸면 결과 해석이 흔들림)

## 6. 비동기 구조의 비교 범위 정의 (Noise 제거)

비동기 구조에서는 "Confirm 응답"과 "전파(SLA)"가 분리되므로 비교 지표도 분리해서 관측한다.

**비교 대상에 포함**
- Payment Confirm API 응답 시간 및 성공률
- 결제 상태 변경 (`READY → IN_PROGRESS → DONE`)

**비교 대상에서 제외**
- Order/Trade/Settlement 연동 결과 (async는 Confirm 시점에 호출 안 함)
- Kafka Consumer 처리 (`auto-startup: false`로 비활성화)
- 부가 리스너(노이즈) 비활성화

---

# 테스트 서비스 코드 설계

## 핵심 아키텍처 분기

```
[K6] → POST /test/api/payment/confirm/sync
            ↓
    TestPaymentController
            ↓
    TestPaymentService.confirmPaymentSync()
       1. prepareForConfirmWithoutOrderLookup()  ← Order 조회 없이 Payment 상태 준비
       2. processSuccessfulPayment()             ← Payment 상태 DONE으로 변경
       3. testOrderClient.pendingShipmentOrder() ← ⚠️ 동기 Feign 호출 (지연 전파 지점)
          ├─ 성공 시: 200 OK 반환
          └─ timeout 시: PAYMENT_CONFIRM_FAILED(5xx) throw

[K6] → POST /test/api/payment/confirm/async
            ↓
    TestPaymentController
            ↓
    TestPaymentService.confirmPaymentAsync()
       1. prepareForConfirmWithoutOrderLookup()  ← Order 조회 없이 Payment 상태 준비
       2. processSuccessfulPayment()             ← Payment 상태 DONE으로 변경
       ✅ 즉시 반환 (Order 호출 없음, 지연 전파 없음)
```

## TestPaymentStatusUpdater — 재실행 친화적 설계

DB seed/reset 없이 K6를 반복 실행할 수 있도록 설계:
- `paymentId`로 Payment 레코드를 찾으면 → 상태와 무관하게 `markAsReady()` 후 `IN_PROGRESS` 전이
- 레코드가 없으면 → `createLoadtestPayment()`로 자동 생성 후 `IN_PROGRESS` 전이
- `@Transactional(REQUIRES_NEW)` — 별도 빈(TestPaymentStatusUpdater)으로 분리하여 Spring 프록시 self-call 문제 해결

## TestOrderClient — 전용 Feign 클라이언트

```java
@FeignClient(name="unbox-order", contextId="testOrderClient", url="${order-service.url}", path="/order")
void pendingShipmentOrder(
    UUID id, UUID paymentId, String updatedBy,
    @RequestHeader("X-Fault-Target") String faultTarget,
    @RequestHeader("X-Fault-Delay-MS") long delayMs)
```

- 기존 `OrderClient`와 완전 분리 (`contextId`로 구분)
- `application-loadtest.yml`의 `order-service.url`이 `MockOrderController`로 라우팅

---

# 테스트 코드

## Sync (`confirm-fault-injection-sync.js`)

```js
const CONFIRM_URL = `${PAYMENT_BASE_URL}/test/api/payment/confirm/sync`;
const FAULT_DELAY_MS = String(__ENV.FAULT_DELAY_MS || __ENV.DELAY || "5000");

// ...

const response = http.post(CONFIRM_URL, payload, {
    timeout: "15s",
    headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${AUTH_TOKEN}`,
        "X-Test-Run-Id": TEST_RUN_ID,
        "X-Fault-Target": "order",       // MockOrderController가 읽어 지연 주입
        "X-Fault-Delay-MS": FAULT_DELAY_MS,
    },
    tags: { scenario_mode: "sync" },
});
```

## Async (`confirm-fault-injection-async.js`)

```js
const CONFIRM_URL = `${PAYMENT_BASE_URL}/test/api/payment/confirm/async`;
// FAULT_DELAY_MS 없음 — async는 Order를 호출하지 않으므로 fault 헤더 불필요

const response = http.post(CONFIRM_URL, payload, {
    timeout: "15s",
    headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${AUTH_TOKEN}`,
        "X-Test-Run-Id": TEST_RUN_ID,
        // 의도적으로 fault 헤더 없음 — 비교 축 명확화
    },
    tags: { scenario_mode: "async" },
});
```

## 실행 명령어

```bash
# 0) 테스트 데이터 파일 1회 생성 (200001~230000, 30,000건)
node /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/payment-load-test/generate-data.js

# 1) Case A - SYNC (Order delay 5,000ms > timeout 3,000ms → 100% 실패 예상)
k6 run \
  -e PAYMENT_BASE_URL=http://localhost:8085/payment \
  -e TEST_RUN_ID=caseA-sync \
  -e FAULT_DELAY_MS=5000 \
  -e DATA_PATH=/Users/jang/Desktop/UNBOX/UNBOX-BE/k6/payment-load-test/data.json \
  /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/payment-load-test/confirm-fault-injection-sync.js

# 2) 쿨다운
sleep 30

# 3) Case A - ASYNC (Order 호출 없음 → 100% 성공 예상)
k6 run \
  -e PAYMENT_BASE_URL=http://localhost:8085/payment \
  -e TEST_RUN_ID=caseA-async \
  -e DATA_PATH=/Users/jang/Desktop/UNBOX/UNBOX-BE/k6/payment-load-test/data.json \
  /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/payment-load-test/confirm-fault-injection-async.js

# 4) 쿨다운
sleep 30

# 5) Case B - SYNC (Order delay 2,000ms < timeout 3,000ms → 변곡점 관찰)
k6 run \
  -e PAYMENT_BASE_URL=http://localhost:8085/payment \
  -e TEST_RUN_ID=caseB-sync \
  -e FAULT_DELAY_MS=2000 \
  -e DATA_PATH=/Users/jang/Desktop/UNBOX/UNBOX-BE/k6/payment-load-test/data.json \
  /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/payment-load-test/confirm-fault-injection-sync.js

# 6) 쿨다운
sleep 30

# 7) Case B - ASYNC (Order 호출 없음 → 대조군, 1회로 A/B 공용 가능)
k6 run \
  -e PAYMENT_BASE_URL=http://localhost:8085/payment \
  -e TEST_RUN_ID=caseB-async \
  -e DATA_PATH=/Users/jang/Desktop/UNBOX/UNBOX-BE/k6/payment-load-test/data.json \
  /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/payment-load-test/confirm-fault-injection-async.js
```

> **서버 기동**: `./gradlew :unbox_payment:bootRun --args='--spring.profiles.active=loadtest'`
> 로그에 `The following 1 profile is active: "loadtest"` 확인 후 타격

---

# 성능 및 안정성 측정 지표

## 1) RPS (처리량)
- ramping 구간별 처리량 비교
- **동기 구조의 변곡점(처리량 하락 시작 구간)** 식별

## 2) p95 응답 지연 (ms)
- 평균 대신 tail latency 중심 비교
- 동기에서 **블로킹/고갈 징후**가 tail latency로 나타나는지 확인
- Case A-SYNC: 3,000ms(timeout)에 수렴하는 구간 관찰

## 3) Error Rate (실패율, %)
- Confirm 기준 2xx vs 5xx
- **하위 지연이 결제 진입점 실패로 전파되는 정도** 수치화

## 4) JVM Live Threads
- Tomcat 스레드 지표 대신 `jvm_threads_live_threads` 사용
- VU 상승 구간에서 계단식 상승 → 임계선 근접 시 대기열/지연/5xx 증가 동반
- **"무너지기 직전"의 직접 원인(스레드 고갈)을 보여주는 선행 지표**

## 5) HikariCP Active Connections
- 병목이 DB인지, 웹/외부 호출인지 구분
- 에러 급등 시점에 Hikari active가 낮으면: **DB 병목 아님, 동기 외부호출 블로킹 가능성 큼**
- Hikari도 같이 포화되면: **DB 경합까지 발생** → 원인 분리(스레드 병목 vs DB 병목)

---

# DEV 환경 테스트 전략

비동기 구조를 도입한 이유를 명확히 보여주기 위해 두 가지 테스트를 설계했습니다.

**Case A (5s > timeout 3s)**: Order 지연이 Payment Feign readTimeout을 초과하도록 설정해 **100% 실패 상황**을 강제 유도. 동기 구조는 `PAYMENT_CONFIRM_FAILED` 5xx 응답으로 실패율 100%에 수렴하고, 비동기 구조는 Order 호출 자체가 없으므로 0% 실패로 응답을 유지하는 차이를 명확히 확인.

**Case B (2s < timeout 3s)**: 지연이 timeout 이하라 즉각 실패는 없지만, VU를 단계적으로 증가시키며 동기 구조가 어느 지점부터 p95 급등과 실패율 증가를 보이는지 **변곡점을 관찰**하여 구조적 한계를 정량적으로 분석.

### 지표 해석 가이드

1. **Error Rate by Service**
   - payment 급등, order 안정 → 장애가 order 내부가 아니라 **payment→order 호출 경계에서 전파**된 패턴 증명
   - **전파 경로를 서비스 단위로 증명**하는 패널

2. **JVM Live Threads (Payment)**
   - VU 상승 구간에서 계단식 스레드 상승 관찰
   - **"무너지기 직전" 직접 원인(스레드 고갈)을 보여주는 선행 지표**

3. **HikariCP Active (Payment)**
   - 에러 급등 시 Hikari Active가 낮으면: **원인은 DB 아님, 동기 외부호출 블로킹**
   - **원인 분리(스레드 병목 vs DB 병목)** 를 위한 패널

---

# 트러블슈팅

### 1. JPA 낙관적 락(Optimistic Lock) — `NullPointerException: current is null`

**현상**: `@Version` 컬럼 추가 후 기존 데이터(version=NULL) 업데이트 시 NPE 발생  
**원인**: 기존 데이터에 version 컬럼이 NULL인 상태, JPA가 버전 비교 시 NULL을 Long으로 변환 실패  
**해결**: 기존 데이터 DELETE 후 version=0으로 새로 입력. `TestPaymentStatusUpdater`에서 `markAsReady()` 호출로 DONE/IN_PROGRESS 상태도 재실행 가능하게 리셋

### 2. Kafka 컨슈머 처리 지연 (Lag)

**현상**: 파티션 3개인데 Lag 해소 안 됨  
**원인**: `concurrency` 기본값 1 — 파티션 3개를 단일 스레드가 순차 처리  
**해결**: `setConcurrency(3)` 설정으로 처리량 3배 확대

### 3. 비동기 처리 중 Silent Failure (데이터 불일치)

**현상**: DB 오류 발생에도 Kafka ack 처리 → 결제 성공, 후속 로직 미실행  
**원인**: `catch` 블록에서 throw 없이 로그만 남김  
**해결**: `throw e` 추가로 Kafka Retry 메커니즘 활성화

### 4. 모니터링 그래프 끊김 + p95 스파이크

**현상**: Grafana 그래프 끊김, p95 20초 치솟음  
**원인**: 동기 호출 체인으로 스레드/CPU 고갈 → Prometheus scrape 자체 실패 / 타임아웃을 60s로 길게 설정해 늦은 성공 응답이 스파이크로 표현  
**결론**: 동기 아키텍처 한계를 보여주는 지표로 테스트 목적 달성 확인

### 5. 가상 유저 인증 오류 (403 Forbidden)

**현상**: K6 VU의 모든 API 호출에서 403 반환  
**원인**: VU는 로그인 없이 API 직접 호출 → Security Context에 인증 객체 없음  
**해결**: `TestPaymentController`에서 `userDetails != null ? userDetails.getUserId() : 1L` 처리로 인증 없이도 테스트 전용 userId 1L로 폴백

### 6. Feign Client contextId 불일치로 타임아웃 미적용

**현상**: `readTimeout: 3000ms` 설정에도 기본값(10s)이 적용됨  
**원인**: `@FeignClient(contextId="testOrderClient")`가 있을 경우, YAML 설정 키는 `name`이 아닌 `contextId` 기준으로 조회됨  
**해결**: YAML 설정 키를 `testOrderClient`로 변경하여 실제 적용

### 7. TestPaymentStatusUpdater Self-Call @Transactional 무효

**현상**: `@Transactional(REQUIRES_NEW)` 메서드가 같은 클래스에서 self-call 되어 트랜잭션 적용 안 됨  
**원인**: Spring 프록시 기반 AOP는 self-call 시 프록시를 거치지 않음  
**해결**: `TestPaymentStatusUpdater` 별도 빈으로 분리, `TestPaymentPreparationService`가 주입받아 호출
