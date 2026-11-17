# Milestone 4.1 change log

This update advances the DTEF 3.0 Milestone 4 validation package. It strengthens the execution and evidence gates; it does not represent a production release.

## Dependency and build pipeline

- Added a manual frontend dependency-refresh workflow. It updates exact Next.js/React versions, regenerates the npm lockfile on GitHub-hosted infrastructure, runs npm audit, typecheck, unit tests and the production build, then creates a PR only if all those checks pass.
- Added weekly Dependabot updates for npm, Maven and GitHub Actions.
- Added OWASP Dependency-Check Maven 13.0.0 to CI with a CVSS 7.0 failure threshold and retained HTML/JSON reports.
- CI now retains JUnit XML, Playwright traces, Compose logs, container build status and dependency scan reports.

## Integration and E2E

- Made Testcontainers mandatory in the JUnit integration suite; CI no longer accepts a silently skipped PostgreSQL/Redis/RabbitMQ test class.
- Added a JUnit report verifier requiring at least 19 tests, three infrastructure tests and zero skipped/failing/error tests.
- Added assertions for persisted application-document links, application status history, a real Redis round-trip and persistent RabbitMQ message delivery.
- Notification queue messages now explicitly use persistent delivery mode.

## Security and load testing

- Added written-authorization reference and exact HTTPS host checks to the active ZAP staging workflow.
- Added a penetration-test evidence template for independent assessor findings and retests.
- Defaulted local staged load tests to 100 VUs, blocked known live government hosts, required explicit remote host authorization and added separate gates above 1,000 and 5,000 VUs.
- Added a protected manual GitHub load-test workflow using k6 2.4.0 and retaining JSON summaries; token modes distinguish dedicated test-JWT staging from pre-issued OIDC bearer tokens.

## Government integration simulations

- Strengthened synthetic institution, social welfare, FNBB and notification request validation.
- Added positive bounded BWP amount checks, FNBB idempotency-key conflict handling, DTEF reference format checks and unknown household handling.
- Increased simulator tests from 7 to 15 and documented the synthetic contract boundary.

## Checks run while preparing this update

- 8 frontend unit/source-regression tests passed.
- 15 government simulator contract tests passed.
- 13 backend static security/configuration tests passed.
- JavaScript and shell syntax checks passed.
- GitHub Actions, Dependabot, Compose, Kubernetes YAML, JSON and Maven XML parsed successfully.
- Load-test safety checks rejected unapproved remote and high-concurrency runs.

## Still not executed

Full Next.js production build, full Maven/JUnit/Testcontainers execution, Playwright, Docker Compose runtime, container image builds, npm/Maven vulnerability audits, ZAP scans, independent penetration testing and k6 load stages remain pending because this preparation environment lacks Maven, Docker, k6 and npm registry access. The committed frontend lockfile and Spring Boot 3.4.3 parent remain release blockers. See `MILESTONE_4_TEST_RESULTS.md` and `DEPENDENCY_AUDIT.md`.
