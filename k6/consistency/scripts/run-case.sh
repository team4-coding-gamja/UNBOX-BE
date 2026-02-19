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
BROKER_DOWN_SECONDS=20
FIXTURE_COUNT=1200
DRAIN_TIMEOUT_SECONDS=180
REQUEST_TIMEOUT="${REQUEST_TIMEOUT:-30s}"
MIN_OBSERVED_BEFORE_FAILURE=30
FAILURE_ARM_TIMEOUT_SECONDS=120

PAYMENT_BASE_URL="${PAYMENT_BASE_URL:-http://localhost:8085/payment}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-unbox-postgres}"
KAFKA_CONTAINER="${KAFKA_CONTAINER:-unbox-kafka}"
PAYMENT_TOPIC="${PAYMENT_TOPIC:-payment-events}"
KAFKA_IMAGE="${KAFKA_IMAGE:-apache/kafka:3.7.0}"
KAFKA_BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-kafka:29092}"
FAILURE_INJECTION_MODE="${FAILURE_INJECTION_MODE:-pause}"

DB_USER="${DB_USER:-${DB_USERNAME:-postgres}}"
DB_PASSWORD="${DB_PASSWORD:-${DB_PASS:-}}"
AUTH_TOKEN="${AUTH_TOKEN:-}"
AUTH_TOKEN_FROM_ARG=0

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
    --min-observed-before-failure)
      MIN_OBSERVED_BEFORE_FAILURE="$2"
      shift 2
      ;;
    --failure-arm-timeout-seconds)
      FAILURE_ARM_TIMEOUT_SECONDS="$2"
      shift 2
      ;;
    --failure-injection-mode)
      FAILURE_INJECTION_MODE="$(echo "$2" | tr '[:upper:]' '[:lower:]')"
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
      AUTH_TOKEN_FROM_ARG=1
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

if [[ "${FAILURE_INJECTION_MODE}" != "pause" && "${FAILURE_INJECTION_MODE}" != "stop" ]]; then
  echo "--failure-injection-mode must be one of: pause | stop" >&2
  exit 1
fi

if [[ "${MIN_OBSERVED_BEFORE_FAILURE}" -lt 0 ]]; then
  echo "--min-observed-before-failure must be >= 0" >&2
  exit 1
fi

if [[ "${FAILURE_ARM_TIMEOUT_SECONDS}" -le 0 ]]; then
  echo "--failure-arm-timeout-seconds must be > 0" >&2
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

TOTAL_SECONDS=$((WARMUP_SECONDS + STEADY_SECONDS + COOLDOWN_SECONDS))
OBSERVER_TIMEOUT_MS=$(((TOTAL_SECONDS + DRAIN_TIMEOUT_SECONDS + 30) * 1000))
OBSERVER_GROUP="consistency-observer-${RUN_ID}-${MODE}"
TOKEN_EXPIRES_IN_SECONDS=$((TOTAL_SECONDS + DRAIN_TIMEOUT_SECONDS + 300))

if [[ "${AUTH_TOKEN_FROM_ARG}" -eq 0 ]]; then
  if GENERATED_TOKEN="$("${SCRIPT_DIR}/generate-test-jwt.sh" \
    --user-id 1 \
    --email "buyer1@unbox.com" \
    --role "ROLE_USER" \
    --expires-in "${TOKEN_EXPIRES_IN_SECONDS}" 2>/dev/null)"; then
    AUTH_TOKEN="${GENERATED_TOKEN}"
    echo "[run-case] generated fresh auth token (ttl=${TOKEN_EXPIRES_IN_SECONDS}s)"
  elif [[ -n "${AUTH_TOKEN}" ]]; then
    echo "[run-case] WARN: failed to generate fresh token; fallback to existing AUTH_TOKEN from environment" >&2
  else
    echo "[run-case] ERROR: failed to generate auth token and no --auth-token was provided." >&2
    exit 1
  fi
