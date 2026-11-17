# DTEF 3.0 Upgrade Notes

## Critical fixes implemented in source

- Removed public access to all API endpoints; default is authenticated and role-specific.
- Replaced fixed OTP `246810` with random, expiring, hashed OTP stored in Redis, attempt caps and rate limits.
- Bound demo OTP challenges to the server-side username so a challenge cannot be completed as another role.
- Restricted mock Omang and OCR controllers to the `demo` profile; production routes fail closed until authorized providers are connected.
- Replaced the in-memory application reference counter with a PostgreSQL sequence.
- Added owner checks for identity verification, documents, application reads and status history.
- Added required document validation and a fresh OTP for application submission.
- Added status transition rules, persisted application status history and durable links from each submitted application to its admission and supporting documents.
- Added JSON error handling and structured security 401/403 responses.

## High-priority fixes implemented in source

- Added Flyway migrations and changed Hibernate schema mode to `validate`.
- Added demo-only private generated-key document storage, content-type/signature checks, file-size limits and SHA-256 fingerprints; production uploads now fail closed until encrypted object storage and malware scanning are integrated.
- Added server-side drafts, database indexes, audit events, Redis OTP/identity state and a durable RabbitMQ notification queue.
- Added Docker health checks, non-root containers, Kubernetes probes, HPA, PDB, network policy and TLS ingress templates.
- Corrected browser API routing to same-origin `/api` and server-side rewrite; restored Next.js `Link` navigation.
- Removed hard-coded demo student details and fake document checkmarks from the application wizard.

## Medium-priority fixes implemented in source

- Persisted language, large-text and high-contrast preferences; update HTML `lang`.
- Added skip link, focus states, live regions, error/loading/not-found states and a CSS token for the previously undefined coral color.
- Removed duplicate `metadata.tsx`; root layout owns metadata.
- Renamed the scripted assistant so it does not claim to be AI.
- Replaced hard-coded dashboard KPIs with API-backed empty/live placeholders and explicit integration notices.

## Remaining external work

See `PRODUCTION_READINESS.md`. Official identity, FNBB, institution, SMS/email/WhatsApp, OCR, malware scanning, encrypted object storage, full translation, production OIDC/BFF, load testing and independent security/accessibility reviews are not implemented or approved in this source.
