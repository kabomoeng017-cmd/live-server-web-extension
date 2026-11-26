package bw.gov.dtef.document;

import jakarta.validation.constraints.Pattern;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;
import bw.gov.dtef.audit.AuditService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
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
    private final DocumentScanner scanner;

    public DocumentController(DocumentStorageService storage, DocumentRepository repository,
                              AuditService audit, DocumentScanner scanner) {
        this.storage = storage;
        this.repository = repository;
        this.audit = audit;
        this.scanner = scanner;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam("type") @Pattern(regexp = "ADMISSION_LETTER|OMANG_COPY|BANK_CONFIRMATION|PASSPORT_PHOTO|OTHER") String type,
                                      @AuthenticationPrincipal Jwt jwt) throws IOException {
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a document before uploading.");
        if (file.getSize() > 10L * 1024 * 1024) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Document exceeds the 10 MB limit.");

        byte[] content = file.getBytes();
        String contentType = file.getContentType();
        final DocumentScanner.ScanResult scan;
        try {
            scan = scanner.scan(content, contentType);
        } catch (ScannerUnavailableException ex) {
            audit.record(jwt.getSubject(), "DOCUMENT_SCAN_UNAVAILABLE", "DOCUMENT", "UPLOAD_BLOCKED");
            throw ex;
        }

        if (scan == null || scan.verdict() == null) {
            audit.record(jwt.getSubject(), "DOCUMENT_SCAN_INVALID_RESULT", "DOCUMENT", "UPLOAD_BLOCKED");
            throw new ScannerUnavailableException("Malware scanner did not provide a valid verdict; upload blocked.");
        }
        if (scan.verdict() == DocumentScanner.Verdict.INFECTED) {
            audit.record(jwt.getSubject(), "DOCUMENT_SCAN_INFECTED", "DOCUMENT", "UPLOAD_REJECTED");
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "The uploaded document failed the malware scan.");
        }

        DocumentStorageService.StoredFile stored = storage.store(file);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int completionStatus) {
                    if (completionStatus != STATUS_COMMITTED) storage.delete(stored.storageKey());
                }
            });
        }
        String status = "SCANNED_CLEAN";
        String extension = switch (stored.contentType()) { case "application/pdf" -> ".pdf"; case "image/jpeg" -> ".jpg"; default -> ".png"; };
        String safeName = type.toLowerCase(java.util.Locale.ROOT) + extension;
        DocumentRecord record = repository.save(new DocumentRecord(jwt.getSubject(), type, stored.storageKey(), safeName, stored.contentType(), stored.sha256(), stored.size(), status));
        audit.record(jwt.getSubject(), "DOCUMENT_SCAN_CLEAN", "DOCUMENT", record.getId().toString());
        audit.record(jwt.getSubject(), "DOCUMENT_UPLOADED", "DOCUMENT", record.getId().toString());
        return Map.of("documentId", record.getId(), "type", record.getDocumentType(), "name", record.getOriginalName(), "size", record.getSize(), "status", status);
    }

    @ExceptionHandler(ScannerUnavailableException.class)
    public ResponseEntity<Map<String, String>> scannerUnavailable(ScannerUnavailableException ignored) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("error", "DOCUMENT_SCANNING_UNAVAILABLE", "message", "Document scanning is temporarily unavailable. Please try again later."));
    }
}
