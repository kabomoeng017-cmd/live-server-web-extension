# DTEF staged load testing

The staged profile increases concurrent virtual users through 100, 500, 1,000, 2,000, 5,000 and 10,000, stopping at `MAX_VUS`. It runs authenticated read-only application, draft and profile requests and checks latency/error thresholds.

## Safe execution gates

- The default ceiling is **100 virtual users**.
- Local targets (`localhost`, `127.0.0.1`, `::1`) can be tested without a remote authorization flag.
- Any non-local target must use HTTPS, exactly match `DTEF_LOAD_ALLOWED_HOST`, and require `DTEF_LOAD_TEST_AUTHORIZED=YES` after written authorization from the staging owner.
- For OIDC staging, supply a short-lived IdP-issued `DTEF_TOKEN`. A generated HMAC test JWT is allowed only if the isolated staging deployment explicitly trusts the dedicated test issuer and `JWT_SECRET_BASE64`; set `DTEF_LOAD_USE_TEST_JWT=YES` and provide both values. The local demo secret is never substituted for a remote target.
- Known live government hosts (`tef.gov.bw`, `www.tef.gov.bw`, `gov.bw`, `www.gov.bw`) are blocked.
- More than 1,000 VUs requires `DTEF_HIGH_SCALE_APPROVED=YES` and separately sized infrastructure.
- More than 5,000 VUs additionally requires `DTEF_EXTREME_SCALE_APPROVED=YES`.
- Never target a production service, bank, identity provider or third-party government service.

## Run

1. Deploy the Compose stack to an isolated performance environment, not a live government service.
2. Ensure the environment has dedicated CPU, memory, PostgreSQL, Redis and RabbitMQ monitoring and contains only synthetic data.
3. Install k6 and Node.js 22.
4. Start at 100 users:

   ```bash
   MAX_VUS=100 ./tests/load/run-staged-load.sh
   ```

5. Increase only after the previous stage passes and infrastructure owners approve:

   ```bash
   MAX_VUS=500 ./tests/load/run-staged-load.sh
   MAX_VUS=1000 ./tests/load/run-staged-load.sh
   ```

6. For a remote approved staging host, also set `DTEF_BASE_URL`, `DTEF_LOAD_ALLOWED_HOST` and `DTEF_LOAD_TEST_AUTHORIZED=YES`. For targets above 1,000, set the separate approval flags described above.
7. Review the generated JSON summary under `artifacts/load/` together with application logs, CPU/memory, DB pool waits, query latency, Redis latency/memory, RabbitMQ queue depth, throughput, p50/p95/p99 latency and recovery after ramp-down.

For GitHub-hosted execution, use the manual **Authorized DTEF staging load test** workflow. Protect the `load-testing` environment with required reviewers and set `DTEF_LOAD_ALLOWED_HOST` plus `DTEF_LOAD_AUTHORIZATION_REFERENCE`. The requested hostname and target URL must both match that protected allow-list; a user-entered hostname alone cannot authorize a target. The workflow requires written-authorization confirmation and, for `test-jwt`, an additional explicit acknowledgement that staging trusts the dedicated test issuer/secret. Choose `test-jwt` only when the isolated staging API explicitly trusts the dedicated test issuer (`DTEF_LOAD_JWT_SECRET` and `DTEF_LOAD_JWT_ISSUER`); choose `preissued-bearer` for a short-lived IdP-issued `DTEF_LOAD_BEARER_TOKEN`. The workflow uses k6 `2.4.0`, passes the token through a permission-restricted temporary environment file, removes that file after execution and stores the summary/authorization record as an artifact.

On Windows, use WSL or run k6 directly after generating a short-lived token with `node tests/load/generate-load-token.mjs`. The token is synthetic, test-only and valid for two hours; never use it outside the isolated demo/staging deployment.

## Interpretation and stop conditions

A completed k6 run is not a capacity certification. Stop the run if error rate breaches 2%, p95 exceeds 1.2 seconds, p99 exceeds 2.5 seconds, resource saturation threatens service stability, or queue/database lag grows without recovery. Record the first failing stage and repeat each accepted stage at least three times. A 5,000+ or 10,000+ run requires a separately sized environment and written authorization.

This test is read-only: it does not submit applications, upload documents or trigger payments.
