SET @easyhome_users_exists = (
  SELECT COUNT(*) FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'users'
);
SET @easyhome_auth_column_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'auth_version'
);
SET @easyhome_auth_ddl = IF(
  @easyhome_users_exists = 1 AND @easyhome_auth_column_exists = 0,
  'ALTER TABLE users ADD COLUMN auth_version INT NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE easyhome_auth_migration FROM @easyhome_auth_ddl;
EXECUTE easyhome_auth_migration;
DEALLOCATE PREPARE easyhome_auth_migration;
