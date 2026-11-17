package bw.gov.dtef.document;

import jakarta.validation.constraints.Pattern;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;
import bw.gov.dtef.audit.AuditService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/documents")
@Profile("demo")
public class DocumentController {
    private final DocumentStorageService storage;
    private final DocumentRepository repository;
    private final AuditService audit;
    public DocumentController(DocumentStorageService storage, DocumentRepository repository, AuditService audit) { this.storage = storage; this.repository = repository; this.audit = audit; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam("type") @Pattern(regexp = "ADMISSION_LETTER|OMANG_COPY|BANK_CONFIRMATION|PASSPORT_PHOTO|OTHER") String type,
                                      @AuthenticationPrincipal Jwt jwt) throws IOException {
        DocumentStorageService.StoredFile stored = storage.store(file);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int completionStatus) {
                    if (completionStatus != STATUS_COMMITTED) storage.delete(stored.storageKey());
                }
            });
        }
        String status = "DEMO_ACCEPTED";
        String extension = switch (stored.contentType()) { case "application/pdf" -> ".pdf"; case "image/jpeg" -> ".jpg"; default -> ".png"; };
        String safeName = type.toLowerCase(java.util.Locale.ROOT) + extension;
        DocumentRecord record = repository.save(new DocumentRecord(jwt.getSubject(), type, stored.storageKey(), safeName, stored.contentType(), stored.sha256(), stored.size(), status));
        audit.record(jwt.getSubject(), "DOCUMENT_UPLOADED", "DOCUMENT", record.getId().toString());
        return Map.of("documentId", record.getId(), "type", record.getDocumentType(), "name", record.getOriginalName(), "size", record.getSize(), "status", status);
    }
}
