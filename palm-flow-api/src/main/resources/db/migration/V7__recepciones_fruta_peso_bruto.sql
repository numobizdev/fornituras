-- Gross weight on recepciones_fruta.
-- Idempotent: safe to re-run if partially applied.

IF OBJECT_ID(N'dbo.recepciones_fruta', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.columns c
                             INNER JOIN sys.tables t ON c.object_id = t.object_id
                             INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                    WHERE s.name = N'dbo' AND t.name = N'recepciones_fruta' AND c.name = N'peso_bruto')
    EXEC sp_executesql N'ALTER TABLE dbo.recepciones_fruta ADD peso_bruto DECIMAL(12, 3) NULL';
