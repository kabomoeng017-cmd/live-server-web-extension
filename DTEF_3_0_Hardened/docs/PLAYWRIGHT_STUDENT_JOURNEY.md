# Playwright student journey verification

The isolated Docker Compose browser suite uses demo-only services and synthetic student data.

## Verified journey

1. Student selects the STUDENT role and authenticates using the local demo OTP flow.
2. Student completes the demo identity-verification step.
3. Student uploads an admission letter, Omang copy, and bank confirmation.
4. Student verifies the application-submission OTP and submits the application.
5. The UI displays a generated DTEF reference and APPLIED status.
6. The student tracker retrieves the application and its initial status-history event.
7. The browser reloads, the student re-enters the reference, and the tracker retrieves the same application, institution, programme, status, and history again.

## Negative and authorization checks

- A student cannot open the officer workspace.
- A file that declares PDF MIME type but does not contain PDF content is rejected.
- An invalid login OTP is rejected, and the student can recover using the valid demo OTP.

## Scope and limitations

These checks run only against the isolated local demo stack. They do not connect to official Omang, FNBB, government, institution, welfare, SMS, email, or WhatsApp services. A green browser run demonstrates the tested demo workflow, not production authorization, live integrations, or a production security assessment.
