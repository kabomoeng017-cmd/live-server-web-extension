package bw.gov.dtef.institution;

import bw.gov.dtef.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InstitutionServiceTest {
    private InstitutionRepository repository;
    private AuditService audit;
    private Environment environment;
    private InstitutionService service;

    @BeforeEach void setUp() {
        repository = mock(InstitutionRepository.class);
        audit = mock(AuditService.class);
        environment = mock(Environment.class);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        service = new InstitutionService(repository, audit, environment);
    }

    @Test void rejectsUnknownInstitutionForStudentApplication() {
        when(repository.findByNormalizedName("unknown college")).thenReturn(Optional.empty());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.requireEligibleForApplication("Unknown College"));
        assertEquals(409, error.getStatusCode().value());
    }

    @Test void onlyVerifiedInstitutionsAppearInProductionCatalogue() {
        when(repository.findByStatusOrderByNameAsc("VERIFIED")).thenReturn(List.of());
        assertTrue(service.catalogue().isEmpty());
        verify(repository).findByStatusOrderByNameAsc("VERIFIED");
        verify(repository, never()).findByStatusInOrderByNameAsc(anyList());
    }

    @Test void demoVerifiedInstitutionIsRejectedOutsideDemoProfile() {
        InstitutionRecord demo = InstitutionRecord.demoVerified("DEMO-UB", "University of Botswana",
            InstitutionService.normalize("University of Botswana"));
        when(repository.findByNormalizedName("university of botswana")).thenReturn(Optional.of(demo));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.requireEligibleForApplication("University of Botswana"));
        assertEquals(409, error.getStatusCode().value());
    }

    @Test void demoVerifiedInstitutionIsAcceptedOnlyInsideDemoProfile() {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);
        InstitutionRecord demo = InstitutionRecord.demoVerified("DEMO-UB", "University of Botswana",
            InstitutionService.normalize("University of Botswana"));
        when(repository.findByNormalizedName("university of botswana")).thenReturn(Optional.of(demo));
        assertEquals("University of Botswana", service.requireEligibleForApplication(" university of botswana "));
    }

    @Test void manualVerificationRequiresDifferentAdministratorAndRecordsActor() {
        InstitutionRecord pending = new InstitutionRecord("UB", "University of Botswana",
            InstitutionService.normalize("University of Botswana"), "PENDING", "admin.creator");
        when(repository.findByCodeIgnoreCase("UB")).thenReturn(Optional.of(pending));
        InstitutionRecord verified = service.verifyManually("ub", "Official institution register ref 2026-001", "admin.verifier");
        assertEquals("VERIFIED", verified.getStatus());
        assertEquals("MANUAL_OFFICIAL_CONFIRMATION", verified.getVerificationSource());
        assertEquals("admin.verifier", verified.getVerifiedBy());
        verify(audit).record("admin.verifier", "INSTITUTION_MANUALLY_VERIFIED", "INSTITUTION", "UB");
    }

    @Test void creatorCannotApproveOwnInstitutionRegistration() {
        InstitutionRecord pending = new InstitutionRecord("UB", "University of Botswana",
            InstitutionService.normalize("University of Botswana"), "PENDING", "admin.creator");
        when(repository.findByCodeIgnoreCase("UB")).thenReturn(Optional.of(pending));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.verifyManually("UB", "Official registry reference 2026-001", "admin.creator"));
        assertEquals(403, error.getStatusCode().value());
        verify(audit, never()).record(anyString(), anyString(), anyString(), anyString());
    }

    @Test void suspendedInstitutionCannotBeUsedForApplication() {
        InstitutionRecord suspended = new InstitutionRecord("UB", "University of Botswana",
            InstitutionService.normalize("University of Botswana"), "SUSPENDED", "admin.creator");
        when(repository.findByNormalizedName("university of botswana")).thenReturn(Optional.of(suspended));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.requireEligibleForApplication("University of Botswana"));
        assertEquals(409, error.getStatusCode().value());
    }

    @Test void registeringInstitutionCreatesPendingRecordAndAuditEvent() {
        when(repository.existsByCodeIgnoreCase("TEST-UNI")).thenReturn(false);
        when(repository.findByNormalizedName("test university")).thenReturn(Optional.empty());
        when(repository.save(any(InstitutionRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        InstitutionRecord created = service.register("test-uni", " Test   University ", "admin.creator");
        assertEquals("TEST-UNI", created.getCode());
        assertEquals("PENDING", created.getStatus());
        verify(audit).record("admin.creator", "INSTITUTION_REGISTERED_PENDING", "INSTITUTION", "TEST-UNI");
    }
}
