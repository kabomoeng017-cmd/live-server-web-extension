package bw.gov.dtef.institution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface InstitutionRepository extends JpaRepository<InstitutionRecord,UUID> {
 Optional<InstitutionRecord> findByCodeIgnoreCase(String code);
 Optional<InstitutionRecord> findByNormalizedName(String normalizedName);
 boolean existsByCodeIgnoreCase(String code);
 List<InstitutionRecord> findAllByOrderByNameAsc();
 List<InstitutionRecord> findByStatusOrderByNameAsc(String status);
 List<InstitutionRecord> findByStatusInOrderByNameAsc(List<String> statuses);
}