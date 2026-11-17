package bw.gov.dtef.officer;

import bw.gov.dtef.application.ApplicationDocumentLink;
import bw.gov.dtef.application.ApplicationDocumentLinkRepository;
import bw.gov.dtef.application.ApplicationRepository;
import bw.gov.dtef.application.ApplicationStatusHistory;
import bw.gov.dtef.application.ApplicationStatusHistoryRepository;
import bw.gov.dtef.application.SponsorshipApplication;
import bw.gov.dtef.document.DocumentRecord;
import bw.gov.dtef.document.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/officer")
public class OfficerController {
    private final ApplicationRepository applications;
    private final ApplicationDocumentLinkRepository applicationDocuments;
    private final ApplicationStatusHistoryRepository statusHistory;
    private final DocumentRepository documents;

    public OfficerController(ApplicationRepository applications,
                             ApplicationDocumentLinkRepository applicationDocuments,
                             ApplicationStatusHistoryRepository statusHistory,
                             DocumentRepository documents) {
        this.applications = applications;
        this.applicationDocuments = applicationDocuments;
        this.statusHistory = statusHistory;
        this.documents = documents;
    }

    @GetMapping("/queue")
    public Map<String, Object> queue() {
        List<?> items = applications.findTop100ByOrderByCreatedAtDesc().stream().map(a -> Map.of(
            "reference", a.getReference(), "institution", a.getInstitution(), "programme", a.getProgramme(),
            "status", a.getStatus(), "createdAt", a.getCreatedAt())).toList();
        return Map.of("items", items, "count", items.size());
    }

    @GetMapping("/applications/{reference}")
    public Map<String, Object> application(@PathVariable String reference) {
        SponsorshipApplication app = applications.findByReference(reference)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application was not found."));

        List<ApplicationDocumentLink> links = applicationDocuments.findByApplicationReferenceOrderByLinkedAtAsc(reference);
        List<UUID> documentIds = links.stream().map(ApplicationDocumentLink::getDocumentId).toList();
        Map<UUID, DocumentRecord> records = documents.findAllById(documentIds).stream()
            .collect(Collectors.toMap(DocumentRecord::getId, Function.identity()));

        List<Map<String, Object>> documentViews = links.stream().map(link -> {
            DocumentRecord document = records.get(link.getDocumentId());
            if (document == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An application document record is missing.");
            }
            return Map.<String, Object>of(
                "documentId", document.getId(),
                "type", link.getDocumentType(),
                "status", document.getStatus(),
                "size", document.getSize(),
                "sha256", document.getSha256(),
                "uploadedAt", document.getCreatedAt()
            );
        }).toList();

        List<Map<String, Object>> historyViews = statusHistory.findByApplicationReferenceOrderByChangedAtAsc(reference)
            .stream().map(this::historyView).toList();

        Map<String, Object> applicationView = Map.of(
            "reference", app.getReference(),
            "institution", app.getInstitution(),
            "programme", app.getProgramme(),
            "status", app.getStatus(),
            "createdAt", app.getCreatedAt()
        );
        return Map.of("application", applicationView, "documents", documentViews, "history", historyViews);
    }

    private Map<String, Object> historyView(ApplicationStatusHistory history) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("fromStatus", history.getFromStatus());
        item.put("toStatus", history.getToStatus());
        item.put("changedBy", history.getChangedBy());
        item.put("decisionReason", history.getDecisionReason());
        item.put("changedAt", history.getChangedAt());
        return item;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return Map.of("newApplications", 0, "missingDocuments", 0, "appeals", 0, "fraudAlerts", 0, "source", "LIVE_DATABASE_COUNTS_PENDING");
    }
}
