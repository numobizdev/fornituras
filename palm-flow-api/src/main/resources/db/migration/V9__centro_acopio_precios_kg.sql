-- Historical price per kg per centro de acopio + payment snapshot on recepciones_fruta.
-- Idempotent: safe to re-run if partially applied.

IF OBJECT_ID(N'dbo.centro_acopio_precios_kg', N'U') IS NULL
    CREATE TABLE dbo.centro_acopio_precios_kg (
        uuid                UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
        tenant_id           VARCHAR(50) NOT NULL,
        centro_acopio_uuid  UNIQUEIDENTIFIER NOT NULL,
        precio_kg           DECIMAL(10, 2) NOT NULL,
        fecha_vigencia      DATE NOT NULL,
        created_at          DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at          DATETIME2 NULL,
        created_by          NVARCHAR(255) NULL,
        updated_by          NVARCHAR(255) NULL,
        CONSTRAINT pk_centro_acopio_precios_kg PRIMARY KEY (uuid),
        CONSTRAINT fk_capk_centro FOREIGN KEY (centro_acopio_uuid) REFERENCES dbo.centros_acopio (uuid),
        CONSTRAINT uq_capk_centro_fecha UNIQUE (centro_acopio_uuid, fecha_vigencia)
    );

IF OBJECT_ID(N'dbo.centro_acopio_precios_kg', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_capk_centro_fecha' AND object_id = OBJECT_ID(N'dbo.centro_acopio_precios_kg'))
    CREATE INDEX ix_capk_centro_fecha ON dbo.centro_acopio_precios_kg (centro_acopio_uuid, fecha_vigencia DESC);

IF OBJECT_ID(N'dbo.recepciones_fruta', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.columns c
                             INNER JOIN sys.tables t ON c.object_id = t.object_id
                             INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                    WHERE s.name = N'dbo' AND t.name = N'recepciones_fruta' AND c.name = N'precio_kg')
    EXEC sp_executesql N'ALTER TABLE dbo.recepciones_fruta ADD precio_kg DECIMAL(10, 2) NULL';

IF OBJECT_ID(N'dbo.recepciones_fruta', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.columns c
                             INNER JOIN sys.tables t ON c.object_id = t.object_id
                             INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                    WHERE s.name = N'dbo' AND t.name = N'recepciones_fruta' AND c.name = N'monto_a_pagar')
    EXEC sp_executesql N'ALTER TABLE dbo.recepciones_fruta ADD monto_a_pagar DECIMAL(14, 2) NULL';
