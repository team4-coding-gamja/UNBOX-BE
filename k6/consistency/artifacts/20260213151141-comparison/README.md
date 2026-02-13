아니 ㅈ# Async vs Outbox 비교 리포트

- 비교 실행 ID: 20260213151141
- Async run_id: 2026021315114101
- Outbox run_id: 2026021315114102

| 비교 항목 | Async | Outbox |
| --- | ---: | ---: |
| Confirm 성공 수(2xx) | 537 | 550 |
| Confirm 실패 수(non-2xx) | 0 | 0 |
| API 성공률(%) | 100.00 | 100.00 |
| Payment DONE 수 | 537 | 550 |
| 이벤트 소비 완료 수 | 171 | 550 |
| 유실 건수 | 366 | 0 |
| 유실률(%) | 68.16 | 0.00 |
| 평균 RPS | 8.94958802063145 | 9.159670967964884 |
| p95 지연(ms) | 6583.814799999999 | 1786.429349999999 |
| 복구 시간(s) | N/A | 22 |
| Outbox Published 수 | N/A | 550 |
| Outbox Pending 최종 | N/A | 0 |
| 컨슈머 드레인 상태 | N/A | DONE |

## 판정 가이드
- 설계 목표:
  - Async: 유실률 > 0
  - Outbox: 유실률 = 0, Outbox Pending 최종 = 0
- 현재 결과가 목표와 다르면 아래 파일의 원인 지표를 먼저 확인하세요.
  - /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026021315114102-outbox/report.json
  - /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026021315114102-outbox/k6.log
  - /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026021315114102-outbox/payment-events.log

## 상세 리포트
- Async: /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026021315114101-async/report.md
- Outbox: /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026021315114102-outbox/report.md
