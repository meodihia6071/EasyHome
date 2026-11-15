-- Legacy XAMPP schemas used short ENUM/VARCHAR columns. Widen before newer
-- statuses such as NEEDS_UPDATE or REJECTED are written by the application.
SET @easyhome_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='listings' AND column_name='status') = 1,
  'ALTER TABLE listings MODIFY status VARCHAR(32) NOT NULL', 'SELECT 1'
);
PREPARE easyhome_stmt FROM @easyhome_ddl; EXECUTE easyhome_stmt; DEALLOCATE PREPARE easyhome_stmt;
SET @easyhome_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='identity_verifications' AND column_name='status') = 1,
  'ALTER TABLE identity_verifications MODIFY status VARCHAR(32) NOT NULL', 'SELECT 1'
);
PREPARE easyhome_stmt FROM @easyhome_ddl; EXECUTE easyhome_stmt; DEALLOCATE PREPARE easyhome_stmt;
SET @easyhome_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='service_orders' AND column_name='status') = 1,
  'ALTER TABLE service_orders MODIFY status VARCHAR(32) NOT NULL', 'SELECT 1'
);
PREPARE easyhome_stmt FROM @easyhome_ddl; EXECUTE easyhome_stmt; DEALLOCATE PREPARE easyhome_stmt;
SET @easyhome_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='service_orders' AND column_name='plan') = 1,
  'ALTER TABLE service_orders MODIFY plan VARCHAR(32) NOT NULL', 'SELECT 1'
);
PREPARE easyhome_stmt FROM @easyhome_ddl; EXECUTE easyhome_stmt; DEALLOCATE PREPARE easyhome_stmt;
