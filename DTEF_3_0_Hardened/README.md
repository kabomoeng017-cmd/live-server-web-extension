# DTEF 3.0 — Hardened Developer & Demonstration Workspace

This workspace is an upgraded continuation of the supplied `DTEF_2_0_Advanced_Portal_Source.zip`. It preserves the Botswana-inspired DTEF interface while correcting the high-risk prototype defects and adding a safer API, workflow validation, migrations, private document storage, deployment scaffolding and automated checks.

> **Important:** This is still a developer/proof-of-concept build, not an approved or production-ready government service. The local `demo` profile uses mock identity validation, simulated OCR and demo accounts. FNBB, official Omang, SMS, email, WhatsApp, government identity, institution registry, malware scanning and production AI integrations are not connected. Do not enter real Omang, bank or sensitive personal information.

## What has been improved

- Backend endpoints are authenticated by default and protected with role-based access (`STUDENT`, `OFFICER`, `INSTITUTION`, `FNBB`, `WELFARE`, `MINISTRY`, `ADMIN`).
- Demo login now requires a password and a cryptographically random, expiring OTP. OTPs are hashed in Redis, rate-limited, attempt-limited and never returned by the API. In the local demo only, the generated OTP is written to backend logs.
- Production profile uses an external OIDC issuer; demo login, mock Omang and simulated OCR controllers are restricted to the `demo` profile.
- Application references come from a PostgreSQL sequence, not an in-memory counter.
- Application submission checks ownership of identity-verification and document records, requires the required document types, and consumes a fresh submission OTP.
- Application status transitions are validated; unauthorized roles cannot access officer, institution, FNBB, welfare or ministry endpoints.
- Drafts are saved server-side and locally. Raw Omang is not saved to browser storage or the application record.
- Uploads are limited to PDF/JPG/PNG, 10 MB, checked against file signatures, stored under generated private keys, fingerprinted with SHA-256 and never exposed as public static files.
- Production document upload is disabled with HTTP 503 until an approved encrypted object-storage and malware/document verification adapter is connected. Demo uploads are labelled `DEMO_ACCEPTED` and must never be treated as production-verified.
- Flyway owns database schema changes; Hibernate uses `ddl-auto: validate`.
- Standard JSON API errors, security headers, CORS allow-list, health probes, Docker health checks, non-root containers and Kubernetes probes/HPA/PDB/network policy templates are included.
- Next.js navigation uses `Link`; API calls use same-origin `/api` routing; language and accessibility preferences persist between pages.
- The scripted help assistant is labelled honestly; illustrative figures are no longer presented as live government statistics.

## 1. Requirements

- Visual Studio Code
- Node.js 22 LTS (or a supported current Node 22 release)
- Java JDK 21
- Maven 3.9+
- Docker Desktop / Docker Engine with Compose v2
- Git
- Optional: k6, Postman, pgAdmin, kubectl, Helm

The build container installs its own Maven and Node toolchains. For local non-Docker development, install the versions above.

## 2. Start the complete local demonstration

From the project root:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Then start the services:

```bash
docker compose up --build -d
```

Check service status:

```bash
docker compose ps
```

Open:

- Frontend: http://localhost:3000
- Backend health: http://localhost:8080/api/health
- RabbitMQ management (local only): http://localhost:15672

PostgreSQL, Redis and RabbitMQ have health checks. The backend waits for them to become healthy, and the frontend waits for the backend health check.

### Demo sign-in

Choose a role on the sign-in page:

| Role | Demo username |
|---|---|
| Student | `student.demo` |
| DTEF officer | `officer.demo` |
| Institution | `institution.demo` |
| FNBB | `fnbb.demo` |
| Social welfare | `welfare.demo` |
| Ministry | `ministry.demo` |
| System admin | `admin.demo` |

Local demo password for all accounts: `DemoPass!2026`.

The OTP is random and expires after five minutes. To view it in this local-only demonstration, run:

```bash
docker compose logs -f backend
```

Find the line beginning `DEMO OTP`. Do not enable OTP logging or use demo credentials in production. The access token expires after 15 minutes.

### Student application demo

