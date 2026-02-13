# 결제 Confirm 정합성 테스트 (Async vs Outbox)

이 디렉토리는 **서비스 코드 동작을 바꾸지 않는 테스트 전용 코드**만 포함합니다.

- 기존 서비스 엔드포인트 그대로 사용: `POST /payment/api/payment/confirm`
- 모드 전환: `X-Test-Mode: async | outbox`
- 장애 주입: Kafka broker `stop/start`
- 결과 산출: `loss_rate`, `drain_time`, `TPS/p95/p99`

## 구성

- `confirm-consistency.js`
  - Constant Load 성격의 `ramping-arrival-rate` 부하 생성
  - Confirm 성공/실패/TPS/지연 지표 수집
- `scripts/setup-fixture.sh`
  - 테스트용 Order/Payment fixture 대량 생성
  - run_id별 `data.json` 생성 (고유 paymentId/paymentKey)
- `scripts/generate-test-jwt.sh`
  - `.env`의 `SPRING_JWT_SECRET` 기반 테스트 JWT 생성
- `scripts/run-case.sh`
  - 단일 케이스 실행(async 또는 outbox)
  - Kafka 장애 자동 주입(20s down, 300s outage 기본값)
  - 기본: `consumer_received_log`(order-group) 기준 집계
  - 보조: Kafka observer 소비 로그 집계 + 리포트 생성
- `scripts/run-all.sh`
  - async -> outbox 순차 실행
  - 실행 후 비교 요약 리포트 자동 생성:
    - `k6/consistency/artifacts/<run_all_id>-comparison/README.md`
- `sql/verify-loss.sql`
  - run_id 기반 DB 검증 쿼리

## 실행

### 1) 단일 케이스 실행

```bash
./k6/consistency/scripts/run-case.sh --mode async
./k6/consistency/scripts/run-case.sh --mode outbox
```

### 2) 두 케이스 연속 실행

```bash
./k6/consistency/scripts/run-all.sh
```

### 3) 주요 옵션 예시

```bash
./k6/consistency/scripts/run-case.sh \
  --mode outbox \
  --rate 10 \
  --request-timeout 30s \
  --warmup-seconds 10 \
  --steady-seconds 40 \
  --cooldown-seconds 10 \
  --failure-at-seconds 20 \
  --broker-down-seconds 300 \
  --fixture-count 1200
```

## 결과물

각 실행마다 아래 경로에 아티팩트가 생성됩니다.

- `k6/consistency/artifacts/<run_id>-<mode>/report.md`
- `k6/consistency/artifacts/<run_id>-<mode>/report.json`
- `k6/consistency/artifacts/<run_id>-<mode>/k6-summary.json`
- `k6/consistency/artifacts/<run_id>-<mode>/payment-events.log`

`run-all.sh` 실행 시에는 비교용 파일도 추가 생성됩니다.

- `k6/consistency/artifacts/<run_all_id>-comparison/README.md`

기본적으로 `run-all.sh`는 async와 outbox 사이에 `70초` 대기(`INTER_CASE_WAIT_SECONDS`)를 두어
이전 케이스의 잔여 지연이 다음 케이스 지표를 오염시키는 문제를 줄입니다.

## 결과 비교 방법 (권장)

1. 비교 리포트 확인
   - `k6/consistency/artifacts/<run_all_id>-comparison/README.md`
2. 자동 판정 확인
   - `최종 판정: PASS`면 설계 목표를 충족한 실행
3. 세부 리포트 교차 검증
   - `k6/consistency/artifacts/<run_id>-async/report.json`
   - `k6/consistency/artifacts/<run_id>-outbox/report.json`

### 유효한 실행 체크리스트

- `Confirm 실패 수(non-2xx)`가 async/outbox 모두 0
- Async `loss_rate > 0`
- Outbox `loss_rate = 0`
- Outbox `pending_final = 0`
- Outbox `consumerDrainStatus = DONE`

위 조건 중 하나라도 깨지면 해당 실행은 결론용 데이터로 쓰지 않는 것을 권장합니다.

## 판정 기준

- `loss_count = payment_done_count - events_consumed_count`
- `loss_rate = loss_count / payment_done_count * 100`
- 기대:
  - Async: `loss_rate > 0`
  - Outbox: `loss_rate = 0`, `drain_time` 유의미

## 설계 대비 구현 매핑

