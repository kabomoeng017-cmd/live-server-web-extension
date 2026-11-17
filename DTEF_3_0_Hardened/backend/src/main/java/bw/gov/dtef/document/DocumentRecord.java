package bw.gov.dtef.document;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class DocumentRecord {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "owner_subject", nullable = false, length = 120)
    private String ownerSubject;
    @Column(name = "document_type", nullable = false, length = 40)
    private String documentType;
    @Column(name = "storage_key", nullable = false, unique = true, length = 80)
    private String storageKey;
    @Column(name = "original_name", nullable = false, length = 180)
    private String originalName;
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;
    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;
    @Column(name = "file_size", nullable = false)
    private long size;
    @Column(nullable = false, length = 40)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    protected DocumentRecord() {}
    public DocumentRecord(String ownerSubject, String documentType, String storageKey, String originalName, String contentType, String sha256, long size, String status) {
        this.ownerSubject = ownerSubject; this.documentType = documentType; this.storageKey = storageKey;
        this.originalName = originalName; this.contentType = contentType; this.sha256 = sha256; this.size = size; this.status = status;
    }
    public UUID getId() { return id; }
    public String getOwnerSubject() { return ownerSubject; }
    public String getDocumentType() { return documentType; }
    public String getStorageKey() { return storageKey; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public String getSha256() { return sha256; }
    public long getSize() { return size; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
