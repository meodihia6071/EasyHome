-- Start the reconciled wallet at zero. Legacy mock balances remain archived
-- in their original columns and are never spendable or withdrawable.
SET @easyhome_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='wallet_accounts') = 1
  AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wallet_accounts' AND column_name='available_balance') = 0,
  'ALTER TABLE wallet_accounts ADD COLUMN available_balance BIGINT NOT NULL DEFAULT 0, ADD COLUMN held_balance BIGINT NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE easyhome_stmt FROM @easyhome_ddl; EXECUTE easyhome_stmt; DEALLOCATE PREPARE easyhome_stmt;
