#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

RUN_ID="$(date +%Y%m%d%H%M%S)"
FIXTURE_COUNT=1200
BUYER_ID=1
SELLER_ID=2
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-unbox-postgres}"
DB_USER="${DB_USER:-${DB_USERNAME:-postgres}}"
DB_PASSWORD="${DB_PASSWORD:-${DB_PASS:-}}"
DATA_FILE=""

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
    --run-id)
      RUN_ID="$2"
      shift 2
      ;;
    --count)
      FIXTURE_COUNT="$2"
      shift 2
      ;;
    --buyer-id)
      BUYER_ID="$2"
      shift 2
      ;;
    --seller-id)
      SELLER_ID="$2"
      shift 2
      ;;
    --postgres-container)
      POSTGRES_CONTAINER="$2"
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
    --data-file)
      DATA_FILE="$2"
      shift 2
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 1
      ;;
  esac
done

if [[ -z "${DATA_FILE}" ]]; then
  DATA_FILE="${ROOT_DIR}/k6/consistency/data/${RUN_ID}.json"
fi

mkdir -p "$(dirname "${DATA_FILE}")"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker command is required." >&2
  exit 1
fi

psql_exec() {
  local db="$1"
  shift
  docker exec -i -e PGPASSWORD="${DB_PASSWORD}" "${POSTGRES_CONTAINER}" \
    psql -v ON_ERROR_STOP=1 -U "${DB_USER}" -d "${db}" "$@"
}

echo "[setup-fixture] run_id=${RUN_ID} count=${FIXTURE_COUNT} data_file=${DATA_FILE}"

psql_exec unbox_order \
  -v run_id="${RUN_ID}" \
  -v fixture_count="${FIXTURE_COUNT}" \
  -v buyer_id="${BUYER_ID}" \
  -v seller_id="${SELLER_ID}" <<'SQL'
WITH seed AS (
    SELECT
        gs AS seq,
        (
            substr(md5('order-' || :'run_id' || '-' || gs::text), 1, 8) || '-' ||
            substr(md5('order-' || :'run_id' || '-' || gs::text), 9, 4) || '-' ||
            substr(md5('order-' || :'run_id' || '-' || gs::text), 13, 4) || '-' ||
            substr(md5('order-' || :'run_id' || '-' || gs::text), 17, 4) || '-' ||
            substr(md5('order-' || :'run_id' || '-' || gs::text), 21, 12)
        )::uuid AS order_id,
        (
            substr(md5('product-' || :'run_id' || '-' || gs::text), 1, 8) || '-' ||
            substr(md5('product-' || :'run_id' || '-' || gs::text), 9, 4) || '-' ||
            substr(md5('product-' || :'run_id' || '-' || gs::text), 13, 4) || '-' ||
            substr(md5('product-' || :'run_id' || '-' || gs::text), 17, 4) || '-' ||
            substr(md5('product-' || :'run_id' || '-' || gs::text), 21, 12)
        )::uuid AS product_id,
        (
            substr(md5('product-option-' || :'run_id' || '-' || gs::text), 1, 8) || '-' ||
            substr(md5('product-option-' || :'run_id' || '-' || gs::text), 9, 4) || '-' ||
            substr(md5('product-option-' || :'run_id' || '-' || gs::text), 13, 4) || '-' ||
            substr(md5('product-option-' || :'run_id' || '-' || gs::text), 17, 4) || '-' ||
            substr(md5('product-option-' || :'run_id' || '-' || gs::text), 21, 12)
        )::uuid AS product_option_id,
        (10000 + ((gs - 1) % 10) * 10000)::numeric(19, 2) AS amount
    FROM generate_series(1, :fixture_count::int) gs
)
INSERT INTO p_orders (
    order_id,
    selling_bid_id,
    buying_bid_id,
    buyer_id,
    seller_id,
    product_option_id,
    product_id,
    price,
    status,
    receiver_name,
    receiver_phone,
    receiver_address,
    receiver_zip_code,
    buyer_name,
    product_name,
    model_number,
    product_option_name,
    product_image_url,
    brand_name,
    created_at,
    updated_at
)
SELECT
    seed.order_id,
    NULL,
    NULL,
    :buyer_id::bigint,
    :seller_id::bigint,
    seed.product_option_id,
    seed.product_id,
    seed.amount,
    'PAYMENT_PENDING',
    'Consistency Tester',
    '010-0000-0000',
    'Seoul',
    '12345',
    'buyer',
    'Consistency Product ' || seed.seq::text,
    'MODEL-' || lpad(seed.seq::text, 6, '0'),
    'Option ' || ((seed.seq - 1) % 5 + 1)::text,
    NULL,
    'UNBOX',
    NOW(),
    NOW()
