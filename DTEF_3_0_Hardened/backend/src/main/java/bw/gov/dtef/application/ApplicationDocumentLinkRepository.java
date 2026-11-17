package bw.gov.dtef.application;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface ApplicationDocumentLinkRepository extends JpaRepository<ApplicationDocumentLink, UUID> {
    List<ApplicationDocumentLink> findByApplicationReferenceOrderByLinkedAtAsc(String applicationReference);
    boolean existsByApplicationReferenceAndDocumentId(String applicationReference, UUID documentId);
}
