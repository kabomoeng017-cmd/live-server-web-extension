package bw.gov.dtef.document;

/** Raised when a scan cannot be completed and the document must remain blocked. */
public class ScannerUnavailableException extends RuntimeException {
    public ScannerUnavailableException(String message) { super(message); }
    public ScannerUnavailableException(String message, Throwable cause) { super(message, cause); }
}
