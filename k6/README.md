# **테스트 목적**

---

테스트의 목적은 **결제 완료(Confirm) API를 진입점으로**, 동기 구조와 비동기 구조가 **하위 서비스 지연(slow dependency)** 상황에서 보이는 구조적 차이(장애 전파 vs 격리)를 **정량 지표로 비교**하는 것이다.

- **전파성(Propagation)**: 하위 서비스(Order) 지연이 **결제 응답 실패/지연으로 전파되는가?**
- **안정성(Stability)**: 트래픽 증가(ramping) 시 **결제 진입점이 어디서부터 무너지는가(변곡점)?**

> **목표:** 동일한 부하(ramping)와 동일한 지연(3s) 조건에서,
>
> **동기 구조는 어떻게 실패하는지(전파/블로킹)**,
>
> 비동기 구조는 무엇을 지켜내는지(응답 안정/격리)를 수치로 비교한다.

# **테스트 시나리오**

---

<aside>
💡

**🏁 진입점 API**: Payment Confirm (결제 완료)

**💉 장애/지연 주입**: Order 서비스 **timeout** **고정 3초**

**🚨 부하 형태**: ramping-vus (10 → 30 → 50 → 100 VU 단계별 증가)

**🆚 비교 축**: **sync** vs **async** / 구간별 TPS·실패율·p95 기록

</aside>

## 시나리오 상황

- 결제 Confirm 요청이 점진적으로 증가(ramping)하는 상황
- 주문 서비스(Order)만 **timeout** **고정 3초**
- 다른 도메인(Trade/Settlement 등)은 **정상 동작 유지**
- **오류(5xx) 주입이 아니라 지연(slow dependency) 주입**으로 현실성 확보

> 핵심 질문: **"Order가 느려졌을 때, 결제 진입점까지 같이 무너지는가?"**

# 테스트 케이스 구성 (2×2)

---

## Case A — 전파를 확실히 증명 (5s, 100% 실패 유도)

- 목적: "동기 구조는 구조적으로 전파된다"를 명확히 증명
- 조건: **Order 지연 5s / Payment→Order timeout 3s**
- 비교: **SYNC-A vs ASYNC-A**
- 기대: 동기는 **Confirm 실패율 100%에 수렴**, 비동기는 **Confirm 성공(이벤트 발행) 유지**

## Case B — 부분 실패/변곡점 관찰 (2s)

- 목적: **"동기 구조가 어디서부터 무너지기 시작하는지"** + 비동기 대비
- 조건: **Order 지연 2s / Payment→Order timeout 3s**
- 비교: **SYNC-B vs ASYNC-B**
- 기대: 동기는 VU 증가에 따라 **p95/p99 급등 + 실패율 증가**, 비동기는 **Confirm 안정 유지**

# **테스트 통제 조건**

---

### 1. 부하 모델 통제 (k6: ramping-vus)

> **의도**
>
> 스파이크가 아니라 변곡점(무너지는 지점)을 관찰하기 위해 부하를 점진 증가시키는 ramping-vus를 사용한다.

```js
stages: [
  { duration: "30s", target: 10  },  // warm-up (기준 구간)
  { duration: "10s", target: 30  },  // ramp
  { duration: "30s", target: 30  },  // sustain (1단계 부하)
  { duration: "10s", target: 50  },  // ramp
  { duration: "30s", target: 50  },  // sustain (2단계 부하)
  { duration: "10s", target: 100 },  // ramp
  { duration: "30s", target: 100 },  // sustain (최대 부하)
  { duration: "30s", target: 0   },  // ramp-down
]
```

- 구조 비교 목적상 **부하 패턴을 동일**하게 유지
- 클라이언트 타임아웃은 실사용 상한선(`15s`)으로 고정 (서버 내부 timeout과 구분)

**관측 구간:**

| VU | 관찰 목적 |
| --- | --- |
| 10 | 기준선 (정상 TPS·실패율·p95) |
| 30 | 변곡점 시작 여부 확인 |
| 50 | 실패율 급등 구간 |
| 100 | 포화·고갈 완전 확증 |

### 2. 장애/지연 주입 통제 (Fault Injection: Order 단일)

> **핵심 원칙**
>
> 장애 원인을 단일화하여 **구조 차이만 비교**한다.

| 구분 | 설정 | 통제 의도 |
| --- | --- | --- |
| 주입 대상 | **Order 서비스 단일** | 원인 혼입 방지 |
| 방식 | **헤더 기반 지연** (`X-Fault-Target: order`, `X-Fault-Delay-MS`) | slow dependency 재현 |
| 오류 | 5xx 미사용 | 현실적인 "느린 의존성" |
| 적용 | 전체 테스트 구간 | 누적 효과 관찰 |
| 동일 적용 | sync/async 모두 | 구조 외 변수 제거 |

