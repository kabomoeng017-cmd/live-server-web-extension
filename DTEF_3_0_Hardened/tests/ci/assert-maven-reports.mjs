import fs from 'node:fs';
import path from 'node:path';

const reportsDir = process.argv[2] || path.resolve('backend/target/surefire-reports');
if (!fs.existsSync(reportsDir)) {
  console.error(`JUnit report directory does not exist: ${reportsDir}`);
  process.exit(1);
}

const files = fs.readdirSync(reportsDir).filter(name => /^TEST-.*\.xml$/.test(name));
if (files.length === 0) {
  console.error('No JUnit XML reports were produced.');
  process.exit(1);
}

const totals = { tests: 0, failures: 0, errors: 0, skipped: 0 };
const suites = [];
for (const file of files) {
  const xml = fs.readFileSync(path.join(reportsDir, file), 'utf8');
  const tag = xml.match(/<testsuite\b[^>]*>/)?.[0];
  if (!tag) {
    console.error(`JUnit report is malformed or has no testsuite root: ${file}`);
    process.exit(1);
  }
  const read = key => Number(tag.match(new RegExp(`\\b${key}="(\\d+)"`))?.[1] || 0);
  const suite = {
    file,
    tests: read('tests'),
    failures: read('failures'),
    errors: read('errors'),
    skipped: read('skipped')
  };
  suites.push(suite);
  for (const key of Object.keys(totals)) totals[key] += suite[key];
}

const infrastructure = suites.find(suite => suite.file.includes('DtefInfrastructureIntegrationTest'));
if (!infrastructure) {
  console.error('The PostgreSQL/Redis/RabbitMQ Testcontainers suite did not produce a JUnit report.');
  process.exit(1);
}
if (infrastructure.tests < 3 || infrastructure.skipped > 0) {
  console.error('Infrastructure integration tests were skipped or not fully discovered:', infrastructure);
  process.exit(1);
}
if (totals.tests < 19) {
  console.error(`Expected at least 19 JUnit tests; found ${totals.tests}.`);
  process.exit(1);
}
if (totals.failures > 0 || totals.errors > 0 || totals.skipped > 0) {
  console.error('JUnit suite has failures, errors or skipped tests:', totals);
  process.exit(1);
}

console.log(`JUnit evidence verified: ${totals.tests} tests, ${totals.failures} failures, ${totals.errors} errors, ${totals.skipped} skipped.`);
console.log(`Infrastructure suite executed: ${infrastructure.tests} tests.`);
