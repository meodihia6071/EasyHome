SET @easyhome_listings_exists = (
  SELECT COUNT(*) FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'listings'
);
SET @easyhome_publisher_column_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'listings' AND column_name = 'publisher_type'
);
SET @easyhome_publisher_ddl = IF(
  @easyhome_listings_exists = 1 AND @easyhome_publisher_column_exists = 0,
  'ALTER TABLE listings ADD COLUMN publisher_type VARCHAR(20) NULL',
  'SELECT 1'
);
PREPARE easyhome_publisher_migration FROM @easyhome_publisher_ddl;
EXECUTE easyhome_publisher_migration;
DEALLOCATE PREPARE easyhome_publisher_migration;
