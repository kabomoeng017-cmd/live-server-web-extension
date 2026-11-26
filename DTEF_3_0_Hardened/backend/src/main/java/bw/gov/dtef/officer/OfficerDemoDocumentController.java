package bw.gov.dtef.officer;

import bw.gov.dtef.application.ApplicationDocumentLinkRepository;
import bw.gov.dtef.audit.AuditService;
import bw.gov.dtef.document.DocumentRecord;
import bw.gov.dtef.document.DocumentRepository;
import bw.gov.dtef.document.DocumentStorageService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/officer/applications/{reference}/documents")
@Profile("demo")
public class OfficerDemoDocumentController {
    private final ApplicationDocumentLinkRepository links;
    private final DocumentRepository documents;
    private final DocumentStorageService storage;
    private final AuditService audit;

    public OfficerDemoDocumentController(ApplicationDocumentLinkRepository links,
                                         DocumentRepository documents,
                                         DocumentStorageService storage,
                                         AuditService audit) {
        this.links = links;
        this.documents = documents;
        this.storage = storage;
        this.audit = audit;
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<byte[]> download(@PathVariable String reference,
                                           @PathVariable UUID documentId,
                                           @AuthenticationPrincipal Jwt jwt) throws IOException {
        if (!links.existsByApplicationReferenceAndDocumentId(reference, documentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document is not linked to this application.");
        }
        DocumentRecord document = documents.findById(documentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document was not found."));
        String status = document.getStatus();
        boolean reviewable = "SCANNED_CLEAN".equals(status) || "CLEAN".equals(status) || "DEMO_ACCEPTED".equals(status);
        if (!reviewable) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document has not completed the required security checks for review.");
        }

        byte[] content;
        try {
            content = storage.readVerified(document.getStorageKey(), document.getSha256(), document.getSize());
        } catch (DocumentStorageService.IntegrityException ex) {
            audit.record(jwt.getSubject(), "DOCUMENT_INTEGRITY_CHECK_FAILED", "DOCUMENT", documentId.toString());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Stored document failed its integrity check.");
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored document was not found.");
        }
        audit.record(jwt.getSubject(), "DOCUMENT_INTEGRITY_VERIFIED", "DOCUMENT", documentId.toString());
        audit.record(jwt.getSubject(), "OFFICER_DOCUMENT_ACCESSED", "DOCUMENT", documentId.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename(document.getOriginalName(), StandardCharsets.UTF_8).build());
        headers.setCacheControl(CacheControl.noStore());
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-DTEF-Document-Mode", "SCANNED_CLEAN".equals(status) ? "MALWARE_SCANNED" :
            ("CLEAN".equals(status) ? "CLEAN" : "DEMO_UNSCANNED"));
        headers.set("X-DTEF-Integrity-Check", "SHA256_MATCH");
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }
}
