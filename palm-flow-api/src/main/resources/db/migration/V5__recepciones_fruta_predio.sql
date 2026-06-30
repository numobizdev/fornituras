-- Link recepciones_fruta to the predio (fruit source).
-- Idempotent: safe to re-run if partially applied.

IF OBJECT_ID(N'dbo.recepciones_fruta', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.columns c
                             INNER JOIN sys.tables t ON c.object_id = t.object_id
                             INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                    WHERE s.name = N'dbo' AND t.name = N'recepciones_fruta' AND c.name = N'predio_uuid')
    EXEC sp_executesql N'ALTER TABLE dbo.recepciones_fruta ADD predio_uuid UNIQUEIDENTIFIER NULL';

IF OBJECT_ID(N'dbo.recepciones_fruta', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.foreign_keys fk
                    WHERE fk.parent_object_id = OBJECT_ID(N'dbo.recepciones_fruta')
                      AND fk.name = N'FK_recepciones_fruta_predio')
    EXEC sp_executesql N'ALTER TABLE dbo.recepciones_fruta ADD CONSTRAINT FK_recepciones_fruta_predio FOREIGN KEY (predio_uuid) REFERENCES dbo.predios (uuid)';
