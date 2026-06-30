-- Migrate productores PK from bigint id to uniqueidentifier uuid.
-- Aligns predios and recepciones_fruta FK columns (productor_uuid -> productores.uuid).
-- Idempotent: safe to re-run if partially applied.

-- 1) Drop FK constraints referencing dbo.productores
DECLARE @dropFks nvarchar(max);
SELECT @dropFks = STUFF((
                            SELECT N'; ALTER TABLE '
                                       + QUOTENAME(OBJECT_SCHEMA_NAME(fk.parent_object_id)) + N'.'
                                       + QUOTENAME(OBJECT_NAME(fk.parent_object_id))
                                       + N' DROP CONSTRAINT ' + QUOTENAME(fk.name)
                            FROM sys.foreign_keys fk
                            WHERE fk.referenced_object_id = OBJECT_ID(N'dbo.productores')
                            ORDER BY fk.name
                            FOR XML PATH(N''), TYPE).value(N'.', N'nvarchar(max)'), 1, 1, N'');
IF @dropFks IS NOT NULL AND LEN(@dropFks) > 0
    EXEC sp_executesql @dropFks;

-- 2) predios: drop legacy productor_id (bigint), ensure productor_uuid exists
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL
    AND EXISTS (SELECT 1
                FROM sys.columns c
                         INNER JOIN sys.tables t ON c.object_id = t.object_id
                         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = N'dbo' AND t.name = N'predios' AND c.name = N'productor_id')
    EXEC sp_executesql N'ALTER TABLE dbo.predios DROP COLUMN productor_id';

IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.columns c
                             INNER JOIN sys.tables t ON c.object_id = t.object_id
                             INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                    WHERE s.name = N'dbo' AND t.name = N'predios' AND c.name = N'productor_uuid')
    EXEC sp_executesql N'ALTER TABLE dbo.predios ADD productor_uuid UNIQUEIDENTIFIER NULL';

-- 3) recepciones_fruta: drop legacy productor_id, ensure productor_uuid exists
IF OBJECT_ID(N'dbo.recepciones_fruta', N'U') IS NOT NULL
    AND EXISTS (SELECT 1
                FROM sys.columns c
                         INNER JOIN sys.tables t ON c.object_id = t.object_id
                         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = N'dbo' AND t.name = N'recepciones_fruta' AND c.name = N'productor_id')
    EXEC sp_executesql N'ALTER TABLE dbo.recepciones_fruta DROP COLUMN productor_id';

IF OBJECT_ID(N'dbo.recepciones_fruta', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.columns c
                             INNER JOIN sys.tables t ON c.object_id = t.object_id
                             INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                    WHERE s.name = N'dbo' AND t.name = N'recepciones_fruta' AND c.name = N'productor_uuid')
    EXEC sp_executesql N'ALTER TABLE dbo.recepciones_fruta ADD productor_uuid UNIQUEIDENTIFIER NULL';

-- 4) productores: add uuid column and backfill
IF OBJECT_ID(N'dbo.productores', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.columns c
                             INNER JOIN sys.tables t ON c.object_id = t.object_id
                             INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                    WHERE s.name = N'dbo' AND t.name = N'productores' AND c.name = N'uuid')
    EXEC sp_executesql N'ALTER TABLE dbo.productores ADD uuid UNIQUEIDENTIFIER NULL';

IF OBJECT_ID(N'dbo.productores', N'U') IS NOT NULL
    AND EXISTS (SELECT 1
                FROM sys.columns c
                         INNER JOIN sys.tables t ON c.object_id = t.object_id
                         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = N'dbo' AND t.name = N'productores' AND c.name = N'uuid')
    EXEC sp_executesql N'UPDATE dbo.productores SET uuid = NEWID() WHERE uuid IS NULL';

IF OBJECT_ID(N'dbo.productores', N'U') IS NOT NULL
    AND EXISTS (SELECT 1
                FROM sys.columns c
                         INNER JOIN sys.tables t ON c.object_id = t.object_id
                         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = N'dbo' AND t.name = N'productores' AND c.name = N'uuid' AND c.is_nullable = 1)
    EXEC sp_executesql N'ALTER TABLE dbo.productores ALTER COLUMN uuid UNIQUEIDENTIFIER NOT NULL';

-- Drop legacy PK on id when uuid is ready
IF OBJECT_ID(N'dbo.productores', N'U') IS NOT NULL
    AND EXISTS (SELECT 1
                FROM sys.columns c
                         INNER JOIN sys.tables t ON c.object_id = t.object_id
                         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = N'dbo' AND t.name = N'productores' AND c.name = N'id')
    AND EXISTS (SELECT 1
                FROM sys.key_constraints kc
                WHERE kc.parent_object_id = OBJECT_ID(N'dbo.productores') AND kc.type = N'PK')
    EXEC sp_executesql N'
        DECLARE @pkName nvarchar(128);
        SELECT @pkName = kc.name
        FROM sys.key_constraints kc
        WHERE kc.parent_object_id = OBJECT_ID(N''dbo.productores'') AND kc.type = N''PK'';
        IF @pkName IS NOT NULL
        BEGIN
            DECLARE @dropPk nvarchar(300) = N''ALTER TABLE dbo.productores DROP CONSTRAINT '' + QUOTENAME(@pkName);
            EXEC sp_executesql @dropPk;
        END';

IF OBJECT_ID(N'dbo.productores', N'U') IS NOT NULL
    AND EXISTS (SELECT 1
                FROM sys.columns c
                         INNER JOIN sys.tables t ON c.object_id = t.object_id
                         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = N'dbo' AND t.name = N'productores' AND c.name = N'id')
    EXEC sp_executesql N'ALTER TABLE dbo.productores DROP COLUMN id';

IF OBJECT_ID(N'dbo.productores', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.key_constraints kc
                    WHERE kc.parent_object_id = OBJECT_ID(N'dbo.productores') AND kc.type = N'PK')
    EXEC sp_executesql N'ALTER TABLE dbo.productores ADD CONSTRAINT PK_productores PRIMARY KEY (uuid)';

-- 5) Re-create FK constraints to productores(uuid)
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.foreign_keys fk
                    WHERE fk.parent_object_id = OBJECT_ID(N'dbo.predios')
                      AND fk.referenced_object_id = OBJECT_ID(N'dbo.productores'))
    EXEC sp_executesql N'ALTER TABLE dbo.predios ADD CONSTRAINT FK_predios_productor FOREIGN KEY (productor_uuid) REFERENCES dbo.productores (uuid)';

IF OBJECT_ID(N'dbo.recepciones_fruta', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.foreign_keys fk
                    WHERE fk.parent_object_id = OBJECT_ID(N'dbo.recepciones_fruta')
                      AND fk.referenced_object_id = OBJECT_ID(N'dbo.productores'))
    EXEC sp_executesql N'ALTER TABLE dbo.recepciones_fruta ADD CONSTRAINT FK_recepciones_fruta_productor FOREIGN KEY (productor_uuid) REFERENCES dbo.productores (uuid)';
