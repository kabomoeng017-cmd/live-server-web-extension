package bw.gov.dtef.application;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/applications/draft")
public class ApplicationDraftController {
    private final ApplicationDraftRepository repository;
    private final ObjectMapper mapper;
    private static final Set<String> ALLOWED_FIELDS = Set.of("step", "institution", "programme", "admissionDocument", "supportingDocuments");
    private static final Set<String> SENSITIVE_KEYS = Set.of("omang", "phone", "email", "password", "otp", "bankaccount", "accountnumber", "dateofbirth", "nationalid", "passport", "identity", "address", "birth", "contact", "bank");
    private static final Set<String> NESTED_FIELDS = Set.of("documentId", "type", "name", "size", "status");
    public ApplicationDraftController(ApplicationDraftRepository repository, ObjectMapper mapper) { this.repository = repository; this.mapper = mapper; }

    @GetMapping
    public Map<String, Object> get(@AuthenticationPrincipal Jwt jwt) {
        return repository.findById(jwt.getSubject())
            .map(d -> {
                try { return Map.<String, Object>of("payload", mapper.readValue(d.getPayloadJson(), Map.class), "updatedAt", d.getUpdatedAt()); }
                catch (Exception e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Draft could not be read."); }
            }).orElse(Map.of("payload", Map.of()));
    }

    @PutMapping
    @Transactional
    public Map<String, Object> save(@RequestBody @NotNull Map<String, Object> payload, @AuthenticationPrincipal Jwt jwt) {
        if (payload == null || !ALLOWED_FIELDS.containsAll(payload.keySet())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft contains unsupported fields.");
        JsonNode tree = mapper.valueToTree(payload);
        if (containsSensitiveKey(tree)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sensitive identity/contact fields must not be stored in an application draft.");
        if (containsUnsupportedNestedKey(tree, true)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft contains unsupported nested document fields.");
        validateDraftShape(tree);
        String json = mapper.writeValueAsString(payload);
        if (json.length() > 200_000) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Draft is too large.");
        ApplicationDraft draft = repository.findById(jwt.getSubject()).orElseGet(() -> new ApplicationDraft(jwt.getSubject(), "{}"));
        draft.setPayloadJson(json);
        repository.save(draft);
        return Map.of("saved", true, "updatedAt", draft.getUpdatedAt());
    }

    private void validateDraftShape(JsonNode tree) {
        JsonNode step = tree.get("step");
        if (step != null && (!step.canConvertToInt() || step.asInt() < 1 || step.asInt() > 5))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft step must be between 1 and 5.");
        for (String field : Set.of("institution", "programme")) {
            JsonNode value = tree.get(field);
            if (value != null && !value.isNull() && (!value.isTextual() || value.asText().length() > 180))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft " + field + " must be text of at most 180 characters.");
        }
        JsonNode admission = tree.get("admissionDocument");
        if (admission != null && !admission.isNull()) validateDocumentNode(admission);
        JsonNode supporting = tree.get("supportingDocuments");
        if (supporting != null && !supporting.isNull()) {
            if (!supporting.isArray() || supporting.size() > 10) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft may contain at most ten supporting documents.");
            supporting.forEach(this::validateDocumentNode);
        }
    }

    private void validateDocumentNode(JsonNode node) {
        if (!node.isObject()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft document metadata is invalid.");
        JsonNode id = node.get("documentId");
        JsonNode type = node.get("type");
        if (id == null || !id.isTextual() || type == null || !type.isTextual()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft document metadata is incomplete.");
        try { java.util.UUID.fromString(id.asText()); } catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft document identifier is invalid."); }
        if (!Set.of("ADMISSION_LETTER", "OMANG_COPY", "BANK_CONFIRMATION", "PASSPORT_PHOTO", "OTHER").contains(type.asText()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft document type is invalid.");
        JsonNode name = node.get("name");
        if (name == null || !name.isTextual() || !name.asText().matches(type.asText().toLowerCase(java.util.Locale.ROOT) + "\\.(pdf|jpg|png)"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft document filename is invalid.");
        JsonNode size = node.get("size");
        if (size == null || !size.canConvertToLong() || size.asLong() < 1 || size.asLong() > 10L * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft document size is invalid.");
        JsonNode status = node.get("status");
        if (status == null || !status.isTextual() || !Set.of("DEMO_ACCEPTED", "QUARANTINED", "CLEAN", "PENDING_SCAN").contains(status.asText()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft document status is invalid.");
    }

    private boolean containsUnsupportedNestedKey(JsonNode node, boolean root) {
        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                if (!root && !NESTED_FIELDS.contains(field.getKey())) return true;
                if (containsUnsupportedNestedKey(field.getValue(), false)) return true;
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) if (containsUnsupportedNestedKey(child, false)) return true;
        }
        return false;
    }

    private boolean containsSensitiveKey(JsonNode node) {
        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                String normalized = field.getKey().replaceAll("[^A-Za-z]", "").toLowerCase();
                if (SENSITIVE_KEYS.stream().anyMatch(normalized::contains) || containsSensitiveKey(field.getValue())) return true;
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) if (containsSensitiveKey(child)) return true;
        }
        return false;
    }

    @DeleteMapping
    @Transactional
    public Map<String, Object> clear(@AuthenticationPrincipal Jwt jwt) {
        repository.deleteById(jwt.getSubject());
        return Map.of("deleted", true);
    }
}