elif [[ -z "${AUTH_TOKEN}" ]]; then
  echo "[run-case] ERROR: --auth-token value is empty." >&2
  exit 1
fi

DOCKER_NETWORK="$(
  docker inspect -f '{{range $k, $v := .NetworkSettings.Networks}}{{println $k}}{{end}}' "${KAFKA_CONTAINER}" \
    | head -n 1 \
    | tr -d '[:space:]'
)"

if [[ -z "${DOCKER_NETWORK}" ]]; then
  echo "Failed to resolve docker network from ${KAFKA_CONTAINER}" >&2
  exit 1
fi

wait_for_kafka_ready() {
  local timeout_seconds="${1:-60}"
  local deadline=$(( $(date +%s) + timeout_seconds ))

  while true; do
    if docker run --rm --network "${DOCKER_NETWORK}" \
      -e BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER}" \
      "${KAFKA_IMAGE}" sh -lc '
        KBT=$(command -v kafka-broker-api-versions.sh 2>/dev/null || true)
        if [ -z "$KBT" ]; then
          KBT="/opt/kafka/bin/kafka-broker-api-versions.sh"
        fi
        "$KBT" --bootstrap-server "$BOOTSTRAP_SERVER" >/dev/null 2>&1
      ' >/dev/null 2>&1; then
      return 0
    fi

    if [[ "$(date +%s)" -ge "${deadline}" ]]; then
      return 1
    fi

    sleep 1
  done
}

produce_observer_probe() {
  local probe_id="$1"
  local probe_payload

  probe_payload="$(
    jq -cn \
      --arg observerProbeId "${probe_id}" \
      --arg runId "${RUN_ID}" \
      --arg mode "${MODE}" \
      '{observerProbeId:$observerProbeId, runId:$runId, mode:$mode, kind:"observer-ready"}'
  )"

  docker run --rm --network "${DOCKER_NETWORK}" \
    -e BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER}" \
    -e TOPIC="${PAYMENT_TOPIC}" \
    -e PROBE_PAYLOAD="${probe_payload}" \
    "${KAFKA_IMAGE}" sh -lc '
      KCP=$(command -v kafka-console-producer.sh 2>/dev/null || true)
      if [ -z "$KCP" ]; then
        KCP="/opt/kafka/bin/kafka-console-producer.sh"
      fi
      printf "%s\n" "$PROBE_PAYLOAD" | "$KCP" --bootstrap-server "$BOOTSTRAP_SERVER" --topic "$TOPIC" >/dev/null
    ' >/dev/null 2>&1
}

wait_for_observer_ready() {
  local timeout_seconds="${1:-20}"
  local deadline=$(( $(date +%s) + timeout_seconds ))
  local probe_id

  while [[ "$(date +%s)" -lt "${deadline}" ]]; do
    probe_id="observer-ready-${RUN_ID}-${MODE}-$(date +%s%N)"

    if produce_observer_probe "${probe_id}"; then
      for _ in 1 2 3; do
        sleep 1
        if grep -Fq "\"observerProbeId\":\"${probe_id}\"" "${EVENT_LOG}"; then
          return 0
        fi
        if ! kill -0 "${OBSERVER_PID}" 2>/dev/null; then
          return 1
        fi
      done
    fi

    if ! kill -0 "${OBSERVER_PID}" 2>/dev/null; then
      return 1
    fi
  done

  return 1
}

inject_broker_down() {
  if [[ "${FAILURE_INJECTION_MODE}" == "pause" ]]; then
    docker pause "${KAFKA_CONTAINER}" >/dev/null
  else
    docker stop "${KAFKA_CONTAINER}" >/dev/null
  fi
}

recover_broker() {
  if [[ "${FAILURE_INJECTION_MODE}" == "pause" ]]; then
    docker unpause "${KAFKA_CONTAINER}" >/dev/null
  else
    docker start "${KAFKA_CONTAINER}" >/dev/null
  fi
}

