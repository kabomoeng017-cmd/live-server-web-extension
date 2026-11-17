#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${DTEF_BASE_URL:-http://localhost:3000}"
API_URL="${DTEF_API_URL:-http://localhost:8080}"

printf '1/4 API health... '
curl --fail --silent --show-error "$API_URL/api/health" | grep -q '"status":"UP"'
echo OK

printf '2/4 Frontend homepage... '
curl --fail --silent --show-error "$BASE_URL/" | grep -qi 'DTEF'
echo OK

printf '3/4 Protected API rejects anonymous access... '
code="$(curl --silent --output /tmp/dtef-smoke-auth.json --write-out '%{http_code}' "$API_URL/api/analytics/summary")"
if [[ "$code" != "401" ]]; then echo "FAILED (expected 401, got $code)"; exit 1; fi
echo OK

printf '4/4 Demo login endpoint is present... '
code="$(curl --silent --output /tmp/dtef-smoke-login.json --write-out '%{http_code}' -H 'Content-Type: application/json' -d '{"username":"student.demo","password":"wrong","role":"STUDENT"}' "$API_URL/api/auth/demo-login")"
if [[ "$code" != "401" ]]; then echo "FAILED (expected 401 for invalid credentials, got $code)"; exit 1; fi
echo OK

echo 'Smoke tests passed. This does not replace integration, penetration or load testing.'
