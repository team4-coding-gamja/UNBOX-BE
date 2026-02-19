# 테스트 목적

테스트의 목적은 결제 완료(Confirm) 이후 이벤트 발행 방식에 따른 **데이터 정합성(Consistency)과 시스템 신뢰성(Reliability)** 을 수치(정량)로 비교하여, **Transactional Outbox 도입의 정당성을 근거로 제시**하는 것이다.

본 테스트는 **주문 상태 변경(E2E 비즈니스 처리)** 여부가 아니라,

> **Kafka 장애 상황에서 이벤트 자체가 유실되었는지(Event Delivery Guarantee)** 를 순수하게 검증하는 데 목적이 있다.

단순 비동기(Fire-and-Forget) 방식은 인프라 장애 상황에서 이벤트 유실(Data Loss)을 유발할 수 있으며, Outbox 패턴은 이를 DB에 안전하게 보관 후 재발행하여 **최종 유실 0(Eventual Consistency)** 로 복구함을 입증한다.

### 🔎 검증 범위 (Scope)

본 테스트가 검증하는 범위는 다음과 같다:

> **Payment가 발행한 이벤트가 payment-events 토픽 관측치로 최종 도달했는가**

즉,

- ❌ 주문 상태 변경
- ❌ 거래 생성 여부
- ❌ 다운스트림 서비스 성공 여부

는 **검증 범위 밖**이다.

이는 주문 로직, DB, 네트워크, 재고 처리 등 다양한 변수가 유실 원인으로 혼입되는 것을 방지하기 위함이다.

### 📌 검증 질문

> Kafka가 죽었을 때, **200 OK로 성공 처리된 결제 건의 이벤트가 최종적으로 토픽에 도달했는가?**

### 검증 항목

- **정합성(Consistency)**

  Kafka 장애 시 이벤트 유실(Data Loss)이 발생하는가?

- **신뢰성(Reliability)**

  장애 복구 후 자동으로 데이터가 복구(Eventual Consistency)되는가?

- **성능 비용(Overhead)**

  Outbox 도입으로 인한 DB write 추가 비용이 TPS / p95 / p99에 허용 가능한 범위인가?

> **목표**
>
> 동일한 부하 + 동일한 Kafka 장애 조건에서
>
> - **Simple Async는 이벤트 유실(loss_rate > 0)을 보이고**
> - **Outbox는 최종 유실 0(loss_rate = 0) + 자동 복구(drain_time 측정)를 보인다**

---

# 테스트 시나리오

<aside>
💡

🏁 **진입점 API:** Payment Confirm (결제 완료)

💉 **장애 주입:** **Kafka 브로커 pause/unpause** (기본값, 20s 시점, 20s 지속)

🚨 **부하 형태:** Constant Load (ramping-arrival-rate 활용하여 일정 부하 유지)

🆚 **비교 대상:** Simple Async vs Transactional Outbox

</aside>

## 시나리오 상황

1. **Setup:** `setup-fixture.sh`로 테스트용 대량 결제 데이터 생성 (Run ID 기반 격리)
2. **Observer 연결 검증:** `kafka-console-consumer.sh` 기동 후 probe 메시지를 직접 토픽에 발행하여 수신 여부로 연결 완료를 확인 (최대 3회 재시도)
3. **Warm-up (0~10s):** 결제 Confirm 요청이 일정하게 유입 (기본: 10 TPS)
4. **Steady State & Failure (10~50s):**
   - **장애 주입 조건이 모두 충족**될 때까지 대기 후 Kafka 브로커 **일시정지(pause)**
     - 조건 1: 테스트 시작 후 `--failure-at-seconds`(기본 20s) 경과
     - 조건 2: observer가 관측한 고유 paymentId 수 ≥ `--min-observed-before-failure`(기본 30건)
   - 약 20초간 장애 유지 후 **재개(unpause)**
   - Kafka 재준비 완료를 `kafka-broker-api-versions.sh`로 확인한 뒤 Outbox drain 폴링 시작
