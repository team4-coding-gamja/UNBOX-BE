#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

MODE=""
RUN_ID="$(date +%Y%m%d%H%M%S)"

RATE=10
WARMUP_SECONDS=10
STEADY_SECONDS=40
COOLDOWN_SECONDS=10
FAILURE_AT_SECONDS=20
BROKER_DOWN_SECONDS=70
FIXTURE_COUNT=1200
DRAIN_TIMEOUT_SECONDS=180
REQUEST_TIMEOUT="${REQUEST_TIMEOUT:-30s}"

PAYMENT_BASE_URL="${PAYMENT_BASE_URL:-http://localhost:8085/payment}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-unbox-postgres}"
KAFKA_CONTAINER="${KAFKA_CONTAINER:-unbox-kafka}"
PAYMENT_TOPIC="${PAYMENT_TOPIC:-payment-events}"
KAFKA_IMAGE="${KAFKA_IMAGE:-apache/kafka:3.7.0}"

DB_USER="${DB_USER:-${DB_USERNAME:-postgres}}"
DB_PASSWORD="${DB_PASSWORD:-${DB_PASS:-}}"
AUTH_TOKEN="${AUTH_TOKEN:-}"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

DB_USER="${DB_USER:-${DB_USERNAME:-postgres}}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode)
      MODE="$(echo "$2" | tr '[:upper:]' '[:lower:]')"
      shift 2
      ;;
    --run-id)
      RUN_ID="$2"
      shift 2
      ;;
    --rate)
      RATE="$2"
      shift 2
      ;;
    --warmup-seconds)
      WARMUP_SECONDS="$2"
      shift 2
      ;;
    --steady-seconds)
      STEADY_SECONDS="$2"
      shift 2
      ;;
    --cooldown-seconds)
      COOLDOWN_SECONDS="$2"
      shift 2
      ;;
    --failure-at-seconds)
      FAILURE_AT_SECONDS="$2"
      shift 2
      ;;
    --broker-down-seconds)
      BROKER_DOWN_SECONDS="$2"
      shift 2
      ;;
    --fixture-count)
      FIXTURE_COUNT="$2"
      shift 2
      ;;
    --drain-timeout-seconds)
      DRAIN_TIMEOUT_SECONDS="$2"
      shift 2
      ;;
    --request-timeout)
      REQUEST_TIMEOUT="$2"
      shift 2
      ;;
    --payment-base-url)
      PAYMENT_BASE_URL="$2"
      shift 2
      ;;
    --postgres-container)
      POSTGRES_CONTAINER="$2"
      shift 2
      ;;
    --kafka-container)
      KAFKA_CONTAINER="$2"
      shift 2
      ;;
    --topic)
      PAYMENT_TOPIC="$2"
      shift 2
      ;;
    --kafka-image)
      KAFKA_IMAGE="$2"
      shift 2
      ;;
    --db-user)
      DB_USER="$2"
      shift 2
      ;;
    --db-password)
      DB_PASSWORD="$2"
      shift 2
      ;;
    --auth-token)
      AUTH_TOKEN="$2"
      shift 2
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 1
      ;;
  esac
done

if [[ "${MODE}" != "async" && "${MODE}" != "outbox" ]]; then
  echo "--mode is required: async | outbox" >&2
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required." >&2
  exit 1
fi

if ! command -v k6 >/dev/null 2>&1; then
  echo "k6 is required." >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required." >&2
  exit 1
fi

ARTIFACT_DIR="${ROOT_DIR}/k6/consistency/artifacts/${RUN_ID}-${MODE}"
mkdir -p "${ARTIFACT_DIR}"

K6_SCRIPT="${ROOT_DIR}/k6/consistency/confirm-consistency.js"
DATA_FILE="${ARTIFACT_DIR}/data.json"
K6_LOG="${ARTIFACT_DIR}/k6.log"
SUMMARY_JSON="${ARTIFACT_DIR}/k6-summary.json"
EVENT_LOG="${ARTIFACT_DIR}/payment-events.log"
REPORT_JSON="${ARTIFACT_DIR}/report.json"
REPORT_MD="${ARTIFACT_DIR}/report.md"

