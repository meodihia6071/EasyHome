CREATE TABLE IF NOT EXISTS staff_login_challenges (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  challenge_hash VARCHAR(64) NOT NULL UNIQUE,
  code_hash VARCHAR(64) NOT NULL,
  nonce VARCHAR(40) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  resend_at DATETIME(6) NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  consumed BOOLEAN NOT NULL DEFAULT FALSE,
  INDEX staff_login_user_created (user_id, created_at),
  CONSTRAINT fk_staff_login_user FOREIGN KEY (user_id) REFERENCES users(id)
);