- 진입점 API
  - 설계: Payment Confirm
  - 구현: `POST /payment/api/payment/confirm` (Docker 로컬 기본 URL 기준)
- 모드 비교
  - 설계: `Simple Async vs Outbox`
  - 구현: 헤더 `X-Test-Mode=async|outbox` 로 분기
- 부하 모델
  - 설계: 일정 부하 + 10s/40s/10s
  - 구현: `ramping-arrival-rate` 로 `RATE`를 고정 유지하고 `10s/40s/10s` 스테이지 적용
- 장애 주입
  - 설계: 테스트 20초 시점 Kafka down 300초
  - 구현: `run-case.sh` 기본값 `--failure-at-seconds 20`, `--broker-down-seconds 300`
- 유실 판정
  - 설계: `payment_done_count` vs `events_consumed_count`
  - 구현: 
    - `payment_done_count`: `unbox_payment.p_payment` 집계
    - `events_consumed_count`: `unbox_order.consumer_received_log`의 `payment_id` 집계
    - `topic_observed_count`: observer consumer가 `payment-events`에서 수신한 `paymentId` 집계(보조)
- 복구 판정(Outbox)
  - 설계: `PENDING -> PUBLISHED` drain 시간 측정
  - 구현: `payment_outbox`의 `PENDING/PROCESSING`이 0이 될 때까지 폴링 후 `drain_time` 계산

## 추가된 통제/전제 조건

- 인증 전제
  - Confirm API는 JWT 인증 필요
  - 테스트는 `generate-test-jwt.sh`로 `userId=1` 토큰 자동 생성
- 테스트 데이터 전제
  - READY 결제를 재사용하면 중복 confirm으로 4xx가 발생할 수 있어 비교가 오염됨
  - `setup-fixture.sh`가 run_id별 대량 fixture를 생성하여 이 문제를 제거
- 환경 명칭 통일
  - Kafka 컨테이너명: `unbox-kafka`
  - Payment base URL: `http://localhost:8085/payment`

## 해석 시 주의사항

- 현재 `events_consumed_count`는 `order-group` 컨슈머 처리 로그(`consumer_received_log`) 기준입니다.
- `topic_observed_count`는 토픽 관측치(보조)이며 observer 재연결 타이밍 영향이 있을 수 있습니다.
- 따라서 본 테스트 결론은 아래 2축으로 읽는 것이 안전합니다.
  - `events_consumed_count`: 서비스 컨슈머 처리 관점
  - `topic_observed_count`: 토픽 도달 관점
- Async 리포트의 `consumerDrainStatus=SKIP_ASYNC_MODE`는 스냅샷 집계임을 의미합니다.
  - 이 경우 실행 직후와 최종(수 분 후) 값이 달라질 수 있으므로 결론 전 DB 재검증이 필요합니다.
- 현재는 `unbox-order`에서 테스트 트래픽(`paymentKey=test_success_*`)에 한해 `consumer_received_log`를 적재합니다.
- 다른 컨슈머(예: trade)까지 동일한 E2E 기준으로 확대하려면 아래 중 1개를 추가하세요.
  - 서비스별 `consumer_received_log` 적재
  - Prometheus `events_consumed_total` 카운터
  - 컨슈머 성공 로그 기반 집계 파이프라인

### 최종 DB 재검증 (선택)

```bash
psql -U <user> -d unbox_payment -v run_id='<async_run_id>' -f k6/consistency/sql/verify-loss.sql
psql -U <user> -d unbox_payment -v run_id='<outbox_run_id>' -f k6/consistency/sql/verify-loss.sql
```

## 이벤트 포맷 통제

- Async/Outbox 모두 `EventEnvelope` JSON 형식으로 발행되도록 통일되어 있습니다.
- 따라서 본 테스트에서 파싱 에러는 구조 차이 요인이 아니라 실제 코드/데이터 문제로 해석할 수 있습니다.
- 검증 방법:
  - `unbox-order` 로그에서 `Failed to parse event JSON` 발생 여부 확인
  - `k6/consistency/artifacts/<run_id>-<mode>/payment-events.log`에서 payload 샘플 확인

## 참고

- Kafka 컨테이너 기본값: `unbox-kafka`
- Postgres 컨테이너 기본값: `unbox-postgres`
- Payment URL 기본값: `http://localhost:8085/payment`
- Observer CLI 이미지 기본값: `apache/kafka:3.7.0`
