package bw.gov.dtef.application;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final ApplicationService service;
    public ApplicationController(ApplicationService service) { this.service = service; }

    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody ApplicationService.CreateApplicationRequest request,
                                      @AuthenticationPrincipal Jwt jwt) {
        SponsorshipApplication app = service.create(jwt.getSubject(), request);
        return Map.of("reference", app.getReference(), "status", app.getStatus(), "createdAt", app.getCreatedAt(), "message", "Application received.");
    }
    @GetMapping
    public List<ApplicationView> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.listMine(jwt.getSubject()).stream().map(ApplicationView::from).toList();
    }
    @GetMapping("/{reference}")
    public ApplicationView one(@PathVariable String reference, @AuthenticationPrincipal Jwt jwt) {
        return ApplicationView.from(service.getMine(jwt.getSubject(), reference));
    }
    @GetMapping("/{reference}/history")
    public List<ApplicationStatusHistory> history(@PathVariable String reference, @AuthenticationPrincipal Jwt jwt) {
        return service.historyMine(jwt.getSubject(), reference);
    }
    @PutMapping("/{reference}/status")
    public ApplicationView status(@PathVariable String reference, @Valid @RequestBody StatusRequest request,
                                  @AuthenticationPrincipal Jwt jwt) {
        return ApplicationView.from(service.updateStatus(jwt.getSubject(), reference, request.status()));
    }
    public record StatusRequest(@jakarta.validation.constraints.NotNull ApplicationStatus status) {}
    public record ApplicationView(String reference, String institution, String programme, ApplicationStatus status,
                                  Instant createdAt, Instant updatedAt) {
        static ApplicationView from(SponsorshipApplication app) {
            return new ApplicationView(app.getReference(), app.getInstitution(), app.getProgramme(), app.getStatus(), app.getCreatedAt(), app.getUpdatedAt());
        }
    }
}
