package bw.gov.dtef.application;

import bw.gov.dtef.audit.AuditService;
import bw.gov.dtef.auth.OtpService;
import bw.gov.dtef.document.DocumentRecord;
import bw.gov.dtef.document.DocumentRepository;
import bw.gov.dtef.identity.IdentityVerificationService;
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
    private ApplicationService service;

    @BeforeEach void setup() {
        applications = mock(ApplicationRepository.class); documents = mock(DocumentRepository.class);
        identity = mock(IdentityVerificationService.class); otp = mock(OtpService.class);
        audit = mock(AuditService.class); environment = mock(Environment.class); history = mock(ApplicationStatusHistoryRepository.class);
        applicationDocuments = mock(ApplicationDocumentLinkRepository.class);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);
        service = new ApplicationService(applications, documents, identity, otp, audit, environment, history, applicationDocuments);
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
        assertThrows(ResponseStatusException.class, () -> service.updateStatus("officer.demo", existing.getReference(), ApplicationStatus.COMPLETED));
    }

    private DocumentRecord document(String type, String status) {
        DocumentRecord value = mock(DocumentRecord.class);
        when(value.getDocumentType()).thenReturn(type); when(value.getStatus()).thenReturn(status);
        when(value.getId()).thenReturn(UUID.randomUUID());
        return value;
    }
}
