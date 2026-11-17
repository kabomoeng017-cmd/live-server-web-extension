CREATE TABLE IF NOT EXISTS application_status_history (
  id UUID PRIMARY KEY,
  application_reference VARCHAR(32) NOT NULL,
  from_status VARCHAR(40),
  to_status VARCHAR(40) NOT NULL,
  changed_by VARCHAR(120) NOT NULL,
  changed_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_app_history_ref_time ON application_status_history(application_reference, changed_at);
