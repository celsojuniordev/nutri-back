CREATE TABLE revoked_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    jti VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_revoked_tokens_jti UNIQUE (jti)
);
