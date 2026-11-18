package bw.gov.dtef.document;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Production default: refuse scans until the approved malware scanner is configured. */
@Component
@Profile("prod")
@ConditionalOnProperty(prefix = "dtef.antivirus", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledDocumentScanner implements DocumentScanner {
    @Override
    public ScanResult scan(byte[] content, String contentType) {
        throw new ScannerUnavailableException(
            "Malware scanning is disabled; the document must remain blocked and cannot be marked CLEAN."
        );
    }
}
