ALTER TABLE sponsorship_applications
  ADD CONSTRAINT fk_application_admission_document
  FOREIGN KEY (admission_document_id) REFERENCES documents(id);

ALTER TABLE application_status_history
  ADD CONSTRAINT fk_application_history_reference
  FOREIGN KEY (application_reference) REFERENCES sponsorship_applications(reference) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS application_documents (
  id UUID PRIMARY KEY,
  application_reference VARCHAR(32) NOT NULL REFERENCES sponsorship_applications(reference) ON DELETE CASCADE,
  document_id UUID NOT NULL REFERENCES documents(id),
  document_type VARCHAR(40) NOT NULL,
  linked_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_application_document_link UNIQUE (application_reference, document_id)
);
CREATE INDEX IF NOT EXISTS idx_application_documents_document ON application_documents(document_id);

ALTER TABLE sponsorship_applications
  ADD CONSTRAINT ck_sponsorship_application_status
  CHECK (status IN ('APPLIED','DOCUMENTS_PENDING','DOCUMENTS_VERIFIED','INSTITUTION_CONFIRMED','FNBB_PENDING','COMPLETED','REJECTED'));
ALTER TABLE documents
  ADD CONSTRAINT ck_document_type
  CHECK (document_type IN ('ADMISSION_LETTER','OMANG_COPY','BANK_CONFIRMATION','PASSPORT_PHOTO','OTHER'));
ALTER TABLE documents
  ADD CONSTRAINT ck_document_status
  CHECK (status IN ('DEMO_ACCEPTED','QUARANTINED','CLEAN','PENDING_SCAN'));
ALTER TABLE documents
  ADD CONSTRAINT ck_document_size
  CHECK (file_size BETWEEN 1 AND 10485760);
