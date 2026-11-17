> Historical V3 preparation record. The latest test counts, Milestone 4.1 changes and pending release gates are recorded in `MILESTONE_4_TEST_RESULTS.md`; use that file as the current evidence record.

# DTEF 3.0 test execution record

Environment inspected during this build:

- Node.js v22.16.0
- npm v10.9.2
- Java 21.0.12
- Maven: not installed in the build environment
- Docker: not installed in the build environment
- Outbound DNS/network access: unavailable during dependency installation

## Executed checks

| Check | Result | Detail |
|---|---|---|
| Frontend workflow unit tests | PASS | 4 tests: Omang shape, OTP shape, required supporting documents, wizard step guards |
| Frontend source regression checks | PASS | 4 tests: same-origin API proxy, Next Link navigation, wizard guards/no raw Omang persistence, scripted assistant labelling |
| TypeScript source parse/type-shape audit | PASS (limited) | Global TypeScript 5.8.3 passed with temporary framework declaration stubs; this is not a substitute for the real Next.js typecheck with installed React/Next typings |
| Backend static security/config checks | PASS | 10 static tests covering explicit profile selection, default authentication, role rules, random/hashed/locked OTP, draft PII restrictions, database sequence references, Flyway validation, production fail-closed settings, OIDC audience validation and demo-only identity/OCR |
| Kubernetes/Docker/Spring YAML parse | PASS | Compose, Spring profiles and all 7 Kubernetes YAML files parsed successfully with PyYAML |
| Maven project XML parse | PASS | `backend/pom.xml` parsed successfully |
| CSS token audit | PASS | No undefined CSS custom properties remain |
| JSON/JavaScript/shell syntax | PASS | Package/config JSON, frontend helper/tests, k6 script and shell scripts parsed successfully |
| Java syntax parse | PASS (limited) | JDK compiler reported missing external dependencies but no Java syntax diagnostics; full compile remains blocked without Maven dependencies |
| Maven/JUnit test suite | NOT RUN | Maven is not installed and dependency download was blocked by unavailable network/DNS |
| npm dependency vulnerability audit | NOT RUN | npm registry access was unavailable; CI includes `npm audit --audit-level=high` as a release gate |
| Spring Boot runtime | NOT RUN | Requires Maven-built JAR plus PostgreSQL, Redis and RabbitMQ; Docker is not installed here |
| Next.js typecheck/build | BLOCKED | npm dependency install timed out; offline install reported uncached `undici-types` package. The partial `node_modules` tree was not treated as a valid install |
| Docker Compose integration test | NOT RUN | Docker/Compose is not installed in this environment |
| k6 load test | NOT RUN | k6 and a running staging stack are not available; no concurrency claim is made |
| Penetration test / WCAG audit | NOT RUN | Requires independent tools and manual review on a deployed staging environment |

## Reproduction commands

```bash
cd frontend
npm ci
npm run typecheck
npm test
npm run build

cd ../backend
mvn clean verify

cd ..
docker compose up --build -d
bash tests/smoke/run-smoke.sh
node --test tests/smoke/static-backend-audit.mjs
```

A successful static test run is not equivalent to a successful full runtime or production acceptance test. Run the blocked checks in a connected developer/staging environment before pitching the build as deployable.

## Milestone 4 validation harness additions

The following test infrastructure has since been added to the project:

- JUnit/Testcontainers integration class for PostgreSQL 16, Redis 7, RabbitMQ 3.13, OTP flow, document uploads, application submission, tracker/history and role enforcement.
- Playwright Chromium end-to-end suite for the complete synthetic student application journey and a student-to-officer access denial check.
- CI workflow jobs for frontend production build, backend Maven/JUnit/Testcontainers verification, Docker image builds, browser E2E and simulator contracts.
- OWASP ZAP passive scan workflow and a separately gated active scan workflow requiring explicit written-authorization confirmation and an exact staging-host allow-list.
- Staged k6 script with 100, 500, 1,000, 2,000, 5,000, 10,000 and custom maximum concurrency stages.
- Government integration simulator with contract tests for synthetic Omang, institution, social welfare, FNBB and notification endpoints.

These additions are test infrastructure only. Their runtime suites have not been executed in this build environment; see `MILESTONE_4_TEST_RESULTS.md` for the current evidence status.
