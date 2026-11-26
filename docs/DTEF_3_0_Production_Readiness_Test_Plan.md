# DTEF 3.0 Production-Readiness Test Programme

**Execution policy:** Each phase has 20 distinct tests. For every test, record expected result, actual result, evidence, defect ID, fix commit and regression result. Do not mark a test passed until it has actually executed. Fix defects on this feature branch, then rerun the failed test and relevant regression suite.

**Safety:** Run destructive, security and load tests only against isolated, disposable test infrastructure. Never load-test the public demo or government production systems without written authorization. Do not use real student data. Keep secrets and OTP values out of logs and reports.

## Phase 1 — Secure document ingestion (20 tests)
1. Valid PDF accepted only after a clean antivirus verdict.
2. Valid JPEG accepted only after a clean antivirus verdict.
3. Valid PNG accepted only after a clean antivirus verdict.
4. EICAR antivirus test file detected and rejected/quarantined.
5. Scanner unavailable fails closed.
6. Scanner timeout fails closed.
7. Unknown scanner verdict fails closed.
8. Malformed scanner response fails closed.
9. Spoofed PDF extension with non-PDF bytes rejected.
10. Spoofed image extension with non-image bytes rejected.
11. MIME type cannot override file-signature validation.
12. Empty file rejected.
13. Zero-byte file rejected.
14. File exactly at configured size limit handled correctly.
15. File one byte over limit rejected.
16. Path traversal filename cannot escape storage root.
17. Unicode and unusual filenames handled safely.
18. Duplicate upload cannot overwrite an existing object.
19. Scanner error cannot leave a document marked clean/available.
20. Audit record captures uploader, document ID, checksum, verdict and timestamp without sensitive content.

## Phase 2 — Persistence and retrieval (20 tests)
1. Upload survives API process restart.
2. Upload survives container replacement with persistent volume.
3. Download returns exact original bytes.
4. SHA-256 checksum matches after retrieval.
5. Missing object returns safe not-found response.
6. Corrupt object fails integrity verification.
7. Interrupted write does not expose a partial document.
8. Concurrent uploads receive unique storage keys.
9. Duplicate content follows documented deduplication policy.
10. Storage permissions prevent untrusted access.
11. Symlink/path manipulation cannot escape storage root.
12. Full storage fails safely.
13. Read-only storage fails safely.
14. Large allowed file streams without loading the entire file into memory.
15. Unsupported content type cannot be retrieved as trusted document.
16. Quarantined document cannot be downloaded by a student.
17. Deletion/retention policy is enforced.
18. Metadata and file object remain consistent after failure.
19. Backup/restore preserves document hashes.
20. Restore preserves ownership metadata.

## Phase 3 — Authorization and isolation (20 tests)
1. Student A can access own document.
2. Student A cannot access Student B document by changing ID.
3. Unauthenticated user cannot upload.
4. Unauthenticated user cannot retrieve.
5. Student cannot access staff-only document endpoint.
6. DTEF officer can access only permitted cases.
7. Institution role cannot access unrelated institution records.
8. FNBB role cannot access academic documents beyond its required scope.
9. Social welfare role cannot access unrelated student documents.
10. Ministry analytics role cannot retrieve raw documents without explicit permission.
11. System administrator actions are audited.
12. Expired token rejected.
13. Invalid signature token rejected.
14. Wrong issuer/audience token rejected.
15. Role downgrade takes effect on the next request.
16. Disabled account access denied.
17. Cross-institution access denied.
18. Range/download requests enforce the same authorization.
19. Guessable document IDs do not disclose existence or content.
20. Concurrent requests cannot bypass ownership checks.

## Phase 4 — Application workflow and resilience (20 tests)
1. Valid application submission succeeds.
2. Invalid required field rejected with validation detail.
3. Duplicate application blocked or idempotent.
4. Application draft persists after reload.
5. Interrupted submission can be safely retried.
6. Concurrent submit requests create one application.
7. Invalid Omang format rejected.
8. Invalid admission document rejected.
9. Application status follows the allowed state machine.
10. Unauthorized status transition denied.
11. Status history is append-only/audited.
12. Student sees only own application.
13. Staff queue is role-scoped.
14. Notification failure does not lose application.
15. Database rollback leaves no partial application.
16. Database restart does not lose committed application.
17. Invalid reference cannot retrieve another application.
18. Appeal submission obeys eligibility rules.
19. Duplicate payment callback is idempotent.
20. Application remains consistent after API restart.

