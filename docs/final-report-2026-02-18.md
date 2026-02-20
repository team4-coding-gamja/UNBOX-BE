# Final Report — Resale Analytics & A/B Simulation (2026-02-18)

## 1. 범위
- 리셀 플랫폼 도메인에 맞춘 **이벤트 스키마/시뮬레이터/분석 파이프라인** 구축 및 검증
- 실사용자 데이터 부재 상황에서 **분석 가능한 데이터 생성**과 **A/B 구조 검증**
- 결과는 **시뮬레이션 데이터 기반**이며, 실데이터 검정은 미수행

## 2. 진행 과정 요약
- 이벤트 스키마 설계: `analytics.events` (공통 속성 + JSONB properties)
- 시뮬레이터 구현: 구매/판매 플로우 분리, 리셀 이벤트 추가
- 시뮬레이션 데이터 적재 및 SQL 분석
- A/B 실험 구조 적용(variant_id 태깅) 및 처치 효과 반영

## 3. 실험/테스트 타임라인
1. **기본 시뮬레이터** 구축 및 작은 샘플 실행(검증용)
2. **리셀 도메인 확장**
   - 구매자: `view_item → place_bid → match_bid → checkout_start → payment_completed`
   - 판매자: `list_item → set_ask → match_bid → sell_completed`
3. **매칭 이후 이탈/결제 실패 분리**
   - `checkout_abandoned` 이벤트 추가
   - `payment_failed`는 기술 실패로 제한
4. **30k users / 14일 시뮬레이션** 실행 및 분석
5. **A/B 처치 실험**: Variant B에 `match_to_checkout_p +10%p` 적용

## 4. 최종 데이터 요약 (30k users, v2)
- 총 이벤트: 1,111,427
- 핵심 이벤트:
  - `view_item`: 326,664
  - `place_bid`: 59,777
  - `match_bid`: 157,665
  - `checkout_start`: 15,755
  - `checkout_abandoned`: 11,307
  - `payment_completed`: 14,530
  - `payment_failed`: 1,225
  - `list_item`: 175,773
  - `sell_completed`: 116,717

## 5. 주요 분석 결과 (보정 포함)
### 구매자 퍼널 (trade_side = buy)
- `bid_to_match_pct`: **45.27%**
- `match_to_checkout_pct`: **58.22%**
- `match_to_abandon_pct`: **41.78%**
- `checkout_to_payment_pct`: **92.22%**
- `checkout_to_fail_pct`: **7.78%**

### 판매자 퍼널
- `set_ask → match_bid`: **89.70%**
- `match_bid → sell_completed`: **74.03%**
- `list_item → sell_completed`: **66.40%**

### 페르소나 전환(구매자)
- `price_sensitive`: **79.58%**
- `impulse`: **49.53%**
- `browser`: **22.83%**

## 6. A/B 테스트 결과
### 6.1 A/B 구조 검증(처치 없음)
- A/B 간 차이가 거의 없음을 확인
- 목적: 분할·태깅·집계 파이프라인 정상 동작 확인

### 6.2 처치 실험 (Variant B: match_to_checkout_p +10%p)
- `match_to_checkout_pct`: **A 57.63% → B 68.45% (+10.82%p)**
- `match_to_abandon_pct`: **A 42.37% → B 31.55% (-10.82%p)**
- `checkout_to_payment_pct`: **A 92.09% → B 92.62% (+0.53%p)**
- `event CVR`: **A 4.44% → B 5.31% (+0.87%p)**

## 7. 해석 요약
- 매칭 이후 결제 진입 이탈이 핵심 병목으로 관측됨.
- 결제 실패는 기술 실패 수준으로 축소되어, “결제 진입”이 주요 개선 포인트.
- A/B 처치 실험에서 결제 진입 개선 효과가 실제 지표 상승으로 연결됨.

## 8. 한계 및 주의사항
- 모든 결과는 **시뮬레이션 데이터 기반**이며 실데이터 검정은 수행되지 않음.
- 실제 유저 데이터 확보 시 통계적 유의성 검정(Z-test/χ² 등) 필요.

## 9. 다음 단계 제안
1. **실서비스 이벤트 연동**: Kafka Consumer 또는 이벤트 수집 API 추가
2. **실데이터 기반 A/B 실험**: 유의미성 검정 포함
3. **결제 진입 개선 실험**:
   - 원클릭 결제, 결제 UX 단축, 리마인드 메시지
4. **세그먼트 분석 고도화**:
   - spread, match_latency, device, category별 전환 비교
