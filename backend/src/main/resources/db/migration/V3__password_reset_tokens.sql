CREATE TABLE IF NOT EXISTS password_reset_tokens (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 token_hash VARCHAR(64) NOT NULL UNIQUE,
 expires_at DATETIME(6) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 attempts INT NOT NULL DEFAULT 0,
 consumed BOOLEAN NOT NULL DEFAULT FALSE,
 INDEX password_reset_user (user_id)
);
