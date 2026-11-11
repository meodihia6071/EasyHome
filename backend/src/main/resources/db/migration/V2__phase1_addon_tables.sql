-- EasyHome V2 Phase 1: additive tables compatible with MariaDB XAMPP and MySQL 8.
-- Hibernate update adds compatible columns to existing users/listings/service_orders in local dev.
-- Production requires a separate validated migration for ALL legacy table changes.
CREATE TABLE IF NOT EXISTS email_otp (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 code_hash VARCHAR(64) NOT NULL,
 nonce VARCHAR(40) NOT NULL,
 expires_at DATETIME(6) NOT NULL,
 resend_at DATETIME(6) NOT NULL,
 attempts INT NOT NULL DEFAULT 0,
 consumed BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE IF NOT EXISTS identity_verifications (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 status VARCHAR(255) NOT NULL,
 note VARCHAR(500),
 submitted_at DATETIME(6) NOT NULL,
 reviewed_at DATETIME(6),
 reviewer_id BIGINT
);
CREATE TABLE IF NOT EXISTS listing_media (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 listing_id BIGINT,
 identity_verification_id BIGINT,
 purpose VARCHAR(20) NOT NULL,
 storage_name VARCHAR(100) NOT NULL,
 content_type VARCHAR(50) NOT NULL,
 bytes BIGINT NOT NULL,
 created_at DATETIME(6) NOT NULL
);
CREATE TABLE IF NOT EXISTS listing_reviews (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 listing_id BIGINT NOT NULL,
 revision INT NOT NULL,
 reviewer_id BIGINT,
 decision VARCHAR(40) NOT NULL,
 note VARCHAR(500),
 created_at DATETIME(6) NOT NULL
);
CREATE TABLE IF NOT EXISTS wallet_accounts (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL UNIQUE,
 demo_balance BIGINT NOT NULL DEFAULT 0,
 demo_held BIGINT NOT NULL DEFAULT 0,
 version BIGINT
);
CREATE TABLE IF NOT EXISTS wallet_ledger (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 wallet_id BIGINT NOT NULL,
 delta BIGINT NOT NULL,
 action VARCHAR(36) NOT NULL,
 reference VARCHAR(110) NOT NULL UNIQUE,
 created_at DATETIME(6) NOT NULL
);
CREATE TABLE IF NOT EXISTS payment_transactions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 order_id BIGINT,
 payer_id BIGINT NOT NULL,
 reference VARCHAR(90) NOT NULL UNIQUE,
 amount BIGINT NOT NULL,
 method VARCHAR(30) NOT NULL,
 status VARCHAR(30) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 confirmed_at DATETIME(6)
);
CREATE TABLE IF NOT EXISTS withdrawal_requests (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 amount BIGINT NOT NULL,
 bank_name VARCHAR(80) NOT NULL,
 account_number VARCHAR(50) NOT NULL,
 account_holder VARCHAR(120) NOT NULL,
 status VARCHAR(30) NOT NULL,
 note VARCHAR(500),
 reviewer_id BIGINT,
 created_at DATETIME(6) NOT NULL,
 decided_at DATETIME(6)
);
CREATE TABLE IF NOT EXISTS account_lock_history (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 actor_id BIGINT NOT NULL,
 action VARCHAR(18) NOT NULL,
 reason VARCHAR(500) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 until_at DATETIME(6)
);
CREATE TABLE IF NOT EXISTS service_plans (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(32) NOT NULL UNIQUE,
 label VARCHAR(100) NOT NULL,
 price INT NOT NULL,
 days INT NOT NULL,
 priority INT NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 version BIGINT,
 updated_at DATETIME(6) NOT NULL
);
CREATE TABLE IF NOT EXISTS service_plan_history (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 plan_code VARCHAR(32) NOT NULL,
 actor_id BIGINT NOT NULL,
 old_price INT NOT NULL,
 new_price INT NOT NULL,
 old_days INT NOT NULL,
 new_days INT NOT NULL,
 at DATETIME(6) NOT NULL
);
