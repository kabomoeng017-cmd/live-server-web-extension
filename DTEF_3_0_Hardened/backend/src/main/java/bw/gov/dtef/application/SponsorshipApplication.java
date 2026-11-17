package bw.gov.dtef.application;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sponsorship_applications")
public class SponsorshipApplication {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true, length = 32)
    private String reference;
    @Column(name = "owner_subject", nullable = false, length = 120)
    private String ownerSubject;
    @Column(nullable = false, length = 180)
    private String institution;
    @Column(nullable = false, length = 180)
    private String programme;
    @Column(name = "admission_document_id", nullable = false)
    private UUID admissionDocumentId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private ApplicationStatus status = ApplicationStatus.APPLIED;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    @Version
    private long version;

    protected SponsorshipApplication() {}
    public SponsorshipApplication(String reference, String ownerSubject, String institution, String programme, UUID admissionDocumentId) {
        this.reference = reference; this.ownerSubject = ownerSubject; this.institution = institution;
        this.programme = programme; this.admissionDocumentId = admissionDocumentId;
    }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public String getOwnerSubject() { return ownerSubject; }
    public String getInstitution() { return institution; }
    public String getProgramme() { return programme; }
    public UUID getAdmissionDocumentId() { return admissionDocumentId; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
