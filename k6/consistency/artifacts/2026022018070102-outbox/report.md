# 결제 정합성 테스트 리포트

- 실행 ID: 2026022018070102
- 모드: outbox
- 결제 API: http://localhost:8085/payment
- 요청 타임아웃: 30s
- 브로커 중단 시각(epoch): 1771578609
- 브로커 복구 시각(epoch): 1771578639

| 지표 | 값 |
| --- | ---: |
| Fixture 개수 | 1200 |
| Confirm 요청 수 | 550 |
| Confirm 성공 수(2xx) | 550 |
| Confirm 실패 수(non-2xx) | 0 |
| API 성공률(%) | 100.00 |
| 평균 RPS | 9.158703479281547 |
| p95 지연(ms) | 2319.165099999999 |
| p99 지연(ms) | 0 |
| Payment DONE 수 | 550 |
| 소비 집계 소스 | topic_observer |
| 이벤트 소비 완료 수(토픽 기준) | 550 |
| 토픽 관측 수 | 550 |
| 토픽 기준 유실 건수 | 0 |
| 토픽 기준 유실률(%) | 0.00 |
| 중복 paymentId 수 | 0 |
| Order Consumer 처리 수(진단) | 550 |
| Order Consumer 중복 수(진단) | 0 |
| 데이터셋 재사용 수 | 0 |
| Outbox PUBLISHED 수 | 550 |
| Outbox 잔여(PENDING/PROCESSING) | 0 |
| 복구 시간(s) | 12 |
| 컨슈머 드레인 상태 | DONE |

## 산출물 경로
- data: /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026022018070102-outbox/data.json
- observer log: /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026022018070102-outbox/payment-events.log
- k6 log: /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026022018070102-outbox/k6.log
- summary json: /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026022018070102-outbox/k6-summary.json
- report json: /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026022018070102-outbox/report.json
