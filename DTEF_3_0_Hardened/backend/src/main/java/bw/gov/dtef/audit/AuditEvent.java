package bw.gov.dtef.audit;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events", indexes = @Index(name = "idx_audit_resource", columnList = "resource_type,resource_id"))
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, length = 120) private String actor;
    @Column(nullable = false, length = 80) private String action;
    @Column(name = "resource_type", nullable = false, length = 80) private String resourceType;
    @Column(name = "resource_id", length = 120) private String resourceId;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt = Instant.now();
    protected AuditEvent() {}
    public AuditEvent(String actor, String action, String resourceType, String resourceId) {
        this.actor = actor; this.action = action; this.resourceType = resourceType; this.resourceId = resourceId;
    }
}
