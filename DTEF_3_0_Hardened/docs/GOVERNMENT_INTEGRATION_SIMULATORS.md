# Government integration simulators

The optional simulator is a contract-test fixture for development and integration rehearsal. It returns synthetic records only and does not contact any government, bank, institution or telecom system.

Start it from the project root:

```bash
docker compose --profile integration-sim up -d government-simulator
curl http://localhost:9090/health
```

## Simulated provider endpoints

| Provider domain | Endpoint | Purpose |
|---|---|---|
| Omang | `POST /omang/verify` | Validates only a synthetic nine-digit test value |
| Institution registry | `GET /institutions/{code}/enrolments/{studentRef}` | Returns a synthetic active registration |
| Institution registry | `POST /institutions/enrolments/confirm` | Returns a synthetic confirmation reference |
| Social welfare | `GET /social-welfare/households/{reference}` | Returns one of two fixed synthetic household records |
| Social welfare | `POST /social-welfare/assessments` | Simulates accepting an assessment request |
| FNBB | `POST /fnbb/payments` | Simulates idempotent payment instruction acceptance |
| FNBB | `GET /fnbb/payments/{reference}` | Returns a simulated payment state |
| Notification gateway | `POST /notifications/messages` | Simulates message acceptance only |

## Failure simulation

Pass one of these request headers to exercise resilience behavior:

- `x-simulate-status: 429|500|502|503|504` — return a synthetic upstream failure.
- `x-simulate-delay-ms: 250` — delay a response; values are capped at five seconds.
- FNBB payment requests require an `Idempotency-Key` header; repeat requests with the same key return the same synthetic payment reference.

Run the contract tests:

```bash
node --test tests/gov-sim/*.test.mjs
```

## Important boundary

The simulator is not a DTEF integration adapter. The current V3 application deliberately fails closed for official Omang, FNBB payment execution, institution and welfare integrations. Connecting a real provider requires an approved data-sharing agreement, API specifications, mutual authentication, signing/verification, consent and audit requirements, production adapter implementation, provider sandbox credentials and contract approval. Do not treat a simulator response as an official identity, enrolment, eligibility or payment decision.
