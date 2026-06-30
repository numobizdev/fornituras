-- Catálogo geográfico de municipios. Dato de referencia compartido por almacenes (005) y, a
-- futuro, por elementos (003). Sin PII. El eje es unicidad de nombre normalizado.

CREATE TABLE municipio (
    id BIGINT IDENTITY(1,1) NOT NULL,
    nombre NVARCHAR(120) NOT NULL,
    nombre_normalizado NVARCHAR(120) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_municipio PRIMARY KEY (id),
    CONSTRAINT uk_municipio_nombre_norm UNIQUE (nombre_normalizado)
);

CREATE INDEX idx_municipio_active ON municipio (active);
