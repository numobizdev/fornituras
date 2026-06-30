-- Fruit quality and peduncle/spike condition on recepciones_fruta.
-- Idempotent: safe to re-run if partially applied.

IF OBJECT_ID(N'dbo.recepciones_fruta', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.columns c
                             INNER JOIN sys.tables t ON c.object_id = t.object_id
                             INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                    WHERE s.name = N'dbo' AND t.name = N'recepciones_fruta' AND c.name = N'calidad_fruta')
    EXEC sp_executesql N'ALTER TABLE dbo.recepciones_fruta ADD calidad_fruta NVARCHAR(30) NULL';

IF OBJECT_ID(N'dbo.recepciones_fruta', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.columns c
                             INNER JOIN sys.tables t ON c.object_id = t.object_id
                             INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                    WHERE s.name = N'dbo' AND t.name = N'recepciones_fruta' AND c.name = N'pedunculo_pinzote_largo')
    EXEC sp_executesql N'ALTER TABLE dbo.recepciones_fruta ADD pedunculo_pinzote_largo BIT NOT NULL CONSTRAINT DF_recepciones_fruta_pedunculo_pinzote_largo DEFAULT 0';
