#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
LOADTEST_DIR="${ROOT_DIR}/k6/payment-load-test"
RESULTS_ROOT_DIR="${LOADTEST_DIR}/results"

CASE_NAME="caseA"
DELAY_MS="5000"
PAYMENT_BASE_URL="${PAYMENT_BASE_URL:-http://localhost:8085/payment}"
AUTH_USER_ID="1"
AUTH_EMAIL="buyer1@unbox.com"
AUTH_ROLE="ROLE_USER"
AUTH_EXPIRES_IN="7200"
SKIP_COMPOSE_UP="false"
SKIP_DATA_GEN="false"
SKIP_ORDER_SEED="false"
SKIP_PAYMENT_RECREATE="false"
K6_BIN="${K6_BIN:-k6}"
SYNC_ASYNC_COOLDOWN_SECONDS="${SYNC_ASYNC_COOLDOWN_SECONDS:-30}"

usage() {
  cat <<'EOF'
Usage:
  run-sync-async.sh [options]

Options:
  --case-name <name>            case label (default: caseA)
  --delay-ms <ms>               fault delay ms (default: 5000)
  --payment-base-url <url>      payment base url (default: http://localhost:8085/payment)
  --auth-user-id <id>           jwt userId (default: 1)
  --auth-email <email>          jwt email (default: buyer1@unbox.com)
  --auth-role <role>            jwt role (default: ROLE_USER)
  --auth-expires-in <seconds>   jwt ttl seconds (default: 7200)
  --skip-compose-up             skip docker compose up -d
  --skip-data-gen               skip generate-data.js
  --skip-order-seed             skip order seed sql
  --skip-payment-recreate       do not recreate payment container with loadtest profile
  --k6-bin <bin>                k6 binary name/path (default: k6)
  (env) SYNC_ASYNC_COOLDOWN_SECONDS   cooldown between sync and async (default: 30)
  -h, --help                    show help

Examples:
  run-sync-async.sh --case-name caseA --delay-ms 5000
  run-sync-async.sh --case-name caseB --delay-ms 2000 --skip-compose-up --skip-data-gen --skip-order-seed --skip-payment-recreate
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --case-name)
      CASE_NAME="$2"
      shift 2
      ;;
    --delay-ms)
      DELAY_MS="$2"
      shift 2
      ;;
    --payment-base-url)
      PAYMENT_BASE_URL="$2"
      shift 2
      ;;
    --auth-user-id)
      AUTH_USER_ID="$2"
      shift 2
      ;;
    --auth-email)
      AUTH_EMAIL="$2"
      shift 2
      ;;
    --auth-role)
      AUTH_ROLE="$2"
      shift 2
      ;;
    --auth-expires-in)
      AUTH_EXPIRES_IN="$2"
      shift 2
      ;;
    --skip-compose-up)
      SKIP_COMPOSE_UP="true"
      shift
      ;;
    --skip-data-gen)
      SKIP_DATA_GEN="true"
      shift
      ;;
    --skip-order-seed)
      SKIP_ORDER_SEED="true"
      shift
      ;;
    --skip-payment-recreate)
      SKIP_PAYMENT_RECREATE="true"
      shift
      ;;
    --k6-bin)
      K6_BIN="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

DB_USERNAME="${DB_USERNAME:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Required command not found: $1" >&2
    exit 1
  }
}

require_cmd docker
require_cmd node
require_cmd jq
require_cmd openssl
require_cmd bash
require_cmd curl
require_cmd "${K6_BIN}"

format_pct_from_ratio() {
  local ratio="$1"
  awk -v v="${ratio}" 'BEGIN { printf "%.2f%%", v * 100 }'
}

format_num() {
  local num="$1"
  awk -v v="${num}" 'BEGIN { printf "%.2f", v }'
}

format_int() {
  local num="$1"
  awk -v v="${num}" 'BEGIN { printf "%.0f", v }'
}

