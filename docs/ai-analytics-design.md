# AI 서비스 고도화 설계서 (크롤링 제외 / 데이터 분석·AI 연동 중심)

## 1. 개요
- **프로젝트명**: UNBOX
- **목표**: 유저 행동 데이터 기반 전환율(CVR) 개선 및 A/B 테스트 설계·검증
- **핵심 방향**: 외부 크롤링 없이 내부 이벤트/리뷰 데이터 + AI API로 인사이트 생성
- **포지션 적합성**: 퍼널/리텐션/실험 설계/SQL 분석 역량을 실증하는 MVP

## 2. 기존 서비스 연계 포인트
- `unbox_user`: 회원가입/로그인/세션 생성 이벤트 로깅
- `unbox_product`: 상품조회/찜/리뷰 작성 이벤트 로깅, AI 리뷰 요약 API 연계
- `unbox_trade`: 입찰/거래 상태 이벤트 로깅
- `unbox_order`: 주문 생성/결제 진행 이벤트 로깅
- `unbox_payment`: 결제 완료/실패 이벤트 로깅
- `unbox_common`: Kafka/Redis 공통 설정 재사용, 이벤트 발행 규격 통일

## 3. 데이터 수집·적재 아키텍처
- 이벤트 발생 → Kafka 발행 → Analytics Consumer 수집 → Postgres `analytics` 스키마 적재
- 로컬 기준: 기존 `docker-compose.yml`의 Kafka/Postgres 사용

```mermaid
graph LR
  U[unbox_user] -->|events| K[Kafka]
  P[unbox_product] -->|events| K
  T[unbox_trade] -->|events| K
  O[unbox_order] -->|events| K
  Pay[unbox_payment] -->|events| K
  K --> C[Analytics Consumer (unbox-ai 또는 별도 모듈)]
  C --> PG[(Postgres analytics schema)]
```

## 4. 이벤트 스키마 (공통 포맷)
- **테이블**: `analytics.events`
- **필수 컬럼**
  - `event_name` (예: `view_item`, `add_to_wishlist`, `checkout_start`, `payment_completed`)
  - `user_id`, `session_id`
  - `occurred_at`
  - `properties` (JSONB, 도메인별 속성)
  - `source_service`
  - `experiment_id`, `variant_id` (A/B 테스트용)

```sql
CREATE SCHEMA IF NOT EXISTS analytics;

CREATE TABLE IF NOT EXISTS analytics.events (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_name    TEXT NOT NULL,
  user_id       UUID NULL,
  session_id    TEXT NULL,
  source_service TEXT NOT NULL,
  occurred_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  properties    JSONB NOT NULL DEFAULT '{}'::jsonb,
  experiment_id TEXT NULL,
  variant_id    TEXT NULL
);
```

## 5. A/B 테스트 프레임워크 (간단형)
- 유저 ID 해시 기반 트래픽 분할 (Control/Variant)
- 실험 설정은 Redis 캐시 + RDB 원본 저장
- 모든 이벤트에 `experiment_id`, `variant_id` 자동 태깅

```sql
CREATE TABLE IF NOT EXISTS analytics.experiments (
  experiment_id TEXT PRIMARY KEY,
  name          TEXT NOT NULL,
  start_at      TIMESTAMPTZ NOT NULL,
  end_at        TIMESTAMPTZ NULL,
  status        TEXT NOT NULL,
  variants      JSONB NOT NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

## 6. AI 기능 (분석 연동)
### 6.1 AI 실험 리포트 자동 생성
- 입력: `analytics.events` 기반 집계 결과
- 출력: 실험 성과 요약 + 권고사항 (승인/보류)

### 6.2 리뷰 감성/키워드 ↔ 전환율 연결
- 입력: 리뷰 텍스트 → AI 요약/키워드 추출
- 조인: 상품별 전환율/환불율과 연결
- 출력: “특정 키워드가 전환에 미친 영향” 인사이트

### 6.3 AI 인사이트 요약기 (Daily/Weekly)
- 입력: 퍼널, 리텐션, 세그먼트 지표
- 출력: 핵심 변화와 액션 제안

## 7. 핵심 분석 지표
- 퍼널 전환율: `view_item → add_to_wishlist → checkout_start → payment_completed`
- 리텐션: D1/D7 재방문율
- 실험 성과: CTR, CVR, 평균 구매 금액, 구매까지 소요시간

## 8. 실행 계획 (하나씩 진행)
**Step 1** 이벤트 스키마 생성 + 로깅 삽입
- `analytics.events` 생성
- 서비스별 이벤트 발행 최소 3종 적용

**Step 1-1 (실사용자 없음 대응)** 이벤트 시뮬레이터로 데이터 생성
- 페르소나 기반 세션/퍼널 이벤트 자동 생성
- A/B 실험 분기 태깅 포함
- 분석용 최소 1만~10만 건 적재

**Step 2** SQL 분석 쿼리 세트 작성
- 퍼널/리텐션/전환율 쿼리 작성
- 결과를 문서화

**Step 3** A/B 테스트 MVP
- 해시 분할 로직
- 이벤트 태깅 자동화

**Step 4** AI 인사이트 연결
- 리뷰 감성/키워드 추출
- 실험 리포트 자동 생성

## 9. 산출물 (포트폴리오용)
- 설계서 (본 문서)
- 이벤트 스키마 + SQL 쿼리 파일
- A/B 테스트 리포트 템플릿
- AI 인사이트 샘플 리포트

## 10. 로컬 실행/검증 (Simulator)
- **엔드포인트**: `POST /api/v1/simulator/run`
- **요청 예시**

```json
{
  "days": 7,
  "users": 2000,
  "avg_sessions_per_user": 1.2,
  "seed": 42,
  "experiment_id": "exp_cvr_home_v1",
  "variant_splits": {"A": 0.5, "B": 0.5}
}
```

- **검증 쿼리**

```sql
SELECT event_name, COUNT(*) 
FROM analytics.events
GROUP BY event_name
ORDER BY COUNT(*) DESC;
```
