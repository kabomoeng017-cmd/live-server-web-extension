package bw.gov.dtef.document;

import java.util.Objects;

/** Malware-scanning boundary. Unavailable or unknown outcomes must never be treated as clean. */
public interface DocumentScanner {
    ScanResult scan(byte[] content, String contentType);

    enum Verdict { CLEAN, INFECTED }

    record ScanResult(Verdict verdict, String signature) {
        public ScanResult {
            Objects.requireNonNull(verdict, "verdict");
            if (verdict == Verdict.CLEAN) signature = null;
            if (verdict == Verdict.INFECTED && (signature == null || signature.isBlank())) signature = "UNKNOWN";
        }
    }
}
