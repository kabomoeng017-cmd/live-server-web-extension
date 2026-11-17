package bw.gov.dtef.institution;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/institutions")
public class InstitutionController {
    private final InstitutionService institutions;
    public InstitutionController(InstitutionService institutions) { this.institutions = institutions; }

    @GetMapping("/catalogue")
    public List<InstitutionView> catalogue() {
        return institutions.catalogue().stream().map(InstitutionView::from).toList();
    }
    @GetMapping("/admin/records")
    public List<InstitutionView> records() {
        return institutions.allRecords().stream().map(InstitutionView::from).toList();
    }
    @PostMapping("/admin/records")
    public InstitutionView register(@Valid @RequestBody CreateInstitutionRequest request, @AuthenticationPrincipal Jwt jwt) {
        return InstitutionView.from(institutions.register(request.code(), request.name(), jwt.getSubject()));
    }
    /** Records an administrator's manual check against an official source; no live registry API is claimed. */
    @PostMapping("/admin/records/{code}/verify")
    public InstitutionView verify(@PathVariable String code, @Valid @RequestBody VerifyInstitutionRequest request,
                                  @AuthenticationPrincipal Jwt jwt) {
        return InstitutionView.from(institutions.verifyManually(code, request.sourceReference(), jwt.getSubject()));
    }
    @PostMapping("/admin/records/{code}/suspend")
    public InstitutionView suspend(@PathVariable String code, @Valid @RequestBody SuspendInstitutionRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return InstitutionView.from(institutions.suspend(code, request.reason(), jwt.getSubject()));
    }
    @GetMapping("/enrolment")
    public Map<String,Object> enrolment() {
        return Map.of("items",List.of(),"count",0,"message","Institution-specific enrolment feed must be connected.");
    }
    @PostMapping("/confirm-registration")
    public Map<String,Object> confirm(@Valid @RequestBody RegistrationRequest request) {
        return Map.of("accepted",false,"mode","INTEGRATION_NOT_CONFIGURED","reference",request.reference(),
            "message","Connect an institution identity and registry adapter before confirming registration.");
    }
    @PostMapping("/bulk-upload")
    public Map<String,Object> bulk() {
        return Map.of("accepted",false,"mode","INTEGRATION_NOT_CONFIGURED",
            "message","Bulk enrolment import is not enabled until schema validation and institution onboarding are complete.");
    }
    public record RegistrationRequest(@NotBlank String reference) {}
    public record CreateInstitutionRequest(@NotBlank @Pattern(regexp="[A-Z0-9-]{3,32}") String code,
                                           @NotBlank @Size(max=180) String name) {}
    public record VerifyInstitutionRequest(@NotBlank @Size(min=5,max=180) String sourceReference) {}
    public record SuspendInstitutionRequest(@NotBlank @Size(min=10,max=500) String reason) {}
    public record InstitutionView(String code,String name,String status,String verificationSource,
                                  String verificationReference,String verifiedBy,Instant verifiedAt) {
        static InstitutionView from(InstitutionRecord item) {
            return new InstitutionView(item.getCode(),item.getName(),item.getStatus(),item.getVerificationSource(),
                item.getVerificationReference(),item.getVerifiedBy(),item.getVerifiedAt());
        }
    }
}
