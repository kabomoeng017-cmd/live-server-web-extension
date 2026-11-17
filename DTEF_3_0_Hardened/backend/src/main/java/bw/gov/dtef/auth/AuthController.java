package bw.gov.dtef.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final OtpService otpService;
    public AuthController(OtpService otpService) { this.otpService = otpService; }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("username", jwt.getSubject(), "roles", jwt.getClaimAsStringList("roles") == null ? java.util.List.of() : jwt.getClaimAsStringList("roles"), "expiresAt", jwt.getExpiresAt());
    }

    @PostMapping("/send-action-otp")
    public Map<String, Object> sendActionOtp(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ActionOtpRequest request) {
        if (!"APPLICATION_SUBMIT".equals(request.purpose())) throw new IllegalArgumentException("Unsupported OTP purpose.");
        otpService.issue("action:" + jwt.getSubject() + ":" + request.purpose(), jwt.getSubject(), request.purpose());
        return Map.of("sent", true, "expiresInSeconds", 300, "delivery", "Configured account channel");
    }

    public record ActionOtpRequest(@NotBlank @Pattern(regexp = "APPLICATION_SUBMIT") String purpose) {}
}
