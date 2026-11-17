package bw.gov.dtef.document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface DocumentRepository extends JpaRepository<DocumentRecord, UUID> {
    Optional<DocumentRecord> findByIdAndOwnerSubject(UUID id, String ownerSubject);
}
