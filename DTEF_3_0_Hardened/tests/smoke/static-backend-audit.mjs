import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
const root = new URL('../../', import.meta.url);
const read = p => readFile(new URL(p, root), 'utf8');

test('backend no longer permits every API request anonymously', async () => {
  const security = await read('backend/src/main/java/bw/gov/dtef/config/SecurityConfig.java');
  assert.doesNotMatch(security, /anyRequest\(\)\.permitAll\(\)/);
  assert.match(security, /anyRequest\(\)\.authenticated\(\)/);
  assert.match(security, /hasRole\("STUDENT"\)/);
  assert.match(security, /hasAnyRole\("FNBB", "ADMIN"\)/);
});
test('OTP is random, stored hashed and never returned in API response', async () => {
  const otp = await read('backend/src/main/java/bw/gov/dtef/auth/OtpService.java');
  const login = await read('backend/src/main/java/bw/gov/dtef/auth/DemoAuthController.java');
  const demoJwt = await read('backend/src/main/java/bw/gov/dtef/config/JwtConfig.java');
  const tokenService = await read('backend/src/main/java/bw/gov/dtef/auth/JwtTokenService.java');
  assert.match(demoJwt, /audience/);
  assert.match(tokenService, /audience\(List\.of\("dtef-api"\)\)/);
  assert.match(otp, /SecureRandom/);
  assert.match(otp, /encoder\.encode\(code\)/);
  assert.match(otp, /Duration\.ofSeconds\(ttlSeconds\)/);
  assert.match(otp, /setIfAbsent/);
  assert.match(otp, /dtef:otp:lock/);
  assert.doesNotMatch(login, /demoOtp|246810/);
});
test('draft storage rejects sensitive fields and restricts the schema', async () => {
  const draft = await read('backend/src/main/java/bw/gov/dtef/application/ApplicationDraftController.java');
  assert.match(draft, /ALLOWED_FIELDS/);
  assert.match(draft, /SENSITIVE_KEYS/);
  assert.match(draft, /containsSensitiveKey/);
  assert.match(draft, /containsUnsupportedNestedKey/);
});
test('submitted supporting documents are durably linked to applications', async () => {
  const service = await read('backend/src/main/java/bw/gov/dtef/application/ApplicationService.java');
  const migration = await read('backend/src/main/resources/db/migration/V3__application_integrity_constraints.sql');
  assert.match(service, /applicationDocuments\.saveAll/);
  assert.match(migration, /CREATE TABLE IF NOT EXISTS application_documents/);
  assert.match(migration, /REFERENCES documents\(id\)/);
});
test('application reference is allocated from database sequence', async () => {
  const repository = await read('backend/src/main/java/bw/gov/dtef/application/ApplicationRepository.java');
  const service = await read('backend/src/main/java/bw/gov/dtef/application/ApplicationService.java');
  assert.match(repository, /nextval\('dtef_application_ref_seq'\)/);
  assert.match(service, /nextReferenceSequence\(\)/);
  assert.doesNotMatch(service, /AtomicInteger/);
});
test('database schema is migration-controlled and Hibernate validates', async () => {
  const app = await read('backend/src/main/resources/application.yml');
  const migration = await read('backend/src/main/resources/db/migration/V1__initial_schema.sql');
  assert.match(app, /ddl-auto: validate/);
  assert.match(app, /flyway:/);
  assert.match(migration, /CREATE SEQUENCE/);
  assert.match(migration, /CREATE TABLE IF NOT EXISTS audit_events/);
});
test('backend does not silently activate demo profile', async () => {
  const app = await read('backend/src/main/resources/application.yml');
  assert.doesNotMatch(app, /profiles:\s*(?:active|default):.*demo/);
});
test('production profile requires external infrastructure credentials instead of demo defaults', async () => {
  const prod = await read('backend/src/main/resources/application-prod.yml');
  assert.match(prod, /url: \${DB_URL}/);
  assert.match(prod, /password: \${DB_PASSWORD}/);
  assert.match(prod, /issuer-uri: \${OIDC_ISSUER_URI}/);
  const oidc = await read('backend/src/main/java/bw/gov/dtef/config/OidcJwtConfig.java');
  assert.match(oidc, /OIDC_AUDIENCE/);
  assert.match(oidc, /audienceValidator/);
  assert.doesNotMatch(prod, /dtef_dev_password|dtef_local_only|localhost/);
});
test('production notification send path fails closed until a delivery provider exists', async () => {
  const notifications = await read('backend/src/main/java/bw/gov/dtef/notification/NotificationController.java');
  assert.match(notifications, /acceptsProfiles\(Profiles\.of\("demo"\)\)/);
  assert.match(notifications, /SERVICE_UNAVAILABLE/);
});
test('production profile disables demo-only identity and OCR adapters', async () => {
  const omang = await read('backend/src/main/java/bw/gov/dtef/auth/OmangController.java');
  const ocr = await read('backend/src/main/java/bw/gov/dtef/ai/AiController.java');
  const documentUpload = await read('backend/src/main/java/bw/gov/dtef/document/DocumentController.java');
  const disabledDocumentUpload = await read('backend/src/main/java/bw/gov/dtef/document/DocumentIntegrationUnavailableController.java');
  assert.match(omang, /@Profile\("demo"\)/);
  assert.match(ocr, /@Profile\("demo"\)/);
  assert.match(documentUpload, /@Profile\("demo"\)/);
  assert.match(disabledDocumentUpload, /@Profile\("!demo"\)/);
});