5. **Cooldown (50~60s):** 잔여 트래픽 처리 및 종료
6. **Verification:** Observer Consumer 로그 및 DB 데이터를 기반으로 유실률 및 복구 시간 자동 산출

> **핵심 질문:**
> "Kafka가 죽었을 때, **성공(200 OK)한 결제 건**에 대한 이벤트가 최종적으로 Consumer에게 도달했는가?"

---

# 판정 기준

## 판정 기준

### 유실 판정: "주문 상태 변경"이 아니라 "이벤트 도달 여부" 기준

주문 상태 변경은 다음과 같은 변수가 혼입된다:

- Consumer 로직 버그
- DB 타임아웃
- 재고 부족
- 멱등성 오류
- 네트워크 장애

따라서 유실 원인을 Kafka 구조 차이로 단정하기 어렵다.

**본 테스트는 Kafka 토픽 수신 여부를 1차 판정 기준으로 삼는다.**

### 판정 공식

```hcl
loss_count = payment_done_count - topic_observed_count
loss_rate  = (topic_loss_count / payment_done_count) * 100
```

- `payment_done_count`: `p_payment` 테이블에서 `status='DONE' AND payment_key LIKE 'test_success_{run_id}_%'` 집계
- `topic_observed_count`: 독립 Observer Consumer(`consistency-observer-{run_id}-{mode}`)가 `payment-events` 토픽에서 수신한 고유 `paymentId` 수

| 구조 | 기대 결과 |
| --- | --- |
| Simple Async | topic_loss_rate > 0 가능 |
| Outbox | topic_loss_rate = 0 |

### ⚠️ Async 유실이 항상 발생하지 않을 수 있음 (중요)

Kafka Producer는 기본적으로 다음 동작을 수행한다:

- 내부 버퍼에 레코드 임시 저장
- 재시도(retries)
- `delivery.timeout.ms`(기본 약 120초) 범위 내 재전송 시도

따라서 브로커 다운 시간이 짧을 경우:

> 유실이 아니라 "지연 전송(Delayed Delivery)"으로 처리될 수 있음

이 경우 loss_rate = 0으로 나타날 수 있으나,

이는 구조적 안정성이 아닌 **Kafka 클라이언트 기본 재시도 동작의 결과**이다.

### 유실 재현을 위한 테스트 전용 설정 (Async)

**유실 확실 재현을 위한 테스트 전용 Async Producer 설정:**

`PaymentDirectAsyncEventProducer`는 환경변수로 다음 값을 주입받도록 구현되어 있음:

```yaml
# application.yml (unbox_payment)
payment:
  test:
    async-producer:
      retries: ${PAYMENT_TEST_ASYNC_PRODUCER_RETRIES:0}
      delivery-timeout-ms: ${PAYMENT_TEST_ASYNC_PRODUCER_DELIVERY_TIMEOUT_MS:5000}
      max-block-ms: ${PAYMENT_TEST_ASYNC_PRODUCER_MAX_BLOCK_MS:5000}
```

이 설정으로 브로커 장애 시 재시도 없이 즉시 실패 → 유실 발생

## 복구판정 (Outbox 전용)

### drain_time 정의

```
drain_time = Kafka 재기동 시점 ~ payment_outbox(PENDING + PROCESSING) = 0 도달 시점
```

`run-case.sh`가 Kafka 복구 후 1초 간격 폴링으로 `drain_time`을 자동 측정하며, `DRAIN_TIMEOUT_SECONDS`(기본 180초) 초과 시 `TIMEOUT` 판정

### Outbox 최종 상태 검증

| **지표** | **기대** | **의미** |
| --- | --- | --- |
| `pendingFinal` | 0 | 모든 이벤트 최종 발행 완료 |
| `publishedCount` | = `paymentDoneCount` | 발행 수 = 결제 성공 수 |
| `consumerDrainStatus` | DONE | 드레인 타임아웃 없이 완료 |

## 자동 판정 기준 (run-all.sh)

