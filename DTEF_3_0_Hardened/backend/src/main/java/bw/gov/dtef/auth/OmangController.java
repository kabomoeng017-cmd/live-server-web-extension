package bw.gov.dtef.auth;

import bw.gov.dtef.identity.IdentityVerificationService;
import bw.gov.dtef.audit.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/omang")
@Profile("demo")
public class OmangController {
    private final IdentityVerificationService verificationService;
    private final AuditService audit;
    public OmangController(IdentityVerificationService verificationService, AuditService audit) { this.verificationService = verificationService; this.audit = audit; }

    @PostMapping("/verify")
    public Map<String, Object> verify(@Valid @RequestBody OmangRequest request, @AuthenticationPrincipal Jwt jwt) {
        // Demo adapter validates shape only; it does not query or claim access to the national identity register.
        String id = verificationService.createDemoVerification(jwt.getSubject());
        audit.record(jwt.getSubject(), "DEMO_IDENTITY_SHAPE_CHECKED", "IDENTITY_VERIFICATION", id);
        String masked = "*****" + request.omang().substring(5);
        return Map.of("verified", true, "verificationId", id, "maskedOmang", masked, "provider", "DEMO_SHAPE_VALIDATION", "message", "Demo-only validation; not an official Omang verification.");
    }
    public record OmangRequest(@NotBlank @Pattern(regexp = "\\d{9}", message = "Omang must contain exactly 9 digits") String omang) {}
}
