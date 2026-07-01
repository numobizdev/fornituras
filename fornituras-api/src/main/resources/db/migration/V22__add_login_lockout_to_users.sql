-- Anti-fuerza-bruta (feature 013, FR-005): contador de intentos fallidos y bloqueo temporal por cuenta.
-- El bloqueo se persiste (no solo en memoria) para que sobreviva a reinicios y a despliegues multi-instancia.
ALTER TABLE users ADD failed_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD locked_until DATETIME2 NULL;