| **판정 항목** | **기준** | **의미** |
| --- | --- | --- |
| Async 유실 발생 | `async_loss_rate > 0` | Async의 구조적 한계 입증 |
| Outbox 무유실 | `outbox_loss_rate = 0` | Outbox의 정합성 보장 입증 |
| Outbox 잔여 없음 | `outbox_pending_final = 0` | 모든 이벤트 최종 발행 |
| Outbox 드레인 완료 | `consumerDrainStatus = DONE` | 복구 완료 |
| API 안정성 | 양 모드 `non-2xx = 0` | 테스트 자체의 유효성 |

→ 5개 모두 PASS일 때 **최종 판정: PASS**

## 중복/멱등성 전제

- Transactional Outbox는 **At-least-once 전송** → 중복 이벤트 가능
- 따라서 Consumer는 멱등성으로 중복을 흡수해야 한다(테스트/문서에 전제 명시)

예시:

- `event_id UNIQUE` 제약
- `processed_event` 테이블(처리 완료 event_id 저장)
- `processed_event_id` 저장 + idempotent update

---

# 테스트 케이스 구성

## **Case A — 데이터 유실 검증 (Simple Async)**

- **목적:** "단순 비동기 방식은 브로커 장애 시 이벤트가 유실된다"는 것을 증명
- **조건:** Kafka pause 20s (`--failure-injection-mode pause`, `--broker-down-seconds 20`)
- **기대:**
  - API 응답 성공률 100% (사용자는 결제 성공으로 인지)
  - **이벤트 유실 발생:** `Payment(Done)` 개수 > `Consumed Event` 개수
  - **Loss Rate > 0%**

## **Case B — 정합성 보장 검증 (Transactional Outbox)**

- **목적:** "Outbox는 Kafka 장애 구간에도 이벤트를 DB에 보관하고, 복구 후 자동 발행하여 최종 유실 0을 달성"을 입증
- **조건:** Kafka pause 20s (동일 조건)
- **기대:**
  - API 응답 성공률 100% (사용자는 결제 성공으로 인지)
  - **이벤트 유실 없음:** `Payment(Done)` == `Consumed Event` (**Loss Rate 0%**)
  - **자동 복구:** Kafka 재개 후 N초 내에 `PENDING` 데이터가 `PUBLISHED`로 변경됨 (**Drain Time 측정**)
  - **중복 발생 가능성:** Consumer의 **멱등성(Idempotency)** 로직으로 방어됨을 확인

---

# 테스트 통제 조건

### **1. 부하 모델 통제 (k6: Constant Load)**

> **의도:**
> 장애 전/중/후를 동일 부하로 유지해 `loss_rate`, `drain_time`을 명확히 계량

```js
stages: [
  { target: RATE, duration: `${WARMUP_SECONDS}s` },   // Warm-up (10s)
  { target: RATE, duration: `${STEADY_SECONDS}s` },    // Steady + 장애 (40s)
  { target: 0,    duration: `${COOLDOWN_SECONDS}s` },  // Cooldown (10s)
]
```

기본값: `RATE=10` TPS, 총 60초, `PRE_ALLOCATED_VUS=50`, `MAX_VUS=200`

### **2. 장애 주입 통제 (Infrastructure Failure)**

> **핵심 원칙**
>
> 장애 원인을 단일화하여 **구조 차이만 비교**한다.

| 구분 | 설정 | 통제 의도 |
| --- | --- | --- |
| 주입 대상 | Kafka Broker 단일 | 원인 혼입 방지 |
| 방식 | `docker pause` / `unpause`(기본), `stop/start` 선택 가능 | 장애 재현 단순화 (`pause`는 컨테이너 IP 유지로 재연결 안정성 확보) |
| 지속 시간 | 20초 고정 | 비교 가능성 확보 |
| 주입 시점 | 테스트 시작 후 ≥20s **AND** 관측 건수 ≥30건 | 관측 기준 확보 후 장애 주입 (측정 유효성 보장) |
| 동일 적용 | async/outbox 모두 | 구조 외 변수 제거 |

실행 흐름

```
k6 시작
→ failure-arm 조건 대기 (elapsed ≥ 20s AND observed ≥ 30)
→ docker pause unbox-kafka
→ sleep 20s
→ docker unpause unbox-kafka
→ wait_for_kafka_ready (재준비 확인)
→ k6 종료 대기
```

