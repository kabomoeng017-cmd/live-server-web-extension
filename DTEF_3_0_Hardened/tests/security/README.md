# DTEF security validation and penetration-test runbook

## Scope and safety

- The automated passive ZAP baseline may run against the isolated local demo stack.
- Active penetration testing is manual-dispatch only and must target an HTTPS staging hostname explicitly allow-listed in the GitHub `security-testing` environment as `DTEF_PENTEST_ALLOWED_HOST`.
- Obtain written authorization, a named test window, source IP allow-listing, a rollback contact and an agreed request-rate ceiling before active testing.
- Configure the GitHub `security-testing` environment with `DTEF_PENTEST_ALLOWED_HOST` and `DTEF_PENTEST_AUTHORIZATION_REFERENCE`; protect that environment with required reviewers.
- Never point the active scanner at `tef.gov.bw`, any government production endpoint, a bank, an identity provider or a third-party service without explicit written authorization from its owner.
- Use synthetic accounts and documents only. Do not put Omang, bank account, real student or government records into test fixtures or reports.

## Automated checks

1. `npm audit --audit-level=high` in the frontend build job.
2. Full Maven unit and Testcontainers suite in the backend job.
3. OWASP ZAP passive baseline scan against the isolated Compose demo.
4. Manual OWASP ZAP active scan against the explicitly allow-listed staging host.

## Manual penetration-test coverage

The independent tester should record evidence and remediation for:

- Authentication: OTP brute-force, replay, expiry, lockout, session expiry, token issuer/audience/algorithm validation and MFA enforcement.
- Authorization: horizontal/vertical privilege escalation across STUDENT, OFFICER, INSTITUTION, FNBB, WELFARE, MINISTRY and ADMIN roles; ownership checks on application and document IDs.
- Application logic: duplicate submissions, invalid status transitions, optimistic-lock conflicts, OTP replay and idempotency.
- Uploads: MIME/signature mismatch, malformed PDFs/images, polyglots, oversized bodies, path traversal, storage isolation, malware quarantine and download authorization.
- API: injection, mass assignment, deserialization, rate limiting, CORS, CSRF assumptions, error disclosure and sensitive data exposure.
- Infrastructure: TLS, HSTS, CSP, secret leakage, container privileges, network segmentation, database permissions, Redis exposure, RabbitMQ access and backup restoration.
- Privacy: logs, traces, analytics and support exports must not expose raw Omang, OTPs, passwords, bank identifiers or document contents.

## Exit criteria

- No unresolved Critical or High findings.
- Medium findings have an owner, mitigation and agreed due date; any accepted risk is signed off by the system owner.
- Retest evidence confirms each fixed finding.
- Security review and data-protection sign-off are recorded before any production pilot.

ZAP is an automated scanner, not a substitute for an independent manual penetration test, architecture review or government security accreditation.
