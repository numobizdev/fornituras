CREATE TABLE verification_tokens (
    id BIGINT IDENTITY(1,1) NOT NULL,
    code NVARCHAR(6) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at DATETIME2 NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_verification_tokens PRIMARY KEY (id),
    CONSTRAINT uk_verification_tokens_code UNIQUE (code),
    CONSTRAINT uk_verification_tokens_user UNIQUE (user_id),
    CONSTRAINT fk_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);
