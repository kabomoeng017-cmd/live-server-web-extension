# DTEF 3.0 Milestone 4.2 execution record

**Overall status: LOCAL REGRESSION TESTS PASS — HOSTED RUNTIME AND RELEASE EXECUTION STILL PENDING**

This record separates tests that were actually executed in the preparation environment from tests that are only configured for a connected CI/staging environment.

## Environment inspected

- Node.js `v22.16.0`
- npm `10.9.2`
- Java `21.0.12`
- Maven: not installed
- Docker / Docker Compose: not installed
- k6: not installed
- npm registry DNS: unavailable (`EAI_AGAIN` resolving `registry.npmjs.org`); an online `npm ci` attempt timed out while fetching React/scheduler/PostCSS packages
- Offline npm install: blocked because required tarballs (including `playwright-core`) are not cached
- Browser/package downloads: unavailable
- GitHub remote repository / authenticated GitHub connection: not configured

## Checks actually executed in this preparation environment

| Check | Result | Evidence |
|---|---|---|
| Frontend unit and source-regression tests | PASS | 8 Node tests passed; 0 failed |
| Government simulator contract tests | PASS | 15 Node tests passed; 0 failed |
| Backend static security/configuration tests | PASS | 14 Node tests passed; 0 failed |
| CI evidence and workflow safety regression tests | PASS | 9 Node tests passed; 0 failed; covers malformed/missing/skipped JUnit reports, Docker build test isolation and load workflow guardrails |
| Load-test safety gates | PASS | Unapproved remote target, missing remote identity token and >1,000 VU run without approval were rejected before any HTTP load was generated |
| JavaScript syntax checks | PASS | Simulator, tests, load-token generator, k6 script and JUnit-report verifier parsed |
| TypeScript / TSX syntax | PASS (syntax only) | TypeScript transpiler parsed 24 `.ts` / `.tsx` files; this is not a full framework-aware typecheck |
| Framework-aware TypeScript typecheck | BLOCKED | `tsc --noEmit` cannot resolve React, Next.js, Playwright and Node type packages because `npm ci` could not complete; install dependencies on a network-enabled runner before judging type errors |
| Shell syntax checks | PASS | Load runner and smoke/Kubernetes shell scripts parsed |
| GitHub Actions / Dependabot / Compose / Kubernetes YAML | PASS | YAML parsed; Kubernetes multi-document manifests parsed individually |
| Frontend package/config JSON and backend Maven XML | PASS | JSON and XML parsed successfully |

**Executed automated test total: 46 passed, 0 failed.** This total does not include JUnit, Playwright, production builds, infrastructure runtime, penetration or load tests.

## Validation harness changes across Milestones 4.1 and 4.2

- Removed the Testcontainers `disabledWithoutDocker` behavior from the mandatory PostgreSQL/Redis/RabbitMQ integration suite. The CI job now fails if Docker is unavailable rather than silently skipping integration tests.
- Added JUnit XML evidence verification. CI requires the infrastructure suite report, at least three executed infrastructure tests, at least 19 total JUnit tests and zero skipped/failing/error tests.
- Expanded the infrastructure integration assertions to verify PostgreSQL persistence of all three application-document links and status history, an actual Redis round-trip, and persistent RabbitMQ message delivery.
- Added explicit persistent delivery mode to notification queue messages.
- Strengthened the synthetic government simulator: required-reference validation, positive bounded BWP payment amounts, FNBB idempotency-key conflict handling, notification template validation, malformed JSON and payload-size handling.
- Increased government simulator contract tests from 7 to 15, including malformed DTEF references and unknown household handling.
- Added a frontend dependency-refresh workflow. It regenerates the npm lockfile on a connected GitHub runner and opens a pull request only after audit, typecheck, frontend tests and production build pass.
- Added weekly Dependabot update configuration for npm, Maven and GitHub Actions.
- Added OWASP Dependency-Check Maven 13.0.0 to CI with a CVSS 7.0 failure threshold and report artifact retention.
- Tightened load-test safety: default 100 VUs, exact staging-host allow-list, HTTPS and written-authorization gates for remote hosts, separate approval flags above 1,000 and 5,000 VUs, and JSON summary export.
- Added a manual GitHub load-test workflow with a protected `load-testing` environment and a pinned k6 release.
- Strengthened active ZAP scan controls with a written authorization reference and exact HTTPS staging host allow-list.
- Added government integration contract documentation and a penetration-test evidence/sign-off template.
- Corrected the backend Dockerfile to compile/package without executing Testcontainers inside `docker build`; the dedicated CI JUnit job runs the real integration suite against the runner Docker daemon.
- Added nine CI evidence/workflow regression tests and made the JUnit verifier reject malformed XML reports instead of silently ignoring them.
- Hardened the load workflow to compare the requested host against protected `DTEF_LOAD_ALLOWED_HOST`, require `DTEF_LOAD_AUTHORIZATION_REFERENCE`, require explicit trust confirmation for test JWT mode, and pass the token through a permission-restricted temporary env file.
- Added an officer/admin role regression test for application status transitions and GitHub repository/environment setup instructions.
- Hardened the test-token generator to enforce a minimum 32-byte HS256 key and added tests for signature, audience, role, expiry and rejection of short secrets.
- Added `scripts/run-local-regression.sh` to reproduce the locally executable test suites without downloading frontend packages.

