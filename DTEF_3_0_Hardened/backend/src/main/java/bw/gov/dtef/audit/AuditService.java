package bw.gov.dtef.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class AuditService {
    private final AuditRepository repository;
    public AuditService(AuditRepository repository) { this.repository = repository; }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String actor, String action, String resourceType, String resourceId) {
        repository.save(new AuditEvent(actor, action, resourceType, resourceId));
    }
}
