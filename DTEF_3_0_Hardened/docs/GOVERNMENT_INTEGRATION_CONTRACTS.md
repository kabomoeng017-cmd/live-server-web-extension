# Government integration simulator contracts

**Environment:** local/test only. Every response is synthetic. The simulator does not connect to the Botswana Government, banks, institutions, identity registers or messaging providers.

Base URL in the Compose integration profile: `http://127.0.0.1:9090`.

| Integration | Request | Success response | Negative / retry behavior |
|---|---|---|---|
| Omang identity | `POST /omang/verify` with a synthetic 9-digit `omang` | `verified`, `verificationReference`, `provider=SIMULATED_OMANG`, `testRecord=true` | Invalid shape returns 422; upstream simulation can return 429/5xx |
| Institution registry | `GET /institutions/{institutionCode}/enrolments/{studentReference}` | `found`, `registrationStatus`, `programmeStatus`, `source` | Unknown route returns 404; simulated upstream failure returns 429/5xx |
| Institution confirmation | `POST /institutions/enrolments/confirm` with a `DTEF-YYYY-00000000` `applicationReference` and a 3–64 character synthetic `studentReference` | `confirmed`, `confirmationReference`, `source` | Missing references return 422 |
| Social welfare | `GET /social-welfare/households/{householdReference}` | `found`, `householdReference`, `assessmentStatus`, `priority`, `source` | Unknown household returns 404; an assessment referencing an unknown household also returns 404 |
| Welfare assessment | `POST /social-welfare/assessments` with a known synthetic household reference and a `DTEF-YYYY-00000000` application reference | 202 with `accepted`, `assessmentReference`, `status=QUEUED`, `source` | Missing references return 422 |
| FNBB payments | `POST /fnbb/payments` with `Idempotency-Key`, a `DTEF-YYYY-00000000` application reference and positive bounded BWP amount | 202 with `accepted`, `paymentReference`, `status=PENDING`, amount, currency and source | Invalid request returns 422; same key with a different payload returns 409; repeated same payload returns same payment reference |
| FNBB payment lookup | `GET /fnbb/payments/{paymentReference}` | `found`, payment reference, status, amount, currency and source | Unknown payment returns 404 |
| Notification gateway | `POST /notifications/messages` with recipient reference, supported channel and uppercase template ID | 202 with provider message reference, `status=QUEUED`, source | Invalid recipient/channel/template returns 422 |

## Common simulator controls

- `GET /health` returns `mode=SIMULATOR_ONLY`.
- `x-simulate-status: 429|500|502|503|504` forces a synthetic upstream error response.
- `x-simulate-delay-ms` adds a bounded delay of up to five seconds.
- Invalid JSON returns 400; payloads above 64 KiB return 413.
- Responses use JSON and `Cache-Control: no-store`.

## Contract test coverage

`node --test tests/gov-sim/*.test.mjs` verifies happy paths, required fields, invalid payloads, FNBB idempotency and conflict handling, malformed DTEF references, unknown household handling, simulated upstream failures/delays, notification validation, malformed JSON and payload-size limits.

These are provider-contract simulator tests. They do not prove that production adapters exist or that a government/bank endpoint has been authorized, integrated, or certified. Each production adapter must undergo a separate owner-approved integration test against an official sandbox, using signed-off schemas, authentication, mTLS or equivalent controls, data-processing agreements and test credentials.
