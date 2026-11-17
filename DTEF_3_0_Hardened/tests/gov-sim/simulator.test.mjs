import test from 'node:test';
import assert from 'node:assert/strict';
import { createGovernmentSimulator } from './server.mjs';

let server;
let baseUrl;

test.before(async () => {
  server = createGovernmentSimulator();
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});

test.after(async () => {
  await new Promise((resolve, reject) => server.close(error => error ? reject(error) : resolve()));
});

test('exposes a health response that identifies itself as a simulator', async () => {
  const response = await fetch(`${baseUrl}/health`);
  assert.equal(response.status, 200);
  assert.equal(response.headers.get('cache-control'), 'no-store');
  assert.match(response.headers.get('content-type') || '', /application\/json/);
  assert.equal((await response.json()).mode, 'SIMULATOR_ONLY');
});

test('simulates Omang validation without using a real identity register', async () => {
  const valid = await fetch(`${baseUrl}/omang/verify`, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ omang: '123456789' }) });
  assert.equal(valid.status, 200);
  const validBody = await valid.json();
  assert.equal(validBody.provider, 'SIMULATED_OMANG');
  assert.equal(validBody.testRecord, true);
  const invalid = await fetch(`${baseUrl}/omang/verify`, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ omang: '123' }) });
  assert.equal(invalid.status, 422);
});

test('simulates idempotent FNBB payment requests', async () => {
  const options = { method: 'POST', headers: { 'content-type': 'application/json', 'idempotency-key': 'test-payment-001' }, body: JSON.stringify({ applicationReference: 'DTEF-2026-00000001', amount: 1000 }) };
  const firstResponse = await fetch(`${baseUrl}/fnbb/payments`, options);
  assert.equal(firstResponse.status, 202);
  const first = await firstResponse.json();
  const secondResponse = await fetch(`${baseUrl}/fnbb/payments`, options);
  assert.equal(secondResponse.status, 202);
  const second = await secondResponse.json();
  assert.equal(first.paymentReference, second.paymentReference);
  assert.equal(first.status, 'PENDING');
  assert.equal(first.currency, 'BWP');
  const lookup = await fetch(`${baseUrl}/fnbb/payments/${first.paymentReference}`);
  assert.equal(lookup.status, 200);
  assert.equal((await lookup.json()).paymentReference, first.paymentReference);
});

test('simulates upstream failure and delay controls', async () => {
  const failure = await fetch(`${baseUrl}/health`, { headers: { 'x-simulate-status': '503' } });
  assert.equal(failure.status, 503);
  assert.equal((await failure.json()).error, 'SIMULATED_UPSTREAM_FAILURE');
  const started = Date.now();
  const delayed = await fetch(`${baseUrl}/health`, { headers: { 'x-simulate-delay-ms': '100' } });
  assert.equal(delayed.status, 200);
  assert.ok(Date.now() - started >= 80);
});

test('simulates institution registry lookup and enrolment confirmation', async () => {
  const lookup = await fetch(`${baseUrl}/institutions/UB/enrolments/STU-TEST-001`);
  assert.equal(lookup.status, 200);
  assert.equal((await lookup.json()).registrationStatus, 'ACTIVE');
  const confirmation = await fetch(`${baseUrl}/institutions/enrolments/confirm`, {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ applicationReference: 'DTEF-2026-00000001', studentReference: 'STU-TEST-001' })
  });
  assert.equal(confirmation.status, 200);
  assert.equal((await confirmation.json()).confirmed, true);
});

test('simulates social welfare household lookup and assessment acceptance', async () => {
  const household = await fetch(`${baseUrl}/social-welfare/households/SIM-HH-002`);
  assert.equal(household.status, 200);
  assert.equal((await household.json()).priority, 'HIGH');
  const assessment = await fetch(`${baseUrl}/social-welfare/assessments`, {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ householdReference: 'SIM-HH-002', applicationReference: 'DTEF-2026-00000001' })
  });
  assert.equal(assessment.status, 202);
});

