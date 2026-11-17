import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const verifier = path.join(here, 'assert-maven-reports.mjs');

function withReports(files, run) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'dtef-junit-'));
  try {
    for (const [name, attributes] of Object.entries(files)) {
      fs.writeFileSync(path.join(dir, name), `<testsuite ${attributes}></testsuite>`, 'utf8');
    }
    return run(dir);
  } finally {
    fs.rmSync(dir, { recursive: true, force: true });
  }
}

function execute(dir) {
  return spawnSync(process.execPath, [verifier, dir], { encoding: 'utf8' });
}

test('JUnit verifier accepts a complete suite with all three infrastructure tests', () => {
  withReports({
    'TEST-ApplicationServiceTest.xml': 'tests="16" failures="0" errors="0" skipped="0"',
    'TEST-DtefInfrastructureIntegrationTest.xml': 'tests="3" failures="0" errors="0" skipped="0"'
  }, dir => {
    const result = execute(dir);
    assert.equal(result.status, 0, result.stderr || result.stdout);
    assert.match(result.stdout, /19 tests, 0 failures, 0 errors, 0 skipped/);
    assert.match(result.stdout, /Infrastructure suite executed: 3 tests/);
  });
});

test('JUnit verifier rejects a missing infrastructure report', () => {
  withReports({
    'TEST-ApplicationServiceTest.xml': 'tests="21" failures="0" errors="0" skipped="0"'
  }, dir => {
    const result = execute(dir);
    assert.equal(result.status, 1);
    assert.match(result.stderr, /Testcontainers suite did not produce a JUnit report/);
  });
});

test('JUnit verifier rejects skipped infrastructure tests', () => {
  withReports({
    'TEST-ApplicationServiceTest.xml': 'tests="18" failures="0" errors="0" skipped="0"',
    'TEST-DtefInfrastructureIntegrationTest.xml': 'tests="3" failures="0" errors="0" skipped="1"'
  }, dir => {
    const result = execute(dir);
    assert.equal(result.status, 1);
    assert.match(result.stderr, /Infrastructure integration tests were skipped/);
  });
});

test('JUnit verifier rejects malformed XML reports instead of silently ignoring them', () => {
  withReports({
    'TEST-ApplicationServiceTest.xml': 'tests=\"16\" failures=\"0\" errors=\"0\" skipped=\"0\"',
    'TEST-DtefInfrastructureIntegrationTest.xml': 'tests=\"3\" failures=\"0\" errors=\"0\" skipped=\"0\"'
  }, dir => {
    fs.writeFileSync(path.join(dir, 'TEST-Broken.xml'), '<testsuite broken', 'utf8');
    const result = execute(dir);
    assert.equal(result.status, 1);
    assert.match(result.stderr, /malformed or has no testsuite root/);
  });
});

test('JUnit verifier rejects failures and incomplete test totals', () => {
  withReports({
    'TEST-ApplicationServiceTest.xml': 'tests="15" failures="1" errors="0" skipped="0"',
    'TEST-DtefInfrastructureIntegrationTest.xml': 'tests="3" failures="0" errors="0" skipped="0"'
  }, dir => {
    const result = execute(dir);
    assert.equal(result.status, 1);
    assert.match(result.stderr, /Expected at least 19 JUnit tests/);
  });
});

test('backend container image build does not execute Testcontainers inside docker build', () => {
  const dockerfile = fs.readFileSync(path.resolve(here, '../../backend/Dockerfile'), 'utf8');
  assert.match(dockerfile, /-DskipTests\s+clean\s+package/);
  assert.doesNotMatch(dockerfile, /mvn[^\n]*clean\s+verify/);
});

test('load workflow binds its target to a protected host and avoids command-line token expansion', () => {
  const workflow = fs.readFileSync(path.resolve(here, '../../.github/workflows/load-test.yml'), 'utf8');
  assert.match(workflow, /APPROVED_HOST:\s*\$\{\{\s*vars\.DTEF_LOAD_ALLOWED_HOST\s*\}\}/);
  assert.match(workflow, /DTEF_LOAD_AUTHORIZATION_REFERENCE/);
  assert.match(workflow, /TEST_JWT_TRUSTED/);
  assert.match(workflow, /--env-file\s+"\$K6_ENV_FILE"/);
  assert.doesNotMatch(workflow, /-e DTEF_TOKEN="\$\(cat load-token\.txt\)"/);
});
