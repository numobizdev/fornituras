-- Multi-select fruit quality: store comma-separated values in calidad_fruta; drop pedunculo_pinzote_largo.
-- Idempotent: safe to re-run if partially applied.

IF OBJECT_ID(N'dbo.recepciones_fruta', N'U') IS NOT NULL
    AND EXISTS (SELECT 1
                FROM sys.columns c
                         INNER JOIN sys.tables t ON c.object_id = t.object_id
                         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = N'dbo' AND t.name = N'recepciones_fruta' AND c.name = N'calidad_fruta')
    EXEC sp_executesql N'ALTER TABLE dbo.recepciones_fruta ALTER COLUMN calidad_fruta NVARCHAR(255) NULL';

IF OBJECT_ID(N'dbo.recepciones_fruta', N'U') IS NOT NULL
    AND EXISTS (SELECT 1
                FROM sys.columns c
                         INNER JOIN sys.tables t ON c.object_id = t.object_id
                         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = N'dbo' AND t.name = N'recepciones_fruta' AND c.name = N'pedunculo_pinzote_largo')
    EXEC sp_executesql N'
        UPDATE dbo.recepciones_fruta
        SET calidad_fruta = CASE
            WHEN pedunculo_pinzote_largo = 1 AND calidad_fruta IS NOT NULL AND calidad_fruta <> N''''
                THEN calidad_fruta + N'',PEDUNCULO_PINZOTE_LARGO''
            WHEN pedunculo_pinzote_largo = 1 AND (calidad_fruta IS NULL OR calidad_fruta = N'''')
                THEN N''PEDUNCULO_PINZOTE_LARGO''
            ELSE calidad_fruta
        END
        WHERE pedunculo_pinzote_largo = 1';

IF OBJECT_ID(N'dbo.recepciones_fruta', N'U') IS NOT NULL
    AND EXISTS (SELECT 1
                FROM sys.columns c
                         INNER JOIN sys.tables t ON c.object_id = t.object_id
                         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = N'dbo' AND t.name = N'recepciones_fruta' AND c.name = N'pedunculo_pinzote_largo')
    EXEC sp_executesql N'
        DECLARE @df SYSNAME;
        SELECT @df = dc.name
        FROM sys.default_constraints dc
                 INNER JOIN sys.columns c ON dc.parent_object_id = c.object_id AND dc.parent_column_id = c.column_id
                 INNER JOIN sys.tables t ON c.object_id = t.object_id
                 INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
        WHERE s.name = N''dbo'' AND t.name = N''recepciones_fruta'' AND c.name = N''pedunculo_pinzote_largo'';

        IF @df IS NOT NULL
            EXEC(''ALTER TABLE dbo.recepciones_fruta DROP CONSTRAINT '' + @df);

        ALTER TABLE dbo.recepciones_fruta DROP COLUMN pedunculo_pinzote_largo;
    ';
