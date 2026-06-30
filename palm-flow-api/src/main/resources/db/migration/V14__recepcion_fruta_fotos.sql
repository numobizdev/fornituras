-- Scale photos linked to fruit receptions (offline sync audit).
IF OBJECT_ID(N'dbo.recepcion_fruta_fotos', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.recepcion_fruta_fotos (
        uuid UNIQUEIDENTIFIER NOT NULL CONSTRAINT PK_recepcion_fruta_fotos PRIMARY KEY DEFAULT NEWID(),
        tenant_id NVARCHAR(50) NOT NULL,
        recepcion_fruta_uuid UNIQUEIDENTIFIER NOT NULL,
        tipo NVARCHAR(20) NOT NULL,
        content_type NVARCHAR(100) NULL,
        storage_path NVARCHAR(500) NOT NULL,
        created_at DATETIME2 NULL,
        updated_at DATETIME2 NULL,
        CONSTRAINT FK_recepcion_fruta_fotos_recepcion FOREIGN KEY (recepcion_fruta_uuid)
            REFERENCES dbo.recepciones_fruta (uuid) ON DELETE CASCADE
    );

    CREATE UNIQUE INDEX UX_recepcion_fruta_fotos_recepcion_tipo
        ON dbo.recepcion_fruta_fotos (recepcion_fruta_uuid, tipo);
END;