EXPECTED_IDS="${ARTIFACT_DIR}/expected-payment-ids.txt"
OBSERVED_ALL_IDS="${ARTIFACT_DIR}/observed-payment-ids-all.txt"
OBSERVED_MATCHED_ALL_IDS="${ARTIFACT_DIR}/observed-payment-ids-matched-all.txt"
OBSERVED_MATCHED_UNIQUE_IDS="${ARTIFACT_DIR}/observed-payment-ids-matched-unique.txt"

psql_scalar() {
  local db="$1"
  local sql="$2"
  docker exec -i -e PGPASSWORD="${DB_PASSWORD}" "${POSTGRES_CONTAINER}" \
    psql -v ON_ERROR_STOP=1 -U "${DB_USER}" -d "${db}" -t -A -c "${sql}" \
    | tr -d '[:space:]'
}

cleanup() {
  if [[ -n "${OBSERVER_PID:-}" ]]; then
    kill "${OBSERVER_PID}" >/dev/null 2>&1 || true
    wait "${OBSERVER_PID}" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

echo "[run-case] mode=${MODE} run_id=${RUN_ID}"
echo "[run-case] preparing fixture data..."
"${SCRIPT_DIR}/setup-fixture.sh" \
  --run-id "${RUN_ID}" \
  --count "${FIXTURE_COUNT}" \
  --data-file "${DATA_FILE}" \
  --postgres-container "${POSTGRES_CONTAINER}" \
  --db-user "${DB_USER}" \
  --db-password "${DB_PASSWORD}"

if [[ -z "${AUTH_TOKEN}" ]]; then
  AUTH_TOKEN="$("${SCRIPT_DIR}/generate-test-jwt.sh" --user-id 1 --email "buyer1@unbox.com" --role "ROLE_USER")"
fi

TOTAL_SECONDS=$((WARMUP_SECONDS + STEADY_SECONDS + COOLDOWN_SECONDS))
OBSERVER_TIMEOUT_MS=$(((TOTAL_SECONDS + DRAIN_TIMEOUT_SECONDS + 30) * 1000))
OBSERVER_GROUP="consistency-observer-${RUN_ID}-${MODE}"
DOCKER_NETWORK="$(
  docker inspect -f '{{range $k, $v := .NetworkSettings.Networks}}{{println $k}}{{end}}' "${KAFKA_CONTAINER}" \
    | head -n 1 \
    | tr -d '[:space:]'
)"

if [[ -z "${DOCKER_NETWORK}" ]]; then
  echo "Failed to resolve docker network from ${KAFKA_CONTAINER}" >&2
  exit 1
fi

echo "[run-case] starting observer consumer: group=${OBSERVER_GROUP}"
OBSERVER_CMD=$(cat <<EOF
KCC=\$(command -v kafka-console-consumer.sh 2>/dev/null || true)
if [ -z "\$KCC" ]; then
  KCC="/opt/kafka/bin/kafka-console-consumer.sh"
fi
"\$KCC" \
  --bootstrap-server kafka:29092 \
  --topic ${PAYMENT_TOPIC} \
  --group ${OBSERVER_GROUP} \
  --consumer-property auto.offset.reset=latest \
  --timeout-ms ${OBSERVER_TIMEOUT_MS} \
  --property print.value=true
EOF
)

docker run --rm --network "${DOCKER_NETWORK}" "${KAFKA_IMAGE}" \
  sh -lc "${OBSERVER_CMD}" >"${EVENT_LOG}" 2>&1 &
OBSERVER_PID=$!

sleep 2

echo "[run-case] starting k6 load..."
TEST_START_EPOCH="$(date +%s)"