count_observed_payment_ids() {
  local matched_lines
  matched_lines="$(
    grep -Eo '"paymentId"[[:space:]]*:[[:space:]]*"[0-9a-fA-F-]{36}"' "${EVENT_LOG}" || true
  )"

  if [[ -z "${matched_lines}" ]]; then
    echo "0"
    return 0
  fi

  printf '%s\n' "${matched_lines}" \
    | sed -E 's/.*"([0-9a-fA-F-]{36})"/\1/' \
    | sort -u \
    | wc -l \
    | tr -d '[:space:]'
}

# Pre-flight: Kafka 컨테이너 상태 확인
KAFKA_STATUS="$(docker inspect --format='{{.State.Status}}' "${KAFKA_CONTAINER}" 2>/dev/null || echo "unknown")"
if [[ "${KAFKA_STATUS}" != "running" ]]; then
  echo "[run-case] ERROR: Kafka container '${KAFKA_CONTAINER}' is not running (status=${KAFKA_STATUS}). Start Kafka first." >&2
  exit 1
fi

if ! wait_for_kafka_ready 60; then
  echo "[run-case] ERROR: Kafka bootstrap is not reachable at ${KAFKA_BOOTSTRAP_SERVER}" >&2
  exit 1
fi

echo "[run-case] starting observer consumer: group=${OBSERVER_GROUP}"
OBSERVER_CMD=$(cat <<EOF
export KAFKA_OPTS="-Dsun.net.inetaddr.ttl=1 -Dsun.net.inetaddr.negative.ttl=1"
KCC=\$(command -v kafka-console-consumer.sh 2>/dev/null || true)
if [ -z "\$KCC" ]; then
  KCC="/opt/kafka/bin/kafka-console-consumer.sh"
fi
"\$KCC" \
  --bootstrap-server ${KAFKA_BOOTSTRAP_SERVER} \
  --topic ${PAYMENT_TOPIC} \
  --group ${OBSERVER_GROUP} \
  --consumer-property auto.offset.reset=latest \
  --consumer-property client.dns.lookup=use_all_dns_ips \
  --consumer-property metadata.max.age.ms=1000 \
  --consumer-property reconnect.backoff.ms=500 \
  --consumer-property reconnect.backoff.max.ms=2000 \
  --timeout-ms ${OBSERVER_TIMEOUT_MS} \
  --property print.value=true
EOF
)

touch "${EVENT_LOG}"

# Observer 시작 (최대 3회 재시도)
# - 프로세스 생존 여부가 아니라 실제 consume 가능 여부(probe 메시지 수신)로 준비 완료 판정
OBSERVER_PID=""
OBSERVER_READY=0
for attempt in 1 2 3; do
  # 이전 실패 프로세스 정리
  if [[ -n "${OBSERVER_PID:-}" ]]; then
    kill "${OBSERVER_PID}" > /dev/null 2>&1 || true
    wait "${OBSERVER_PID}" > /dev/null 2>&1 || true
    OBSERVER_PID=""
  fi

  echo "[run-case] observer attempt ${attempt}/3 ..."
  docker run --rm --network "${DOCKER_NETWORK}" "${KAFKA_IMAGE}" \
    sh -lc "${OBSERVER_CMD}" >> "${EVENT_LOG}" 2>&1 &
  OBSERVER_PID=$!

  if wait_for_observer_ready 20; then
    echo "[run-case] observer confirmed ready (attempt ${attempt})"
    OBSERVER_READY=1
    break
  fi

  echo "[run-case] WARN: observer was not ready at attempt ${attempt}. Check: ${EVENT_LOG}" >&2

  if [[ "${attempt}" -lt 3 ]]; then
    echo "[run-case] retrying observer in 2s ..."
    sleep 2
  fi
done

if [[ "${OBSERVER_READY}" -eq 0 ]]; then
  echo "[run-case] ERROR: observer failed to connect after 3 attempts. Check: ${EVENT_LOG}" >&2
  exit 1
fi

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

