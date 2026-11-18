package bw.gov.dtef.application;

import bw.gov.dtef.audit.AuditService;
import bw.gov.dtef.auth.OtpService;
import bw.gov.dtef.document.DocumentRecord;
import bw.gov.dtef.document.DocumentRepository;
import bw.gov.dtef.identity.IdentityVerificationService;
import bw.gov.dtef.institution.InstitutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApplicationServiceTest {
    private ApplicationRepository applications;
    private DocumentRepository documents;
    private IdentityVerificationService identity;
    private OtpService otp;
    private AuditService audit;
    private Environment environment;
    private ApplicationStatusHistoryRepository history;
    private ApplicationDocumentLinkRepository applicationDocuments;
    private InstitutionService institutions;
    private ApplicationService service;

    @BeforeEach void setup() {
        applications = mock(ApplicationRepository.class); documents = mock(DocumentRepository.class);
        identity = mock(IdentityVerificationService.class); otp = mock(OtpService.class);
        audit = mock(AuditService.class); environment = mock(Environment.class); history = mock(ApplicationStatusHistoryRepository.class);
        applicationDocuments = mock(ApplicationDocumentLinkRepository.class);
        institutions = mock(InstitutionService.class);
        when(institutions.requireEligibleForApplication("University of Botswana")).thenReturn("University of Botswana");
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);
        service = new ApplicationService(applications, documents, identity, otp, audit, environment, history, applicationDocuments, institutions);
    }

    @Test void createsReferenceFromDatabaseSequenceAndChecksOwnershipAndOtp() {
        String owner = "student.demo";
        UUID admissionId = UUID.randomUUID(), omangId = UUID.randomUUID(), bankId = UUID.randomUUID();
        DocumentRecord admission = document("ADMISSION_LETTER", "DEMO_ACCEPTED");
        DocumentRecord omang = document("OMANG_COPY", "DEMO_ACCEPTED");
        DocumentRecord bank = document("BANK_CONFIRMATION", "DEMO_ACCEPTED");
        when(identity.belongsTo("verified-1", owner)).thenReturn(true);
        when(documents.findByIdAndOwnerSubject(admissionId, owner)).thenReturn(Optional.of(admission));
        when(documents.findByIdAndOwnerSubject(omangId, owner)).thenReturn(Optional.of(omang));
        when(documents.findByIdAndOwnerSubject(bankId, owner)).thenReturn(Optional.of(bank));
        when(otp.verifyAndConsume("action:" + owner + ":APPLICATION_SUBMIT", "123456")).thenReturn(true);
        when(applications.nextReferenceSequence()).thenReturn(42L);
        when(applications.save(any(SponsorshipApplication.class))).thenAnswer(call -> call.getArgument(0));
        var request = new ApplicationService.CreateApplicationRequest("University of Botswana", "BSc Computer Science", "verified-1", admissionId, List.of(omangId, bankId), "123456");
        SponsorshipApplication created = service.create(owner, request);
        assertTrue(created.getReference().matches("DTEF-\\d{4}-00000042"));
        assertEquals(owner, created.getOwnerSubject());
        verify(audit).record(owner, "APPLICATION_SUBMITTED", "APPLICATION", created.getReference());
        verify(history).save(any(ApplicationStatusHistory.class));
        verify(applicationDocuments).saveAll(any());
    }

    @Test void rejectsIdentityVerificationBelongingToAnotherUser() {
        when(identity.belongsTo(anyString(), anyString())).thenReturn(false);
        var request = new ApplicationService.CreateApplicationRequest("UB", "BSc", "wrong-owner", UUID.randomUUID(), List.of(UUID.randomUUID()), "123456");
        assertThrows(ResponseStatusException.class, () -> service.create("student.demo", request));
        verifyNoInteractions(applications);
    }

    @Test void rejectsInvalidStatusTransition() {
        SponsorshipApplication existing = new SponsorshipApplication("DTEF-2026-00000001", "student.demo", "UB", "BSc", UUID.randomUUID());
        when(applications.findByReference("DTEF-2026-00000001")).thenReturn(Optional.of(existing));
        assertThrows(ResponseStatusException.class, () -> service.updateStatus("officer.demo", existing.getReference(), ApplicationStatus.COMPLETED, null));
    }


    @Test void officerCanAdvanceApplicationWhenDemoDocumentsAreExplicitlyMarkedAccepted() {
        String reference = "DTEF-2026-00000002";
        SponsorshipApplication existing = new SponsorshipApplication(reference, "student.demo", "UB", "BSc", UUID.randomUUID());
        DocumentRecord admission = document("ADMISSION_LETTER", "DEMO_ACCEPTED");
        UUID admissionId = admission.getId();
        when(applications.findByReference(reference)).thenReturn(Optional.of(existing));
        when(applicationDocuments.findByApplicationReferenceOrderByLinkedAtAsc(reference))
            .thenReturn(List.of(new ApplicationDocumentLink(reference, admissionId, "ADMISSION_LETTER")));
        when(documents.findAllById(List.of(admissionId))).thenReturn(List.of(admission));

        SponsorshipApplication updated = service.updateStatus("officer.demo", reference, ApplicationStatus.DOCUMENTS_VERIFIED, null);

        assertEquals(ApplicationStatus.DOCUMENTS_VERIFIED, updated.getStatus());
        org.mockito.ArgumentCaptor<ApplicationStatusHistory> captured = org.mockito.ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(history).save(captured.capture());
        assertEquals("APPLIED", captured.getValue().getFromStatus());
        assertEquals("DOCUMENTS_VERIFIED", captured.getValue().getToStatus());
        assertEquals("officer.demo", captured.getValue().getChangedBy());
        verify(audit).record("officer.demo", "APPLICATION_STATUS_CHANGED", "APPLICATION", reference);
    }

    @Test void officerCannotVerifyApplicationWhileAnyDocumentIsPendingScan() {
        String reference = "DTEF-2026-00000006";
        SponsorshipApplication existing = new SponsorshipApplication(reference, "student.demo", "UB", "BSc", UUID.randomUUID());
        DocumentRecord pending = document("ADMISSION_LETTER", "PENDING_SCAN");
        UUID pendingId = pending.getId();
        when(applications.findByReference(reference)).thenReturn(Optional.of(existing));
        when(applicationDocuments.findByApplicationReferenceOrderByLinkedAtAsc(reference))
            .thenReturn(List.of(new ApplicationDocumentLink(reference, pendingId, "ADMISSION_LETTER")));
        when(documents.findAllById(List.of(pendingId))).thenReturn(List.of(pending));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.updateStatus("officer.demo", reference, ApplicationStatus.DOCUMENTS_VERIFIED, null));

        assertEquals(409, error.getStatusCode().value());
        assertEquals(ApplicationStatus.APPLIED, existing.getStatus());
        verify(history, never()).save(any());
        verify(audit, never()).record(anyString(), anyString(), anyString(), anyString());
    }

    @Test void productionDocumentVerificationRejectsDemoAcceptedFiles() {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        String reference = "DTEF-2026-00000007";
        SponsorshipApplication existing = new SponsorshipApplication(reference, "student.demo", "UB", "BSc", UUID.randomUUID());
        DocumentRecord demoAccepted = document("ADMISSION_LETTER", "DEMO_ACCEPTED");
        UUID demoAcceptedId = demoAccepted.getId();
        when(applications.findByReference(reference)).thenReturn(Optional.of(existing));
        when(applicationDocuments.findByApplicationReferenceOrderByLinkedAtAsc(reference))
            .thenReturn(List.of(new ApplicationDocumentLink(reference, demoAcceptedId, "ADMISSION_LETTER")));
        when(documents.findAllById(List.of(demoAcceptedId))).thenReturn(List.of(demoAccepted));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.updateStatus("officer.demo", reference, ApplicationStatus.DOCUMENTS_VERIFIED, null));

        assertEquals(409, error.getStatusCode().value());
    }

    @Test void cleanDocumentsCanBeVerifiedOutsideDemoProfile() {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        String reference = "DTEF-2026-00000011";
        SponsorshipApplication existing = new SponsorshipApplication(reference, "student.demo", "University of Botswana", "BSc", UUID.randomUUID());
        DocumentRecord clean = document("ADMISSION_LETTER", "CLEAN");
        UUID cleanId = clean.getId();
        when(applications.findByReference(reference)).thenReturn(Optional.of(existing));
        when(applicationDocuments.findByApplicationReferenceOrderByLinkedAtAsc(reference))
            .thenReturn(List.of(new ApplicationDocumentLink(reference, cleanId, "ADMISSION_LETTER")));
        when(documents.findAllById(List.of(cleanId))).thenReturn(List.of(clean));

        SponsorshipApplication updated = service.updateStatus("officer.demo", reference, ApplicationStatus.DOCUMENTS_VERIFIED, null);
        assertEquals(ApplicationStatus.DOCUMENTS_VERIFIED, updated.getStatus());
    }

    @Test void rejectionRequiresAReasonAndDoesNotChangeTheApplication() {
        String reference = "DTEF-2026-00000003";
        SponsorshipApplication existing = new SponsorshipApplication(reference, "student.demo", "UB", "BSc", UUID.randomUUID());
        when(applications.findByReference(reference)).thenReturn(Optional.of(existing));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.updateStatus("officer.demo", reference, ApplicationStatus.REJECTED, "   "));

        assertEquals(400, error.getStatusCode().value());
        assertEquals(ApplicationStatus.APPLIED, existing.getStatus());
        verify(history, never()).save(any());
        verify(audit, never()).record(anyString(), anyString(), anyString(), anyString());
    }

    @Test void rejectionReasonIsStoredInTheApplicationHistory() {
        String reference = "DTEF-2026-00000004";
        SponsorshipApplication existing = new SponsorshipApplication(reference, "student.demo", "UB", "BSc", UUID.randomUUID());
        when(applications.findByReference(reference)).thenReturn(Optional.of(existing));

        service.updateStatus("officer.demo", reference, ApplicationStatus.REJECTED, "Admission letter could not be verified.");

        org.mockito.ArgumentCaptor<ApplicationStatusHistory> captured = org.mockito.ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(history).save(captured.capture());
        assertEquals("Admission letter could not be verified.", captured.getValue().getDecisionReason());
        verify(audit).record("officer.demo", "APPLICATION_REJECTED", "APPLICATION", reference);
    }

    @Test void studentCannotReadAnotherStudentsApplication() {
        String reference = "DTEF-2026-00000005";
        when(applications.findByReferenceAndOwnerSubject(reference, "student.other")).thenReturn(Optional.empty());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.getMine("student.other", reference));
        assertEquals(404, error.getStatusCode().value());
    }

    private DocumentRecord document(String type, String status) {
        DocumentRecord value = mock(DocumentRecord.class);
        when(value.getDocumentType()).thenReturn(type); when(value.getStatus()).thenReturn(status);
        when(value.getId()).thenReturn(UUID.randomUUID());
        return value;
    }
}
