-- Extend the document lifecycle to record a successful malware verdict.
-- Keep legacy values valid so existing rows remain readable during rolling upgrades.
ALTER TABLE documents
  DROP CONSTRAINT ck_document_status;

ALTER TABLE documents
  ADD CONSTRAINT ck_document_status
  CHECK (status IN ('DEMO_ACCEPTED','QUARANTINED','CLEAN','PENDING_SCAN','SCANNED_CLEAN'));
