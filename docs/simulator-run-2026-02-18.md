# Simulator Run Summary (2026-02-18)

## 목적
- 실사용자가 없는 상태에서 분석 가능한 이벤트 로그를 생성하고, 퍼널/전환/실험 분석을 가능하게 만드는 기반 데이터 확보.

## 실행 환경
- 서비스: `unbox-ai` (FastAPI)
- 엔드포인트: `POST /api/v1/simulator/run`
- 적재 대상: Postgres `analytics.events`

## 입력 파라미터
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

## 실행 결과 (요약)
- 사용자 수: 2,000
- 세션 수: 16,808
- 총 이벤트 수: 26,674
- 이벤트 분포:
  - `view_item`: 16,808
  - `add_to_wishlist`: 3,955
  - `place_bid`: 2,438
  - `checkout_start`: 2,819
  - `payment_completed`: 654

## 해석 포인트
- 세션 수가 `view_item`과 동일한 이유:
  - 시뮬레이터가 세션당 최소 1회 `view_item` 이벤트를 생성하도록 설계되어 있음.
- 나머지 이벤트는 페르소나 기반 확률로 생성됨.
  - 예: `price_sensitive`는 `bid/checkout` 비중이 높음.

## 핵심 전환 지표 (이번 샘플 기준)
- `view_item → add_to_wishlist`: **23.53%**
- `view_item → place_bid`: **14.50%**
- `view_item → checkout_start`: **16.77%**
- `view_item → payment_completed` (전체 CVR): **3.89%**
- `checkout_start → payment_completed`: **23.20%**

## 데이터 구조 (분석 활용 포인트)
- 이벤트 공통 컬럼:
  - `event_name`, `user_id`, `session_id`, `occurred_at`, `source_service`
  - `experiment_id`, `variant_id`
  - `properties` (JSONB)
- `properties`에는 다음 정보가 포함됨:
  - `product_id`, `category`, `price`, `device`, `referrer`, `persona`

## 확인 쿼리
```sql
SELECT event_name, COUNT(*)
FROM analytics.events
GROUP BY event_name
ORDER BY COUNT(*) DESC;
```

## 다음 단계 제안
1. 퍼널/리텐션/세그먼트 분석 SQL 세트 작성
2. A/B variant별 전환율 비교 쿼리 추가
3. `persona`, `device`, `referrer` 기준 세그먼트 분석
