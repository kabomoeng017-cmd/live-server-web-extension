package bw.gov.dtef.officer;

import bw.gov.dtef.application.ApplicationRepository;
import bw.gov.dtef.application.ApplicationStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/officer")
public class OfficerController {
    private final ApplicationRepository applications;
    public OfficerController(ApplicationRepository applications) { this.applications = applications; }

    @GetMapping("/queue")
    public Map<String, Object> queue() {
        List<?> items = applications.findTop100ByOrderByCreatedAtDesc().stream().map(a -> Map.of(
            "reference", a.getReference(), "institution", a.getInstitution(), "programme", a.getProgramme(),
            "status", a.getStatus(), "createdAt", a.getCreatedAt())).toList();
        return Map.of("items", items, "count", items.size());
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return Map.of("newApplications", 0, "missingDocuments", 0, "appeals", 0, "fraudAlerts", 0, "source", "LIVE_DATABASE_COUNTS_PENDING");
    }
}
