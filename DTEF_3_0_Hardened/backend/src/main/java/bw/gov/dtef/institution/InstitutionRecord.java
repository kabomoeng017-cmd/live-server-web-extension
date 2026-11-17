package bw.gov.dtef.institution;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="institutions")
public class InstitutionRecord {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(name="institution_code",nullable=false,unique=true,length=32) private String code;
 @Column(nullable=false,length=180) private String name;
 @Column(name="normalized_name",nullable=false,unique=true,length=180) private String normalizedName;
 @Column(nullable=false,length=24) private String status;
 @Column(name="verification_source",length=60) private String verificationSource;
 @Column(name="verification_reference",length=180) private String verificationReference;
 @Column(name="verified_by",length=120) private String verifiedBy;
 @Column(name="verified_at") private Instant verifiedAt;
 @Column(name="created_by",nullable=false,length=120) private String createdBy;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
 @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
 @Version private long version;
 protected InstitutionRecord() {}
 public InstitutionRecord(String code,String name,String normalizedName,String status,String createdBy) {
  this.code=code; this.name=name; this.normalizedName=normalizedName; this.status=status; this.createdBy=createdBy;
 }
 public static InstitutionRecord demoVerified(String code,String name,String normalizedName) {
  InstitutionRecord i=new InstitutionRecord(code,name,normalizedName,"DEMO_VERIFIED","demo-seed");
  i.verificationSource="DEMO_FIXTURE"; i.verificationReference="LOCAL-DEMO-ONLY";
  i.verifiedBy="demo-seed"; i.verifiedAt=Instant.now(); return i;
 }
 public void verifyManually(String actor,String reference) {
  status="VERIFIED"; verificationSource="MANUAL_OFFICIAL_CONFIRMATION";
  verificationReference=reference; verifiedBy=actor; verifiedAt=Instant.now(); updatedAt=Instant.now();
 }
 public void suspend() { status="SUSPENDED"; updatedAt=Instant.now(); }
 public UUID getId(){return id;} public String getCode(){return code;} public String getName(){return name;}
 public String getNormalizedName(){return normalizedName;} public String getStatus(){return status;}
 public String getVerificationSource(){return verificationSource;} public String getVerificationReference(){return verificationReference;}
 public String getVerifiedBy(){return verifiedBy;} public Instant getVerifiedAt(){return verifiedAt;}
 public String getCreatedBy(){return createdBy;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}