FAILURE_ARM_STARTED_AT="$(date +%s)"
FAILURE_ARM_DEADLINE=$((FAILURE_ARM_STARTED_AT + FAILURE_ARM_TIMEOUT_SECONDS))
echo "[run-case] waiting failure-arm conditions: elapsed>=${FAILURE_AT_SECONDS}s AND observed>=${MIN_OBSERVED_BEFORE_FAILURE}"
while true; do
  now_epoch="$(date +%s)"
  elapsed="$((now_epoch - FAILURE_ARM_STARTED_AT))"
  observed_now="$(count_observed_payment_ids)"

  if [[ "${elapsed}" -ge "${FAILURE_AT_SECONDS}" && "${observed_now}" -ge "${MIN_OBSERVED_BEFORE_FAILURE}" ]]; then
    echo "[run-case] failure-arm ready: elapsed=${elapsed}s observed=${observed_now}"
    break
  fi

  if [[ "${now_epoch}" -ge "${FAILURE_ARM_DEADLINE}" ]]; then
    echo "[run-case] ERROR: failed to arm failure in time. observed=${observed_now}, required=${MIN_OBSERVED_BEFORE_FAILURE}, timeout=${FAILURE_ARM_TIMEOUT_SECONDS}s" >&2
    kill "${K6_PID}" >/dev/null 2>&1 || true
    wait "${K6_PID}" >/dev/null 2>&1 || true
    exit 1
  fi

  sleep 1
done

echo "[run-case] injecting failure (${FAILURE_INJECTION_MODE}): ${KAFKA_CONTAINER}"
BROKER_DOWN_EPOCH="$(date +%s)"
inject_broker_down

sleep "${BROKER_DOWN_SECONDS}"
echo "[run-case] recovering broker (${FAILURE_INJECTION_MODE}): ${KAFKA_CONTAINER}"
recover_broker
if ! wait_for_kafka_ready 90; then
  echo "[run-case] ERROR: Kafka did not become ready after recovery." >&2
  exit 1
fi
BROKER_UP_EPOCH="$(date +%s)"

wait "${K6_PID}"
echo "[run-case] k6 finished"

OUTBOX_PENDING_FINAL="N/A"
OUTBOX_PUBLISHED_COUNT="N/A"
DRAIN_TIME_SECONDS="N/A"
CONSUMER_DRAIN_STATUS="SKIP_ASYNC_MODE"
CONSUMER_LOG_EXISTS="$(psql_scalar unbox_order "SELECT CASE WHEN to_regclass('public.consumer_received_log') IS NULL THEN 0 ELSE 1 END;")"
CONSUMER_LOG_EXISTS="${CONSUMER_LOG_EXISTS:-0}"

if [[ "${MODE}" == "outbox" ]]; then
  CONSUMER_DRAIN_STATUS="WAITING"

  deadline=$((BROKER_UP_EPOCH + DRAIN_TIMEOUT_SECONDS))
  while true; do
    pending_now="$(psql_scalar unbox_payment "SELECT COUNT(*) FROM payment_outbox WHERE payload LIKE '%test_success_${RUN_ID}_%' AND status IN ('PENDING','PROCESSING');")"
    pending_now="${pending_now:-0}"

    if [[ "${pending_now}" -eq 0 ]]; then
      DRAIN_TIME_SECONDS="$(( $(date +%s) - BROKER_UP_EPOCH ))"
      CONSUMER_DRAIN_STATUS="DONE"
      break
    fi

    if [[ "$(date +%s)" -ge "${deadline}" ]]; then
      DRAIN_TIME_SECONDS="TIMEOUT"
      CONSUMER_DRAIN_STATUS="TIMEOUT"
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
TOPIC_DUPLICATE_PAYMENT_ID_COUNT="${DUPLICATE_PAYMENT_ID_COUNT}"