### **3. 서버/DB 자원 통제**

- Payment/DB 스펙, HikariCP, JVM, 컨테이너 리소스는 **두 케이스 동일**
- Outbox로 인해 DB write가 늘어나므로 DB 커넥션 풀은 병목이 되지 않도록 여유 설정 (단, Async와 동일 설정 유지)

### **4. 인증/테스트 데이터 통제**

- Confirm API는 JWT 인증 필요
- `--auth-token`을 명시하지 않으면 케이스 실행마다 `generate-test-jwt.sh`로 fresh JWT를 자동 생성 (TTL = 총 테스트 시간 + drain 여유 + 300s)
- READY 결제를 재사용하면 중복 confirm(4xx)로 비교가 오염될 수 있으므로 `setup-fixture.sh`가 run_id별로 fixture를 생성하여 **중복 오염 제거**

### **5. Observer 연결 통제**

- Observer(`kafka-console-consumer.sh`)는 테스트 시작 전 **probe 메시지 수신 확인**으로 실제 연결 완료 검증
- 연결 실패 시 최대 3회 재시도 (회당 20초 대기)
- Kafka pre-flight 체크 (`wait_for_kafka_ready 60s`)로 브로커가 실제 응답 가능 상태임을 확인 후 시작
- Consumer 재연결 설정: `reconnect.backoff.ms=500`, `reconnect.backoff.max.ms=2000`, `metadata.max.age.ms=1000`

### **6. 환경 명칭 통일 (재현성)**

- Kafka 컨테이너명: `unbox-kafka`
- Postgres 컨테이너명: `unbox-postgres`
- Payment URL: `http://localhost:8085/payment`

---

# 테스트 설계

## 테스트 구성

- `confirm-consistency.js`: k6 부하 테스트 스크립트. 성공/실패/TPS/지연 지표 수집.
- `scripts/setup-fixture.sh`: 테스트용 Order/Payment fixture 대량 생성 (데이터 오염 방지).
- `scripts/generate-test-jwt.sh`: 인증 통과를 위한 테스트용 JWT 생성.
- `scripts/run-case.sh`: 단일 케이스 실행 및 장애 자동 주입, 리포트 생성의 핵심 스크립트.
- `scripts/run-all.sh`: Async → Outbox 순차 실행 자동화.
- `sql/verify-loss.sql`: Run ID 기반 정합성 검증 쿼리.

## 테스트 코드