1. Sign in as `student.demo`.
2. Enter a test-only nine-digit value in the identity step. The demo validates the format only; it does not contact the national Omang register.
3. Upload a sample PDF/JPG/PNG admission letter.
4. Confirm or edit the institution and programme fields. OCR is simulated and is not a real extraction service.
5. Upload an Omang-copy test file and bank-confirmation test file.
6. Review the application and request a fresh submission OTP.
7. Find the OTP in backend logs and submit. The backend creates a database-sequence application reference.

The tracker only returns an application belonging to the signed-in student. The FNBB, institution and welfare workspaces display integration status and do not execute real transactions or eligibility decisions.

## 3. Run frontend and backend separately

Start infrastructure:

```bash
docker compose up -d postgres redis rabbitmq
```

Backend terminal (explicitly select the local demo profile):

```bash
cd backend
SPRING_PROFILES_ACTIVE=demo mvn spring-boot:run
```

On Windows PowerShell:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = "demo"
mvn spring-boot:run
```

The backend deliberately has no implicit demo profile. If no supported profile is selected, it fails to start rather than silently exposing demo authentication.

Frontend terminal:

```bash
cd frontend
npm ci
npm run dev
```

The browser calls `/api/...` on the same origin. Next.js rewrites those requests to `API_INTERNAL_BASE` (local default `http://localhost:8080/api`); the browser is not pointed at a user's own `localhost:8080`.

## 4. Automated checks and Milestone 4 validation

Fast local regression checks that do not require frontend package downloads:

```bash
./scripts/run-local-regression.sh
```

This runs the Node-based frontend, simulator, static security and CI-evidence tests plus syntax checks. It does not replace the Maven/Testcontainers, Playwright, production build, Docker, security or k6 gates below.


Frontend production checks:

```bash
cd frontend
npm ci
npm run typecheck
npm test
npm run build
npm audit --audit-level=high
```

Backend JUnit and infrastructure integration checks (requires Docker for Testcontainers):

```bash
cd backend
mvn -B clean verify
```

The Testcontainers suite starts disposable PostgreSQL 16, Redis 7 and RabbitMQ 3.13 services. Docker is mandatory for this suite: it is deliberately configured to fail rather than silently skip when the container runtime is unavailable. CI verifies that the infrastructure test report exists, contains all three integration tests and has zero skipped tests.

Browser end-to-end test (requires the local Compose demo stack and Chromium):

```bash
# From the project root
cp .env.example .env
docker compose up -d --build
cd frontend
npx playwright install chromium
npm run test:e2e
```

The Playwright journey uses synthetic values and reads demo OTPs from local backend logs. It verifies sign-in, demo identity, admission/supporting document uploads, submission OTP, application reference creation and the persisted tracker/status-history view.

Local smoke and simulator contract tests:

```bash
# From the project root
bash tests/smoke/run-smoke.sh
node --test tests/smoke/static-backend-audit.mjs
node --test tests/gov-sim/*.test.mjs
docker compose --profile integration-sim up -d government-simulator
```

The government simulator is available at `http://localhost:9090` only when its Compose profile is enabled. It returns synthetic test data for Omang, institution registry, social welfare, FNBB and notification gateway scenarios. The 15 contract tests include invalid references, bounded payment validation, idempotency conflicts, upstream failures and payload limits. It is not connected to official provider systems or production DTEF adapters.

Staged load test (install k6 first; use an isolated environment and begin at 100 users):

```bash
MAX_VUS=100 ./tests/load/run-staged-load.sh
MAX_VUS=500 ./tests/load/run-staged-load.sh
MAX_VUS=1000 ./tests/load/run-staged-load.sh
```

The runner defaults to 100 VUs and blocks known live government hosts. Remote targets must use HTTPS, match `DTEF_LOAD_ALLOWED_HOST` exactly and require `DTEF_LOAD_TEST_AUTHORIZED=YES` after written approval. More than 1,000 VUs requires `DTEF_HIGH_SCALE_APPROVED=YES`; more than 5,000 additionally requires `DTEF_EXTREME_SCALE_APPROVED=YES`. Each run writes a JSON summary to `artifacts/load/`. Do not load-test the live DTEF website or production services.