CONSUMER_PROCESSED_COUNT="$(psql_scalar unbox_order "SELECT CASE WHEN to_regclass('public.consumer_received_log') IS NULL THEN -1 ELSE COALESCE((SELECT COUNT(DISTINCT payment_id) FROM consumer_received_log WHERE consumer_group='order-group' AND payment_key LIKE 'test_success_${RUN_ID}_%'), 0) END;")"
CONSUMER_PROCESSED_COUNT="${CONSUMER_PROCESSED_COUNT:--1}"

CONSUMER_DUPLICATE_PAYMENT_ID_COUNT="$(psql_scalar unbox_order "SELECT CASE WHEN to_regclass('public.consumer_received_log') IS NULL THEN -1 ELSE COALESCE((SELECT COUNT(*) FROM (SELECT payment_id FROM consumer_received_log WHERE consumer_group='order-group' AND payment_key LIKE 'test_success_${RUN_ID}_%' GROUP BY payment_id HAVING COUNT(*) > 1) t), 0) END;")"
CONSUMER_DUPLICATE_PAYMENT_ID_COUNT="${CONSUMER_DUPLICATE_PAYMENT_ID_COUNT:--1}"

CONSUMED_SOURCE="topic_observer"
EVENTS_CONSUMED_COUNT="${TOPIC_OBSERVED_COUNT}"
DUPLICATE_PAYMENT_ID_COUNT="${TOPIC_DUPLICATE_PAYMENT_ID_COUNT}"

PAYMENT_DONE_COUNT="$(psql_scalar unbox_payment "SELECT COUNT(*) FROM p_payment WHERE status='DONE' AND payment_key LIKE 'test_success_${RUN_ID}_%';")"
PAYMENT_DONE_COUNT="${PAYMENT_DONE_COUNT:-0}"
EVENTS_CONSUMED_COUNT="${EVENTS_CONSUMED_COUNT:-0}"

LOSS_COUNT=$((PAYMENT_DONE_COUNT - EVENTS_CONSUMED_COUNT))
if [[ "${LOSS_COUNT}" -lt 0 ]]; then
  LOSS_COUNT=0
fi

LOSS_RATE="$(awk -v d="${PAYMENT_DONE_COUNT}" -v l="${LOSS_COUNT}" 'BEGIN { if (d == 0) { printf "0.00" } else { printf "%.2f", (l / d) * 100 } }')"

TOPIC_LOSS_COUNT="${LOSS_COUNT}"
TOPIC_LOSS_RATE="${LOSS_RATE}"

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
  --arg topic_loss_rate "${TOPIC_LOSS_RATE}" \
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
  --argjson topic_loss_count "${TOPIC_LOSS_COUNT}" \
  --argjson consumer_processed_count "${CONSUMER_PROCESSED_COUNT}" \
  --argjson consumer_duplicate_payment_id_count "${CONSUMER_DUPLICATE_PAYMENT_ID_COUNT}" \
  --argjson consumer_log_exists "${CONSUMER_LOG_EXISTS}" \
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
      topicLossCount: $topic_loss_count,
      topicLossRate: ($topic_loss_rate | tonumber),
      lossCount: $loss_count,
      lossRate: ($loss_rate | tonumber),
      duplicatePaymentIdCount: $duplicate_payment_id_count
    },
    consumerDiagnostics: {
      consumerLogExists: ($consumer_log_exists == 1),
      processedCount: $consumer_processed_count,
      duplicatePaymentIdCount: $consumer_duplicate_payment_id_count
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
| 이벤트 소비 완료 수(토픽 기준) | ${EVENTS_CONSUMED_COUNT} |
| 토픽 관측 수 | ${TOPIC_OBSERVED_COUNT} |
| 토픽 기준 유실 건수 | ${TOPIC_LOSS_COUNT} |
| 토픽 기준 유실률(%) | ${TOPIC_LOSS_RATE} |
| 중복 paymentId 수 | ${DUPLICATE_PAYMENT_ID_COUNT} |
| Order Consumer 처리 수(진단) | ${CONSUMER_PROCESSED_COUNT} |
| Order Consumer 중복 수(진단) | ${CONSUMER_DUPLICATE_PAYMENT_ID_COUNT} |
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
