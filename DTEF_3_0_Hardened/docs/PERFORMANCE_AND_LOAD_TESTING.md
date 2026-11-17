# Performance and load-testing plan

## Current engineering controls

- PostgreSQL owner/status indexes and a database sequence for collision-resistant references.
- Redis for OTP TTLs, attempt counters, rate limits and short-lived identity verification.
- RabbitMQ durable queue for notification messages.
- Docker health checks, startup dependencies and Kubernetes readiness/liveness probes.
- API pool settings, bounded upload size, private document storage and server-side draft persistence.
- Same-origin `/api` routing to avoid browser `localhost` misrouting and unnecessary CORS.

## Milestone 4 staged load protocol

1. Establish a representative isolated staging environment with the API, database, Redis, RabbitMQ, ingress/WAF, storage and OIDC topology planned for production.
2. Use synthetic data only. Never load test using real Omang, bank, welfare or student records.
3. Run `tests/load/run-staged-load.sh` starting at 100 virtual users. Increase to 500 and 1,000, then 2,000, 5,000, 10,000 or another approved ceiling only after the previous stage is stable.
4. The included staged script currently exercises authenticated read-only application, draft and profile endpoints. It does not yet represent upload-heavy, officer-queue, institution-feed or payment workloads. Add separate controlled workload profiles before using these tests to claim full business-workflow capacity.
5. Track p50/p95/p99 latency, throughput, 4xx/5xx rate, CPU/memory, JVM GC, thread pools, DB connection pool, slow queries, Redis latency/hit rate, Rabbit queue depth, upload storage latency and autoscaling events.
6. Repeat each approved target at least three times. Include ramp-down and recovery; separately rehearse pod restart, database failover, Redis restart, queue consumer restart and network interruption.
7. The k6 defaults use p95 under 1.2 seconds and p99 under 2.5 seconds as provisional test thresholds, not measured results or service-level commitments. DTEF and the platform owner must approve production SLOs.
8. Publish the environment, build hash, dataset size, test script, achieved throughput, saturation point, errors and remediation. Only then state a validated concurrent-user figure.

No concurrent-user capacity is currently certified. Do not run load tests against the live DTEF website or government production services without written authorization and an agreed test window.
