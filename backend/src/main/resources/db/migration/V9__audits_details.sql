-- Include audit_logs for new databases that do not have the legacy Hibernate-created table yet.
CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  actor_id BIGINT NOT NULL,
  action VARCHAR(70) NOT NULL,
  target VARCHAR(80) NOT NULL,
  created_at DATETIME(6) NOT NULL
);

ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS details VARCHAR(500) NULL;
