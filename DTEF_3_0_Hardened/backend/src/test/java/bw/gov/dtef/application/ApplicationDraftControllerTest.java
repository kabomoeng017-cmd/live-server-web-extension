package bw.gov.dtef.application;

import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApplicationDraftControllerTest {
    @Test void acceptsOnlyAllowedNonSensitiveDraftFields() throws Exception {
        ApplicationDraftRepository repository = mock(ApplicationDraftRepository.class);
        when(repository.findById("student.demo")).thenReturn(Optional.empty());
        Jwt jwt = mock(Jwt.class); when(jwt.getSubject()).thenReturn("student.demo");
        ApplicationDraftController controller = new ApplicationDraftController(repository, JsonMapper.builder().build());
        var result = controller.save(Map.of("step", 2, "institution", "UB", "programme", "BSc"), jwt);
        assertEquals(true, result.get("saved"));
        verify(repository).save(any(ApplicationDraft.class));
    }

    @Test void rejectsOmangOrContactInformationInDraftPayload() {
        ApplicationDraftRepository repository = mock(ApplicationDraftRepository.class);
        Jwt jwt = mock(Jwt.class); when(jwt.getSubject()).thenReturn("student.demo");
        ApplicationDraftController controller = new ApplicationDraftController(repository, JsonMapper.builder().build());
        var payload = Map.of("step", 2, "admissionDocument", Map.of("documentId", "0c4a5f08-0b3a-4c58-a32f-9c40f56d2c18", "type", "ADMISSION_LETTER", "omangNumber", "123456789"));
        assertThrows(ResponseStatusException.class, () -> controller.save(payload, jwt));
        verify(repository, never()).save(any(ApplicationDraft.class));
    }

    @Test void rejectsUnsupportedNestedDraftFields() {
        ApplicationDraftRepository repository = mock(ApplicationDraftRepository.class);
        Jwt jwt = mock(Jwt.class); when(jwt.getSubject()).thenReturn("student.demo");
        ApplicationDraftController controller = new ApplicationDraftController(repository, JsonMapper.builder().build());
        var payload = Map.of("step", 2, "admissionDocument", Map.of("documentId", "0c4a5f08-0b3a-4c58-a32f-9c40f56d2c18", "type", "ADMISSION_LETTER", "externalUrl", "https://evil.example"));
        assertThrows(ResponseStatusException.class, () -> controller.save(payload, jwt));
        verify(repository, never()).save(any(ApplicationDraft.class));
    }
}