## Phase 5 — Integrations and database (20 tests)
1. PostgreSQL clean start and migrations.
2. Migration from previous schema succeeds.
3. Migration failure blocks unsafe startup.
4. Transaction rollback works.
5. Connection pool exhaustion handled safely.
6. Database unavailable produces controlled failure.
7. Redis available path works.
8. Redis outage does not corrupt authoritative records.
9. Cache invalidation after application update.
10. Stale cache cannot expose revoked access.
11. RabbitMQ unavailable handling matches profile policy.
12. Queue retry is bounded.
13. Dead-letter handling records terminal failures.
14. Duplicate message is idempotent.
15. Out-of-order message cannot regress status.
16. Payment webhook signature validation.
17. Payment webhook replay protection.
18. External integration timeout handling.
19. Retry does not duplicate side effects.
20. Secrets are absent from logs and error responses.

## Phase 6 — Performance and load (20 tests)
1. 10-user baseline.
2. 50-user baseline.
3. 100 concurrent users.
4. 250 concurrent users.
5. 500 concurrent users.
6. 1,000 concurrent users.
7. 2,500 concurrent users.
8. 5,000 concurrent users.
9. 10,000 concurrent users, only after earlier gates pass.
10. Login throughput.
11. Application draft-save throughput.
12. Application submission throughput.
13. Document upload throughput with scanning enabled.
14. Document download throughput.
15. Database saturation behaviour.
16. Redis degradation behaviour.
17. API CPU/memory under sustained load.
18. p95/p99 latency and error-rate thresholds.
19. 30-minute soak test.
20. Recovery and backlog drain after load stops.

## Phase 7 — Security and abuse resistance (20 tests)
1. OWASP API broken object-level authorization.
2. Broken authentication checks.
3. Broken function-level authorization.
4. Excessive data exposure.
5. Rate limiting.
6. Credential-stuffing simulation with test accounts.
7. OTP brute-force resistance.
8. CSRF checks where applicable.
9. CORS allowlist enforcement.
10. Security headers.
11. SQL injection probes.
12. XSS probes in user-controlled fields.
13. SSRF probes against URL-fetch features.
14. Parser/deserialization abuse.
15. File parser/decompression bomb limits.
16. Dependency vulnerability scan.
17. Container image vulnerability scan.
18. Secret scanning.
19. Audit log access/tamper controls.
20. Error responses do not leak stack traces or secrets.

## Phase 8 — Recovery and operations (20 tests)
1. API process restart.
2. Portal restart.
3. PostgreSQL restart.
4. Redis restart.
5. Persistent volume remount.
6. Backup creation verification.
7. Database restore drill.
8. Document backup restore/hash verification.
9. Failed deployment rollback.
10. Health/readiness probe behaviour.
11. Startup with missing required secret fails safely.
12. Upload scanning fails closed when scanner is unavailable.
13. Log rotation and retention.
14. Alert on repeated upload scan failures.
15. Alert on elevated 5xx rates.
16. Alert on storage nearing capacity.
17. Graceful shutdown drains in-flight requests.
18. Queue recovery after restart.
19. Disaster recovery RPO/RTO measurement.
20. Operational runbook rehearsal.

## Exit criteria
- At least 20 executed tests per phase with evidence.
- Zero unresolved critical/high defects before phase sign-off.
- Defects fixed on a dedicated branch and regression-tested.
- Security-sensitive failures fail closed.
- Performance thresholds agreed and measured before capacity claims.
- No production deployment until independent security review and government integration approvals are complete.

## Current status
This document is the test specification, not evidence of execution. Begin with Phase 1 only. Each test must be marked pending until executed and evidenced.