## Outstanding release gates — not executed

| Gate | Current status | Required evidence |
|---|---|---|
| GitHub repository creation/push | PENDING | Owner-created private GitHub repository, successful initial push, enabled Actions and protected default branch |
| Frontend dependency refresh | PENDING | Successful GitHub dependency-refresh workflow, reviewed package.json/lockfile PR and npm audit report |
| Frontend production typecheck/build | PENDING | Successful `npm ci`, `npm run typecheck`, `npm test`, `npm run build` logs |
| Backend production build | PENDING | Successful `mvn clean verify` and packaged JAR |
| Complete JUnit suite | PENDING | Surefire XML report with all tests passed and none skipped |
| PostgreSQL, Redis and RabbitMQ integration | PENDING | Testcontainers report proving all three services ran and all assertions passed |
| Playwright browser E2E | PENDING | Chromium HTML report/traces showing submission, tracker/history, role-denial, invalid OTP and spoofed-upload journeys passed |
| Application/document E2E | PENDING | Created synthetic application reference, three durable document links and tracker/history evidence |
| Docker Compose / container image builds | PENDING | Successful Compose health checks and backend/frontend image build logs |
| npm and Maven vulnerability audits | PENDING | Current resolved dependency reports with no unresolved Critical/High findings |
| Passive ZAP baseline | PENDING | ZAP HTML/JSON artifact from isolated demo stack |
| Active security scan / independent penetration test | PENDING | Written authorization, allow-listed staging report, independent manual findings and retest evidence |
| 100-user load stage | PENDING | k6 summary plus API/database/cache/queue and host telemetry |
| 500-user load stage | PENDING | Same evidence and stable result |
| 1,000-user load stage | PENDING | Same evidence and stable result |
| 2,000 / 5,000 / 10,000-user stages | PENDING | Separate capacity approval, repeated k6 summaries and recovery telemetry |
| Government integration contract simulation | PARTIAL | 15 simulator contract tests pass; official provider adapter contract tests and owner-approved sandbox checks remain pending |
| Protected GitHub environments | PENDING | `security-testing` and `load-testing` configured with required reviewers and protected allow-list/authorization variables |

## Known dependency release blockers

- Frontend lockfile still resolves Next.js `15.2.4` and React/React DOM `19.0.0`. The dependency-refresh workflow defaults to Next.js `16.4.6` and React `19.3.1`, but it has not run in this disconnected environment. Do not call the framework upgrade complete until its generated lockfile passes all gates and is reviewed.
- Backend parent POM still uses Spring Boot `3.4.3`. A controlled migration to the supported Spring Boot 4.1.x line is required before production clearance; see `docs/DEPENDENCY_AUDIT.md`.

## Reproduction commands

```bash
# Frontend (network-enabled Node 22 environment)
cd frontend
npm ci
npm run typecheck
npm test
npm run build
npm audit --audit-level=high

# Backend (Java 21, Maven 3.9+, Docker available)
cd ../backend
mvn -B -ntp clean verify
node ../tests/ci/assert-maven-reports.mjs target/surefire-reports

# Browser E2E (Docker Compose and Chromium available)
cd ..
cp .env.example .env
docker compose up -d --build
cd frontend
npx playwright install --with-deps chromium
npm run test:e2e

# Synthetic government provider contracts
cd ..
node --test tests/gov-sim/*.test.mjs
```

A static/source test passing is not evidence of a production build, live integration, security certification or capacity. Do not mark any pending gate as passed until the actual CI/staging evidence is retained and reviewed.
