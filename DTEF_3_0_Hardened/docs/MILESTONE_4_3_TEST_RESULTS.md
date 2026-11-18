# DTEF 3.0 Milestone 4.3 — hosted verification record

**Status: FULL BRANCH CI PASSED. Production release remains blocked by external integrations and security approvals.**

- Repository: `kabomoeng017-cmd/live-server-web-extension`
- Branch: `dtef-3.0-milestone-4.2`
- Commit: `14c482bed9f9d7c33e80cf0266b8b7178ed8cef0`
- Main CI: [run 44247409279](https://github.com/kabomoeng017-cmd/live-server-web-extension/actions/runs/44247409279)
- Backend dependency audit: [run 44247409696](https://github.com/kabomoeng017-cmd/live-server-web-extension/actions/runs/44247409696)

## Results

| Validation | Result | Evidence |
|---|---|---|
| Backend Maven build and JUnit | PASS | 51 tests, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS` |
| Testcontainers infrastructure integration | PASS | 4 PostgreSQL/Redis/RabbitMQ integration tests passed |
| Document scanner adapter tests | PASS | 6 tests against a fake ClamAV TCP daemon |
| Frontend install and typecheck | PASS | `npm ci` and `npm run typecheck` |
| Frontend unit tests | PASS | All configured frontend unit tests passed |
| Next.js production build | PASS | Build completed successfully |
| Source/simulator/security/workflow regressions | PASS | 55 tests passed, 0 failed |
| Docker Compose validation | PASS | Compose validation completed |
| Backend and frontend container image builds | PASS | Both images built successfully |
| Playwright Chromium E2E | PASS | 5 tests passed |
| Backend OSV dependency audit | PASS | Scanner reported “No issues found” |

## Browser journeys verified

1. Student authenticates, completes demo identity verification, uploads the required documents, submits an application and checks the persisted tracker/status history.
2. Student role is denied access to the officer workspace.
3. Spoofed PDF content is rejected and an invalid login OTP is handled.
4. Officer reviews linked documents, downloads a demo document, verifies its SHA-256 integrity result and records `DOCUMENTS_VERIFIED`.
5. Institution registration requires a different administrator to verify the pending record; the verifier can then suspend it.

## Scope and limits of this evidence

- The ClamAV tests use a fake local daemon. No real ClamAV service or signature database was connected.
- The scanner adapter is not integrated into production upload. Production document upload and officer download remain disabled and return `503 Service Unavailable`.
- SHA-256 integrity matching is not document authenticity verification and is not malware scanning.
- The institution workflow uses manual maker-checker attestation. No official institution registry API is connected.
- The CI run did not perform active ZAP scanning, independent penetration testing, k6 load testing, government-provider sandbox integration, WCAG 2.2 AA certification, production data migration or disaster-recovery validation.
- Passing CI does not authorize processing real student, Omang, welfare or bank/payment data.

## Next release gates

1. Implement encrypted object storage with KMS, retention, secure deletion, access logs and recovery testing.
2. Wire the scanner into a production ingestion service with `PENDING_SCAN`, `CLEAN`, `QUARANTINED` and fail-closed unavailable states.
3. Run integration tests against a real isolated ClamAV daemon and confirm scanner/database health and size limits.
4. Connect only authorized government and bank APIs after signed contracts, sandbox credentials, data-sharing approval and security review.
5. Complete independent penetration testing, authorized active scanning, WCAG 2.2 AA audit and staged load/capacity tests.