GitHub Actions includes a manual **Refresh patched frontend dependencies** workflow. It defaults to Next.js 16.4.6 and React 19.3.1, regenerates `package-lock.json` on a network-enabled runner, runs npm audit/typecheck/unit tests/production build, and creates a pull request only if all checks pass. The current committed lockfile remains unchanged until that workflow passes and its PR is reviewed/merged. Configure the protected `load-testing` environment with `DTEF_LOAD_ALLOWED_HOST` and `DTEF_LOAD_AUTHORIZATION_REFERENCE`; the workflow now requires the requested host to match that protected allow-list and requires explicit test-JWT trust confirmation when that token mode is selected. It passes the short-lived token to the k6 container through a permission-restricted temporary env file rather than expanding it into the Docker command line. Use `DTEF_LOAD_JWT_SECRET` plus `DTEF_LOAD_JWT_ISSUER` only for a dedicated test issuer trusted by that isolated staging deployment, or use a short-lived IdP-issued `DTEF_LOAD_BEARER_TOKEN` for OIDC staging.

Security checks: CI runs npm audit and OWASP Dependency-Check Maven (CVSS 7.0 release gate), plus a passive OWASP ZAP baseline against an isolated demo deployment. Configure the optional `NVD_API_KEY` GitHub secret to reduce NVD rate limiting. Active ZAP scans are manual-dispatch only and require written authorization, an authorization reference and a staging hostname allow-listed in the GitHub `security-testing` environment. See `tests/security/README.md`.

See `docs/GITHUB_REPOSITORY_SETUP.md` for the remote repository, Actions and protected-environment setup. See `docs/MILESTONE_4_TEST_PLAN.md` for acceptance gates, `docs/MILESTONE_4_TEST_RESULTS.md` for the evidence record, `docs/MILESTONE_4_2_CHANGELOG.md` for this update, `docs/GOVERNMENT_INTEGRATION_CONTRACTS.md` for simulator schemas and `docs/PENETRATION_TEST_EVIDENCE_TEMPLATE.md` for assessor sign-off. Adding a test or workflow does not mean that test has passed. The current Next.js 15.2.4 lockfile and Spring Boot 3.4.3 parent remain release blockers until their supported-version migrations are validated.

## 5. VS Code

Open `dtef-3.0.code-workspace`. Useful terminal tasks and recommended extensions are in `.vscode/`.

## 6. Production profile (not a turnkey switch)

Production profile uses an external OIDC issuer:

```text
SPRING_PROFILES_ACTIVE=prod
OIDC_ISSUER_URI=https://<approved-identity-provider>/issuer
OIDC_AUDIENCE=<DTEF API audience configured in the identity provider>
```

The identity provider must issue the configured `OIDC_AUDIENCE` and trusted role claims in the `roles` JWT claim, using values such as `ROLE_STUDENT`, `ROLE_OFFICER`, `ROLE_FNBB`, `ROLE_INSTITUTION`, `ROLE_WELFARE`, `ROLE_MINISTRY`, and `ROLE_ADMIN`. Enforce MFA/2FA at the identity provider and map verified user identifiers to DTEF accounts.

Before production, replace/configure all items in `docs/PRODUCTION_READINESS.md`. Kubernetes examples are in `infrastructure/k8s/`; they contain placeholder registry names, hostnames, storage class and secret values. Build the frontend image for the target cluster with `API_INTERNAL_BASE=http://dtef-backend:8080/api` if using the Next.js internal rewrite. When using the included Ingress path routing, `/api` is routed directly to the backend service.

## 7. Important implementation boundaries

- The `demo` profile is intentionally for demonstrations only. Never deploy it publicly.
- The sample JWT secret, demo password, database password and RabbitMQ password are local placeholders. Replace them using a secret manager.
- The application is not approved to access Omang, FNBB, government cloud, social welfare, university registries or official DTEF data.
- Document storage in the local demo is private filesystem storage but not KMS-encrypted or malware-scanned. The production document-upload endpoint is disabled until approved encrypted object storage, quarantine scanning and retention controls are connected.
- The frontend uses short-lived bearer tokens in `sessionStorage` for this demonstration. A production deployment should use the government's approved OIDC/BFF pattern with secure HttpOnly cookies, refresh-token rotation and CSRF protection.
- No capacity claim is made. The 50,000+ concurrent-user target has not been load-tested or independently verified.

See `docs/MILESTONE_4_TEST_RESULTS.md` for the exact checks that could be executed in this build environment and the checks blocked by missing Docker/Maven/network access.
