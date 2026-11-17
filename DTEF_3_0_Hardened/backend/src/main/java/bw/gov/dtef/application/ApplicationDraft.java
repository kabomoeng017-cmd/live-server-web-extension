package bw.gov.dtef.application;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "application_drafts")
public class ApplicationDraft {
    @Id @Column(name = "owner_subject", length = 120)
    private String ownerSubject;
    @Column(name = "payload_json", nullable = false, columnDefinition = "text")
    private String payloadJson;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    protected ApplicationDraft() {}
    public ApplicationDraft(String ownerSubject, String payloadJson) { this.ownerSubject = ownerSubject; this.payloadJson = payloadJson; }
    public String getOwnerSubject() { return ownerSubject; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; this.updatedAt = Instant.now(); }
    public Instant getUpdatedAt() { return updatedAt; }
}
