package bw.gov.dtef.ai;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai")
@Profile("!demo")
public class AiIntegrationUnavailableController {
    @PostMapping("/admission-letter")
    public void readAdmissionLetter() {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "Approved OCR/document extraction provider is not configured. Do not rely on automated extraction until verified.");
    }
}