FROM seed;
SQL

psql_exec unbox_payment \
  -v run_id="${RUN_ID}" \
  -v fixture_count="${FIXTURE_COUNT}" \
  -v buyer_id="${BUYER_ID}" \
  -v seller_id="${SELLER_ID}" <<'SQL'
WITH seed AS (
    SELECT
        gs AS seq,
        (
            substr(md5('order-' || :'run_id' || '-' || gs::text), 1, 8) || '-' ||
            substr(md5('order-' || :'run_id' || '-' || gs::text), 9, 4) || '-' ||
            substr(md5('order-' || :'run_id' || '-' || gs::text), 13, 4) || '-' ||
            substr(md5('order-' || :'run_id' || '-' || gs::text), 17, 4) || '-' ||
            substr(md5('order-' || :'run_id' || '-' || gs::text), 21, 12)
        )::uuid AS order_id,
        (
            substr(md5('payment-' || :'run_id' || '-' || gs::text), 1, 8) || '-' ||
            substr(md5('payment-' || :'run_id' || '-' || gs::text), 9, 4) || '-' ||
            substr(md5('payment-' || :'run_id' || '-' || gs::text), 13, 4) || '-' ||
            substr(md5('payment-' || :'run_id' || '-' || gs::text), 17, 4) || '-' ||
            substr(md5('payment-' || :'run_id' || '-' || gs::text), 21, 12)
        )::uuid AS payment_id,
        (10000 + ((gs - 1) % 10) * 10000)::numeric(19, 2) AS amount
    FROM generate_series(1, :fixture_count::int) gs
)
INSERT INTO p_payment (
    payment_id,
    payment_key,
    order_id,
    selling_bid_id,
    buying_bid_id,
    buyer_id,
    seller_id,
    method,
    amount,
    status,
    approved_at,
    ready_at,
    version,
    created_at,
    updated_at
)
SELECT
    seed.payment_id,
    NULL,
    seed.order_id,
    NULL,
    NULL,
    :buyer_id::bigint,
    :seller_id::bigint,
    'CARD',
    seed.amount,
    'READY',
    NULL,
    NOW(),
    0,
    NOW(),
    NOW()
FROM seed;
SQL

JSON_DATA="$(
  psql_exec unbox_payment \
    -t -A \
    -v run_id="${RUN_ID}" \
    -v fixture_count="${FIXTURE_COUNT}" \
    -v buyer_id="${BUYER_ID}" <<'SQL'
WITH seed AS (
    SELECT
        gs AS seq,
        (
            substr(md5('payment-' || :'run_id' || '-' || gs::text), 1, 8) || '-' ||
            substr(md5('payment-' || :'run_id' || '-' || gs::text), 9, 4) || '-' ||
            substr(md5('payment-' || :'run_id' || '-' || gs::text), 13, 4) || '-' ||
            substr(md5('payment-' || :'run_id' || '-' || gs::text), 17, 4) || '-' ||
            substr(md5('payment-' || :'run_id' || '-' || gs::text), 21, 12)
        )::uuid AS payment_id,
        (10000 + ((gs - 1) % 10) * 10000)::numeric(19, 2) AS amount
    FROM generate_series(1, :fixture_count::int) gs
)
SELECT json_agg(
    json_build_object(
        'paymentId', seed.payment_id::text,
        'paymentKey', 'test_success_' || :'run_id' || '_' || seed.seq::text,
        'amount', seed.amount::int,
        'buyerId', :buyer_id::int
    )
    ORDER BY seed.seq
)
FROM seed;
SQL
)"

if [[ -z "${JSON_DATA}" || "${JSON_DATA}" == "null" ]]; then
  echo "Failed to generate data.json from fixture seed." >&2
  exit 1
fi

printf '%s\n' "${JSON_DATA}" | jq '.' > "${DATA_FILE}"

cat <<EOF
[setup-fixture] completed
RUN_ID=${RUN_ID}
DATA_FILE=${DATA_FILE}
COUNT=${FIXTURE_COUNT}
EOF
