package bw.gov.dtef.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Profile("demo")
public class DocumentStorageService {
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("application/pdf", "image/jpeg", "image/png");
    private final Path root;

    public DocumentStorageService(@Value("${dtef.storage-dir:./var/uploads}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public StoredFile store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Choose a document before uploading.");
        if (file.getSize() > MAX_BYTES) throw new IllegalArgumentException("Document exceeds the 10 MB limit.");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!TYPES.contains(contentType)) throw new IllegalArgumentException("Only PDF, JPG and PNG documents are accepted.");
        byte[] bytes = file.getBytes();
        if (!signatureMatches(contentType, bytes)) throw new IllegalArgumentException("The file content does not match its declared type.");
        String storageKey = UUID.randomUUID().toString();
        Files.createDirectories(root);
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("Invalid storage path.");
        Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        String hash;
        try { hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception ex) { throw new IllegalStateException("Unable to fingerprint uploaded document.", ex); }
        return new StoredFile(storageKey, contentType, hash, bytes.length);
    }

    public byte[] read(String storageKey) throws IOException {
        if (storageKey == null || !storageKey.matches("[0-9a-fA-F-]{36}"))
            throw new IllegalArgumentException("Invalid document storage key.");
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root) || Files.isSymbolicLink(target) || !Files.isRegularFile(target, java.nio.file.LinkOption.NOFOLLOW_LINKS))
            throw new IllegalArgumentException("Stored document was not found.");
        return Files.readAllBytes(target);
    }

    public byte[] readVerified(String storageKey, String expectedSha256, long expectedSize) throws IOException {
        if (expectedSha256 == null || !expectedSha256.matches("[0-9a-fA-F]{64}") || expectedSize < 1 || expectedSize > MAX_BYTES)
            throw new IntegrityException("Stored document metadata is invalid.");
        byte[] bytes = read(storageKey);
        if (bytes.length != expectedSize)
            throw new IntegrityException("Stored document size does not match its recorded metadata.");
        byte[] expected = HexFormat.of().parseHex(expectedSha256);
        byte[] actual;
        try { actual = MessageDigest.getInstance("SHA-256").digest(bytes); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 is unavailable.", ex); }
        if (!MessageDigest.isEqual(expected, actual))
            throw new IntegrityException("Stored document fingerprint does not match its recorded metadata.");
        return bytes;
    }

    public static final class IntegrityException extends IOException {
        public IntegrityException(String message) { super(message); }
    }

    public void delete(String storageKey) {
        if (storageKey == null || !storageKey.matches("[0-9a-fA-F-]{36}")) return;
        try { Files.deleteIfExists(root.resolve(storageKey).normalize()); }
        catch (IOException ignored) { /* A storage reconciliation job should detect orphaned objects. */ }
    }

    private boolean signatureMatches(String type, byte[] b) {
        if ("application/pdf".equals(type)) return b.length >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-';
        if ("image/png".equals(type)) return b.length >= 8 && (b[0] & 255) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
            && (b[4] & 255) == 0x0D && (b[5] & 255) == 0x0A && (b[6] & 255) == 0x1A && (b[7] & 255) == 0x0A;
        return "image/jpeg".equals(type) && b.length >= 3 && (b[0] & 255) == 0xFF && (b[1] & 255) == 0xD8 && (b[2] & 255) == 0xFF;
    }
    public record StoredFile(String storageKey, String contentType, String sha256, long size) {}
}
