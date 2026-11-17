package bw.gov.dtef.ai;

import bw.gov.dtef.document.DocumentRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@Profile("demo")
public class AiController {
    private final DocumentRepository documents;
    public AiController(DocumentRepository documents) { this.documents = documents; }

    @PostMapping("/admission-letter")
    public Map<String, Object> readAdmissionLetter(@Valid @RequestBody AdmissionLetterRequest request, @AuthenticationPrincipal Jwt jwt) {
        var document = documents.findByIdAndOwnerSubject(request.documentId(), jwt.getSubject())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Uploaded admission letter was not found."));
        if (!"ADMISSION_LETTER".equals(document.getDocumentType())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected document is not an admission letter.");
        // Explicitly simulated OCR output. It must not be treated as verified or relied upon for eligibility decisions.
        return Map.of("institution", "University of Botswana (sample)", "programme", "Enter programme from your letter",
            "campus", "Confirm from letter", "confidence", 0.0, "mode", "DEMO_SIMULATION",
            "requiresStudentConfirmation", true, "sourceDocumentId", document.getId());
    }

    @PostMapping("/fraud-score")
    public Map<String, Object> fraud(@RequestBody Map<String, Object> request) {
        return Map.of("risk", "NOT_ASSESSED", "score", -1, "mode", "DEMO_ONLY",
            "message", "No automated fraud decision is performed by this prototype.");
    }
    public record AdmissionLetterRequest(@NotNull UUID documentId) {}
}
