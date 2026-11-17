# Milestone 4.2 change log — runtime readiness corrections

This update addresses issues found while reviewing the Milestone 4.1 execution pipeline. It improves the test harness and protects the external test workflows; it does not represent a completed production build or release.

## Corrected build execution

- Corrected `backend/Dockerfile`: the image build now compiles/packages the application with `-DskipTests`. The dedicated CI job remains responsible for running the full JUnit/Testcontainers suite against the runner's Docker daemon. This avoids attempting to start PostgreSQL, Redis and RabbitMQ containers from inside a Docker image build where the Docker socket is not mounted.
- Added CI regression tests that prevent the backend Dockerfile from reintroducing Testcontainers execution during `docker build`.

## Stronger JUnit evidence

- Added automated tests for the Surefire XML verifier.
- The verifier is now fail-closed when any report lacks a valid `<testsuite>` root instead of silently ignoring that report.
- Regression coverage verifies successful complete evidence, missing infrastructure report, skipped integration tests, malformed XML, failures and incomplete totals.

## Safer load-test workflow

- The GitHub load workflow now takes its authoritative host from the protected `load-testing` environment variable `DTEF_LOAD_ALLOWED_HOST`; the operator-entered host and URL must match it exactly.
- Requires a protected `DTEF_LOAD_AUTHORIZATION_REFERENCE` before a run can start.
- Adds an explicit trust acknowledgement when using a dedicated test JWT. A short-lived IdP bearer token remains the alternative.
- Passes the short-lived load token to the k6 container through a permission-restricted temporary environment file rather than expanding the token into the Docker command line. The file is removed after the run.
- Added workflow contract tests for the allow-list, authorization reference, test-JWT acknowledgement and token handoff.

## Security regression coverage

- Added a static regression check that application status transitions are protected by the officer/admin role rule.
- Hardened the load-token generator to reject HS256 secrets shorter than the backend minimum and added signature/claims/expiry tests for the synthetic token.
- Added `docs/GITHUB_REPOSITORY_SETUP.md` with the steps to create the remote repository, enable Actions, protect the default branch and configure the `security-testing` and `load-testing` environments.

## Checks executed in this preparation environment

- Frontend Node unit/source tests: 8 passed.
- Government simulator contracts: 15 passed.
- Backend static security/configuration tests: 14 passed.
- CI evidence/workflow regression tests: 9 passed.
- **Total executed Node tests: 46 passed, 0 failed.**
- Node JavaScript syntax, shell syntax, TypeScript/TSX syntax parsing, JSON, XML and YAML syntax checks were also run where supported.

## Still pending

The current environment has no Maven, Docker or k6 executable. npm registry downloads fail with `EAI_AGAIN` and the required packages are not available offline. Therefore the frontend production build, actual JUnit/Testcontainers execution, Playwright, Docker Compose, container image builds, dependency audits, ZAP scans, independent penetration test and k6 stages remain pending. The project is not connected to an authenticated GitHub repository, so hosted workflows have not been started. Official government-provider sandboxes have not been connected. See `MILESTONE_4_TEST_RESULTS.md` for the current evidence matrix.
