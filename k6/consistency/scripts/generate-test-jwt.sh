#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

USER_ID=1
EMAIL="buyer1@unbox.com"
ROLE="ROLE_USER"
EXPIRES_IN_SECONDS=3600
JWT_SECRET="${JWT_SECRET:-${SPRING_JWT_SECRET:-}}"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

JWT_SECRET="${JWT_SECRET:-${SPRING_JWT_SECRET:-}}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --user-id)
      USER_ID="$2"
      shift 2
      ;;
    --email)
      EMAIL="$2"
      shift 2
      ;;
    --role)
      ROLE="$2"
      shift 2
      ;;
    --expires-in)
      EXPIRES_IN_SECONDS="$2"
      shift 2
      ;;
    --secret)
      JWT_SECRET="$2"
      shift 2
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 1
      ;;
  esac
done

if [[ -z "${JWT_SECRET}" ]]; then
  echo "JWT secret is missing. set SPRING_JWT_SECRET in .env or pass --secret." >&2
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required." >&2
  exit 1
fi

if ! command -v openssl >/dev/null 2>&1; then
  echo "openssl is required." >&2
  exit 1
fi

b64url() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

NOW_EPOCH="$(date +%s)"
EXP_EPOCH="$((NOW_EPOCH + EXPIRES_IN_SECONDS))"

HEADER='{"alg":"HS256","typ":"JWT"}'
PAYLOAD="$(
  jq -cn \
    --argjson userId "${USER_ID}" \
    --arg email "${EMAIL}" \
    --arg role "${ROLE}" \
    --argjson iat "${NOW_EPOCH}" \
    --argjson exp "${EXP_EPOCH}" \
    '{userId:$userId,email:$email,role:$role,iat:$iat,exp:$exp}'
)"

ENC_HEADER="$(printf '%s' "${HEADER}" | b64url)"
ENC_PAYLOAD="$(printf '%s' "${PAYLOAD}" | b64url)"
SIGNING_INPUT="${ENC_HEADER}.${ENC_PAYLOAD}"
ENC_SIGNATURE="$(
  printf '%s' "${SIGNING_INPUT}" \
    | openssl dgst -binary -sha256 -hmac "${JWT_SECRET}" \
    | b64url
)"

printf '%s.%s.%s\n' "${ENC_HEADER}" "${ENC_PAYLOAD}" "${ENC_SIGNATURE}"
