# OTP throttling in browser tests

The demo login endpoint intentionally rate-limits OTP issuance per demo account. The Playwright suite must not raise or bypass this limit to make tests pass.

Login-dependent scenarios are therefore grouped into a single student session where practical. The primary student journey verifies draft persistence and resumes the same authenticated session after a page reload; it does not request a fourth login OTP for the same demo account.

This keeps the browser test representative of the configured rate-limit behavior while avoiding real credentials and external delivery services.
