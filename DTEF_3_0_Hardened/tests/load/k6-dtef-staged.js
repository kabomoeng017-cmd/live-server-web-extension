import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const failures = new Rate('dtef_request_failures');
const apiLatency = new Trend('dtef_api_latency', true);
const baseUrl = (__ENV.DTEF_BASE_URL || 'http://127.0.0.1:8080').replace(/\/$/, '');
const token = __ENV.DTEF_TOKEN;
const requestedMaximum = Number.parseInt(__ENV.MAX_VUS || '100', 10);
const maximum = Number.isFinite(requestedMaximum) ? Math.min(100000, Math.max(100, requestedMaximum)) : 100;
const levels = [100, 500, 1000, 2000, 5000, 10000].filter(value => value <= maximum);
if (!levels.includes(maximum)) levels.push(maximum);
levels.sort((a, b) => a - b);
const stages = [];
for (const level of levels) {
  stages.push({ target: level, duration: level <= 100 ? '2m' : '3m' });
  stages.push({ target: level, duration: level <= 1000 ? '3m' : '5m' });
}
stages.push({ target: 0, duration: '2m' });

export const options = {
  stages,
  gracefulRampDown: '30s',
  thresholds: {
    http_req_failed: ['rate<0.02'],
    dtef_request_failures: ['rate<0.02'],
    dtef_api_latency: ['p(95)<1200', 'p(99)<2500']
  },
  tags: { project: 'dtef-3', test_type: 'staged-load' }
};

export function setup() {
  if (!token) throw new Error('DTEF_TOKEN is required. Generate a short-lived token using tests/load/generate-load-token.mjs.');
  const health = http.get(`${baseUrl}/api/health`);
  check(health, { 'backend health endpoint responds': response => response.status === 200 });
  if (health.status !== 200) throw new Error(`Backend health preflight failed with HTTP ${health.status}`);
  const authenticated = http.get(`${baseUrl}/api/applications`, { headers: { Authorization: `Bearer ${token}` } });
  check(authenticated, { 'load-test token is accepted': response => response.status === 200 });
  if (authenticated.status !== 200) throw new Error(`Authenticated preflight failed with HTTP ${authenticated.status}`);
  return { token };
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}`, Accept: 'application/json' };
  const requests = [
    ['applications', http.get(`${baseUrl}/api/applications`, { headers })],
    ['draft', http.get(`${baseUrl}/api/applications/draft`, { headers })],
    ['identity', http.get(`${baseUrl}/api/auth/me`, { headers })]
  ];
  for (const [name, response] of requests) {
    const ok = check(response, { [`${name} request returns success`]: result => result.status === 200 });
    failures.add(!ok);
    apiLatency.add(response.timings.duration, { endpoint: name });
  }
  sleep(1);
}
