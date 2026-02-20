# Async vs Outbox 비교 리포트

- 비교 실행 ID: 20260220180701
- Async run_id: 2026022018070101
- Outbox run_id: 2026022018070102

| 비교 항목 | Async | Outbox |
| --- | ---: | ---: |
| Confirm 성공 수(2xx) | 503 | 550 |
| Confirm 실패 수(non-2xx) | 0 | 0 |
| API 성공률(%) | 100.00 | 100.00 |
| Payment DONE 수 | 503 | 550 |
| 토픽 관측 이벤트 수 | 295 | 550 |
| 토픽 기준 유실 건수 | 208 | 0 |
| 토픽 기준 유실률(%) | 41.35 | 0.00 |
| 평균 RPS | 8.381610493296437 | 9.158703479281547 |
| p95 지연(ms) | 13030.9147 | 2319.165099999999 |
| 복구 시간(s) | N/A | 12 |
| Outbox Published 수 | N/A | 550 |
| Outbox Pending 최종 | N/A | 0 |
| 컨슈머 드레인 상태 | SKIP_ASYNC_MODE | DONE |

## 자동 판정
| 판정 항목 | 기준 | 결과 |
| --- | --- | --- |
| Async 유실 발생 | Async topic_loss_rate > 0 | PASS |
| Outbox 무유실 | Outbox topic_loss_rate = 0 | PASS |
| Outbox 잔여 없음 | Outbox pending_final = 0 | PASS |
| Outbox 소비 드레인 완료 | Outbox consumerDrainStatus = DONE | PASS |
| API 안정성 | async/outbox non-2xx = 0 | PASS |

**최종 판정: PASS**

## 판정 가이드
- 설계 목표:
  - Async: 토픽 기준 유실률 > 0
  - Outbox: 토픽 기준 유실률 = 0, Outbox Pending 최종 = 0
- 현재 결과가 목표와 다르면 아래 파일의 원인 지표를 먼저 확인하세요.
  - /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026022018070101-async/report.json
  - /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026022018070102-outbox/report.json
  - /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026022018070101-async/k6.log
  - /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026022018070102-outbox/k6.log
  - /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026022018070101-async/payment-events.log
  - /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026022018070102-outbox/payment-events.log

## 상세 리포트
- Async: /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026022018070101-async/report.md
- Outbox: /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026022018070102-outbox/report.md
