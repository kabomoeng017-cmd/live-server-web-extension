import test from 'node:test';
import assert from 'node:assert/strict';
import { createHmac, timingSafeEqual } from 'node:crypto';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const generator = path.join(root, 'tests/load/generate-load-token.mjs');

function run(secret, issuer = 'dtef-load-test', subject = 'student.loadtest') {
  return spawnSync(process.execPath, [generator], {
    encoding: 'utf8',
    env: { ...process.env, JWT_SECRET_BASE64: secret, JWT_ISSUER: issuer, DTEF_LOAD_SUBJECT: subject }
  });
}

test('load-token generator creates a signed, short-lived student test token', () => {
  const key = Buffer.from('0123456789abcdef0123456789abcdef');
  const result = run(key.toString('base64'));
  assert.equal(result.status, 0, result.stderr);
  const token = result.stdout.trim();
  const parts = token.split('.');
  assert.equal(parts.length, 3);
  const header = JSON.parse(Buffer.from(parts[0], 'base64url').toString('utf8'));
  const claims = JSON.parse(Buffer.from(parts[1], 'base64url').toString('utf8'));
  const expected = createHmac('sha256', key).update(`${parts[0]}.${parts[1]}`).digest();
  const actual = Buffer.from(parts[2], 'base64url');
  assert.equal(header.alg, 'HS256');
  assert.equal(claims.iss, 'dtef-load-test');
  assert.deepEqual(claims.aud, ['dtef-api']);
  assert.equal(claims.sub, 'student.loadtest');
  assert.deepEqual(claims.roles, ['ROLE_STUDENT']);
  assert.equal(claims.token_use, 'access');
  assert.equal(claims.exp - claims.iat, 7200);
  assert.equal(actual.length, expected.length);
  assert.equal(timingSafeEqual(actual, expected), true);
});

test('load-token generator rejects secrets shorter than the backend HS256 minimum', () => {
  const result = run(Buffer.from('too-short').toString('base64'));
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /must decode to at least 32 bytes/);
  assert.equal(result.stdout, '');
});
