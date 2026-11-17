package bw.gov.dtef.document;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/documents")
@Profile("!demo")
public class DocumentIntegrationUnavailableController {
    @PostMapping
    public void upload() {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "Production encrypted object storage and malware scanning are not configured. Document upload is disabled.");
    }
}
