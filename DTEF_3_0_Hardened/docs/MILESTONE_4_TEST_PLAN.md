# DTEF 3.0 — Milestone 4: Production Build, Integration, Security and Capacity Validation

## Objective

Turn the hardened V3 developer workspace into an evidence-backed release candidate by running real production builds, complete automated tests, end-to-end workflows, authorized security testing, staged capacity tests and government-provider contract simulations.

This milestone does not authorize production deployment or establish access to government, identity, banking or institution systems.

## Workstreams and acceptance gates

| Workstream | Automated coverage | Acceptance gate |
|---|---|---|
| 1. Production builds and dependency remediation | Frontend dependency-refresh workflow; Next.js typecheck/build; Maven clean verify/package; Docker image builds | Patched dependency PR reviewed; both production builds and both container image builds succeed; logs and artifacts retained |
| 2. JUnit and Playwright | JUnit unit tests plus mandatory Testcontainers integration; Chromium journeys for submission, role denial, invalid OTP and spoofed PDF | All JUnit tests pass; CI asserts infrastructure tests were executed and none skipped; Playwright journeys pass |
| 3. Infrastructure integration | Disposable PostgreSQL 16, Redis 7 and RabbitMQ 3.13 containers | Flyway reaches V3; application and document links persist; Redis OTP path works; RabbitMQ accepts persistent messages |
| 4. Submission and upload E2E | Browser-driven student login, synthetic identity, three document uploads, OTP and submission | Application reference is created; three documents are linked in PostgreSQL; tracker/history show persisted state; OTP replay is rejected; signature/size/ownership rules pass |
| 5. Security | npm audit, Maven dependency scan, passive ZAP baseline, allow-listed active ZAP scan and independent manual penetration test | No unresolved Critical/High findings; Medium findings have signed-off remediation plans; retest evidence stored |
| 6. Capacity | k6 progressive stages at 100, 500, 1,000, 2,000, 5,000 and 10,000 VUs | Each approved stage has stable error/latency and infrastructure telemetry; repeat runs and recovery evidence archived; high stages separately authorized |
| 7. Government simulations | Synthetic contract endpoints for Omang, institutions, social welfare, FNBB and notification gateway | Success, invalid input, idempotency, upstream delay and outage cases pass; production adapters separately validated against approved official sandboxes |

## Test layers

1. **Unit tests:** fast JUnit and Node tests for business rules, validation and security regressions.
2. **Infrastructure integration:** Testcontainers starts isolated PostgreSQL, Redis and RabbitMQ. Docker is mandatory in CI; the suite fails rather than silently skipping if Docker is unavailable.
3. **Browser E2E:** Playwright uses the local demo stack and retrieves demo OTPs only from local backend logs. Use synthetic values only.
4. **Contract simulation:** the `integration-sim` Compose profile exposes synthetic provider APIs on port 9090. It is a simulator, not a live government connector.
5. **Security validation:** passive scans can run in CI; active scans require written authorization, HTTPS, exact hostname allow-listing and protected GitHub environment approval.
6. **Performance validation:** k6 performs authenticated read-only requests. The default is 100 VUs. Remote tests require written authorization, an authorization reference stored in the protected `load-testing` environment, and exact matching between the requested host and protected `DTEF_LOAD_ALLOWED_HOST`. Test-JWT mode requires explicit confirmation that the isolated staging system trusts the dedicated issuer/secret; otherwise use a short-lived IdP bearer token. More than 1,000 and 5,000 VUs require separate approvals.

## Commands

```bash
# Frontend production checks
cd frontend
npm ci
npm run typecheck
npm test
npm run build
npm audit --audit-level=high
npx playwright install --with-deps chromium

# Backend JUnit + mandatory Testcontainers integration suite
cd ../backend
mvn -B -ntp clean verify
node ../tests/ci/assert-maven-reports.mjs target/surefire-reports

# Local end-to-end browser journey (requires Docker Compose)
cd ..
cp .env.example .env
docker compose down -v --remove-orphans
docker compose up -d --build
cd frontend
npm run test:e2e

# CI evidence and workflow safety regressions
cd ..
node --test tests/ci/*.test.mjs

# Government integration simulator contracts
node --test tests/gov-sim/*.test.mjs
docker compose --profile integration-sim up -d government-simulator

# Staged load test: increase only after previous stage and owner approval
MAX_VUS=100 ./tests/load/run-staged-load.sh
MAX_VUS=500 ./tests/load/run-staged-load.sh
MAX_VUS=1000 ./tests/load/run-staged-load.sh
```

## GitHub setup

Follow `docs/GITHUB_REPOSITORY_SETUP.md` to create and push the remote repository, enable Actions, protect the default branch and configure the `security-testing` and `load-testing` environments. These steps require an authenticated repository owner.

## Exit decision

Milestone 4 is accepted only when the frontend/backend production builds, full JUnit suite, Playwright workflow, PostgreSQL/Redis/RabbitMQ integration tests, security review and agreed load-test stages have actual evidence attached to the release record. No test may be marked passed because a script or workflow exists. Government provider simulations are not official integrations or authorization to exchange real data.
