CREATE SEQUENCE IF NOT EXISTS dtef_application_ref_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS sponsorship_applications (
  id UUID PRIMARY KEY,
  reference VARCHAR(32) NOT NULL UNIQUE,
  owner_subject VARCHAR(120) NOT NULL,
  institution VARCHAR(180) NOT NULL,
  programme VARCHAR(180) NOT NULL,
  admission_document_id UUID NOT NULL,
  status VARCHAR(40) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_app_owner_created ON sponsorship_applications(owner_subject, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_app_status_created ON sponsorship_applications(status, created_at DESC);

CREATE TABLE IF NOT EXISTS application_drafts (
  owner_subject VARCHAR(120) PRIMARY KEY,
  payload_json TEXT NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS documents (
  id UUID PRIMARY KEY,
  owner_subject VARCHAR(120) NOT NULL,
  document_type VARCHAR(40) NOT NULL,
  storage_key VARCHAR(80) NOT NULL UNIQUE,
  original_name VARCHAR(180) NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  sha256 VARCHAR(64) NOT NULL,
  file_size BIGINT NOT NULL,
  status VARCHAR(40) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_documents_owner_created ON documents(owner_subject, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_documents_hash ON documents(sha256);

CREATE TABLE IF NOT EXISTS audit_events (
  id UUID PRIMARY KEY,
  actor VARCHAR(120) NOT NULL,
  action VARCHAR(80) NOT NULL,
  resource_type VARCHAR(80) NOT NULL,
  resource_id VARCHAR(120),
  occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_audit_resource ON audit_events(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_actor_time ON audit_events(actor, occurred_at DESC);
