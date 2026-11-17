import http from 'k6/http';
import { check, sleep } from 'k6';

const base = __ENV.DTEF_BASE_URL || 'http://localhost:8080';
const token = __ENV.DTEF_TOKEN || '';
const target = Number(__ENV.TARGET_VUS || 100);
export const options = {
  stages: [
    { duration: '2m', target: Math.max(1, Math.floor(target * 0.25)) },
    { duration: '3m', target },
    { duration: '3m', target },
    { duration: '2m', target: 0 }
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000', 'p(99)<2000']
  }
};
export default function () {
  const health = http.get(`${base}/api/health`);
  check(health, { 'health is 200': r => r.status === 200 });
  if (token) {
    const headers = { headers: { Authorization: `Bearer ${token}` } };
    const me = http.get(`${base}/api/auth/me`, headers);
    check(me, { 'authenticated identity is 200': r => r.status === 200 });
    const applications = http.get(`${base}/api/applications`, headers);
    check(applications, { 'student applications endpoint is 200': r => r.status === 200 });
  }
  sleep(1);
}