| 케이스 | Order 지연 | Payment→Order readTimeout | 기대 효과 |
| --- | --- | --- | --- |
| **Case A** | 5s | 3s | 지연 > timeout → 모든 동기 요청 timeout 즉각 전파 |
| **Case B** | 2s | 3s | 지연 < timeout → 스레드 점유 누적으로 변곡점 관찰 |

### 3. 내부 동기 통신(Feign) 보호 장치 최소화

> **의도**
>
> 이번 실험은 "장애를 막는 실험"이 아니라 **장애 전파를 관찰하는 실험**이다.
>
> 따라서 서킷/재시도는 1차 실험에서 배제한다. (2차 실험에서 별도 검증)

```yaml
# unbox_payment/src/main/resources/application.yml (실제 적용)
spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: false
```

✅ **서킷 OFF / 재시도 OFF** → 전파가 어디까지 도달하는지 "있는 그대로" 측정

### 4. 내부 timeout

> **의도**
>
> Case A(지연 > timeout)와 Case B(지연 < timeout)를 나눠 **완전 전파(100% 실패)**와 **부분 실패(변곡점)**를 구분 관찰한다.

```yaml
# unbox_payment/src/main/resources/application.yml (실제 적용)
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 1000
            readTimeout: 3000       # 전체 기본값 3s
          orderClient:
            readTimeout: 3000       # Order 클라이언트 명시
```

### 5. 서버/DB 자원 통제 — "공정 비교" 중심

> **의도**
>
> 실험 결과가 "풀 설정 차이"로 왜곡되지 않도록, **동기/비동기에서 동일 설정을 유지**한다.
>
> DB 풀은 이번 실험의 주 원인(내부 HTTP 지연)이 아니므로 **병목이 되지 않게 충분히 확보**한다.

**서버(웹) 스레드 풀**

- Tomcat 스레드 별도 설정 없음 → Spring Boot 기본값(`max: 200`) 적용
- 동기 구조의 블로킹은 **스레드 고갈보다 readTimeout 3s 대기 누적**으로 나타남

**DB 커넥션 풀(Hikari)**

```yaml
# unbox_payment/src/main/resources/application.yml (실제 적용)
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000
```

> 포인트: "최적화"가 아니라 **비교를 위한 통제** — 동기/비동기 모두 동일 설정으로 공정 비교 보장

### 6. 비동기 구조의 비교 범위 정의 (Noise 제거)

> **의도**
>
> 비동기 구조에서는 "Confirm 응답"과 "전파(SLA)"가 분리되므로 비교 지표도 분리해서 관측한다.

**비교 대상에 포함**

- Payment Confirm API 응답 (TPS, 실패율, p95)
- 결제 상태 변경
- (비동기) 핵심 이벤트 발행 / (Outbox) outbox insert

**비교 대상에서 제외**

- 알림/통계/로그성 이벤트
- 테스트 목적과 무관한 추가 동기 호출
- 부가 리스너(노이즈) 비활성화

---

# 테스트 설계

## 테스트 코드

```js
import http from "k6/http";
import { check, sleep } from "k6";
import { Rate } from "k6/metrics";
import exec from "k6/execution";

// ── 실행 모드 (sync / async) ──
const MODE = (__ENV.MODE || "async").toLowerCase();

// ── 환경 변수 ──
const PAYMENT_BASE_URL = __ENV.PAYMENT_BASE_URL || "http://localhost:8085/payment";
const DATA_PATH        = __ENV.DATA_PATH || "./data.json";

// ── 클라이언트 타임아웃 (사용자 관점 최대 대기) ──
const CONFIRM_TIMEOUT = __ENV.CONFIRM_TIMEOUT || "15s";

// ── Fault Injection ──
const ORDER_DELAY_MS = Number(__ENV.ORDER_DELAY_MS || 3000);
const FAULT_TARGET   = ((__ENV.FAULT_TARGET !== undefined ? __ENV.FAULT_TARGET : "order") || "").toLowerCase();

// ── think time ──
const SLEEP_MS = Number(__ENV.SLEEP_MS || 50);

const CONFIRM_URL = `${PAYMENT_BASE_URL}/api/payment/confirm`;

// ── 커스텀 메트릭 ──
const failRate = new Rate("confirm_fail_rate");

// ── 테스트 데이터 ──
const data = JSON.parse(open(DATA_PATH));

// ── 부하 모델: 단계별 VU 증가 ──
export const options = {
  scenarios: {
    payment_flow: {
      executor: "ramping-vus",
      stages: [
        { duration: "30s", target: 10  },   // warm-up
        { duration: "10s", target: 30  },   // ramp
        { duration: "30s", target: 30  },   // sustain
        { duration: "10s", target: 50  },   // ramp
        { duration: "30s", target: 50  },   // sustain
        { duration: "10s", target: 100 },   // ramp
        { duration: "30s", target: 100 },   // sustain
        { duration: "30s", target: 0   },   // ramp-down
      ],
      gracefulRampDown: "10s",
    },
  },
  thresholds: {
    confirm_fail_rate: ["rate<0.99"],   // 실험용 (거의 제한 없음)
    http_req_duration: ["p(95)<15000"],
  },
};

// ── VU/iteration 기반 데이터 분배 ──
function pick(arr) {
  const idx = exec.scenario.iterationInTest % arr.length;
  return arr[idx];
}

// ── 메인 VU 함수 ──
export default function () {
  const d = pick(data);

  const headers = {
    "Content-Type":   "application/json",
    "X-Test-Mode":    MODE,
    "X-Test-User-ID": String(d.buyerId ?? 1),
  };

  const tags = { api: "payment_confirm", mode: MODE };

  if (FAULT_TARGET) {
    headers["X-Fault-Target"]   = FAULT_TARGET;
    headers["X-Fault-Delay-MS"] = String(ORDER_DELAY_MS);
    tags.fault_target           = FAULT_TARGET;
    tags.order_delay_ms         = String(ORDER_DELAY_MS);
  }

  const res = http.post(
    CONFIRM_URL,
    JSON.stringify({ paymentId: d.paymentId, paymentKey: d.paymentKey, amount: d.amount }),
    { timeout: CONFIRM_TIMEOUT, headers, tags }
  );

  const ok = check(res, { "confirm 2xx": (r) => r.status >= 200 && r.status < 300 });
  failRate.add(!ok);

  if (!ok) {
    console.error(`[FAIL] mode=${MODE} status=${res.status} vu=${exec.vu.idInTest} body=${(res.body || "").slice(0, 200)}`);
  }

  sleep(SLEEP_MS / 1000);
}
```

