package bw.gov.dtef.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<SponsorshipApplication, UUID> {
    Optional<SponsorshipApplication> findByReferenceAndOwnerSubject(String reference, String ownerSubject);
    Optional<SponsorshipApplication> findByReference(String reference);
    List<SponsorshipApplication> findTop100ByOrderByCreatedAtDesc();
    List<SponsorshipApplication> findAllByOwnerSubjectOrderByCreatedAtDesc(String ownerSubject);
    @Query(value = "select nextval('dtef_application_ref_seq')", nativeQuery = true)
    long nextReferenceSequence();
}
