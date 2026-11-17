package bw.gov.dtef.application;

import bw.gov.dtef.audit.AuditService;
import bw.gov.dtef.auth.OtpService;
import bw.gov.dtef.document.DocumentRecord;
import bw.gov.dtef.document.DocumentRepository;
import bw.gov.dtef.identity.IdentityVerificationService;
import bw.gov.dtef.institution.InstitutionService;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
public class ApplicationService {
    private final ApplicationRepository applications;
    private final DocumentRepository documents;
    private final IdentityVerificationService identity;
    private final OtpService otp;
    private final AuditService audit;
    private final Environment environment;
    private final ApplicationStatusHistoryRepository history;
    private final ApplicationDocumentLinkRepository applicationDocuments;
    private final InstitutionService institutions;

    public ApplicationService(ApplicationRepository applications, DocumentRepository documents,
                              IdentityVerificationService identity, OtpService otp, AuditService audit,
                              Environment environment, ApplicationStatusHistoryRepository history,
                              ApplicationDocumentLinkRepository applicationDocuments,
                              InstitutionService institutions) {
        this.applications = applications; this.documents = documents; this.identity = identity; this.otp = otp;
        this.audit = audit; this.environment = environment; this.history = history; this.applicationDocuments = applicationDocuments;
        this.institutions = institutions;
    }

