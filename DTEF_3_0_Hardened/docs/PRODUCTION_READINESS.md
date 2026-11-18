# Production Readiness Gate — DTEF 3.0

The following items are deliberately **not marked complete**. The source code is a hardened prototype and deployment template; it is not authorized to process real student, identity, welfare or payment data.

## Critical gates — must be completed before any live data

- [ ] Government-approved identity provider / Omang verification contract, API access, consent, data minimization and audit approval.
- [ ] Production OIDC integration with MFA, account lifecycle, verified email/phone, role provisioning and account recovery. Confirm the issuer's `roles` claim, API audience and signing-key rotation.
- [ ] Replace demo bearer-token flow with the approved OIDC/BFF session pattern (Secure, HttpOnly, SameSite cookies; refresh-token rotation; CSRF controls; session revocation).
- [ ] FNBB signed integration contract, bank-account verification, idempotent payment instructions, signature verification, callback replay protection, reconciliation ledger, settlement reports and dual-control approvals. Payment execution is disabled in this source.
- [ ] PostgreSQL production topology, encryption at rest, TLS, backups, point-in-time recovery, retention and tested restore.
- [ ] Secrets manager / KMS. Remove every sample secret/password and rotate all service credentials.
- [ ] Private object storage with server-side encryption/KMS, malware scanning/quarantine, access logging, retention/deletion, legal hold and disaster recovery. Local filesystem storage is not a production document store.
- [ ] Independent security architecture review, threat model, penetration test, vulnerability scan and remediation sign-off.
- [ ] Botswana Data Protection Act and applicable government information-security/privacy requirements reviewed by legal, DPO and system owner; complete DPIA where required.

## High-priority gates

- [ ] Approved institution onboarding and registry API, signed admission/enrolment messages, bulk import validation, student withdrawal/result workflows and dispute process.
- [ ] SMS/email/WhatsApp delivery providers, sender registration, delivery receipts, retry/backoff, suppression lists and verified contact ownership. Current production OTP delivery intentionally returns unavailable.
- [ ] OCR provider and document-extraction validation, confidence thresholds, human confirmation, file malware scanning and tamper-evidence. No eligibility decision may be made solely by AI.
- [ ] Social welfare data-sharing agreement, source-of-truth validation, human review, consent and access restrictions. No automated vulnerability score is implemented.
- [ ] Notification outbox/retry/dead-letter handling and production queue consumer. Demo consumer only logs messages.
- [ ] Application status history backfill for legacy applications, officer assignment/queue workflow, appeals/evidence review, and official sponsorship-letter PDF generation with approved digital signatures.
- [ ] Data warehouse / national district reference data and reconciled KPI definitions. Analytics currently returns zero/empty values.
- [ ] Load tests using production-like data volumes and a signed test window; measure p50/p95/p99, throughput, queue depth, DB pool saturation, cache hit rate and error rates.
- [ ] Autoscaling and capacity plan across frontend, API, PostgreSQL, Redis, RabbitMQ, storage, CDN/WAF and identity/SMS providers.
- [ ] Monitoring, alerting, distributed tracing, SIEM export, immutable audit retention and incident response playbooks.
- [ ] Accessibility audit against WCAG 2.2 AA using automated and manual testing, screen readers, keyboard-only operation, zoom/reflow and Setswana linguistic review.

## Medium-priority gates

- [ ] Full English/Setswana translation of every page, validation message, notification template and accessible label. Current translations are partial draft strings.
- [ ] Add browser end-to-end tests for each role and critical application workflow; complete PWA/offline requirements only where policy and data-protection rules permit.
- [ ] Add API contract tests and database migration tests on the exact PostgreSQL version used in production.
- [ ] Add rate limiting / bot mitigation at WAF/API gateway, login anomaly detection and device/session controls.
- [ ] Add CDN caching for static assets, image optimization, lazy loading and Core Web Vitals monitoring.
- [ ] Add a proper transactional outbox for notification/payment events and idempotency keys for all externally retried operations.
- [ ] Add data retention automation, export/access request workflows and secure deletion.
- [ ] Confirm support model, service desk escalation, service hours, RTO/RPO and DR exercises.

## Deployment rule

Do not deploy the `demo` profile to a public or government network. Use the `prod` profile only after the Critical gates have been approved and the external OIDC issuer, secrets, databases, document scanner/object storage and service integrations have been configured and tested. Kubernetes YAML contains placeholders and is not ready to apply unchanged.

## Kubernetes deployment notes

1. Create the `dtef` namespace and choose an encrypted RWX storage class only if retaining the temporary filesystem adapter. The preferred production change is an encrypted object-storage adapter with malware scanning; do not use a single-node RWO claim with multiple backend replicas.
2. Provision managed/high-availability PostgreSQL, Redis and RabbitMQ, with TLS, private networking, backups and monitoring.
3. Create `dtef-backend-secrets` from the approved secret manager. `secret-template.yaml` is a placeholder and must not be applied as-is.
4. Build/push images using `infrastructure/k8s/build-images.sh` after setting `REGISTRY` and `VERSION`; the frontend build argument points its internal API rewrite at the Kubernetes backend Service.
5. Create the TLS secret and replace `portal.example.gov.bw`, storage class, image names and OIDC issuer values.
6. Apply namespace/storage, secrets, backend/frontend deployments, HPA/PDB/network policy and ingress in the order agreed with the platform team.
7. Confirm the cluster has metrics-server, ingress controller, policy enforcement, encrypted storage, secrets integration and the approved network egress routes.
8. Recalculate Hikari pool size against the maximum HPA replicas and PostgreSQL connection limit. Use PgBouncer or an equivalent connection pooler if required; do not assume the sample pool numbers are suitable for the production database.

The included Kubernetes manifests are deployment examples, not validated manifests for a particular Botswana government cluster. Validate them with the platform's admission policies and staging environment before release.


## Current branch implementation notes — 18 November 2026

The following controls are implemented in the development branch, but they do not close the production gates above:

- Institution catalogue records are maintained through an admin workflow with independent maker-checker verification. This is a manual official-source attestation, not a connection to an official institution registry API. The University of Botswana fixture is demo-only.
- Officer review now exposes linked application documents, SHA-256 metadata, status history and controlled status decisions. Moving an application to `DOCUMENTS_VERIFIED` requires all linked records to exist and to have `CLEAN` scan status in production. The demo profile accepts only the explicit `DEMO_ACCEPTED` fixture status.
- Demo officer downloads compare stored file size and SHA-256 against recorded metadata. This detects byte changes; it does not establish document authenticity or malware cleanliness.
- A ClamAV `INSTREAM` adapter and a fail-closed disabled scanner are available for the `prod` profile. The adapter is disabled by default and is not yet connected to the upload path. Production document upload and officer download endpoints still return `503 Service Unavailable`.
- The scanner's protocol tests use a local fake daemon. No real ClamAV service, signature database, encrypted object store or production quarantine workflow has been connected or certified.

See `DOCUMENT_SCANNING.md` and `MILESTONE_4_3_TEST_RESULTS.md` for the precise implementation boundary and hosted test evidence.
