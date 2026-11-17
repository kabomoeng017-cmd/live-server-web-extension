package bw.gov.dtef.application;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "application_documents",
    uniqueConstraints = @UniqueConstraint(name = "uq_application_document_link", columnNames = {"application_reference", "document_id"}),
    indexes = @Index(name = "idx_application_documents_document", columnList = "document_id"))
public class ApplicationDocumentLink {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "application_reference", nullable = false, length = 32) private String applicationReference;
    @Column(name = "document_id", nullable = false) private UUID documentId;
    @Column(name = "document_type", nullable = false, length = 40) private String documentType;
    @Column(name = "linked_at", nullable = false) private Instant linkedAt = Instant.now();
    protected ApplicationDocumentLink() {}
    public ApplicationDocumentLink(String applicationReference, UUID documentId, String documentType) {
        this.applicationReference = applicationReference; this.documentId = documentId; this.documentType = documentType;
    }
    public String getApplicationReference() { return applicationReference; }
    public UUID getDocumentId() { return documentId; }
    public String getDocumentType() { return documentType; }
    public Instant getLinkedAt() { return linkedAt; }
}
