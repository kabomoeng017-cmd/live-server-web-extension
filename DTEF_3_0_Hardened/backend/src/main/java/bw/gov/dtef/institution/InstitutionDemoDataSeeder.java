package bw.gov.dtef.institution;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
@Component @Profile("demo")
public class InstitutionDemoDataSeeder implements CommandLineRunner {
 private final InstitutionRepository repository;
 public InstitutionDemoDataSeeder(InstitutionRepository repository){this.repository=repository;}
 @Override public void run(String... args){
  if(repository.findByCodeIgnoreCase("DEMO-UB").isEmpty()
      && repository.findByNormalizedName(InstitutionService.normalize("University of Botswana")).isEmpty())
   repository.save(InstitutionRecord.demoVerified("DEMO-UB","University of Botswana",InstitutionService.normalize("University of Botswana")));
 }
}