```js
import http from "k6/http";
import exec from "k6/execution";
import { check, sleep } from "k6";
import { Counter, Rate } from "k6/metrics";
import { SharedArray } from "k6/data";

// ── 환경 변수 ──
const MODE = (__ENV.MODE || "outbox").toLowerCase();
const CONFIRM_URL = `${(__ENV.PAYMENT_BASE_URL || "http://localhost:8085/payment")
  .replace(/\/$/, "")}/api/payment/confirm`;
const AUTH_TOKEN = __ENV.AUTH_TOKEN || "";
const DATA_PATH = __ENV.DATA_PATH || "../data.json";
const TEST_RUN_ID = __ENV.TEST_RUN_ID || "manual";

const RATE = Number(__ENV.RATE || 10);
const WARMUP_SECONDS = Number(__ENV.WARMUP_SECONDS || 10);
const STEADY_SECONDS = Number(__ENV.STEADY_SECONDS || 40);
const COOLDOWN_SECONDS = Number(__ENV.COOLDOWN_SECONDS || 10);

// ── Validation ──
if (!["async", "outbox"].includes(MODE)) {
  throw new Error(`MODE must be one of: async | outbox. got=${MODE}`);
}
if (!AUTH_TOKEN) {
  throw new Error("AUTH_TOKEN is required.");
}

// ── Fixture 데이터 로딩 ──
const testData = new SharedArray("confirm-test-data", function () {
  const parsed = JSON.parse(open(DATA_PATH));
  if (!Array.isArray(parsed) || parsed.length === 0) {
    throw new Error(`Invalid DATA_PATH. path=${DATA_PATH}`);
  }
  return parsed;
});

// ── 커스텀 메트릭 ──
const confirmRequestsTotal  = new Counter("confirm_requests_total");
const confirmSuccessTotal   = new Counter("confirm_success_total");
const confirmNon2xxTotal    = new Counter("confirm_non2xx_total");
const datasetReuseTotal     = new Counter("dataset_reuse_total");
const confirm2xxRate        = new Rate("confirm_2xx_rate");

// ── 부하 모델: 일정 부하 유지 ──
export const options = {
  scenarios: {
    confirm_consistency: {
      executor: "ramping-arrival-rate",
      startRate: RATE,
      timeUnit: "1s",
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 50),
      maxVUs: Number(__ENV.MAX_VUS || 200),
      stages: [
        { target: RATE, duration: `${WARMUP_SECONDS}s` },
        { target: RATE, duration: `${STEADY_SECONDS}s` },
        { target: 0,    duration: `${COOLDOWN_SECONDS}s` },
      ],
    },
  },
};

// ── fixture 순차 소비 (소진 시 테스트 중단) ──
function pickData() {
  const iteration = exec.scenario.iterationInTest;
  if (iteration >= testData.length) {
    datasetReuseTotal.add(1);
    exec.test.abort(
      `Dataset exhausted: iteration=${iteration}, dataSize=${testData.length}. ` +
      `Increase --fixture-count or decrease --rate to avoid duplicate confirms.`
    );
  }
  return testData[iteration];
}

// ── 메인 VU 함수 ──
export default function () {
  const row = pickData();
  confirmRequestsTotal.add(1);

  const response = http.post(CONFIRM_URL, JSON.stringify({
    paymentId: row.paymentId,
    paymentKey: row.paymentKey,
    amount: row.amount,
  }), {
    timeout: __ENV.REQUEST_TIMEOUT || "10s",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${AUTH_TOKEN}`,
      "X-Test-Mode": MODE,            // async | outbox 분기
      "X-Test-Run-Id": TEST_RUN_ID,   // run_id 격리
    },
  });

  const ok = check(response, {
    "confirm 2xx": (r) => r.status >= 200 && r.status < 300,
  });

  confirm2xxRate.add(ok);
  ok ? confirmSuccessTotal.add(1) : confirmNon2xxTotal.add(1);
}
```

## 테스트 실행

```bash
# 옵션 1: 전체 케이스 자동 실행 (권장)
./k6/consistency/scripts/run-all.sh

# 옵션 2: 단일 케이스 상세 실행 (디버깅용)
./k6/consistency/scripts/run-case.sh \
  --mode outbox \
  --rate 10 \
  --request-timeout 30s \
  --warmup-seconds 10 \
  --steady-seconds 40 \
  --cooldown-seconds 10 \
  --failure-at-seconds 20 \
  --broker-down-seconds 20 \
  --min-observed-before-failure 30 \
  --failure-arm-timeout-seconds 120 \
  --failure-injection-mode pause \
  --fixture-count 1200
```

`run-all.sh`가 내부에서 자동으로:

- fixture 생성 (`setup-fixture.sh`)
- 케이스별 fresh JWT 생성 (`generate-test-jwt.sh`, TTL 자동 계산)
- Kafka pre-flight 연결 확인 (`wait_for_kafka_ready`)
- Observer 기동 및 probe 메시지로 **실제 연결 완료 검증** (최대 3회 재시도)
- k6 실행
- 장애 주입 조건(경과 시간 AND 관측 건수) 충족 시 `docker pause` / `unpause`
- 브로커 재준비 확인 후 Outbox drain 폴링
- 비교 리포트 생성

까지 처리합니다.

### 결과 확인

```
k6/consistency/artifacts/<run_id>-<mode>/
├── report.md                              # 요약 리포트
├── report.json                            # 정량 지표 (자동 판정 입력)
├── k6-summary.json                        # k6 성능 지표 원본
├── payment-events.log                     # Observer가 수신한 이벤트 로그
├── data.json                              # 사용된 fixture 데이터
├── expected-payment-ids.txt
├── observed-payment-ids-all.txt
├── observed-payment-ids-matched-all.txt
└── observed-payment-ids-matched-unique.txt

