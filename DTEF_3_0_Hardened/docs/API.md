# API overview

All non-health endpoints require a valid bearer token. Role enforcement is described below. JSON errors use a common response shape with `timestamp`, `status`, `error`, `message` and `path`.

| Method | Endpoint | Role | Notes |
|---|---|---|---|
| GET | `/api/health` | Public | Basic health only |
| POST | `/api/auth/demo-login` | Public, demo profile only | Password challenge; returns challenge ID, not token |
| POST | `/api/auth/verify-demo-otp` | Public, demo profile only | Consumes login OTP and returns short-lived access token |
| GET | `/api/auth/me` | Any authenticated | Current subject and role claims |
| POST | `/api/auth/send-action-otp` | Student | Fresh application-submit OTP |
| POST | `/api/omang/verify` | Student | Demo shape-validation only; production adapter unavailable until configured |
| POST | `/api/documents` | Student | Demo profile only; production returns 503 until encrypted object storage and malware scanning are integrated |
| POST | `/api/ai/admission-letter` | Student | Simulated OCR in demo only; production returns unavailable until provider configured |
| GET/PUT/DELETE | `/api/applications/draft` | Student | Server-side draft |
| POST | `/api/applications` | Student | Requires verified identity token, admission letter, Omang copy, bank proof and fresh OTP |
| GET | `/api/applications` | Student | Own applications only |
| GET | `/api/applications/{reference}` | Student | Own application only |
| GET | `/api/applications/{reference}/history` | Student | Own application status timeline |
| PUT | `/api/applications/{reference}/status` | Officer/Admin | Validated status transitions |
| GET | `/api/officer/queue` | Officer/Admin | Recent application queue |
| GET | `/api/institutions/enrolment` | Institution/Admin | Placeholder until institution registry integration |
| POST | `/api/institutions/confirm-registration` | Institution/Admin | Does not confirm a live registration |
| GET | `/api/payments/student/{reference}` | Student/FNBB/Admin | Placeholder only |
| GET | `/api/payments/summary` | FNBB/Admin | Placeholder only |
| POST | `/api/payments/process/{reference}` | FNBB/Admin | Deliberately disabled; returns 501 |
| GET/POST | `/api/welfare/queue`, `/api/welfare/assess` | Welfare/Admin | No automated welfare decision |
| GET | `/api/analytics/summary`, `/api/analytics/districts` | Ministry/Admin | Zero/empty until reporting data connected |
| GET | `/api/notifications` | Authenticated | Empty until user notification store is connected |
| POST | `/api/notifications/send` | Officer/Admin | Demo profile only; production returns 503 until an approved delivery provider and transactional outbox are configured |

## Important

The OIDC provider must map trusted roles to the JWT `roles` claim, e.g. `ROLE_STUDENT`. Do not allow users to self-assign their production roles. The demo login role selection is restricted to the local demo profile and is not a production authentication design.
