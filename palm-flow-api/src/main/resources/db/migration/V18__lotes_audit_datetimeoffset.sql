-- Align lotes audit timestamps with Hibernate Instant mapping (datetimeoffset).
-- Idempotent: safe to re-run if partially applied.

IF OBJECT_ID(N'dbo.lotes', N'U') IS NOT NULL
    AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.lotes') AND name = N'created_at')
    AND EXISTS (
        SELECT 1
        FROM sys.columns c
        INNER JOIN sys.types t ON c.user_type_id = t.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.lotes') AND c.name = N'created_at' AND t.name = N'datetime2'
    )
    EXEC sp_executesql N'ALTER TABLE dbo.lotes ALTER COLUMN created_at DATETIMEOFFSET(7) NULL';

IF OBJECT_ID(N'dbo.lotes', N'U') IS NOT NULL
    AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.lotes') AND name = N'updated_at')
    AND EXISTS (
        SELECT 1
        FROM sys.columns c
        INNER JOIN sys.types t ON c.user_type_id = t.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.lotes') AND c.name = N'updated_at' AND t.name = N'datetime2'
    )
    EXEC sp_executesql N'ALTER TABLE dbo.lotes ALTER COLUMN updated_at DATETIMEOFFSET(7) NULL';
