# DTEF 3.0 — Milestone 4.2 branch

This branch keeps the existing Live Server Web Extension project on `master` untouched while preparing the DTEF 3.0 project in its own branch.

## Where the DTEF source lives

The first GitHub Actions run verifies the uploaded Milestone 4.2 ZIP by SHA-256, extracts it, and commits the source into:

`DTEF_3_0_Hardened/`

The extraction step only writes to this branch. It does not modify `master` or replace the Live Server extension files.

## Automated checks

The root workflow at `.github/workflows/dtef-ci.yml` runs:

- Source, simulator, configuration and security regression checks
- Frontend dependency installation, TypeScript typecheck, unit tests and Next.js production build
- Backend Maven/JUnit and Testcontainers verification
- Docker Compose startup and Playwright student-journey tests
- Docker image builds

The source-extraction step runs once. Later pushes to this branch run the tests against the committed DTEF source.

## Important integration and safety limits

DTEF 3.0 remains a development/proof-of-concept build. Official Omang, FNBB, institution registry, welfare, SMS, email and WhatsApp services are not connected. Use only synthetic test data. Do not run active security scans or load tests against live government or banking systems. Those require written authorization and an isolated staging environment.

See `DTEF_3_0_Hardened/README.md` and `DTEF_3_0_Hardened/docs/PRODUCTION_READINESS.md` for the project details and readiness limits.