k6/consistency/artifacts/<run_all_id>-comparison/
└── README.md                              # Async vs Outbox 비교 + 자동 판정 결과
```

### 📊 검증용 SQL (참고)

```sql
-- Loss 계산 (payment DONE 대비 observer 수신 이벤트 수)
SELECT
  (SELECT COUNT(*)
     FROM unbox_payment.p_payment
    WHERE status = 'DONE'
      AND created_at BETWEEN :start AND :end) AS payment_done_count,

  (SELECT COUNT(*)
     FROM event_consume_log
    WHERE processed_at BETWEEN :start AND :end) AS events_consumed_count,

  ((SELECT COUNT(*) FROM unbox_payment.p_payment WHERE status='DONE' AND created_at BETWEEN :start AND :end)
   -
   (SELECT COUNT(*) FROM event_consume_log WHERE processed_at BETWEEN :start AND :end)) AS loss_count;

-- (Outbox) 멱등 검증: event_id 중복 수신/처리 여부
SELECT event_id, COUNT(*)
  FROM event_consume_log
 GROUP BY event_id
HAVING COUNT(*) > 1;
```

---

# 성능 및 안정성 측정 지표

- **처리량(TPS/RPS)**: Outbox 추가 write로 TPS가 유의미하게 감소하는지
- **지연(p95/p99)**: tail latency 상승폭이 허용 범위인지
- **실패율(2xx vs non-2xx)**: Async/Outbox의 진입점 안정성 비교
- **이벤트 유실(loss_rate)**: 핵심 도입 근거 (Async > 0, Outbox = 0)
- **복구시간(drain_time)**: 회복 탄력성 근거 (Outbox에서만 측정 의미 큼)

---

# 트러블 슈팅

- **Kafka 클라이언트 라이브러리가 JVM 메모리 버퍼에 레코드를 잠시 보관**

  "프로듀서가 들고 있다"는 뜻은, **네 코드가 아니라 Kafka 클라이언트 라이브러리가 JVM 메모리 버퍼에 레코드를 잠시 보관**한다는 의미입니다.

  - 네 코드: `send()`만 호출하고 즉시 리턴 (`PaymentDirectAsyncEventProducer.java`)
  - 설정: `acks: all`만 지정, `delivery.timeout` 미지정 (`application.yml`)

  그래서 "몇 초 들고 있냐?"

  - 따로 설정 안 했으면 Kafka Producer **기본값 기준**으로 동작합니다.
  - 핵심은 `delivery.timeout.ms`(기본 약 120초) 범위 내에서 재시도/재전송 시도.
  - 즉 브로커 다운 70초면, **복구 후 전송 성공해서 최종 유실 0**이 될 수 있습니다.

  정리

  - "내가 그런 로직 구현했어?" → 직접 구현한 건 아님. Kafka 클라이언트 기본 동작.
  - "왜 유실 안 생겨?" → 70초가 producer delivery timeout보다 짧아, 실패가 아니라 지연 전송으로 끝난 케이스.

  원하면 테스트 전용으로 Async만 `retries=0`, `delivery.timeout.ms=5000`으로 바꿔서 유실을 강제로 재현되게 만들 수 있습니다.

- **Async observer가 Kafka에 연결 실패 (유실률 100%)**

  **1. 현상 (Phenomenon)**

  - Observer(`kafka-console-consumer.sh`)가 Kafka에 TCP 연결 자체를 맺지 못한 채로 테스트가 진행됨.
  - 결과적으로 `eventsConsumedCount=0`, `topicLossRate=100%`로 집계.

  **2. 원인 (Cause)**

  - 이전 구현에서는 `sleep 2`만 대기한 후 k6를 시작했기 때문에, JVM 기동 지연 혹은 간헐적 네트워크 지연으로 Observer가 연결을 맺기 전에 장애 주입이 시작됨.
  - Observer 프로세스가 살아있어도 Kafka에 실제 연결이 된 상태가 아닐 수 있음(alive ≠ connected).

  **3. 해결 (Solution)**

  - Kafka pre-flight 체크: `wait_for_kafka_ready`로 브로커 응답 가능 상태를 먼저 확인.
  - Observer probe 기반 ready 확인: Observer 기동 후 실제로 probe 메시지를 토픽에 발행하고, Observer가 그 메시지를 수신하면 연결 완료로 판정. 최대 3회 재시도.
  - 장애 주입 조건 추가: `--min-observed-before-failure`(기본 30건) 이상 관측 후 장애 주입.

- **Kafka Broker 복구 시 Outbox 이벤트 유실**

  **1. 현상 (Phenomenon)**

  - Kafka 브로커가 `stop/start`로 재시작되면 컨테이너 IP가 변경됨.
  - Observer가 재연결 시 `auto.offset.reset=latest` 설정으로 인해 재연결 순간 이전에 발행된 이벤트를 누락.

  **2. 원인 (Cause)**

  - `docker stop/start`: 컨테이너가 새 IP를 부여받아 Observer의 Bootstrap 연결 정보가 무효화.
  - `auto.offset.reset=latest`: 재연결 시 "연결 시점 이후" 메시지만 읽기 시작해, Relay burst 발행된 이벤트를 skip.

  **3. 해결 (Solution)**

  - 장애 주입 방식을 `docker pause/unpause`(기본값)로 변경: 컨테이너 프로세스만 일시 정지하므로 IP 유지, 재연결 안정성 확보.
  - Consumer 재연결 설정 튜닝: `reconnect.backoff.ms=500`, `reconnect.backoff.max.ms=2000`, `metadata.max.age.ms=1000`.
  - 브로커 복구 후 `wait_for_kafka_ready`로 실제 응답 가능 상태 확인 후 drain 폴링 시작.

- **JWT 만료로 인한 API 실패 (403)**

  **1. 현상 (Phenomenon)**

  - async → outbox 순차 실행 시, async Observer retry로 인한 전체 시작 지연으로 JWT가 만료되어 임시 발급 토큰이 outbox 케이스 시작 때 이미 만료됨.

  **2. 해결 (Solution)**

  - 케이스별 fresh JWT 자동 생성: `AUTH_TOKEN_FROM_ARG=0`인 경우 각 케이스 실행 직전에 `generate-test-jwt.sh`로 JWT 재발급.
  - TTL 자동 계산: `TOKEN_EXPIRES_IN_SECONDS = TOTAL_SECONDS + DRAIN_TIMEOUT_SECONDS + 300`.

---

# DEV 환경 테스트 전략

## 환경 전제

| 항목 | 값 |
| --- | --- |
| Payment 서버 | `http://localhost:8085/payment` |
| Kafka 컨테이너 | `unbox-kafka` (Docker, `kafka:29092` internal) |
| Postgres 컨테이너 | `unbox-postgres` |
| Kafka observer 이미지 | `apache/kafka:3.7.0` (linux/amd64, ARM Mac은 에뮬레이션) |
| 실행 OS | macOS (Apple Silicon 포함) |

