-- Contact and address fields on centros_acopio.
-- Idempotent: safe to re-run if partially applied.

IF OBJECT_ID(N'dbo.centros_acopio', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.columns c
                             INNER JOIN sys.tables t ON c.object_id = t.object_id
                             INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                    WHERE s.name = N'dbo' AND t.name = N'centros_acopio' AND c.name = N'direccion')
    EXEC sp_executesql N'ALTER TABLE dbo.centros_acopio ADD direccion NVARCHAR(300) NULL';

IF OBJECT_ID(N'dbo.centros_acopio', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.columns c
                             INNER JOIN sys.tables t ON c.object_id = t.object_id
                             INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                    WHERE s.name = N'dbo' AND t.name = N'centros_acopio' AND c.name = N'correo')
    EXEC sp_executesql N'ALTER TABLE dbo.centros_acopio ADD correo NVARCHAR(80) NULL';

IF OBJECT_ID(N'dbo.centros_acopio', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.columns c
                             INNER JOIN sys.tables t ON c.object_id = t.object_id
                             INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                    WHERE s.name = N'dbo' AND t.name = N'centros_acopio' AND c.name = N'rfc')
    EXEC sp_executesql N'ALTER TABLE dbo.centros_acopio ADD rfc NVARCHAR(13) NULL';

IF OBJECT_ID(N'dbo.centros_acopio', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.columns c
                             INNER JOIN sys.tables t ON c.object_id = t.object_id
                             INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                    WHERE s.name = N'dbo' AND t.name = N'centros_acopio' AND c.name = N'telefono')
    EXEC sp_executesql N'ALTER TABLE dbo.centros_acopio ADD telefono NVARCHAR(10) NULL';