k6 run \
  --summary-export "${SUMMARY_JSON}" \
  -e MODE="${MODE}" \
  -e AUTH_TOKEN="${AUTH_TOKEN}" \
  -e PAYMENT_BASE_URL="${PAYMENT_BASE_URL}" \
  -e DATA_PATH="${DATA_FILE}" \
  -e TEST_RUN_ID="${RUN_ID}" \
  -e RATE="${RATE}" \
  -e WARMUP_SECONDS="${WARMUP_SECONDS}" \
  -e STEADY_SECONDS="${STEADY_SECONDS}" \
  -e COOLDOWN_SECONDS="${COOLDOWN_SECONDS}" \
  -e REQUEST_TIMEOUT="${REQUEST_TIMEOUT}" \
  "${K6_SCRIPT}" >"${K6_LOG}" 2>&1 &
K6_PID=$!

sleep "${FAILURE_AT_SECONDS}"
echo "[run-case] injecting failure: stop ${KAFKA_CONTAINER}"
BROKER_DOWN_EPOCH="$(date +%s)"
docker stop "${KAFKA_CONTAINER}" >/dev/null

sleep "${BROKER_DOWN_SECONDS}"
echo "[run-case] recovering broker: start ${KAFKA_CONTAINER}"
docker start "${KAFKA_CONTAINER}" >/dev/null
BROKER_UP_EPOCH="$(date +%s)"

wait "${K6_PID}"
echo "[run-case] k6 finished"

OUTBOX_PENDING_FINAL="N/A"
OUTBOX_PUBLISHED_COUNT="N/A"
DRAIN_TIME_SECONDS="N/A"
CONSUMER_DRAIN_STATUS="SKIP_ASYNC_MODE"
PAYMENT_DONE_TARGET="$(psql_scalar unbox_payment "SELECT COUNT(*) FROM p_payment WHERE status='DONE' AND payment_key LIKE 'test_success_${RUN_ID}_%';")"
PAYMENT_DONE_TARGET="${PAYMENT_DONE_TARGET:-0}"
CONSUMER_LOG_EXISTS="$(psql_scalar unbox_order "SELECT CASE WHEN to_regclass('public.consumer_received_log') IS NULL THEN 0 ELSE 1 END;")"
CONSUMER_LOG_EXISTS="${CONSUMER_LOG_EXISTS:-0}"

if [[ "${MODE}" == "outbox" ]]; then
  if [[ "${CONSUMER_LOG_EXISTS}" == "1" ]]; then
    CONSUMER_DRAIN_STATUS="WAITING"
  else
    CONSUMER_DRAIN_STATUS="SKIP_NO_CONSUMER_LOG"
  fi

  deadline=$((BROKER_UP_EPOCH + DRAIN_TIMEOUT_SECONDS))
  while true; do
    pending_now="$(psql_scalar unbox_payment "SELECT COUNT(*) FROM payment_outbox WHERE payload LIKE '%test_success_${RUN_ID}_%' AND status IN ('PENDING','PROCESSING');")"
    pending_now="${pending_now:-0}"

    consumer_now=0
    if [[ "${CONSUMER_LOG_EXISTS}" == "1" ]]; then
      consumer_now="$(psql_scalar unbox_order "SELECT COALESCE((SELECT COUNT(DISTINCT payment_id) FROM consumer_received_log WHERE consumer_group='order-group' AND payment_key LIKE 'test_success_${RUN_ID}_%'), 0);")"
      consumer_now="${consumer_now:-0}"
    fi

    if [[ "${pending_now}" -eq 0 ]]; then
      if [[ "${CONSUMER_LOG_EXISTS}" != "1" || "${consumer_now}" -ge "${PAYMENT_DONE_TARGET}" ]]; then
        DRAIN_TIME_SECONDS="$(( $(date +%s) - BROKER_UP_EPOCH ))"
        CONSUMER_DRAIN_STATUS="DONE"
        break
      fi
    fi

    if [[ "$(date +%s)" -ge "${deadline}" ]]; then
      DRAIN_TIME_SECONDS="TIMEOUT"
      if [[ "${CONSUMER_LOG_EXISTS}" == "1" ]]; then
        CONSUMER_DRAIN_STATUS="TIMEOUT"
      fi
      break
    fi

    sleep 1
  done

  OUTBOX_PENDING_FINAL="$(psql_scalar unbox_payment "SELECT COUNT(*) FROM payment_outbox WHERE payload LIKE '%test_success_${RUN_ID}_%' AND status IN ('PENDING','PROCESSING');")"
  OUTBOX_PUBLISHED_COUNT="$(psql_scalar unbox_payment "SELECT COUNT(*) FROM payment_outbox WHERE payload LIKE '%test_success_${RUN_ID}_%' AND status = 'PUBLISHED';")"
