#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_SCRIPT="${SCRIPT_DIR}/run-sync-async.sh"

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<'EOF'
Usage:
  run-all-cases.sh [run-sync-async options]

Description:
  Executes both scenarios sequentially:
  1) caseA (delay=5000ms)
  2) caseB (delay=2000ms)

Examples:
  run-all-cases.sh
  run-all-cases.sh --payment-base-url http://localhost:8085/payment
  run-all-cases.sh --k6-bin k6
EOF
  exit 0
fi

echo "[run-all] caseA (delay=5000ms)"
"${RUN_SCRIPT}" \
  --case-name caseA \
  --delay-ms 5000 \
  "$@"

echo "[run-all] caseB (delay=2000ms)"
"${RUN_SCRIPT}" \
  --case-name caseB \
  --delay-ms 2000 \
  --skip-compose-up \
  --skip-data-gen \
  --skip-payment-recreate \
  "$@"

echo "[run-all] completed"
