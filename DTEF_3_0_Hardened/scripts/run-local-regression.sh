#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

command -v node >/dev/null 2>&1 || { echo 'Node.js 22+ is required.' >&2; exit 127; }
NODE_MAJOR="$(node -p 'Number(process.versions.node.split(".")[0])')"
if (( NODE_MAJOR < 22 )); then echo 'Node.js 22+ is required.' >&2; exit 2; fi

echo '== Frontend unit/source tests =='
(cd frontend && npm test)

echo '== Government simulator contract tests =='
node --test tests/gov-sim/*.test.mjs

echo '== Backend static security/configuration tests =='
node --test tests/smoke/static-backend-audit.mjs

echo '== CI evidence/workflow regression tests =='
node --test tests/ci/*.test.mjs

echo '== JavaScript syntax checks =='
node --check tests/gov-sim/server.mjs
node --check tests/gov-sim/simulator.test.mjs
node --check tests/load/generate-load-token.mjs
node --check tests/ci/load-token-generator.test.mjs
node --check tests/ci/assert-maven-reports.mjs
node --check tests/ci/assert-maven-reports.test.mjs
node --input-type=module --check < tests/load/k6-dtef-staged.js
node --input-type=module --check < tests/load/k6-dtef.js

echo '== Shell syntax checks =='
bash -n tests/load/run-staged-load.sh tests/smoke/run-smoke.sh infrastructure/k8s/build-images.sh

echo '== JSON and XML syntax checks =='
python3 - <<'PY'
import json
import xml.etree.ElementTree as ET
from pathlib import Path
for name in ('frontend/package.json', 'frontend/package-lock.json', 'frontend/tsconfig.json'):
    json.loads(Path(name).read_text())
ET.parse('backend/pom.xml')
print('Parsed 3 JSON files and backend/pom.xml')
PY

echo 'Local regression checks completed. This does not execute Maven/JUnit, Playwright, Docker, ZAP or k6.'