fi

kill "${OBSERVER_PID}" >/dev/null 2>&1 || true
wait "${OBSERVER_PID}" >/dev/null 2>&1 || true
unset OBSERVER_PID

jq -r '.[].paymentId' "${DATA_FILE}" | sort -u > "${EXPECTED_IDS}"

grep -Eo '"paymentId"[[:space:]]*:[[:space:]]*"[0-9a-fA-F-]{36}"' "${EVENT_LOG}" \
  | sed -E 's/.*"([0-9a-fA-F-]{36})"/\1/' > "${OBSERVED_ALL_IDS}" || true

if [[ ! -s "${OBSERVED_ALL_IDS}" ]]; then
  : > "${OBSERVED_ALL_IDS}"
fi

grep -Fxf "${EXPECTED_IDS}" "${OBSERVED_ALL_IDS}" > "${OBSERVED_MATCHED_ALL_IDS}" || true
sort -u "${OBSERVED_MATCHED_ALL_IDS}" > "${OBSERVED_MATCHED_UNIQUE_IDS}"

EVENTS_CONSUMED_COUNT="$(wc -l < "${OBSERVED_MATCHED_UNIQUE_IDS}" | tr -d '[:space:]')"
DUPLICATE_PAYMENT_ID_COUNT="$(sort "${OBSERVED_MATCHED_ALL_IDS}" | uniq -d | wc -l | tr -d '[:space:]')"
TOPIC_OBSERVED_COUNT="${EVENTS_CONSUMED_COUNT}"

CONSUMER_PROCESSED_COUNT="$(psql_scalar unbox_order "SELECT CASE WHEN to_regclass('public.consumer_received_log') IS NULL THEN -1 ELSE COALESCE((SELECT COUNT(DISTINCT payment_id) FROM consumer_received_log WHERE consumer_group='order-group' AND payment_key LIKE 'test_success_${RUN_ID}_%'), 0) END;")"
CONSUMER_PROCESSED_COUNT="${CONSUMER_PROCESSED_COUNT:--1}"

CONSUMER_DUPLICATE_PAYMENT_ID_COUNT="$(psql_scalar unbox_order "SELECT CASE WHEN to_regclass('public.consumer_received_log') IS NULL THEN -1 ELSE COALESCE((SELECT COUNT(*) FROM (SELECT payment_id FROM consumer_received_log WHERE consumer_group='order-group' AND payment_key LIKE 'test_success_${RUN_ID}_%' GROUP BY payment_id HAVING COUNT(*) > 1) t), 0) END;")"
CONSUMER_DUPLICATE_PAYMENT_ID_COUNT="${CONSUMER_DUPLICATE_PAYMENT_ID_COUNT:--1}"

CONSUMED_SOURCE="topic_observer"
if [[ "${CONSUMER_PROCESSED_COUNT}" != "-1" ]]; then
  EVENTS_CONSUMED_COUNT="${CONSUMER_PROCESSED_COUNT}"
  DUPLICATE_PAYMENT_ID_COUNT="${CONSUMER_DUPLICATE_PAYMENT_ID_COUNT}"
  CONSUMED_SOURCE="order_consumer_received_log"
fi

PAYMENT_DONE_COUNT="$(psql_scalar unbox_payment "SELECT COUNT(*) FROM p_payment WHERE status='DONE' AND payment_key LIKE 'test_success_${RUN_ID}_%';")"
PAYMENT_DONE_COUNT="${PAYMENT_DONE_COUNT:-0}"
EVENTS_CONSUMED_COUNT="${EVENTS_CONSUMED_COUNT:-0}"

LOSS_COUNT=$((PAYMENT_DONE_COUNT - EVENTS_CONSUMED_COUNT))
if [[ "${LOSS_COUNT}" -lt 0 ]]; then
  LOSS_COUNT=0
