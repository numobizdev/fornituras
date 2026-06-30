ALTER TABLE password_reset_tokens ADD code VARCHAR(6) NULL;
EXEC sp_executesql N'CREATE UNIQUE INDEX uq_password_reset_tokens_code ON password_reset_tokens(code) WHERE code IS NOT NULL;';
