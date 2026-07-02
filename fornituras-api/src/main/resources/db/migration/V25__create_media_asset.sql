-- Metadatos de fotos almacenadas de forma segura (017, ADR 0016). La imagen NO vive aquí: se guarda
-- cifrada (AES-256-GCM) en el filesystem, fuera del repo, bajo `storage_key`. Esta tabla NO contiene
-- PII; la sensibilidad se marca con `is_pii` para gobernar RBAC + auditoría del acceso. El `id` es un
-- UUID opaco (no enumerable) que las entidades referencian desde su `foto_url` como `/api/v1/media/{id}`.
-- Numeración: V24 fue la corrección de la landing; se toma la siguiente libre.

CREATE TABLE media_asset (
    id UNIQUEIDENTIFIER NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    iv VARBINARY(16) NULL,
    is_pii BIT NOT NULL DEFAULT 0,
    uploaded_by BIGINT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_media_asset PRIMARY KEY (id),
    CONSTRAINT uq_media_asset_storage_key UNIQUE (storage_key)
);

-- Deduplicación/integridad por huella de la imagen saneada.
CREATE INDEX idx_media_asset_sha256 ON media_asset (sha256);
