# Milestone 4.3 change log — institution controls and document safety

## Institution registry workflow

- Added a persisted institution catalogue with pending, verified, demo-verified and suspended states.
- Added a student-readable catalogue containing only eligible records.
- Added admin registration, manual verification and suspension controls.
- Manual verification uses a maker-checker rule: the administrator who registers an institution cannot verify their own record.
- The demo-only University of Botswana fixture is marked `DEMO_VERIFIED`; it is not evidence of an official registry connection.
- Application creation now requires an eligible registered institution and stores the canonical institution name.

## Officer review and application status controls

- Added an officer workspace for application queues, document metadata, status history and controlled status decisions.
- Added a document download flow with a SHA-256 integrity check and audit events.
- Application transitions to `DOCUMENTS_VERIFIED` now require all linked document records to exist and be in an allowed scan state.
- Production accepts only `CLEAN`; demo accepts `DEMO_ACCEPTED` only under the demo profile.
- Pending-scan, quarantined, missing or mismatched documents cannot pass the gate.

## Malware-scanning contract

- Added the `DocumentScanner` interface and explicit clean/infected verdicts.
- Added a production-only ClamAV TCP `INSTREAM` adapter, activated only by `DTEF_ANTIVIRUS_ENABLED=true`.
- Added a disabled-by-default production scanner that throws a fail-closed exception.
- Added protocol tests for clean/infected replies, chunk framing, unknown responses, invalid inputs and scanner unavailability.
- The scanner is not wired to the document upload path. Production upload and officer download endpoints remain unavailable until encrypted object storage and scan orchestration are implemented.

## Hosted CI evidence

The full DTEF branch CI passed at commit `14c482bed9f9d7c33e80cf0266b8b7178ed8cef0` on 18 November 2026:

- 51 JUnit tests passed, with zero failures, errors or skips. This includes four infrastructure integration tests using Testcontainers and six ClamAV adapter tests using a fake daemon.
- Frontend locked install, TypeScript typecheck, unit tests and Next.js production build passed.
- 55 source/simulator/security/workflow regression tests passed.
- Docker Compose validation and backend/frontend image builds passed.
- All five Chromium Playwright journeys passed: student submission/tracker, student role denial, spoofed upload and invalid OTP, officer document review/status decision, and independent administrator institution verification.
- Backend OSV dependency audit completed with no issues found.

Evidence: [DTEF 3.0 branch CI run](https://github.com/kabomoeng017-cmd/live-server-web-extension/actions/runs/44247409279) and [backend dependency audit](https://github.com/kabomoeng017-cmd/live-server-web-extension/actions/runs/44247409696).

## Still not production-ready

This milestone does not connect Omang, FNBB, official institution registry, social welfare, SMS/email/WhatsApp providers, real ClamAV, production object storage, or KMS. It does not constitute a penetration test, accessibility certification, load/capacity test or government security approval. See `PRODUCTION_READINESS.md` and `DOCUMENT_SCANNING.md`.