fi

LOSS_RATE="$(awk -v d="${PAYMENT_DONE_COUNT}" -v l="${LOSS_COUNT}" 'BEGIN { if (d == 0) { printf "0.00" } else { printf "%.2f", (l / d) * 100 } }')"

CONFIRM_REQUESTS_COUNT="$(jq -r '(.metrics.confirm_requests_total.count // .metrics.confirm_requests_total.values.count // 0)' "${SUMMARY_JSON}")"
CONFIRM_SUCCESS_COUNT="$(jq -r '(.metrics.confirm_success_total.count // .metrics.confirm_success_total.values.count // 0)' "${SUMMARY_JSON}")"
CONFIRM_NON2XX_COUNT="$(jq -r '(.metrics.confirm_non2xx_total.count // .metrics.confirm_non2xx_total.values.count // 0)' "${SUMMARY_JSON}")"
DATASET_REUSE_TOTAL="$(jq -r '(.metrics.dataset_reuse_total.count // .metrics.dataset_reuse_total.values.count // 0)' "${SUMMARY_JSON}")"

HTTP_RPS="$(jq -r '(.metrics.http_reqs.rate // .metrics.http_reqs.values.rate // 0)' "${SUMMARY_JSON}")"
P95_MS="$(jq -r '(.metrics.http_req_duration["p(95)"] // .metrics.http_req_duration.values["p(95)"] // 0)' "${SUMMARY_JSON}")"
P99_MS="$(jq -r '(.metrics.http_req_duration["p(99)"] // .metrics.http_req_duration.values["p(99)"] // 0)' "${SUMMARY_JSON}")"

API_SUCCESS_RATE="$(awk -v s="${CONFIRM_SUCCESS_COUNT}" -v t="${CONFIRM_REQUESTS_COUNT}" 'BEGIN { if (t == 0) { printf "0.00" } else { printf "%.2f", (s / t) * 100 } }')"

jq -n \
  --arg mode "${MODE}" \
  --arg run_id "${RUN_ID}" \
  --arg payment_base_url "${PAYMENT_BASE_URL}" \
  --arg broker_down_at "${BROKER_DOWN_EPOCH}" \
  --arg broker_up_at "${BROKER_UP_EPOCH}" \
  --arg drain_time_seconds "${DRAIN_TIME_SECONDS}" \
  --arg outbox_pending_final "${OUTBOX_PENDING_FINAL}" \
  --arg outbox_published_count "${OUTBOX_PUBLISHED_COUNT}" \
  --arg loss_rate "${LOSS_RATE}" \
  --arg api_success_rate "${API_SUCCESS_RATE}" \
  --arg http_rps "${HTTP_RPS}" \
  --arg p95_ms "${P95_MS}" \
  --arg p99_ms "${P99_MS}" \
  --arg consumed_source "${CONSUMED_SOURCE}" \
  --arg consumer_drain_status "${CONSUMER_DRAIN_STATUS}" \
  --arg request_timeout "${REQUEST_TIMEOUT}" \
  --argjson fixture_count "${FIXTURE_COUNT}" \
  --argjson confirm_requests_count "${CONFIRM_REQUESTS_COUNT}" \
  --argjson confirm_success_count "${CONFIRM_SUCCESS_COUNT}" \
  --argjson confirm_non2xx_count "${CONFIRM_NON2XX_COUNT}" \
  --argjson dataset_reuse_total "${DATASET_REUSE_TOTAL}" \
  --argjson payment_done_count "${PAYMENT_DONE_COUNT}" \
  --argjson events_consumed_count "${EVENTS_CONSUMED_COUNT}" \
  --argjson topic_observed_count "${TOPIC_OBSERVED_COUNT}" \
  --argjson duplicate_payment_id_count "${DUPLICATE_PAYMENT_ID_COUNT}" \
  --argjson loss_count "${LOSS_COUNT}" \
  '{
    mode: $mode,
    runId: $run_id,
    paymentBaseUrl: $payment_base_url,
    timing: {
      brokerDownEpoch: ($broker_down_at | tonumber),
      brokerUpEpoch: ($broker_up_at | tonumber),
      drainTimeSeconds: $drain_time_seconds,
      consumerDrainStatus: $consumer_drain_status
    },
    load: {
      fixtureCount: $fixture_count,
      requestTimeout: $request_timeout,
      confirmRequests: $confirm_requests_count,
      confirmSuccess: $confirm_success_count,
      confirmNon2xx: $confirm_non2xx_count,
      datasetReuseTotal: $dataset_reuse_total
    },
    consistency: {
      consumedSource: $consumed_source,
      paymentDoneCount: $payment_done_count,
      eventsConsumedCount: $events_consumed_count,
      topicObservedCount: $topic_observed_count,
      lossCount: $loss_count,
      lossRate: ($loss_rate | tonumber),
      duplicatePaymentIdCount: $duplicate_payment_id_count
    },
    outbox: {
      publishedCount: $outbox_published_count,
      pendingFinal: $outbox_pending_final
    },
    performance: {
      avgRps: ($http_rps | tonumber),
      p95Ms: ($p95_ms | tonumber),
      p99Ms: ($p99_ms | tonumber),
      apiSuccessRate: ($api_success_rate | tonumber)
    },
    artifacts: {
      dataFile: "'"${DATA_FILE}"'",
      eventLog: "'"${EVENT_LOG}"'",
      k6Log: "'"${K6_LOG}"'",
      summaryJson: "'"${SUMMARY_JSON}"'"
    }
  }' > "${REPORT_JSON}"

