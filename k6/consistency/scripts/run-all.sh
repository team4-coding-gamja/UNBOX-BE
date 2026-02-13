#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
RUN_ALL_ID="${RUN_ALL_ID:-$(date +%Y%m%d%H%M%S)}"
INTER_CASE_WAIT_SECONDS="${INTER_CASE_WAIT_SECONDS:-70}"

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required." >&2
  exit 1
fi

ASYNC_RUN_ID="${RUN_ALL_ID}01"
OUTBOX_RUN_ID="${RUN_ALL_ID}02"

ASYNC_DIR="${ROOT_DIR}/k6/consistency/artifacts/${ASYNC_RUN_ID}-async"
OUTBOX_DIR="${ROOT_DIR}/k6/consistency/artifacts/${OUTBOX_RUN_ID}-outbox"
ASYNC_REPORT_JSON="${ASYNC_DIR}/report.json"
OUTBOX_REPORT_JSON="${OUTBOX_DIR}/report.json"
COMPARE_DIR="${ROOT_DIR}/k6/consistency/artifacts/${RUN_ALL_ID}-comparison"
COMPARE_README="${COMPARE_DIR}/README.md"

echo "[run-all] running async case..."
"${SCRIPT_DIR}/run-case.sh" --mode async --run-id "${ASYNC_RUN_ID}" "$@"

if [[ "${INTER_CASE_WAIT_SECONDS}" -gt 0 ]]; then
  echo "[run-all] waiting ${INTER_CASE_WAIT_SECONDS}s before outbox case..."
  sleep "${INTER_CASE_WAIT_SECONDS}"
fi

echo "[run-all] running outbox case..."
"${SCRIPT_DIR}/run-case.sh" --mode outbox --run-id "${OUTBOX_RUN_ID}" "$@"

mkdir -p "${COMPARE_DIR}"

async_confirm_success="$(jq -r '.load.confirmSuccess // 0' "${ASYNC_REPORT_JSON}")"
async_confirm_non2xx="$(jq -r '.load.confirmNon2xx // 0' "${ASYNC_REPORT_JSON}")"
async_payment_done="$(jq -r '.consistency.paymentDoneCount // 0' "${ASYNC_REPORT_JSON}")"
async_events_consumed="$(jq -r '.consistency.eventsConsumedCount // 0' "${ASYNC_REPORT_JSON}")"
async_loss_count="$(jq -r '.consistency.lossCount // 0' "${ASYNC_REPORT_JSON}")"
async_loss_rate="$(jq -r '.consistency.lossRate // 0' "${ASYNC_REPORT_JSON}")"
async_avg_rps="$(jq -r '.performance.avgRps // 0' "${ASYNC_REPORT_JSON}")"
async_p95_ms="$(jq -r '.performance.p95Ms // 0' "${ASYNC_REPORT_JSON}")"
async_api_success_rate="$(jq -r '.performance.apiSuccessRate // 0' "${ASYNC_REPORT_JSON}")"

outbox_confirm_success="$(jq -r '.load.confirmSuccess // 0' "${OUTBOX_REPORT_JSON}")"
outbox_confirm_non2xx="$(jq -r '.load.confirmNon2xx // 0' "${OUTBOX_REPORT_JSON}")"
outbox_payment_done="$(jq -r '.consistency.paymentDoneCount // 0' "${OUTBOX_REPORT_JSON}")"
outbox_events_consumed="$(jq -r '.consistency.eventsConsumedCount // 0' "${OUTBOX_REPORT_JSON}")"
outbox_loss_count="$(jq -r '.consistency.lossCount // 0' "${OUTBOX_REPORT_JSON}")"
outbox_loss_rate="$(jq -r '.consistency.lossRate // 0' "${OUTBOX_REPORT_JSON}")"
outbox_avg_rps="$(jq -r '.performance.avgRps // 0' "${OUTBOX_REPORT_JSON}")"
outbox_p95_ms="$(jq -r '.performance.p95Ms // 0' "${OUTBOX_REPORT_JSON}")"
outbox_api_success_rate="$(jq -r '.performance.apiSuccessRate // 0' "${OUTBOX_REPORT_JSON}")"
outbox_drain_time="$(jq -r '.timing.drainTimeSeconds // "N/A"' "${OUTBOX_REPORT_JSON}")"
outbox_pending_final="$(jq -r '.outbox.pendingFinal // "N/A"' "${OUTBOX_REPORT_JSON}")"
outbox_published_count="$(jq -r '.outbox.publishedCount // "N/A"' "${OUTBOX_REPORT_JSON}")"
outbox_consumer_drain_status="$(jq -r '.timing.consumerDrainStatus // "N/A"' "${OUTBOX_REPORT_JSON}")"

cat > "${COMPARE_README}" <<EOF
# Async vs Outbox 비교 리포트

- 비교 실행 ID: ${RUN_ALL_ID}
- Async run_id: ${ASYNC_RUN_ID}
- Outbox run_id: ${OUTBOX_RUN_ID}

| 비교 항목 | Async | Outbox |
| --- | ---: | ---: |
| Confirm 성공 수(2xx) | ${async_confirm_success} | ${outbox_confirm_success} |
| Confirm 실패 수(non-2xx) | ${async_confirm_non2xx} | ${outbox_confirm_non2xx} |
| API 성공률(%) | ${async_api_success_rate} | ${outbox_api_success_rate} |
| Payment DONE 수 | ${async_payment_done} | ${outbox_payment_done} |
| 이벤트 소비 완료 수 | ${async_events_consumed} | ${outbox_events_consumed} |
| 유실 건수 | ${async_loss_count} | ${outbox_loss_count} |
| 유실률(%) | ${async_loss_rate} | ${outbox_loss_rate} |
| 평균 RPS | ${async_avg_rps} | ${outbox_avg_rps} |
| p95 지연(ms) | ${async_p95_ms} | ${outbox_p95_ms} |
| 복구 시간(s) | N/A | ${outbox_drain_time} |
| Outbox Published 수 | N/A | ${outbox_published_count} |
| Outbox Pending 최종 | N/A | ${outbox_pending_final} |
| 컨슈머 드레인 상태 | N/A | ${outbox_consumer_drain_status} |

## 판정 가이드
- 설계 목표:
  - Async: 유실률 > 0
  - Outbox: 유실률 = 0, Outbox Pending 최종 = 0
- 현재 결과가 목표와 다르면 아래 파일의 원인 지표를 먼저 확인하세요.
  - ${OUTBOX_REPORT_JSON}
  - ${OUTBOX_DIR}/k6.log
  - ${OUTBOX_DIR}/payment-events.log

## 상세 리포트
- Async: ${ASYNC_DIR}/report.md
- Outbox: ${OUTBOX_DIR}/report.md
EOF

echo "[run-all] done"
echo "[run-all] comparison: ${COMPARE_README}"
