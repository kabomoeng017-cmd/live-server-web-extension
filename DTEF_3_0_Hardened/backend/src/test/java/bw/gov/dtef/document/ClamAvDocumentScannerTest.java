package bw.gov.dtef.document;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class ClamAvDocumentScannerTest {
    private static final byte[] PDF = "%PDF-1.4\nsynthetic".getBytes(StandardCharsets.US_ASCII);

    @Test void sendsClamAvInstreamFramesAndAcceptsCleanVerdict() throws Exception {
        DocumentScanner.ScanResult result = scanWithFakeDaemon("stream: OK", PDF);
        assertEquals(DocumentScanner.Verdict.CLEAN, result.verdict());
        assertNull(result.signature());
    }

    @Test void reportsInfectedVerdictAndSignature() throws Exception {
        DocumentScanner.ScanResult result = scanWithFakeDaemon("stream: Eicar-Test-Signature FOUND", PDF);
        assertEquals(DocumentScanner.Verdict.INFECTED, result.verdict());
        assertEquals("Eicar-Test-Signature", result.signature());
    }

    @Test void unknownDaemonResponseFailsClosed() {
        assertThrows(ScannerUnavailableException.class, () -> scanWithFakeDaemon("stream: MAYBE", PDF));
    }

    @Test void unavailableDaemonFailsClosed() throws Exception {
        int unusedPort;
        try (ServerSocket reservation = new ServerSocket(0)) { unusedPort = reservation.getLocalPort(); }
        ClamAvDocumentScanner scanner = new ClamAvDocumentScanner("127.0.0.1", unusedPort, 300);
        assertThrows(ScannerUnavailableException.class, () -> scanner.scan(PDF, "application/pdf"));
    }

    @Test void rejectsEmptyOrUnsupportedDocumentsBeforeConnecting() {
        ClamAvDocumentScanner scanner = new ClamAvDocumentScanner("127.0.0.1", 3310, 500);
        assertThrows(IllegalArgumentException.class, () -> scanner.scan(new byte[0], "application/pdf"));
        assertThrows(IllegalArgumentException.class, () -> scanner.scan(PDF, "text/plain"));
    }

    @Test void disabledProductionScannerFailsClosed() {
        DisabledDocumentScanner scanner = new DisabledDocumentScanner();
        assertThrows(ScannerUnavailableException.class, () -> scanner.scan(PDF, "application/pdf"));
    }

    @Test void rejectsNullContent() {
        ClamAvDocumentScanner scanner = new ClamAvDocumentScanner("127.0.0.1", 3310, 500);
        assertThrows(IllegalArgumentException.class, () -> scanner.scan(null, "application/pdf"));
    }

    @Test void rejectsContentLargerThanTenMegabytes() {
        ClamAvDocumentScanner scanner = new ClamAvDocumentScanner("127.0.0.1", 3310, 500);
        assertThrows(IllegalArgumentException.class, () -> scanner.scan(new byte[10 * 1024 * 1024 + 1], "application/pdf"));
    }

    @Test void rejectsNullContentType() {
        ClamAvDocumentScanner scanner = new ClamAvDocumentScanner("127.0.0.1", 3310, 500);
        assertThrows(IllegalArgumentException.class, () -> scanner.scan(PDF, null));
    }

    @Test void rejectsInvalidPort() {
        assertThrows(IllegalArgumentException.class, () -> new ClamAvDocumentScanner("127.0.0.1", 0, 500));
        assertThrows(IllegalArgumentException.class, () -> new ClamAvDocumentScanner("127.0.0.1", 65536, 500));
    }

    @Test void rejectsBlankHost() {
        assertThrows(IllegalArgumentException.class, () -> new ClamAvDocumentScanner(" ", 3310, 500));
    }

    @Test void rejectsTimeoutOutsideSafeRange() {
        assertThrows(IllegalArgumentException.class, () -> new ClamAvDocumentScanner("127.0.0.1", 3310, 99));
        assertThrows(IllegalArgumentException.class, () -> new ClamAvDocumentScanner("127.0.0.1", 3310, 60001));
    }

    @Test void rejectsUnsupportedTypeBeforeNetworkConnection() {
        ClamAvDocumentScanner scanner = new ClamAvDocumentScanner("127.0.0.1", 3310, 500);
        assertThrows(IllegalArgumentException.class, () -> scanner.scan(PDF, "application/octet-stream"));
    }

    @Test void daemonClosingWithoutVerdictFailsClosed() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> closeWithoutResponse = executor.submit(() -> {
                try (Socket ignored = server.accept()) { /* Deliberately close without a verdict. */ }
                catch (IOException ex) { throw new RuntimeException(ex); }
            });
            try {
                ClamAvDocumentScanner scanner = new ClamAvDocumentScanner("127.0.0.1", server.getLocalPort(), 1000);
                assertThrows(ScannerUnavailableException.class, () -> scanner.scan(PDF, "application/pdf"));
                closeWithoutResponse.get(2, TimeUnit.SECONDS);
            } finally { executor.shutdownNow(); }
        }
    }

    @Test void malformedFoundResponseFailsClosed() {
        assertThrows(ScannerUnavailableException.class, () -> scanWithFakeDaemon("FOUND", PDF));
    }

    @Test void streamsPayloadAcrossMultipleFrames() throws Exception {
        byte[] content = new byte[5001];
        System.arraycopy("%PDF-1.4".getBytes(StandardCharsets.US_ASCII), 0, content, 0, 8);
        DocumentScanner.ScanResult result = scanWithFakeDaemon("stream: OK", content);
        assertEquals(DocumentScanner.Verdict.CLEAN, result.verdict());
    }

    @Test void rejectsUnsupportedUppercaseTypeRatherThanGuessing() {
        ClamAvDocumentScanner scanner = new ClamAvDocumentScanner("127.0.0.1", 3310, 500);
        assertThrows(IllegalArgumentException.class, () -> scanner.scan(PDF, "APPLICATION/PDF "));
    }

    private DocumentScanner.ScanResult scanWithFakeDaemon(String daemonResponse, byte[] expectedContent) throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<byte[]> received = executor.submit(() -> {
                try (Socket client = server.accept()) {
                    client.setSoTimeout(3000);
                    DataInputStream input = new DataInputStream(client.getInputStream());
                    String command = readNullTerminated(input);
                    if (!"zINSTREAM".equals(command)) throw new IOException("Unexpected ClamAV command: " + command);
                    ByteArrayOutputStream payload = new ByteArrayOutputStream();
                    while (true) {
                        int length = input.readInt();
                        if (length == 0) break;
                        if (length < 0 || length > 2048) throw new IOException("Invalid INSTREAM frame length.");
                        byte[] chunk = input.readNBytes(length);
                        if (chunk.length != length) throw new EOFException("Incomplete INSTREAM frame.");
                        payload.write(chunk);
                    }
                    DataOutputStream output = new DataOutputStream(client.getOutputStream());
                    output.write(daemonResponse.getBytes(StandardCharsets.US_ASCII));
                    output.writeByte(0);
                    output.flush();
                    return payload.toByteArray();
                }
            });
            try {
                DocumentScanner.ScanResult result = new ClamAvDocumentScanner(
                    "127.0.0.1", server.getLocalPort(), 3000
                ).scan(expectedContent, "application/pdf");
                assertArrayEquals(expectedContent, received.get(3, TimeUnit.SECONDS));
                return result;
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private String readNullTerminated(DataInputStream input) throws IOException {
        ByteArrayOutputStream value = new ByteArrayOutputStream();
        while (true) {
            int next = input.read();
            if (next < 0) throw new EOFException("Missing INSTREAM command terminator.");
            if (next == 0) return value.toString(StandardCharsets.US_ASCII);
            value.write(next);
        }
    }
}
