# 결제 정합성 테스트 리포트

- 실행 ID: 2026022018070101
- 모드: async
- 결제 API: http://localhost:8085/payment
- 요청 타임아웃: 30s
- 브로커 중단 시각(epoch): 1771578456
- 브로커 복구 시각(epoch): 1771578484

| 지표 | 값 |
| --- | ---: |
| Fixture 개수 | 1200 |
| Confirm 요청 수 | 503 |
| Confirm 성공 수(2xx) | 503 |
| Confirm 실패 수(non-2xx) | 0 |
| API 성공률(%) | 100.00 |
| 평균 RPS | 8.381610493296437 |
| p95 지연(ms) | 13030.9147 |
| p99 지연(ms) | 0 |
| Payment DONE 수 | 503 |
| 소비 집계 소스 | topic_observer |
| 이벤트 소비 완료 수(토픽 기준) | 295 |
| 토픽 관측 수 | 295 |
| 토픽 기준 유실 건수 | 208 |
| 토픽 기준 유실률(%) | 41.35 |
| 중복 paymentId 수 | 0 |
| Order Consumer 처리 수(진단) | 295 |
| Order Consumer 중복 수(진단) | 0 |
| 데이터셋 재사용 수 | 0 |
| Outbox PUBLISHED 수 | N/A |
| Outbox 잔여(PENDING/PROCESSING) | N/A |
| 복구 시간(s) | N/A |
| 컨슈머 드레인 상태 | SKIP_ASYNC_MODE |

## 산출물 경로
- data: /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026022018070101-async/data.json
- observer log: /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026022018070101-async/payment-events.log
- k6 log: /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026022018070101-async/k6.log
- summary json: /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026022018070101-async/k6-summary.json
- report json: /Users/jang/Desktop/UNBOX/UNBOX-BE/k6/consistency/artifacts/2026022018070101-async/report.json