## 사전 준비

```bash
# 1. Docker 컨테이너 상태 확인
docker ps | grep -E 'unbox-kafka|unbox-postgres'

# 2. Payment 서버 Health 확인
curl -s http://localhost:8085/payment/actuator/health | jq .status

# 3. 의존 도구 확인
command -v k6 jq docker
```

## 실행 절차

```bash
# 전체 자동 실행 (권장)
cd /path/to/UNBOX-BE
./k6/consistency/scripts/run-all.sh

# 케이스 간 대기시간 조정이 필요한 경우
INTER_CASE_WAIT_SECONDS=90 ./k6/consistency/scripts/run-all.sh
```

### run-all.sh 자동 처리 단계

```
[async]
  1. setup-fixture.sh — 1200건 fixture 생성 (run_id 격리)
  2. generate-test-jwt.sh — fresh JWT 발급 (TTL 자동 계산)
  3. wait_for_kafka_ready — Kafka 브로커 응답 확인
  4. Observer 기동 + probe 연결 검증 (최대 3회)
  5. k6 실행 (10 TPS, 60s)
  6. failure-arm 대기 (≥20s elapsed AND ≥30 observed)
  7. docker pause unbox-kafka (20s)
  8. docker unpause unbox-kafka → wait_for_kafka_ready
  9. k6 종료 → report.json 생성

[70s 대기]

[outbox] — 동일 절차 반복
  + Outbox drain 폴링 (pendingFinal=0까지, 최대 180s)

[비교 리포트] — artifacts/{id}-comparison/README.md
```

