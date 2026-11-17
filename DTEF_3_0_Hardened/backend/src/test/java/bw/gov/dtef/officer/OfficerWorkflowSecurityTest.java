package bw.gov.dtef.officer;

import bw.gov.dtef.application.ApplicationRepository;
import bw.gov.dtef.application.ApplicationService;
import bw.gov.dtef.application.ApplicationStatus;
import bw.gov.dtef.application.SponsorshipApplication;
import bw.gov.dtef.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {OfficerController.class, bw.gov.dtef.application.ApplicationController.class})
@Import(SecurityConfig.class)
class OfficerWorkflowSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean JwtDecoder jwtDecoder;
    @MockBean ApplicationRepository applications;
    @MockBean ApplicationService service;

    @Test void officerCanReadTheReviewQueue() throws Exception {
        mvc.perform(get("/api/officer/queue")
                .with(jwt().jwt(token -> token.subject("officer.demo"))
                    .authorities(new SimpleGrantedAuthority("ROLE_OFFICER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(0));
    }

    @Test void studentCannotReadTheOfficerQueue() throws Exception {
        mvc.perform(get("/api/officer/queue")
                .with(jwt().jwt(token -> token.subject("student.demo"))
                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
            .andExpect(status().isForbidden());
    }

    @Test void anonymousUserCannotReadTheOfficerQueue() throws Exception {
        mvc.perform(get("/api/officer/queue"))
            .andExpect(status().isUnauthorized());
    }

    @Test void officerCanRecordAnApplicationStatusDecision() throws Exception {
        String reference = "DTEF-2026-00000007";
        SponsorshipApplication app = new SponsorshipApplication(reference, "student.demo", "University of Botswana", "BSc Computer Science", UUID.randomUUID());
        app.setStatus(ApplicationStatus.DOCUMENTS_VERIFIED);
        when(service.updateStatus("officer.demo", reference, ApplicationStatus.DOCUMENTS_VERIFIED, null)).thenReturn(app);

        mvc.perform(put("/api/applications/{reference}/status", reference)
                .with(jwt().jwt(token -> token.subject("officer.demo"))
                    .authorities(new SimpleGrantedAuthority("ROLE_OFFICER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DOCUMENTS_VERIFIED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reference").value(reference))
            .andExpect(jsonPath("$.status").value("DOCUMENTS_VERIFIED"));

        verify(service).updateStatus("officer.demo", reference, ApplicationStatus.DOCUMENTS_VERIFIED, null);
    }

    @Test void studentCannotChangeApplicationStatus() throws Exception {
        String reference = "DTEF-2026-00000008";
        mvc.perform(put("/api/applications/{reference}/status", reference)
                .with(jwt().jwt(token -> token.subject("student.demo"))
                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DOCUMENTS_VERIFIED\"}"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }
}