    @Transactional
    public SponsorshipApplication create(String owner, CreateApplicationRequest request) {
        if (!identity.belongsTo(request.identityVerificationId(), owner))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Complete identity verification before submitting.");
        String verifiedInstitutionName = institutions.requireEligibleForApplication(request.institution());
        DocumentRecord admission = documents.findByIdAndOwnerSubject(request.admissionDocumentId(), owner)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admission document was not found for this account."));
        if (!"ADMISSION_LETTER".equals(admission.getDocumentType()) || !isSubmittable(admission.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admission document is not cleared for submission.");
        if (request.supportingDocumentIds() == null || request.supportingDocumentIds().isEmpty() || request.supportingDocumentIds().size() > 10)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide between one and ten supporting documents.");
        if (request.supportingDocumentIds().contains(request.admissionDocumentId()) ||
            new java.util.HashSet<>(request.supportingDocumentIds()).size() != request.supportingDocumentIds().size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supporting document identifiers must be unique and separate from the admission letter.");
        java.util.List<DocumentRecord> supporting = request.supportingDocumentIds().stream()
            .map(id -> documents.findByIdAndOwnerSubject(id, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "A supporting document was not found for this account.")))
            .toList();
        if (supporting.stream().anyMatch(d -> "ADMISSION_LETTER".equals(d.getDocumentType()) || !isSubmittable(d.getStatus())))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Every supporting document must be an allowed type and cleared for submission.");
        boolean hasOmangCopy = supporting.stream().anyMatch(d -> "OMANG_COPY".equals(d.getDocumentType()) && isSubmittable(d.getStatus()));
        boolean hasBankProof = supporting.stream().anyMatch(d -> "BANK_CONFIRMATION".equals(d.getDocumentType()) && isSubmittable(d.getStatus()));
        if (!hasOmangCopy || !hasBankProof) throw new ResponseStatusException(HttpStatus.CONFLICT, "Upload an Omang copy and bank confirmation cleared for submission.");
        if (!otp.verifyAndConsume("action:" + owner + ":APPLICATION_SUBMIT", request.otp()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Submission OTP is invalid or expired.");

        long sequence = applications.nextReferenceSequence();
        String reference = "DTEF-" + Year.now() + "-" + String.format("%08d", sequence);
        SponsorshipApplication app = applications.save(new SponsorshipApplication(reference, owner,
            verifiedInstitutionName, request.programme().trim(), admission.getId()));
        java.util.List<ApplicationDocumentLink> links = new java.util.ArrayList<>();
        links.add(new ApplicationDocumentLink(reference, admission.getId(), admission.getDocumentType()));
        supporting.forEach(document -> links.add(new ApplicationDocumentLink(reference, document.getId(), document.getDocumentType())));
        applicationDocuments.saveAll(links);
        history.save(new ApplicationStatusHistory(reference, null, ApplicationStatus.APPLIED.name(), owner));
        audit.record(owner, "APPLICATION_SUBMITTED", "APPLICATION", reference);
        return app;
    }

    @Transactional(readOnly = true)
    public List<SponsorshipApplication> listMine(String owner) { return applications.findAllByOwnerSubjectOrderByCreatedAtDesc(owner); }

    @Transactional(readOnly = true)
    public SponsorshipApplication getMine(String owner, String reference) {
        return applications.findByReferenceAndOwnerSubject(reference, owner)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application was not found."));
    }

    @Transactional(readOnly = true)
    public List<ApplicationStatusHistory> historyMine(String owner, String reference) {
        getMine(owner, reference);
        return history.findByApplicationReferenceOrderByChangedAtAsc(reference);
    }

    @Transactional
    public SponsorshipApplication updateStatus(String actor, String reference, ApplicationStatus status, String decisionReason) {
        if (status == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A target application status is required.");
        String normalizedReason = decisionReason == null ? null : decisionReason.trim();
        if (normalizedReason != null && normalizedReason.length() > 500)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision reason must be at most 500 characters.");
        if (status == ApplicationStatus.REJECTED && (normalizedReason == null || normalizedReason.isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reason is required when rejecting an application.");

        SponsorshipApplication app = applications.findByReference(reference)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application was not found."));
        ApplicationStatus current = app.getStatus();
        boolean allowed = switch (current) {
            case APPLIED -> status == ApplicationStatus.DOCUMENTS_PENDING || status == ApplicationStatus.DOCUMENTS_VERIFIED || status == ApplicationStatus.REJECTED;
            case DOCUMENTS_PENDING -> status == ApplicationStatus.DOCUMENTS_VERIFIED || status == ApplicationStatus.REJECTED;
            case DOCUMENTS_VERIFIED -> status == ApplicationStatus.INSTITUTION_CONFIRMED || status == ApplicationStatus.REJECTED;
            case INSTITUTION_CONFIRMED -> status == ApplicationStatus.FNBB_PENDING || status == ApplicationStatus.REJECTED;
            case FNBB_PENDING -> status == ApplicationStatus.COMPLETED || status == ApplicationStatus.REJECTED;
            case COMPLETED, REJECTED -> false;
        };
        if (!allowed) throw new ResponseStatusException(HttpStatus.CONFLICT, "That application status transition is not permitted.");
        if (status == ApplicationStatus.DOCUMENTS_VERIFIED) requireDocumentsReadyForVerification(reference);
        if (status == ApplicationStatus.INSTITUTION_CONFIRMED) institutions.requireEligibleForApplication(app.getInstitution());
        app.setStatus(status);
        history.save(new ApplicationStatusHistory(reference, current.name(), status.name(), actor, normalizedReason));
        String auditAction = status == ApplicationStatus.REJECTED ? "APPLICATION_REJECTED" : "APPLICATION_STATUS_CHANGED";
        audit.record(actor, auditAction, "APPLICATION", reference);
        return app;
    }

    private void requireDocumentsReadyForVerification(String reference) {
        List<ApplicationDocumentLink> links = applicationDocuments.findByApplicationReferenceOrderByLinkedAtAsc(reference);
        if (links.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Application documents must be linked before document verification.");
        List<UUID> ids = links.stream().map(ApplicationDocumentLink::getDocumentId).toList();
        List<DocumentRecord> linkedDocuments = documents.findAllById(ids);
        if (linkedDocuments.size() != ids.size()) throw new ResponseStatusException(HttpStatus.CONFLICT, "One or more application document records are missing.");
        boolean demo = environment.acceptsProfiles(Profiles.of("demo"));
        boolean hasUnclearedDocument = linkedDocuments.stream().anyMatch(document ->
            !"CLEAN".equals(document.getStatus()) && !(demo && "DEMO_ACCEPTED".equals(document.getStatus())));
        if (hasUnclearedDocument) throw new ResponseStatusException(HttpStatus.CONFLICT,
            "Every application document must pass malware scanning before it can be marked verified.");
    }

    private boolean isSubmittable(String status) {
        return "CLEAN".equals(status) || (environment.acceptsProfiles(Profiles.of("demo")) && "DEMO_ACCEPTED".equals(status));
    }

    public record CreateApplicationRequest(
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 180) String institution,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 180) String programme,
        @jakarta.validation.constraints.NotBlank String identityVerificationId,
        @jakarta.validation.constraints.NotNull UUID admissionDocumentId,
        @jakarta.validation.constraints.NotEmpty @jakarta.validation.constraints.Size(max = 10) java.util.List<UUID> supportingDocumentIds,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Pattern(regexp = "\\d{6}") String otp
    ) {}
}