test('notification messages are explicitly marked persistent for RabbitMQ', async () => {
  const queue = await read('backend/src/main/java/bw/gov/dtef/notification/NotificationQueueService.java');
  assert.match(queue, /setDeliveryMode\(org\.springframework\.amqp\.core\.MessageDeliveryMode\.PERSISTENT\)/);
});

test('RabbitMQ Testcontainers credentials come from the container admin configuration', async () => {
  const integration = await read('backend/src/test/java/bw/gov/dtef/integration/DtefInfrastructureIntegrationTest.java');
  assert.match(integration, /\.withAdminUser\("dtef_it"\)/);
  assert.match(integration, /\.withAdminPassword\("dtef_it_password"\)/);
  assert.match(integration, /registry\.add\("spring\.rabbitmq\.username", RABBIT::getAdminUsername\)/);
  assert.match(integration, /registry\.add\("spring\.rabbitmq\.password", RABBIT::getAdminPassword\)/);
  assert.match(integration, /getMessageProperties\(\)\.getReceivedDeliveryMode\(\)/);
  assert.doesNotMatch(integration, /getMessageProperties\(\)\.getDeliveryMode\(\)/);
  assert.doesNotMatch(integration, /\.withEnv\("RABBITMQ_DEFAULT_(?:USER|PASS)"/);
});

test('CI requires Testcontainers execution and validates JUnit evidence', async () => {
  const integration = await read('backend/src/test/java/bw/gov/dtef/integration/DtefInfrastructureIntegrationTest.java');
  const workflow = await read('.github/workflows/ci.yml');
  assert.match(integration, /@Testcontainers\s+class DtefInfrastructureIntegrationTest/);
  assert.doesNotMatch(integration, /disabledWithoutDocker/);
  assert.match(workflow, /mvn -B -ntp clean verify/);
  assert.match(workflow, /assert-maven-reports\.mjs/);
});

test('staged load profile defaults to 100 users and retains progressive capacity steps', async () => {
  const load = await read('tests/load/k6-dtef-staged.js');
  const runner = await read('tests/load/run-staged-load.sh');
  assert.match(load, /MAX_VUS \|\| '100'/);
  assert.match(load, /\[100, 500, 1000, 2000, 5000, 10000\]/);
  assert.match(runner, /DTEF_LOAD_TEST_AUTHORIZED/);
  assert.match(runner, /DTEF_HIGH_SCALE_APPROVED/);
  assert.match(runner, /DTEF_EXTREME_SCALE_APPROVED/);
  assert.match(runner, /Remote OIDC staging must provide a short-lived DTEF_TOKEN/);
  assert.match(runner, /local demo defaults are never used remotely/);
});

test('application status transitions are restricted to officer and admin roles', async () => {
  const security = await read('backend/src/main/java/bw/gov/dtef/config/SecurityConfig.java');
  const controller = await read('backend/src/main/java/bw/gov/dtef/application/ApplicationController.java');
  assert.ok(security.includes('.requestMatchers(HttpMethod.PUT, "/api/applications/**").hasAnyRole("OFFICER", "ADMIN")'));
  assert.match(controller, /@PutMapping\(\"\/\{reference\}\/status\"\)/);
});


test('officer rejection decisions require and persist a reason', async () => {
  const service = await read('backend/src/main/java/bw/gov/dtef/application/ApplicationService.java');
  const controller = await read('backend/src/main/java/bw/gov/dtef/application/ApplicationController.java');
  const history = await read('backend/src/main/java/bw/gov/dtef/application/ApplicationStatusHistory.java');
  const migration = await read('backend/src/main/resources/db/migration/V4__application_decision_reason.sql');
  assert.match(service, /status == ApplicationStatus\.REJECTED && \(normalizedReason == null \|\| normalizedReason\.isBlank\(\)\)/);
  assert.match(service, /APPLICATION_REJECTED/);
  assert.match(controller, /request\.reason\(\)/);
  assert.match(history, /decision_reason/);
  assert.match(migration, /ADD COLUMN decision_reason VARCHAR\(500\)/);
});

test('officer review endpoints remain role-gated', async () => {
  const security = await read('backend/src/main/java/bw/gov/dtef/config/SecurityConfig.java');
  assert.match(security, /requestMatchers\("\/api\/officer\/\*\*"\)\.hasAnyRole\("OFFICER", "ADMIN"\)/);
  assert.match(security, /requestMatchers\(HttpMethod\.PUT, "\/api\/applications\/\*\*"\)\.hasAnyRole\("OFFICER", "ADMIN"\)/);
});


test('backend uses patched Spring Boot 4.1.2 modular starters and Jackson 3', async () => {
  const pom = await read('backend/pom.xml');
  const security = await read('backend/src/main/java/bw/gov/dtef/config/SecurityConfig.java');
  const draft = await read('backend/src/main/java/bw/gov/dtef/application/ApplicationDraftController.java');
  const queue = await read('backend/src/main/java/bw/gov/dtef/notification/NotificationQueueConfig.java');
  assert.match(pom, /<version>4\.1\.2<\/version>/);
  assert.match(pom, /spring-boot-starter-webmvc/);
  assert.match(pom, /spring-boot-starter-security-oauth2-resource-server/);
  assert.match(pom, /spring-boot-starter-flyway/);
  assert.match(pom, /spring-boot-starter-webmvc-test/);
  assert.doesNotMatch(pom, /spring-boot-starter-web<\/artifactId>/);
  assert.doesNotMatch(security + draft, /com\.fasterxml\.jackson\.databind/);
  assert.match(security + draft, /tools\.jackson\.databind/);
  assert.match(queue, /JacksonJsonMessageConverter/);
  assert.doesNotMatch(queue, /Jackson2JsonMessageConverter/);
});
