package bw.gov.dtef.identity;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/omang")
@Profile("!demo")
public class OmangIntegrationUnavailableController {
    @PostMapping("/verify")
    public void verify() {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "Official identity verification adapter is not configured. No national identity register is accessed by this build.");
    }
}
