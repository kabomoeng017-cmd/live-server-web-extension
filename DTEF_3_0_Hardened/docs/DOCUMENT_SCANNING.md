# Document malware scanning — implementation boundary and rollout

## Implemented in the development branch

The backend now defines a `DocumentScanner` contract with two outcomes:

- `CLEAN`: the scanner completed and returned a clean verdict.
- `INFECTED`: the scanner completed and returned a detection signature.

A scanner connection failure, timeout, malformed response or unrecognized verdict raises `ScannerUnavailableException`. It must never be converted to `CLEAN`.

The production-only `ClamAvDocumentScanner` adapter:

- Is created only when the `prod` Spring profile is active and `DTEF_ANTIVIRUS_ENABLED=true`.
- Uses ClamAV's NUL-framed `zINSTREAM` command and four-byte big-endian chunk lengths.
- Sends a zero-length chunk to finish the stream.
- Accepts PDF, JPEG and PNG metadata types, enforces a 10 MB maximum, sends 2 KB chunks and applies bounded connect/read timeouts.
- Maps `stream: OK` to `CLEAN`, `FOUND` to `INFECTED`, and all other replies to a fail-closed scanner exception.

When antivirus is not enabled, `DisabledDocumentScanner` is selected in the `prod` profile and refuses to scan. Default configuration is disabled:

- `DTEF_ANTIVIRUS_ENABLED=false`
- `CLAMAV_HOST=127.0.0.1`
- `CLAMAV_PORT=3310`
- `CLAMAV_TIMEOUT_MS=5000`

The ClamAV adapter tests use a local fake TCP daemon to verify framing, payload transfer, clean/infected responses, malformed replies and unavailable-daemon behavior. They are protocol tests, not evidence that a real ClamAV engine or signature database has been deployed.

## Explicit limitations

The scanner is not yet called by the document upload controller. The current document controller and local filesystem storage are demo-profile components. Outside the demo profile:

- `POST /api/documents` returns `503 Service Unavailable`.
- Officer document download returns `503 Service Unavailable`.
- No uploaded document can be marked `CLEAN` by this adapter because the production ingestion workflow is not yet implemented.

The demo SHA-256 check is an integrity check only: it detects whether stored bytes differ from the recorded fingerprint. It does not prove who issued a document, whether its contents are genuine, or whether it is malware-free.

## Network and daemon requirements

ClamAV's TCP daemon protocol is not encrypted or authenticated. Keep the daemon on a private, isolated application network; do not expose its TCP port to untrusted networks. Configure the daemon's `StreamMaxLength` to accommodate the application's 10 MB maximum with suitable operational headroom. The adapter uses ClamAV's documented NUL-framed `INSTREAM` protocol: [ClamD Protocol](https://docs.clamav.net/manual/Usage/ClamdProtocol.html).

## Required production integration sequence

1. Select and approve the malware-scanning service and deploy it on a private network with monitored signature-database updates.
2. Implement approved encrypted object storage using managed keys/KMS, private access, retention, audit logging and tested recovery. Do not use the demo local filesystem as production storage.
3. Implement a production ingestion service that stores new uploads as `PENDING_SCAN`, calculates immutable metadata, and prevents student/officer downloads while pending.
4. Invoke `DocumentScanner` before any document becomes `CLEAN`. Persist engine/version, scan timestamp, verdict and detection signature. Quarantine infected files and keep unavailable/unknown outcomes blocked.
5. Add retry, timeout, queue back-pressure, idempotency, quarantine review and secure deletion policies. A scanner outage must not silently approve or expose documents.
6. Add an integration test against a real ClamAV daemon in an isolated CI environment, including a harmless standard antivirus test signature, maximum-size handling, daemon outage and signature-database health.
7. Connect the production controller only after storage, scan orchestration, authorization, audit, retention and security review have passed.

Do not set `DTEF_ANTIVIRUS_ENABLED=true` in a live environment merely because the adapter compiles or its fake-daemon protocol tests pass. Production upload remains disabled until the entire ingestion pipeline is connected and verified.
