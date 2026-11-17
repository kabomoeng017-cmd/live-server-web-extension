import { createHmac } from 'node:crypto';

const secretBase64 = process.env.JWT_SECRET_BASE64 || 'MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=';
const issuer = process.env.JWT_ISSUER || 'dtef-local-demo';
const subject = process.env.DTEF_LOAD_SUBJECT || 'student.loadtest';
const key = Buffer.from(secretBase64, 'base64');

if (key.length < 32) {
  throw new Error('JWT_SECRET_BASE64 must decode to at least 32 bytes.');
}
if (!issuer.trim() || !subject.trim()) {
  throw new Error('JWT_ISSUER and DTEF_LOAD_SUBJECT must be non-empty.');
}

const now = Math.floor(Date.now() / 1000);
const encode = value => Buffer.from(JSON.stringify(value)).toString('base64url');
const header = encode({ alg: 'HS256', typ: 'JWT' });
const payload = encode({
  iss: issuer,
  aud: ['dtef-api'],
  sub: subject,
  iat: now,
  exp: now + 7200,
  roles: ['ROLE_STUDENT'],
  token_use: 'access'
});
const signingInput = `${header}.${payload}`;
const signature = createHmac('sha256', key).update(signingInput).digest('base64url');
process.stdout.write(`${signingInput}.${signature}\n`);
