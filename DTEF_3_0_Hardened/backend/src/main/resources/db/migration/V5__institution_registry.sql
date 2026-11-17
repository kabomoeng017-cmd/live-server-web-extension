CREATE TABLE IF NOT EXISTS institutions (
 id UUID PRIMARY KEY,
 institution_code VARCHAR(32) NOT NULL UNIQUE,
 name VARCHAR(180) NOT NULL,
 normalized_name VARCHAR(180) NOT NULL UNIQUE,
 status VARCHAR(24) NOT NULL,
 verification_source VARCHAR(60),
 verification_reference VARCHAR(180),
 verified_by VARCHAR(120),
 verified_at TIMESTAMPTZ,
 created_by VARCHAR(120) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT ck_institution_status CHECK(status IN ('PENDING','VERIFIED','SUSPENDED','DEMO_VERIFIED')),
 CONSTRAINT ck_institution_verified_metadata CHECK(status NOT IN ('VERIFIED','DEMO_VERIFIED') OR
  (verification_source IS NOT NULL AND verification_reference IS NOT NULL AND verified_by IS NOT NULL AND verified_at IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS idx_institution_status_name ON institutions(status,name);