cat > "${REPORT_MD}" <<EOF
# 결제 정합성 테스트 리포트

- 실행 ID: ${RUN_ID}
- 모드: ${MODE}
- 결제 API: ${PAYMENT_BASE_URL}
- 요청 타임아웃: ${REQUEST_TIMEOUT}
- 브로커 중단 시각(epoch): ${BROKER_DOWN_EPOCH}
- 브로커 복구 시각(epoch): ${BROKER_UP_EPOCH}

| 지표 | 값 |
| --- | ---: |
| Fixture 개수 | ${FIXTURE_COUNT} |
| Confirm 요청 수 | ${CONFIRM_REQUESTS_COUNT} |
| Confirm 성공 수(2xx) | ${CONFIRM_SUCCESS_COUNT} |
| Confirm 실패 수(non-2xx) | ${CONFIRM_NON2XX_COUNT} |
| API 성공률(%) | ${API_SUCCESS_RATE} |
| 평균 RPS | ${HTTP_RPS} |
| p95 지연(ms) | ${P95_MS} |
| p99 지연(ms) | ${P99_MS} |
| Payment DONE 수 | ${PAYMENT_DONE_COUNT} |
| 소비 집계 소스 | ${CONSUMED_SOURCE} |
| 이벤트 소비 완료 수 | ${EVENTS_CONSUMED_COUNT} |
| 토픽 관측 수(보조) | ${TOPIC_OBSERVED_COUNT} |
| 유실 건수 | ${LOSS_COUNT} |
| 유실률(%) | ${LOSS_RATE} |
| 중복 paymentId 수 | ${DUPLICATE_PAYMENT_ID_COUNT} |
| 데이터셋 재사용 수 | ${DATASET_REUSE_TOTAL} |
| Outbox PUBLISHED 수 | ${OUTBOX_PUBLISHED_COUNT} |
| Outbox 잔여(PENDING/PROCESSING) | ${OUTBOX_PENDING_FINAL} |
| 복구 시간(s) | ${DRAIN_TIME_SECONDS} |
| 컨슈머 드레인 상태 | ${CONSUMER_DRAIN_STATUS} |

## 산출물 경로
- data: ${DATA_FILE}
- observer log: ${EVENT_LOG}
- k6 log: ${K6_LOG}
- summary json: ${SUMMARY_JSON}
- report json: ${REPORT_JSON}
EOF

echo "[run-case] completed"
echo "[run-case] report: ${REPORT_MD}"
echo "[run-case] report_json: ${REPORT_JSON}"
