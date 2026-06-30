-- RSPO certification level on productores.
-- Idempotent: safe to re-run if partially applied.

IF OBJECT_ID(N'dbo.productores', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.columns c
                             INNER JOIN sys.tables t ON c.object_id = t.object_id
                             INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                    WHERE s.name = N'dbo' AND t.name = N'productores' AND c.name = N'nivel_rspo')
    EXEC sp_executesql N'ALTER TABLE dbo.productores ADD nivel_rspo NVARCHAR(30) NULL';