## ARM Mac (Apple Silicon) 유의사항

`apache/kafka:3.7.0`은 `linux/amd64` 이미지이므로 Apple Silicon에서 에뮬레이션으로 실행됩니다.

- **JVM 기동 지연:** 에뮬레이션 오버헤드로 Observer 컨테이너의 JVM 시작이 느릴 수 있음 → probe 기반 ready 확인으로 해결
- **docker pause 권장:** `stop/start` 사용 시 컨테이너 재시작 + IP 변경 가능성 있음. `pause/unpause`는 프로세스만 일시 정지하여 IP 유지
- 성능 수치(p95/p99)는 native 환경 대비 높게 측정될 수 있으므로 **절대값보다 Async vs Outbox 상대 비교**에 의미를 둘 것

## 결과 해석 가이드

### 정상 PASS 패턴

```
Async  : loss_rate > 0  (브로커 pause 중 발행 실패 → 유실)
Outbox : loss_rate = 0  (DB 보관 → 재발행 → 전량 도달)
         drain_time: 10~30s 내외 (릴레이 재발행 완료)
         pendingFinal = 0
```

### 주요 FAIL 패턴 및 원인

| 증상 | 가능한 원인 | 확인 파일 |
| --- | --- | --- |
| Async loss_rate = 0 | `delivery.timeout.ms` 기본값(120s)으로 지연 전송 성공 | `payment-events.log` 이벤트 타임스탬프 확인 |
| Outbox loss_rate > 0 | Observer가 실제 연결 못 한 채 진행됨, 또는 replay offset 누락 | `payment-events.log` WARN 확인 |
| API non-2xx > 0 | JWT 만료, fixture 중복 사용, Payment 서버 에러 | `k6.log` error 라인 확인 |
| consumerDrainStatus = TIMEOUT | Outbox relay 미동작, DB 상태 이상 | `p_payment_outbox` 상태 직접 조회 |
| observer attempt 3회 모두 실패 | Kafka 브로커 미기동, Docker network 불일치 | `docker ps`, `docker network ls` |

### 진단 명령

```bash
# Observer 연결 이력 확인
grep -E 'WARN|ERROR|Processed' artifacts/<run_id>-async/payment-events.log

# Outbox 잔여 이벤트 확인
docker exec unbox-postgres psql -U postgres unbox_payment \
  -c "SELECT status, COUNT(*) FROM payment_outbox GROUP BY status;"

# k6 API 실패 원인 확인
grep 'confirm-fail' artifacts/<run_id>-async/k6.log
```

## 재현성 확보 체크리스트

실행 전 아래 조건이 충족되어야 동일 조건 비교가 보장됩니다.

- [ ] `unbox-kafka` 컨테이너 **정상 running** 상태
- [ ] `unbox-postgres` 컨테이너 **정상 running** 상태
- [ ] Payment 서버(`8085`) **정상 응답** 확인
- [ ] Async Producer 테스트 설정(`retries=0`, `delivery.timeout.ms=5000`) 적용 여부 확인
- [ ] 이전 테스트 잔여 fixture 간섭 없음 (run_id 격리로 자동 처리)
- [ ] `PAYMENT_TEST_ASYNC_PRODUCER_RETRIES=0`, `PAYMENT_TEST_ASYNC_PRODUCER_DELIVERY_TIMEOUT_MS=5000` 환경변수 서버에 적용 확인
