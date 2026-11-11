-- Fresh dev database: record schema version before Hibernate creates legacy base tables.
-- Existing EasyHome databases are baselined at V1 and upgraded with V2.
CREATE TABLE IF NOT EXISTS easyhome_schema_marker (
  id INT PRIMARY KEY,
  description VARCHAR(150) NOT NULL
);
INSERT INTO easyhome_schema_marker(id, description) VALUES (1,'EasyHome schema migration enabled');
