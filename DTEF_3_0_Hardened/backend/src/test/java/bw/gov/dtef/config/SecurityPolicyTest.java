package bw.gov.dtef.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HealthController.class)
@Import(SecurityConfig.class)
class SecurityPolicyTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test void healthEndpointIsPublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }
    @Test void protectedApiRejectsAnonymousRequest() throws Exception {
        mvc.perform(get("/api/analytics/summary")).andExpect(status().isUnauthorized());
    }
    @Test @WithMockUser(roles = "STUDENT")
    void studentCannotReadMinistryAnalytics() throws Exception {
        mvc.perform(get("/api/analytics/summary")).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(roles = "OFFICER")
    void officerCannotAccessFnbbPaymentSummary() throws Exception {
        mvc.perform(get("/api/payments/summary")).andExpect(status().isForbidden());
    }
}