## 실행 명령어

```bash
# Case A — sync (Order 5s 지연, 전파 확증)
k6 run \
  -e PAYMENT_BASE_URL=http://localhost:8085/payment \
  -e DATA_PATH=./k6/data.json \
  -e MODE=sync \
  -e ORDER_DELAY_MS=5000 \
  -e FAULT_TARGET=order \
  ./k6/payment-syn-vs-async.js

# Case A — async (동일 조건)
k6 run \
  -e PAYMENT_BASE_URL=http://localhost:8085/payment \
  -e DATA_PATH=./k6/data.json \
  -e MODE=async \
  -e ORDER_DELAY_MS=5000 \
  -e FAULT_TARGET=order \
  ./k6/payment-syn-vs-async.js

# Case B — sync (Order 2s 지연, 변곡점 관찰)
k6 run \
  -e PAYMENT_BASE_URL=http://localhost:8085/payment \
  -e DATA_PATH=./k6/data.json \
  -e MODE=sync \
  -e ORDER_DELAY_MS=2000 \
  -e FAULT_TARGET=order \
  ./k6/payment-syn-vs-async.js

# Case B — async (동일 조건)
k6 run \
  -e PAYMENT_BASE_URL=http://localhost:8085/payment \
  -e DATA_PATH=./k6/data.json \
  -e MODE=async \
  -e ORDER_DELAY_MS=2000 \
  -e FAULT_TARGET=order \
  ./k6/payment-syn-vs-async.js
```

---

# 결과 관측 및 해석 가이드

## 기록 항목 (구간별)

| VU 구간 | 측정 지표 | sync | async |
| --- | --- | --- | --- |
| 10 (기준) | TPS / 실패율 / p95 | 기준선 | 기준선 |
| 30 | TPS / 실패율 / p95 | 변곡점 시작? | 안정 유지? |
| 50 | TPS / 실패율 / p95 | 실패 급등? | 안정 유지? |
| 100 | TPS / 실패율 / p95 | 포화 확증 | 격리 확증 |

## 정상 관측 패턴

```
[Case A — sync]  Order 5s 지연 / readTimeout 3s
  VU 전 구간: 실패율 ≈ 100%, p95 ≈ 3s (매 요청 timeout 즉각 전파)

[Case B — sync]  Order 2s 지연 / readTimeout 3s
  VU=10  : 실패율 낮음, p95 ≈ 2s (스레드 여유 있음)
  VU=30  : 실패율 ↑ 시작, p95 급등           ← 변곡점
  VU=50~ : 실패율 급등, TPS 하락 (스레드 점유 누적)

[async]  Case A/B 공통
  VU=10~100: 실패율 ≈ 0%, p95 낮게 유지 (Order 지연과 무관)
```

## 성능 및 안정성 측정 지표

| 지표 | sync | async | 관점 |
| --- | --- | --- | --- |
| `confirm_fail_rate` | VU 증가에 따라 ↑ | ≈ 0% 유지 | 장애 전파 여부 |
| `p95` latency | 급등 (변곡점) | 낮게 유지 | 블로킹 누적 여부 |
| TPS | 포화 후 하락 | 선형 유지 | 처리 능력 |
| 변곡점 VU | 관찰 (핵심 목표) | N/A | 동기 구조 한계 |
