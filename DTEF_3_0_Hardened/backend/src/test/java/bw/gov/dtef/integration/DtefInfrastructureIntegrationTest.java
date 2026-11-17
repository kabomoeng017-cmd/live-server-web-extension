package bw.gov.dtef.integration;

import bw.gov.dtef.auth.OtpDeliveryService;
import bw.gov.dtef.notification.NotificationQueueConfig;
import bw.gov.dtef.notification.NotificationQueueService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real infrastructure integration tests. These use disposable PostgreSQL, Redis and RabbitMQ
 * containers and must never be pointed at government or production services.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles({"demo", "integration"})
@Testcontainers
class DtefInfrastructureIntegrationTest {
    private static final String STORAGE_DIR = System.getProperty("java.io.tmpdir") + "/dtef-it-uploads";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("dtef_it").withUsername("dtef_it").withPassword("dtef_it_password");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
        .withAdminUser("dtef_it")
        .withAdminPassword("dtef_it_password");

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registry.add("dtef.storage-dir", () -> STORAGE_DIR);
    }

    @Autowired TestRestTemplate http;
    @Autowired CapturingOtpDeliveryService otpDelivery;
    @Autowired JdbcTemplate jdbc;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired RabbitProperties rabbitProperties;
    @Autowired CachingConnectionFactory rabbitConnectionFactory;
    @Autowired NotificationQueueService notificationQueue;
    @Autowired StringRedisTemplate redis;

    @Test
    void postgresFlywayRedisOtpAndApplicationDocumentSubmissionWorkTogether() {
        String migrationVersion = jdbc.queryForObject(
            "select max(version) from flyway_schema_history where success = true", String.class);
        assertThat(migrationVersion).isEqualTo("4");

        // Prove the integration test is using the disposable Redis service, not an in-memory substitute.
        String redisProbe = "dtef:integration:probe:" + java.util.UUID.randomUUID();
        redis.opsForValue().set(redisProbe, "connected", Duration.ofSeconds(15));
        assertThat(redis.opsForValue().get(redisProbe)).isEqualTo("connected");
        redis.delete(redisProbe);

        String token = loginAsStudent();
        Map<String, Object> identity = postJson("/api/omang/verify", token, Map.of("omang", "123456789"), HttpStatus.OK);
        assertThat(identity.get("verified")).isEqualTo(true);
        String verificationId = (String) identity.get("verificationId");

        Map<String, Object> admission = upload(token, "ADMISSION_LETTER", "admission.pdf");
        Map<String, Object> omangCopy = upload(token, "OMANG_COPY", "omang-copy.pdf");
        Map<String, Object> bankProof = upload(token, "BANK_CONFIRMATION", "bank-proof.pdf");

        postJson("/api/auth/send-action-otp", token, Map.of("purpose", "APPLICATION_SUBMIT"), HttpStatus.OK);
        String submitOtp = otpDelivery.latest("APPLICATION_SUBMIT", "student.demo");
        Map<String, Object> request = Map.of(
            "institution", "University of Botswana",
            "programme", "BSc Computer Science",
            "identityVerificationId", verificationId,
            "admissionDocumentId", admission.get("documentId"),
            "supportingDocumentIds", java.util.List.of(omangCopy.get("documentId"), bankProof.get("documentId")),
            "otp", submitOtp
        );

        Map<String, Object> submitted = postJson("/api/applications", token, request, HttpStatus.OK);
        String reference = (String) submitted.get("reference");
        assertThat(reference).matches("DTEF-\\d{4}-\\d{8}");
        assertThat(submitted.get("status")).isEqualTo("APPLIED");
        assertThat(jdbc.queryForObject(
            "select count(*) from application_documents where application_reference = ?", Integer.class, reference))
            .isEqualTo(3);
        assertThat(jdbc.queryForObject(
            "select count(*) from documents where owner_subject = ?", Integer.class, "student.demo"))
            .isEqualTo(3);
        assertThat(jdbc.queryForObject(
            "select count(*) from application_status_history where application_reference = ?", Integer.class, reference))
            .isEqualTo(1);

        ResponseEntity<Map> tracker = http.exchange("/api/applications/" + reference, HttpMethod.GET,
            new HttpEntity<>(bearer(token)), Map.class);
        assertThat(tracker.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tracker.getBody()).containsEntry("reference", reference);

        ResponseEntity<java.util.List> history = http.exchange("/api/applications/" + reference + "/history", HttpMethod.GET,
            new HttpEntity<>(bearer(token)), java.util.List.class);
        assertThat(history.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(history.getBody()).hasSize(1);

        ResponseEntity<Map> replay = http.exchange("/api/applications", HttpMethod.POST,
            new HttpEntity<>(request, jsonBearer(token)), Map.class);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rabbitMqDurablyAcceptsQueuedNotificationMessages() throws Exception {
        assertThat(rabbitProperties.getUsername()).isEqualTo(RABBIT.getAdminUsername());
        assertThat(rabbitProperties.getPassword()).isEqualTo(RABBIT.getAdminPassword());
        assertThat(rabbitConnectionFactory.getUsername()).isEqualTo(RABBIT.getAdminUsername());
        assertThat(rabbitTemplate.getConnectionFactory()).isSameAs(rabbitConnectionFactory);
        assertThat(rabbitConnectionFactory.getHost()).isEqualTo(RABBIT.getHost());
        assertThat(rabbitConnectionFactory.getPort()).isEqualTo(RABBIT.getAmqpPort());
        assertThat(rabbitConnectionFactory.getRabbitConnectionFactory().getUsername()).isEqualTo(RABBIT.getAdminUsername());
        assertThat(rabbitConnectionFactory.getRabbitConnectionFactory().getPassword()).isEqualTo(RABBIT.getAdminPassword());
        assertThat(rabbitConnectionFactory.getRabbitConnectionFactory().getVirtualHost()).isEqualTo("/");

        com.rabbitmq.client.ConnectionFactory directFactory = new com.rabbitmq.client.ConnectionFactory();
        directFactory.setHost(RABBIT.getHost());
        directFactory.setPort(RABBIT.getAmqpPort());
        directFactory.setUsername(RABBIT.getAdminUsername());
        directFactory.setPassword(RABBIT.getAdminPassword());
        try (com.rabbitmq.client.Connection connection = directFactory.newConnection()) {
            assertThat(connection.isOpen()).isTrue();
        }

        rabbitTemplate.execute(channel -> {
            channel.queuePurge(NotificationQueueConfig.QUEUE);
            return null;
        });
        String id = notificationQueue.enqueue("student.demo", "APPLICATION_RECEIVED", "IN_APP");
        assertThat(id).isNotBlank();
        Message message = rabbitTemplate.receive(NotificationQueueConfig.QUEUE, 2_000);
        assertThat(message).isNotNull();
        assertThat(message.getMessageProperties().getReceivedDeliveryMode())
            .isEqualTo(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
        assertThat(new String(message.getBody(), StandardCharsets.UTF_8)).contains(id, "APPLICATION_RECEIVED", "student.demo");
    }

    @Test
    void studentTokenCannotCallOfficerOnlyNotificationEndpoint() {
        String token = loginAsStudent();
        ResponseEntity<Map> response = http.exchange("/api/notifications/send", HttpMethod.POST,
            new HttpEntity<>(Map.of("recipientSubject", "student.demo", "templateId", "TEST_NOTICE", "channel", "IN_APP"), jsonBearer(token)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private String loginAsStudent() {
        otpDelivery.clear();
        Map<?, ?> challenge = http.postForObject("/api/auth/demo-login", Map.of(
            "username", "student.demo", "password", "DemoPass!2026", "role", "STUDENT"), Map.class);
        String challengeId = (String) challenge.get("challengeId");
        String loginOtp = otpDelivery.latest("DEMO_LOGIN", "student.demo");
        Map<?, ?> result = http.postForObject("/api/auth/verify-demo-otp", Map.of("challengeId", challengeId, "otp", loginOtp), Map.class);
        return (String) result.get("accessToken");
    }

    private Map<String, Object> upload(String token, String type, String filename) {
        byte[] pdf = "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF".getBytes(StandardCharsets.US_ASCII);
        ByteArrayResource resource = new ByteArrayResource(pdf) { @Override public String getFilename() { return filename; } };
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.APPLICATION_PDF);
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new HttpEntity<>(resource, partHeaders));
        parts.add("type", type);
        HttpHeaders headers = bearer(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<Map> response = http.exchange("/api/documents", HttpMethod.POST, new HttpEntity<>(parts, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "DEMO_ACCEPTED");
        return response.getBody();
    }

    private Map<String, Object> postJson(String path, String token, Map<String, ?> body, HttpStatus expected) {
        ResponseEntity<Map> response = http.exchange(path, HttpMethod.POST, new HttpEntity<>(body, jsonBearer(token)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(expected);
        return response.getBody();
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private HttpHeaders jsonBearer(String token) {
        HttpHeaders headers = bearer(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestOtpConfiguration {
        @Bean @Primary CapturingOtpDeliveryService capturingOtpDeliveryService() { return new CapturingOtpDeliveryService(); }
    }

    static class CapturingOtpDeliveryService implements OtpDeliveryService {
        private final Map<String, String> codes = new ConcurrentHashMap<>();
        @Override public void deliver(String destination, String code, String purpose) { codes.put(purpose + ":" + destination, code); }
        String latest(String purpose, String destination) {
            String code = codes.get(purpose + ":" + destination);
            if (code == null) throw new AssertionError("No OTP captured for " + purpose + " / " + destination);
            return code;
        }
        void clear() { codes.clear(); }
    }
}
