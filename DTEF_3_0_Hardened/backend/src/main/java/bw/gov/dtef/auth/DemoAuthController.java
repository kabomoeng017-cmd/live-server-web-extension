package bw.gov.dtef.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@Profile("demo")
public class DemoAuthController {
    private static final Map<String, String> DEMO_USERS = Map.of(
        "student.demo", "STUDENT", "officer.demo", "OFFICER", "institution.demo", "INSTITUTION",
        "fnbb.demo", "FNBB", "welfare.demo", "WELFARE", "ministry.demo", "MINISTRY", "admin.demo", "ADMIN", "admin.verifier", "ADMIN"
    );
    private static final Set<String> ALLOWED_ROLES = Set.of("STUDENT", "OFFICER", "INSTITUTION", "FNBB", "WELFARE", "MINISTRY", "ADMIN");
    private final OtpService otpService;
    private final JwtTokenService tokenService;
    private final StringRedisTemplate redis;
    private final String demoPassword;

    public DemoAuthController(OtpService otpService, JwtTokenService tokenService, StringRedisTemplate redis,
                              @Value("${dtef.demo-password}") String demoPassword) {
        this.otpService = otpService; this.tokenService = tokenService; this.redis = redis; this.demoPassword = demoPassword;
    }

    @PostMapping("/demo-login")
    public Map<String, Object> begin(@Valid @RequestBody DemoLoginRequest request) {
        String expectedRole = DEMO_USERS.get(request.username());
        String attemptKey = "dtef:login:attempts:" + request.username();
        Long attempts = redis.opsForValue().increment(attemptKey);
        if (attempts != null && attempts == 1L) redis.expire(attemptKey, Duration.ofMinutes(15));
        if (attempts != null && attempts > 10L) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many sign-in attempts. Try again later.");
        boolean passwordMatches = MessageDigest.isEqual(demoPassword.getBytes(StandardCharsets.UTF_8), request.password().getBytes(StandardCharsets.UTF_8));
        if (!passwordMatches || expectedRole == null || !expectedRole.equals(request.role()) || !ALLOWED_ROLES.contains(request.role())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Demo credentials are invalid.");
        }
        String challengeId = UUID.randomUUID().toString();
        redis.opsForValue().set("dtef:login:challenge:" + challengeId, request.username(), Duration.ofMinutes(5));
        otpService.issue("login:" + challengeId, request.username(), "DEMO_LOGIN", "login-user:" + request.username());
        return Map.of("challengeId", challengeId, "expiresInSeconds", 300, "message", "OTP generated. In local demo mode, check the backend log.");
    }

    @PostMapping("/verify-demo-otp")
    public Map<String, Object> complete(@Valid @RequestBody DemoOtpRequest request) {
        String username = redis.opsForValue().get("dtef:login:challenge:" + request.challengeId());
        if (username == null || !otpService.verifyAndConsume("login:" + request.challengeId(), request.otp())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OTP is invalid, expired or challenge is unknown.");
        }
        redis.delete("dtef:login:challenge:" + request.challengeId());
        String role = DEMO_USERS.get(username);
        if (role == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Demo account is not authorized.");
        return Map.of("accessToken", tokenService.issue(username, role), "tokenType", "Bearer", "expiresInSeconds", 900, "role", role, "username", username);
    }

    public record DemoLoginRequest(@NotBlank @Size(max = 60) @Pattern(regexp = "[a-z0-9._-]{3,60}") String username,
                                   @NotBlank @Size(min = 8, max = 128) String password,
                                   @NotBlank @Pattern(regexp = "STUDENT|OFFICER|INSTITUTION|FNBB|WELFARE|MINISTRY|ADMIN") String role) {}
    public record DemoOtpRequest(@NotBlank String challengeId, @NotBlank @Pattern(regexp = "\\d{6}") String otp) {}
}
