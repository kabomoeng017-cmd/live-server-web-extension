#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

export DTEF_LOAD_SUBJECT="${DTEF_LOAD_SUBJECT:-student.loadtest}"
export DTEF_BASE_URL="${DTEF_BASE_URL:-http://127.0.0.1:8080}"
export MAX_VUS="${MAX_VUS:-100}"

node --input-type=module <<'NODE'
try {
  const target = new URL(process.env.DTEF_BASE_URL);
  const host = target.hostname.toLowerCase();
  const localHosts = new Set(['localhost', '127.0.0.1', '::1', '[::1]']);
  const productionHosts = new Set(['tef.gov.bw', 'www.tef.gov.bw', 'gov.bw', 'www.gov.bw']);
  const max = Number.parseInt(process.env.MAX_VUS, 10);
  if (!Number.isInteger(max) || max < 100 || max > 100000) throw new Error('MAX_VUS must be a whole number between 100 and 100000.');
  if (productionHosts.has(host)) throw new Error('Load testing is blocked for known live government hosts. Use an isolated staging host.');
  if (!localHosts.has(host)) {
    if (target.protocol !== 'https:' || target.username || target.password || target.search || target.hash) throw new Error('Non-local targets must be clean HTTPS staging URLs.');
    if (process.env.DTEF_LOAD_TEST_AUTHORIZED !== 'YES') throw new Error('Set DTEF_LOAD_TEST_AUTHORIZED=YES only after written authorization.');
    if (!process.env.DTEF_LOAD_ALLOWED_HOST || host !== process.env.DTEF_LOAD_ALLOWED_HOST.toLowerCase()) throw new Error('The target hostname must exactly match DTEF_LOAD_ALLOWED_HOST.');
    if (!process.env.DTEF_TOKEN && process.env.DTEF_LOAD_USE_TEST_JWT !== 'YES') throw new Error('Remote OIDC staging must provide a short-lived DTEF_TOKEN. Use a generated test JWT only when the isolated staging service explicitly trusts that test issuer and secret.');
    if (!process.env.DTEF_TOKEN && (!process.env.JWT_SECRET_BASE64 || !process.env.JWT_ISSUER)) throw new Error('Remote test-JWT mode requires explicit JWT_SECRET_BASE64 and JWT_ISSUER values; local demo defaults are never used remotely.');
  }
  if (max > 1000 && process.env.DTEF_HIGH_SCALE_APPROVED !== 'YES') throw new Error('Above 1000 VUs requires separate infrastructure approval: DTEF_HIGH_SCALE_APPROVED=YES.');
  if (max > 5000 && process.env.DTEF_EXTREME_SCALE_APPROVED !== 'YES') throw new Error('Above 5000 VUs requires explicit high-capacity approval: DTEF_EXTREME_SCALE_APPROVED=YES.');
} catch (error) {
  console.error(`Load-test safety check failed: ${error.message}`);
  process.exit(1);
}
NODE

if [[ "$(node -p 'new URL(process.env.DTEF_BASE_URL).hostname')" =~ ^(localhost|127\.0\.0\.1|::1|\[::1\])$ ]]; then
  export JWT_SECRET_BASE64="${JWT_SECRET_BASE64:-MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=}"
  export JWT_ISSUER="${JWT_ISSUER:-dtef-local-demo}"
fi

if ! command -v k6 >/dev/null 2>&1; then
  echo 'k6 is required. Install k6 before running the staged load test.' >&2
  exit 127
fi

if [[ -z "${DTEF_TOKEN:-}" ]]; then
  DTEF_TOKEN="$(node tests/load/generate-load-token.mjs)"
fi
export DTEF_TOKEN
trap 'unset DTEF_TOKEN' EXIT

STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
SUMMARY_FILE="artifacts/load/dtef-load-${STAMP}-max-${MAX_VUS}.json"
mkdir -p artifacts/load

echo "Running DTEF staged load test up to ${MAX_VUS} concurrent virtual users against ${DTEF_BASE_URL}."
echo 'Use synthetic data, isolated infrastructure, written authorization, and active monitoring.'
echo "k6 summary will be written to ${SUMMARY_FILE}."
k6 run --summary-export="$SUMMARY_FILE" \
  -e DTEF_BASE_URL="$DTEF_BASE_URL" \
  -e DTEF_TOKEN="$DTEF_TOKEN" \
  -e MAX_VUS="$MAX_VUS" \
  tests/load/k6-dtef-staged.js
