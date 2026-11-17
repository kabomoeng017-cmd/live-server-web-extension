package bw.gov.dtef.application;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "application_status_history", indexes = @Index(name = "idx_app_history_ref_time", columnList = "application_reference,changed_at"))
public class ApplicationStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "application_reference", nullable = false, length = 32) private String applicationReference;
    @Column(name = "from_status", length = 40) private String fromStatus;
    @Column(name = "to_status", nullable = false, length = 40) private String toStatus;
    @Column(name = "changed_by", nullable = false, length = 120) private String changedBy;
    @Column(name = "changed_at", nullable = false) private Instant changedAt = Instant.now();
    protected ApplicationStatusHistory() {}
    public ApplicationStatusHistory(String reference, String fromStatus, String toStatus, String changedBy) {
        this.applicationReference = reference; this.fromStatus = fromStatus; this.toStatus = toStatus; this.changedBy = changedBy;
    }
    public String getApplicationReference() { return applicationReference; }
    public String getFromStatus() { return fromStatus; }
    public String getToStatus() { return toStatus; }
    public String getChangedBy() { return changedBy; }
    public Instant getChangedAt() { return changedAt; }
}
