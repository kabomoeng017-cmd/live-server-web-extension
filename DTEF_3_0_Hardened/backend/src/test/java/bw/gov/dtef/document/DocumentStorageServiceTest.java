package bw.gov.dtef.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class DocumentStorageServiceTest {
    @TempDir Path temp;
    @Test void acceptsPdfWithPdfSignatureAndStoresUnderGeneratedKey() throws Exception {
        DocumentStorageService service = new DocumentStorageService(temp.toString());
        byte[] pdf = "%PDF-1.4\nmock".getBytes();
        var file = new MockMultipartFile("file", "admission.pdf", "application/pdf", pdf);
        var stored = service.store(file);
        assertNotNull(stored.storageKey());
        assertEquals(64, stored.sha256().length());
        assertTrue(java.nio.file.Files.exists(temp.resolve(stored.storageKey())));
    }
    @Test void rejectsSpoofedPdfMimeType() {
        DocumentStorageService service = new DocumentStorageService(temp.toString());
        var file = new MockMultipartFile("file", "fake.pdf", "application/pdf", "not a pdf".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service.store(file));
    }
    @Test void rejectsOversizedUpload() {
        DocumentStorageService service = new DocumentStorageService(temp.toString());
        var file = new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[10 * 1024 * 1024 + 1]);
        assertThrows(IllegalArgumentException.class, () -> service.store(file));
    }
}