test('simulates notification gateway acceptance without sending a real message', async () => {
  const response = await fetch(`${baseUrl}/notifications/messages`, {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ recipientReference: 'student.test', channel: 'SMS', templateId: 'APPLICATION_RECEIVED' })
  });
  assert.equal(response.status, 202);
  const result = await response.json();
  assert.equal(result.source, 'SIMULATED_NOTIFICATION_GATEWAY');
  assert.equal(result.status, 'QUEUED');
});


test('rejects institution confirmation when either reference is missing', async () => {
  const response = await fetch(`${baseUrl}/institutions/enrolments/confirm`, {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ applicationReference: 'DTEF-2026-00000002' })
  });
  assert.equal(response.status, 422);
  assert.equal((await response.json()).code, 'MISSING_REFERENCE');
});

test('rejects social welfare assessments without both references', async () => {
  const response = await fetch(`${baseUrl}/social-welfare/assessments`, {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ householdReference: 'SIM-HH-001' })
  });
  assert.equal(response.status, 422);
  assert.equal((await response.json()).code, 'MISSING_ASSESSMENT_REFERENCE');
});

test('rejects invalid FNBB amounts and missing idempotency keys', async () => {
  const invalidAmount = await fetch(`${baseUrl}/fnbb/payments`, {
    method: 'POST', headers: { 'content-type': 'application/json', 'idempotency-key': 'bad-amount-001' },
    body: JSON.stringify({ applicationReference: 'DTEF-2026-00000003', amount: -1 })
  });
  assert.equal(invalidAmount.status, 422);
  const missingKey = await fetch(`${baseUrl}/fnbb/payments`, {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ applicationReference: 'DTEF-2026-00000003', amount: 1 })
  });
  assert.equal(missingKey.status, 422);
});

test('rejects reuse of an FNBB idempotency key with a different payment payload', async () => {
  const headers = { 'content-type': 'application/json', 'idempotency-key': 'same-key-different-payload' };
  const first = await fetch(`${baseUrl}/fnbb/payments`, {
    method: 'POST', headers, body: JSON.stringify({ applicationReference: 'DTEF-2026-00000004', amount: 100 })
  });
  assert.equal(first.status, 202);
  const second = await fetch(`${baseUrl}/fnbb/payments`, {
    method: 'POST', headers, body: JSON.stringify({ applicationReference: 'DTEF-2026-00000004', amount: 200 })
  });
  assert.equal(second.status, 409);
  assert.equal((await second.json()).code, 'IDEMPOTENCY_KEY_REUSED');
});

test('rejects malformed notification payloads', async () => {
  const response = await fetch(`${baseUrl}/notifications/messages`, {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ recipientReference: 'student.test', channel: 'SMS', templateId: 'bad template!' })
  });
  assert.equal(response.status, 422);
  assert.equal((await response.json()).code, 'INVALID_NOTIFICATION_REQUEST');
});

test('rejects malformed JSON and oversized simulator payloads safely', async () => {
  const malformed = await fetch(`${baseUrl}/omang/verify`, {
    method: 'POST', headers: { 'content-type': 'application/json' }, body: '{'
  });
  assert.equal(malformed.status, 400);
  assert.equal((await malformed.json()).error, 'INVALID_JSON');

  const oversized = await fetch(`${baseUrl}/omang/verify`, {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ omang: 'x'.repeat(70_000) })
  });
  assert.equal(oversized.status, 413);
  assert.equal((await oversized.json()).error, 'PAYLOAD_TOO_LARGE');
});


test('rejects unknown social welfare household assessment references', async () => {
  const response = await fetch(`${baseUrl}/social-welfare/assessments`, {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ householdReference: 'SIM-HH-404', applicationReference: 'DTEF-2026-00000005' })
  });
  assert.equal(response.status, 404);
  assert.equal((await response.json()).code, 'HOUSEHOLD_NOT_FOUND');
});

test('rejects malformed DTEF application references at provider boundaries', async () => {
  const response = await fetch(`${baseUrl}/fnbb/payments`, {
    method: 'POST', headers: { 'content-type': 'application/json', 'idempotency-key': 'bad-reference-001' },
    body: JSON.stringify({ applicationReference: 'not-a-dtef-reference', amount: 100 })
  });
  assert.equal(response.status, 422);
});
