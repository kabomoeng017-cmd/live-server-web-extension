import http from 'node:http';
import { randomUUID } from 'node:crypto';

const payments = new Map();
const households = new Map([
  ['SIM-HH-001', { householdReference: 'SIM-HH-001', assessmentStatus: 'ASSESSED', priority: 'STANDARD', source: 'SIMULATED_SOCIAL_WELFARE' }],
  ['SIM-HH-002', { householdReference: 'SIM-HH-002', assessmentStatus: 'ASSESSED', priority: 'HIGH', source: 'SIMULATED_SOCIAL_WELFARE' }]
]);

function send(response, status, payload) {
  response.writeHead(status, { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store' });
  response.end(JSON.stringify(payload));
}

async function readJson(request) {
  let body = '';
  let bytes = 0;
  for await (const chunk of request) {
    const text = Buffer.isBuffer(chunk) ? chunk.toString('utf8') : String(chunk);
    bytes += Buffer.byteLength(text, 'utf8');
    if (bytes > 64 * 1024) throw new Error('PAYLOAD_TOO_LARGE');
    body += text;
  }
  if (!body) return {};
  return JSON.parse(body);
}

function validApplicationReference(value) {
  return typeof value === 'string' && /^DTEF-\d{4}-\d{8}$/.test(value);
}

async function simulate(request, response) {
  const requestedDelay = Number(request.headers['x-simulate-delay-ms'] || 0);
  if (Number.isFinite(requestedDelay) && requestedDelay > 0) {
    await new Promise(resolve => setTimeout(resolve, Math.min(requestedDelay, 5000)));
  }
  const failure = Number(request.headers['x-simulate-status'] || 0);
  if ([429, 500, 502, 503, 504].includes(failure)) {
    send(response, failure, { error: 'SIMULATED_UPSTREAM_FAILURE', retryable: failure >= 500 });
    return true;
  }
  return false;
}

export function createGovernmentSimulator() {
  return http.createServer(async (request, response) => {
    try {
      if (await simulate(request, response)) return;
      const url = new URL(request.url || '/', 'http://127.0.0.1');
      const method = request.method || 'GET';

      if (method === 'GET' && url.pathname === '/health') {
        send(response, 200, { status: 'UP', mode: 'SIMULATOR_ONLY' });
        return;
      }

      if (method === 'POST' && url.pathname === '/omang/verify') {
        const body = await readJson(request);
        if (!/^\d{9}$/.test(String(body.omang || ''))) {
          send(response, 422, { verified: false, code: 'INVALID_TEST_ID', provider: 'SIMULATED_OMANG' });
          return;
        }
        send(response, 200, { verified: true, verificationReference: 'SIM-OMANG-0001', provider: 'SIMULATED_OMANG', testRecord: true });
        return;
      }

      if (method === 'GET' && /^\/institutions\/[A-Z0-9_-]+\/enrolments\/[A-Z0-9_-]+$/.test(url.pathname)) {
        send(response, 200, { found: true, registrationStatus: 'ACTIVE', programmeStatus: 'CURRENT', source: 'SIMULATED_INSTITUTION_REGISTRY' });
        return;
      }

      if (method === 'POST' && url.pathname === '/institutions/enrolments/confirm') {
        const body = await readJson(request);
        if (!validApplicationReference(body.applicationReference) ||
            typeof body.studentReference !== 'string' || !/^[A-Z0-9_-]{3,64}$/.test(body.studentReference)) {
          send(response, 422, { confirmed: false, code: 'MISSING_REFERENCE', source: 'SIMULATED_INSTITUTION_REGISTRY' });
          return;
        }
        send(response, 200, { confirmed: true, confirmationReference: 'SIM-INST-0001', source: 'SIMULATED_INSTITUTION_REGISTRY' });
        return;
      }

      const householdMatch = url.pathname.match(/^\/social-welfare\/households\/([A-Z0-9_-]+)$/);
      if (method === 'GET' && householdMatch) {
        const result = households.get(householdMatch[1]);
        if (!result) { send(response, 404, { found: false, source: 'SIMULATED_SOCIAL_WELFARE' }); return; }
        send(response, 200, { found: true, ...result });
        return;
      }

      if (method === 'POST' && url.pathname === '/social-welfare/assessments') {
        const body = await readJson(request);
        if (typeof body.householdReference !== 'string' || !body.householdReference.trim() ||
            !validApplicationReference(body.applicationReference)) {
          send(response, 422, { accepted: false, code: 'MISSING_ASSESSMENT_REFERENCE', source: 'SIMULATED_SOCIAL_WELFARE' });
          return;
        }
        if (!households.has(body.householdReference)) {
          send(response, 404, { accepted: false, code: 'HOUSEHOLD_NOT_FOUND', source: 'SIMULATED_SOCIAL_WELFARE' });
          return;
        }
        send(response, 202, { accepted: true, assessmentReference: `SIM-WELFARE-${randomUUID()}`, status: 'QUEUED', source: 'SIMULATED_SOCIAL_WELFARE' });
        return;
      }

      if (method === 'POST' && url.pathname === '/fnbb/payments') {
        const body = await readJson(request);
        const idempotencyKey = String(request.headers['idempotency-key'] || '');
        const amount = Number(body.amount);
        if (!idempotencyKey || idempotencyKey.length > 128 ||
            !validApplicationReference(body.applicationReference) ||
            !Number.isFinite(amount) || amount <= 0 || amount > 10_000_000) {
          send(response, 422, { accepted: false, code: 'INVALID_PAYMENT_REQUEST', source: 'SIMULATED_FNBB' });
          return;
        }
        const existing = payments.get(idempotencyKey);
        if (existing && (existing.applicationReference !== body.applicationReference || existing.amount !== amount)) {
          send(response, 409, { accepted: false, code: 'IDEMPOTENCY_KEY_REUSED', source: 'SIMULATED_FNBB' });
          return;
        }
        if (!existing) {
          payments.set(idempotencyKey, { paymentReference: `SIM-FNBB-${payments.size + 1}`,
            applicationReference: body.applicationReference, status: 'PENDING', amount, currency: 'BWP' });
        }
        send(response, 202, { accepted: true, ...payments.get(idempotencyKey), source: 'SIMULATED_FNBB' });
        return;
      }

      const paymentMatch = url.pathname.match(/^\/fnbb\/payments\/(SIM-FNBB-\d+)$/);
      if (method === 'GET' && paymentMatch) {
        const result = [...payments.values()].find(item => item.paymentReference === paymentMatch[1]);
        if (!result) { send(response, 404, { found: false, source: 'SIMULATED_FNBB' }); return; }
        send(response, 200, { found: true, ...result, source: 'SIMULATED_FNBB' });
        return;
      }

      if (method === 'POST' && url.pathname === '/notifications/messages') {
        const body = await readJson(request);
        if (typeof body.recipientReference !== 'string' || !body.recipientReference.trim() ||
            !['SMS', 'EMAIL', 'WHATSAPP', 'IN_APP'].includes(body.channel) ||
            typeof body.templateId !== 'string' || !/^[A-Z0-9_]{3,64}$/.test(body.templateId)) {
          send(response, 422, { accepted: false, code: 'INVALID_NOTIFICATION_REQUEST', source: 'SIMULATED_NOTIFICATION_GATEWAY' });
          return;
        }
        send(response, 202, { accepted: true, providerMessageReference: `SIM-MSG-${randomUUID()}`, status: 'QUEUED', source: 'SIMULATED_NOTIFICATION_GATEWAY' });
        return;
      }

      send(response, 404, { error: 'SIMULATOR_ROUTE_NOT_FOUND', path: url.pathname });
    } catch (error) {
      const status = error instanceof SyntaxError ? 400 : error?.message === 'PAYLOAD_TOO_LARGE' ? 413 : 500;
      send(response, status, { error: status === 400 ? 'INVALID_JSON' : status === 413 ? 'PAYLOAD_TOO_LARGE' : 'SIMULATOR_ERROR' });
    }
  });
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const port = Number(process.env.PORT || 9090);
  createGovernmentSimulator().listen(port, '0.0.0.0', () => {
    process.stdout.write(`DTEF government integration simulator listening on ${port}; test data only\n`);
  });
}
