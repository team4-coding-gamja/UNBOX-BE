# 결제 정합성 테스트 리포트

- 실행 ID: 2026021315114101
- 모드: async
- 결제 API: http://localhost:8085/payment
- 요청 타임아웃: 30s
- 브로커 중단 시각(epoch): 1770963124
- 브로커 복구 시각(epoch): 1770963196

| 지표 | 값 |
| --- | ---: |
| Fixture 개수 | 1200 |
| Confirm 요청 수 | 537 |
| Confirm 성공 수(2xx) | 537 |
| Confirm 실패 수(non-2xx) | 0 |
| API 성공률(%) | 100.00 |
| 평균 RPS | 8.94958802063145 |
| p95 지연(ms) | 6583.814799999999 |
| p99 지연(ms) | 0 |
| Payment DONE 수 | 537 |
| 소비 집계 소스 | order_consumer_received_log |
| 이벤트 소비 완료 수 | 171 |
| 토픽 관측 수(보조) | 186 |
| 유실 건수 | 366 |
| 유실률(%) | 68.16 |
| 중복 paymentId 수 | 0 |
| 데이터셋 재사용 수 | 0 |
| Outbox PUBLISHED 수 | N/A |
| Outbox 잔여(PENDING/PROCESSING) | N/A |
| 복구 시간(s) | N/A |
| 컨슈머 드레인 상태 | SKIP_ASYNC_MODE |

## 산출물 경로
- data: /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026021315114101-async/data.json
- observer log: /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026021315114101-async/payment-events.log
- k6 log: /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026021315114101-async/k6.log
- summary json: /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026021315114101-async/k6-summary.json
- report json: /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026021315114101-async/report.json
