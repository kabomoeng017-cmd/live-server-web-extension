package bw.gov.dtef.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * ClamAV TCP INSTREAM adapter. Created only in prod when explicitly enabled.
 * The upload controller is not wired to this adapter yet; production uploads remain disabled.
 */
@Component
@Profile("demo")
@ConditionalOnProperty(prefix = "dtef.antivirus", name = "enabled", havingValue = "true")
public class ClamAvDocumentScanner implements DocumentScanner {
    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final int CHUNK_BYTES = 2 * 1024;
    private static final int MAX_RESPONSE_BYTES = 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("application/pdf", "image/jpeg", "image/png");
    private final String host;
    private final int port;
    private final int timeoutMs;

    public ClamAvDocumentScanner(
        @Value("${dtef.antivirus.clamav.host}") String host,
        @Value("${dtef.antivirus.clamav.port}") int port,
        @Value("${dtef.antivirus.clamav.timeout-ms}") int timeoutMs
    ) {
        if (host == null || host.isBlank()) throw new IllegalArgumentException("ClamAV host is required.");
        if (port < 1 || port > 65535) throw new IllegalArgumentException("ClamAV port is invalid.");
        if (timeoutMs < 100 || timeoutMs > 60000) throw new IllegalArgumentException("ClamAV timeout must be between 100 and 60000 ms.");
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public ScanResult scan(byte[] content, String contentType) {
        if (content == null || content.length == 0 || content.length > MAX_BYTES)
            throw new IllegalArgumentException("Document content must be between 1 byte and 10 MB.");
        String normalizedType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(normalizedType))
            throw new IllegalArgumentException("Unsupported document content type for malware scanning.");

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            DataInputStream input = new DataInputStream(socket.getInputStream());
            output.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
            int offset = 0;
            while (offset < content.length) {
                int length = Math.min(CHUNK_BYTES, content.length - offset);
                output.writeInt(length);
                output.write(content, offset, length);
                offset += length;
            }
            output.writeInt(0);
            output.flush();
            return parseResponse(readResponse(input));
        } catch (IOException ex) {
            throw new ScannerUnavailableException("Malware scanning did not complete; the document must remain blocked.", ex);
        }
    }

    private String readResponse(DataInputStream input) throws IOException {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        while (true) {
            int value = input.read();
            if (value < 0) throw new IOException("ClamAV closed the connection before returning a verdict.");
            if (value == 0) return response.toString(StandardCharsets.US_ASCII);
            if (response.size() >= MAX_RESPONSE_BYTES) throw new IOException("ClamAV response exceeded the allowed size.");
            response.write(value);
        }
    }

    private ScanResult parseResponse(String response) throws IOException {
        if ("stream: OK".equals(response)) return new ScanResult(Verdict.CLEAN, null);
        int foundAt = response.lastIndexOf(" FOUND");
        if (foundAt > 0) {
            String signature = response.substring(0, foundAt);
            int separator = signature.indexOf(':');
            if (separator >= 0) signature = signature.substring(separator + 1).trim();
            return new ScanResult(Verdict.INFECTED, signature);
        }
        throw new IOException("ClamAV returned an unrecognized scan response.");
    }
}
