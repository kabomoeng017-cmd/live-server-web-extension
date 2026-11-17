package bw.gov.dtef.institution;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/institutions")
public class InstitutionController {
    @GetMapping("/enrolment")
    public Map<String, Object> enrolment() {
        return Map.of("items", List.of(), "count", 0, "message", "Institution-specific enrolment feed must be connected.");
    }
    @PostMapping("/confirm-registration")
    public Map<String, Object> confirm(@Valid @RequestBody RegistrationRequest request) {
        return Map.of("accepted", false, "mode", "INTEGRATION_NOT_CONFIGURED", "reference", request.reference(),
            "message", "Connect an institution identity and registry adapter before confirming registration.");
    }
    @PostMapping("/bulk-upload")
    public Map<String, Object> bulk() {
        return Map.of("accepted", false, "mode", "INTEGRATION_NOT_CONFIGURED", "message", "Bulk enrolment import is not enabled until schema validation and institution onboarding are complete.");
    }
    public record RegistrationRequest(@NotBlank String reference) {}
}