write_case_report() {
  local case_name="$1"
  local delay_ms="$2"
  local run_ts="$3"
  local sync_run_id="$4"
  local async_run_id="$5"
  local sync_summary="$6"
  local async_summary="$7"
  local output_file="$8"

  local sync_rps async_rps sync_req_cnt async_req_cnt
  local sync_p95 async_p95 sync_p99 async_p99
  local sync_fail_ratio async_fail_ratio sync_fail_cnt async_fail_cnt
  local sync_2xx_ratio async_2xx_ratio

  sync_rps="$(jq -r '.metrics.http_reqs.values.rate // 0' "${sync_summary}")"
  async_rps="$(jq -r '.metrics.http_reqs.values.rate // 0' "${async_summary}")"
  sync_req_cnt="$(jq -r '.metrics.http_reqs.values.count // 0' "${sync_summary}")"
  async_req_cnt="$(jq -r '.metrics.http_reqs.values.count // 0' "${async_summary}")"

  sync_p95="$(jq -r '.metrics.http_req_duration.values["p(95)"] // 0' "${sync_summary}")"
  async_p95="$(jq -r '.metrics.http_req_duration.values["p(95)"] // 0' "${async_summary}")"
  sync_p99="$(jq -r '.metrics.http_req_duration.values["p(99)"] // 0' "${sync_summary}")"
  async_p99="$(jq -r '.metrics.http_req_duration.values["p(99)"] // 0' "${async_summary}")"

  sync_fail_ratio="$(jq -r '.metrics.http_req_failed.values.rate // 0' "${sync_summary}")"
  async_fail_ratio="$(jq -r '.metrics.http_req_failed.values.rate // 0' "${async_summary}")"
  sync_fail_cnt="$(jq -r '.metrics.confirm_non2xx_total.values.count // 0' "${sync_summary}")"
  async_fail_cnt="$(jq -r '.metrics.confirm_non2xx_total.values.count // 0' "${async_summary}")"

  sync_2xx_ratio="$(jq -r '.metrics.confirm_2xx_rate.values.rate // 0' "${sync_summary}")"
  async_2xx_ratio="$(jq -r '.metrics.confirm_2xx_rate.values.rate // 0' "${async_summary}")"

  cat > "${output_file}" <<EOF
# ${case_name} Sync vs Async 비교 결과

- 실행 시각: ${run_ts}
- 지연 주입: Order ${delay_ms}ms
- Sync Run ID: \`${sync_run_id}\`
- Async Run ID: \`${async_run_id}\`
- Sync Summary: \`${sync_summary}\`
- Async Summary: \`${async_summary}\`

## 핵심 비교표

| 지표 | Sync | Async |
|---|---:|---:|
| 처리량 RPS (http_reqs.rate) | $(format_num "${sync_rps}") | $(format_num "${async_rps}") |
| 총 요청 수 (http_reqs.count) | $(format_int "${sync_req_cnt}") | $(format_int "${async_req_cnt}") |
| p95 지연 (ms) | $(format_num "${sync_p95}") | $(format_num "${async_p95}") |
| p99 지연 (ms) | $(format_num "${sync_p99}") | $(format_num "${async_p99}") |
| 실패율 (http_req_failed.rate) | $(format_pct_from_ratio "${sync_fail_ratio}") | $(format_pct_from_ratio "${async_fail_ratio}") |
| Confirm 2xx 비율 (confirm_2xx_rate) | $(format_pct_from_ratio "${sync_2xx_ratio}") | $(format_pct_from_ratio "${async_2xx_ratio}") |
| Confirm non-2xx 건수 | $(format_int "${sync_fail_cnt}") | $(format_int "${async_fail_cnt}") |

## Grafana 패널 체크

| Panel | Sync 관찰값 | Async 관찰값 | 해석 |
|---|---|---|---|
| Error Rate by Service (panel 200) | 직접 입력 | 직접 입력 | 서비스별 전파 경로 확인 |
| Payment Tomcat Busy Threads (panel 201) | 직접 입력 | 직접 입력 | 동기 블로킹/스레드 고갈 선행 징후 |
| Payment HikariCP Active (panel 202) | 직접 입력 | 직접 입력 | DB 병목 vs 외부호출 병목 분리 |

## 1) 처리량 (Throughput)

- RPS/TPS: ramping 구간별 처리량 비교
- 목표: **동기 구조의 변곡점(처리량 하락 시작 구간)** 식별
- 결과 요약: Sync $(format_num "${sync_rps}") rps / Async $(format_num "${async_rps}") rps

## 2) 응답 지연 (Latency)

- p95/p99: 평균 대신 tail latency 중심 비교
- 목표: 동기에서 **블로킹/고갈 징후**가 tail latency로 나타나는지 확인
- 결과 요약: Sync p95/p99 $(format_num "${sync_p95}") / $(format_num "${sync_p99}") ms, Async p95/p99 $(format_num "${async_p95}") / $(format_num "${async_p99}") ms

## 3) 실패율 (Error Rate)

- Confirm 기준 2xx vs non-2xx
- 목표: **하위 지연이 결제 진입점 실패로 전파되는 정도**를 수치화
- 결과 요약: Sync 실패율 $(format_pct_from_ratio "${sync_fail_ratio}") (non-2xx $(format_int "${sync_fail_cnt}")건), Async 실패율 $(format_pct_from_ratio "${async_fail_ratio}") (non-2xx $(format_int "${async_fail_cnt}")건)
EOF
}

seed_db() {
  local db_name="$1"
  local sql_file="$2"
  echo "[seed] ${db_name} <= ${sql_file}"
  cat "${sql_file}" | docker compose exec -T -e PGPASSWORD="${DB_PASSWORD}" db \
    psql -h localhost -U "${DB_USERNAME}" -d "${db_name}" >/dev/null
}

wait_payment_health() {
  local health_url="${PAYMENT_BASE_URL}/actuator/health"
  local max_try=90
  local i=1
  while [[ ${i} -le ${max_try} ]]; do
    local code
    code="$(curl -s -o /dev/null -w "%{http_code}" "${health_url}" || true)"
    if [[ "${code}" == "200" || "${code}" == "401" ]]; then
      return 0
    fi
    sleep 2
    ((i++))
  done
  echo "Payment health check failed: ${health_url}" >&2
  exit 1
}

check_loadtest_endpoint() {
  local code
  code="$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    "${PAYMENT_BASE_URL}/test/api/payment/confirm/sync" \
    -H "Content-Type: application/json" \
    -d '{}' || true)"
  if [[ "${code}" == "404" ]]; then
    echo "loadtest endpoint not found (404). payment service is likely not running with loadtest profile." >&2
    exit 1
  fi
}

recreate_payment_loadtest() {
  echo "[compose] recreate unbox-payment with loadtest profile"
  docker compose stop unbox-payment >/dev/null 2>&1 || true
  docker rm -f unbox-payment >/dev/null 2>&1 || true
  docker compose run -d --no-deps --service-ports --name unbox-payment \
    -e SPRING_PROFILES_ACTIVE=loadtest \
    -e ORDER_SERVICE_URL=http://localhost:8080/payment/mock \
    -e TRADE_SERVICE_URL=http://localhost:8080/payment/mock \
    -e USER_SERVICE_URL=http://localhost:8080/payment/mock \
    -e PRODUCT_SERVICE_URL=http://localhost:8080/payment/mock \
    unbox-payment >/dev/null
}

generate_auth_token() {
  bash "${ROOT_DIR}/k6/consistency/scripts/generate-test-jwt.sh" \
    --user-id "${AUTH_USER_ID}" \
    --email "${AUTH_EMAIL}" \
    --role "${AUTH_ROLE}" \
    --expires-in "${AUTH_EXPIRES_IN}"
}

run_k6_mode() {
  local mode="$1"
  local run_id="$2"
  local summary_file="$3"
  local script_file
  if [[ "${mode}" == "sync" ]]; then
    script_file="${LOADTEST_DIR}/confirm-fault-injection-sync.js"
    echo "[k6] ${mode} start (run_id=${run_id}, delay_ms=${DELAY_MS})"
    "${K6_BIN}" run \
      --summary-export "${summary_file}" \
      -e PAYMENT_BASE_URL="${PAYMENT_BASE_URL}" \
      -e AUTH_TOKEN="${AUTH_TOKEN}" \
      -e TEST_RUN_ID="${run_id}" \
      -e FAULT_DELAY_MS="${DELAY_MS}" \
      -e DATA_PATH="${LOADTEST_DIR}/data.json" \
      "${script_file}"
  else
    script_file="${LOADTEST_DIR}/confirm-fault-injection-async.js"
    echo "[k6] ${mode} start (run_id=${run_id})"
    "${K6_BIN}" run \
      --summary-export "${summary_file}" \
      -e PAYMENT_BASE_URL="${PAYMENT_BASE_URL}" \
      -e AUTH_TOKEN="${AUTH_TOKEN}" \
      -e TEST_RUN_ID="${run_id}" \
      -e DATA_PATH="${LOADTEST_DIR}/data.json" \
      "${script_file}"
  fi
}

RUN_TS="$(date +%Y%m%d%H%M%S)"
SYNC_RUN_ID="${CASE_NAME}-sync-${RUN_TS}"
ASYNC_RUN_ID="${CASE_NAME}-async-${RUN_TS}"
CASE_RESULTS_DIR="${RESULTS_ROOT_DIR}/${CASE_NAME}/${RUN_TS}"
SYNC_SUMMARY_FILE="${CASE_RESULTS_DIR}/${SYNC_RUN_ID}.summary.json"
ASYNC_SUMMARY_FILE="${CASE_RESULTS_DIR}/${ASYNC_RUN_ID}.summary.json"
CASE_REPORT_FILE="${CASE_RESULTS_DIR}/README-${CASE_NAME}.md"
LATEST_CASE_REPORT_FILE="${RESULTS_ROOT_DIR}/README-${CASE_NAME}.md"

mkdir -p "${CASE_RESULTS_DIR}"

echo "[info] case=${CASE_NAME}, delay_ms=${DELAY_MS}, base_url=${PAYMENT_BASE_URL}"

if [[ "${SKIP_COMPOSE_UP}" != "true" ]]; then
  echo "[compose] up -d"
  docker compose up -d
fi

if [[ "${SKIP_PAYMENT_RECREATE}" != "true" ]]; then
  recreate_payment_loadtest
fi

wait_payment_health
check_loadtest_endpoint

if [[ "${SKIP_DATA_GEN}" != "true" ]]; then
  echo "[data] generate payment load-test data.json"
  node "${LOADTEST_DIR}/generate-data.js" >/dev/null
fi

# sync 직전 데이터 리셋
if [[ "${SKIP_ORDER_SEED}" != "true" ]]; then
  seed_db "unbox_order" "${LOADTEST_DIR}/order-data-setup.sql"
fi
seed_db "unbox_payment" "${LOADTEST_DIR}/payment-data-setup.sql"

AUTH_TOKEN="$(generate_auth_token)"
echo "[auth] token generated for userId=${AUTH_USER_ID}"

run_k6_mode "sync" "${SYNC_RUN_ID}" "${SYNC_SUMMARY_FILE}"

# sync/async 그래프 구간이 겹치지 않도록 쿨다운
if [[ "${SYNC_ASYNC_COOLDOWN_SECONDS}" -gt 0 ]]; then
  echo "[cooldown] waiting ${SYNC_ASYNC_COOLDOWN_SECONDS}s before async run"
  sleep "${SYNC_ASYNC_COOLDOWN_SECONDS}"
fi

# async 직전 데이터 리셋
if [[ "${SKIP_ORDER_SEED}" != "true" ]]; then
  seed_db "unbox_order" "${LOADTEST_DIR}/order-data-setup.sql"
fi

# sync 실행 후 결제 상태가 DONE으로 변하므로 async 전 결제 재시드
seed_db "unbox_payment" "${LOADTEST_DIR}/payment-data-setup.sql"

run_k6_mode "async" "${ASYNC_RUN_ID}" "${ASYNC_SUMMARY_FILE}"

write_case_report \
  "${CASE_NAME}" \
  "${DELAY_MS}" \
  "${RUN_TS}" \
  "${SYNC_RUN_ID}" \
  "${ASYNC_RUN_ID}" \
  "${SYNC_SUMMARY_FILE}" \
  "${ASYNC_SUMMARY_FILE}" \
  "${CASE_REPORT_FILE}"

cp "${CASE_REPORT_FILE}" "${LATEST_CASE_REPORT_FILE}"

echo "[done] sync_run_id=${SYNC_RUN_ID}, async_run_id=${ASYNC_RUN_ID}, report=${CASE_REPORT_FILE}"
