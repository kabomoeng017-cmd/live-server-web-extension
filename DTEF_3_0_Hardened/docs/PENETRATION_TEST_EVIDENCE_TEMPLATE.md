# DTEF penetration-test evidence record

> Complete this record with the authorized assessor. A ZAP report alone is not an independent penetration test or a production security approval.

## Authorization and scope

- System owner / authorizing official:
- Written authorization reference:
- Staging hostname and environment:
- Approved source IPs:
- Test window and timezone:
- Maximum request rate / concurrency:
- Included roles and test accounts:
- Explicitly excluded systems and third parties:
- Emergency stop contact:
- Backup / rollback reference:

## Test conditions

- Application version / commit:
- Deployment date:
- Database and queue versions:
- Synthetic test data confirmation:
- Scanner and version:
- Manual assessor / organization:
- Test methodology / standard:

## Findings register

| ID | Area | Finding | Severity | Evidence reference | Owner | Remediation due | Retest result |
|---|---|---|---|---|---|---|---|
| | | | | | | | |

## Minimum coverage

- [ ] Authentication, OTP brute force/replay/expiry and MFA
- [ ] Session/JWT issuer, audience, algorithm, expiry and revocation
- [ ] Role and object-level authorization across all seven user roles
- [ ] Application ownership, duplicate submission, status transition and concurrency controls
- [ ] Document upload signatures, polyglots, oversized payloads, malware quarantine and download access
- [ ] API injection, mass assignment, deserialization, rate limiting and error disclosure
- [ ] CORS, CSRF assumptions, TLS, HSTS and CSP
- [ ] Secrets, container privileges, network segmentation and exposed service ports
- [ ] PostgreSQL permissions, Redis ACL/network isolation and RabbitMQ credentials/queue permissions
- [ ] Audit logs, traces, analytics and support exports for sensitive-data leakage
- [ ] Backup restoration and incident recovery

## Exit and sign-off

- Critical findings: 0 unresolved
- High findings: 0 unresolved
- Medium findings: owner, mitigation and due date recorded
- Retest evidence attached:
- Data-protection / privacy review:
- Government security review:
- System owner acceptance:
- Date:

Do not begin an active scan or manual test until the scope, target, rate ceiling, source IP and written authorization are approved by the system owner. Do not test `tef.gov.bw`, `gov.bw`, banks, identity providers or other government systems without their owners' explicit written authorization.
