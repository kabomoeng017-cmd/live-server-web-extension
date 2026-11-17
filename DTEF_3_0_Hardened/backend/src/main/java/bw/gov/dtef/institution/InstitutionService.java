package bw.gov.dtef.institution;
import bw.gov.dtef.audit.AuditService;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
@Service
public class InstitutionService {
 private final InstitutionRepository repository; private final AuditService audit; private final Environment environment;
 public InstitutionService(InstitutionRepository repository,AuditService audit,Environment environment) {
  this.repository=repository; this.audit=audit; this.environment=environment;
 }
 @Transactional(readOnly=true) public List<InstitutionRecord> catalogue() {
  if(environment.acceptsProfiles(Profiles.of("demo")))
   return repository.findByStatusInOrderByNameAsc(List.of("VERIFIED","DEMO_VERIFIED"));
  return repository.findByStatusOrderByNameAsc("VERIFIED");
 }
 @Transactional(readOnly=true) public List<InstitutionRecord> allRecords(){return repository.findAllByOrderByNameAsc();}
 @Transactional public InstitutionRecord register(String rawCode,String rawName,String actor) {
  String code=rawCode==null?"":rawCode.trim().toUpperCase(Locale.ROOT);
  String name=rawName==null?"":rawName.trim().replaceAll("\\s+"," ");
  if(!code.matches("[A-Z0-9-]{3,32}")||name.isBlank()||name.length()>180)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Provide a valid institution code and name.");
  String normalized=normalize(name);
  if(repository.existsByCodeIgnoreCase(code)||repository.findByNormalizedName(normalized).isPresent())
   throw new ResponseStatusException(HttpStatus.CONFLICT,"An institution with that code or name is already registered.");
  InstitutionRecord saved=repository.save(new InstitutionRecord(code,name,normalized,"PENDING",actor));
  audit.record(actor,"INSTITUTION_REGISTERED_PENDING","INSTITUTION",code); return saved;
 }
 @Transactional public InstitutionRecord verifyManually(String rawCode,String rawReference,String actor) {
  String code=rawCode==null?"":rawCode.trim().toUpperCase(Locale.ROOT);
  String reference=rawReference==null?"":rawReference.trim();
  if(reference.length()<5||reference.length()>180)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Provide the reference for the official source checked.");
  InstitutionRecord item=repository.findByCodeIgnoreCase(code)
   .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Institution was not found."));
  if(!"PENDING".equals(item.getStatus()))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Only pending institutions can be manually verified.");
  if(item.getCreatedBy().equals(actor))
   throw new ResponseStatusException(HttpStatus.FORBIDDEN,"A different administrator must verify this institution (maker-checker control).");
  item.verifyManually(actor,reference); audit.record(actor,"INSTITUTION_MANUALLY_VERIFIED","INSTITUTION",code); return item;
 }
 @Transactional public InstitutionRecord suspend(String rawCode,String rawReason,String actor) {
  String code=rawCode==null?"":rawCode.trim().toUpperCase(Locale.ROOT);
  String reason=rawReason==null?"":rawReason.trim();
  if(reason.length()<10||reason.length()>500)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Provide a suspension reason between 10 and 500 characters.");
  InstitutionRecord item=repository.findByCodeIgnoreCase(code)
   .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Institution was not found."));
  if("SUSPENDED".equals(item.getStatus()))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Institution is already suspended.");
  item.suspend(); audit.record(actor,"INSTITUTION_SUSPENDED","INSTITUTION",code); return item;
 }
 @Transactional(readOnly=true) public String requireEligibleForApplication(String name) {
  InstitutionRecord item=repository.findByNormalizedName(normalize(name))
   .orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Select an institution listed in the verified DTEF institution catalogue."));
  boolean demoAllowed="DEMO_VERIFIED".equals(item.getStatus())&&environment.acceptsProfiles(Profiles.of("demo"));
  if(!"VERIFIED".equals(item.getStatus())&&!demoAllowed)
   throw new ResponseStatusException(HttpStatus.CONFLICT,"This institution is not currently verified for DTEF applications.");
  return item.getName();
 }
 static String normalize(String value) {
  if(value==null)return "";
  return Normalizer.normalize(value.trim().replaceAll("\\s+"," "),Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
 }
}