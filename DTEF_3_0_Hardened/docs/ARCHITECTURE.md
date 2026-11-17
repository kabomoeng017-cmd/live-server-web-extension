# DTEF 3.0 Architecture

## Runtime shape

```text
Browser
  | same-origin HTTPS /api
  v
Ingress / reverse proxy
  |---------------------------> Next.js 15 frontend
  |                                 |
  |                                 +-- role-aware portal UI
  |                                 +-- local/server-side draft recovery
  |
  +----------------------------> Spring Boot 3.4 API
                                    |
              +---------------------+---------------------+
              |                     |                     |
          PostgreSQL              Redis                RabbitMQ
          Flyway schema           OTP / identity       notification queue
          application data        verification TTL     (demo consumer only)
          audit events
              |
          private document storage (local demo volume)
```

## Security boundary

- The API is stateless and validates bearer JWTs.
- In local `demo`, a restricted allow-list of demonstration accounts completes password + random OTP challenge. The signing key is a local placeholder and must never be reused.
- In `prod`, the API validates tokens from the configured external OIDC issuer. The identity provider must enforce MFA and issue role claims.
- Every endpoint is authenticated unless explicitly listed as public health or demo authentication.
- Role checks are applied at API route level; student application reads additionally enforce owner matching.
- Application submission requires a fresh action OTP and documents owned by the same subject.

## Data and workflow

- Application references are generated from a PostgreSQL sequence.
- Every submitted admission/supporting document is linked to the application in `application_documents`, with database foreign keys and unique constraints.
- Application drafts are stored per authenticated subject and are limited to 200 KB.
- Raw Omang values are not persisted by the application workflow. The demo identity endpoint only validates input shape and stores a short-lived, owner-bound verification token in Redis.
- Document uploads use generated storage keys and SHA-256 fingerprints. Only PDF/JPG/PNG signatures are accepted; original filenames are sanitized and never used as storage paths.
- Demo documents are labelled `DEMO_ACCEPTED` only to support the local walkthrough. The production upload endpoint is disabled until an approved encrypted object-storage and malware/document verification adapter is connected.
- Application status transitions are constrained; completed/rejected states are terminal in this initial workflow.
- Audit events record application submission and status changes without copying sensitive document contents.

## Scale and resilience

- PostgreSQL has indexes for owner/date and status/date queries.
- Redis supports OTP, identity-verification TTL and rate counters.
- RabbitMQ carries notification messages in the demo profile; the demo consumer logs them without sending external communications. Production enqueue is disabled until an approved delivery provider and transactional outbox are configured.
- Docker Compose includes service health checks and startup dependencies.
- Kubernetes templates include non-root containers, probes, resource requests/limits, HPA, PDB, TLS ingress and backend network policy.

These are architectural foundations, not proof of a specific concurrent-user capacity. Real sizing requires production-like load tests and infrastructure metrics.
