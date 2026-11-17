package bw.gov.dtef.officer;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/officer/applications/{reference}/documents")
@Profile("!demo")
public class OfficerDocumentUnavailableController {
    @GetMapping("/{documentId}")
    public void downloadUnavailable() {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "Production encrypted document storage and malware scanning are not configured. Officer document download is disabled.");
    }
}
