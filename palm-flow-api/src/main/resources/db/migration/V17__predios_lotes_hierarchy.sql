-- Productor → Predio → Lote hierarchy: move geo/compliance to lotes, fiscal_id to predios.
-- Idempotent: safe to re-run if partially applied.

-- 1. Create lotes table
IF OBJECT_ID(N'dbo.lotes', N'U') IS NULL
    EXEC sp_executesql N'
        CREATE TABLE dbo.lotes (
            uuid UNIQUEIDENTIFIER NOT NULL PRIMARY KEY DEFAULT NEWID(),
            tenant_id NVARCHAR(50) NOT NULL,
            nombre NVARCHAR(200) NOT NULL,
            predio_uuid UNIQUEIDENTIFIER NOT NULL,
            anio_plantacion INT NULL,
            etapa NVARCHAR(20) NULL,
            id_gis NVARCHAR(50) NULL,
            latitud DECIMAL(10, 6) NULL,
            longitud DECIMAL(10, 6) NULL,
            x DECIMAL(15, 6) NULL,
            y DECIMAL(15, 6) NULL,
            hectareas DECIMAL(12, 6) NULL,
            ramsar BIT NOT NULL DEFAULT 0,
            anp BIT NOT NULL DEFAULT 0,
            cambio BIT NOT NULL DEFAULT 0,
            eudr INT NOT NULL DEFAULT 0,
            riesgo INT NOT NULL DEFAULT 0,
            wkt NVARCHAR(MAX) NULL,
            created_at DATETIME2 NULL,
            updated_at DATETIME2 NULL,
            created_by_user_id BIGINT NULL,
            updated_by_user_id BIGINT NULL,
            CONSTRAINT FK_lotes_predio FOREIGN KEY (predio_uuid) REFERENCES dbo.predios (uuid),
            CONSTRAINT fk_lotes_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES dbo.users (id),
            CONSTRAINT fk_lotes_updated_by_user FOREIGN KEY (updated_by_user_id) REFERENCES dbo.users (id)
        )';

-- 2. Add fiscal_id to predios
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'fiscal_id')
    EXEC sp_executesql N'ALTER TABLE dbo.predios ADD fiscal_id NVARCHAR(50) NULL';

-- 3. Migrate productores.fiscal_id → predios.fiscal_id (one-time, before drop)
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL
    AND OBJECT_ID(N'dbo.productores', N'U') IS NOT NULL
    AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.productores') AND name = N'fiscal_id')
    AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'fiscal_id')
    EXEC sp_executesql N'
        UPDATE pr
        SET pr.fiscal_id = p.fiscal_id
        FROM dbo.predios pr
        INNER JOIN dbo.productores p ON pr.productor_uuid = p.uuid
        WHERE pr.fiscal_id IS NULL AND p.fiscal_id IS NOT NULL';

-- 4. Backfill lotes from existing predio geo columns (one lote per predio)
IF OBJECT_ID(N'dbo.lotes', N'U') IS NOT NULL
    AND OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL
    AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'id_gis')
    AND NOT EXISTS (SELECT 1 FROM dbo.lotes)
    EXEC sp_executesql N'
        INSERT INTO dbo.lotes (
            uuid, tenant_id, nombre, predio_uuid, anio_plantacion, etapa, id_gis,
            latitud, longitud, x, y, hectareas, ramsar, anp, cambio, eudr, riesgo, wkt,
            created_at, updated_at, created_by_user_id, updated_by_user_id
        )
        SELECT
            NEWID(), pr.tenant_id, pr.nombre, pr.uuid, pr.anio_plantacion, pr.etapa, pr.id_gis,
            pr.latitud, pr.longitud, pr.x, pr.y, pr.hectareas, pr.ramsar, pr.anp, pr.cambio,
            ISNULL(pr.eudr, 0), ISNULL(pr.riesgo, 0), pr.wkt,
            pr.created_at, pr.updated_at, pr.created_by_user_id, pr.updated_by_user_id
        FROM dbo.predios pr';

-- 5. Make geo FKs on predios nullable
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL
    AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'comunidad_id')
    EXEC sp_executesql N'ALTER TABLE dbo.predios ALTER COLUMN comunidad_id BIGINT NULL';

IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL
    AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'municipio_id')
    EXEC sp_executesql N'ALTER TABLE dbo.predios ALTER COLUMN municipio_id BIGINT NULL';

IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL
    AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'estado_id')
    EXEC sp_executesql N'ALTER TABLE dbo.predios ALTER COLUMN estado_id BIGINT NULL';

-- 5b. Drop DEFAULT/CHECK constraints on predios columns before DROP COLUMN (SQL Server requirement)
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL
BEGIN
    DECLARE @dropConstraintSql NVARCHAR(MAX);
    DECLARE @columnsToDrop TABLE (name SYSNAME PRIMARY KEY);
    INSERT INTO @columnsToDrop (name) VALUES
        (N'anio_plantacion'), (N'etapa'), (N'id_gis'), (N'latitud'), (N'longitud'),
        (N'x'), (N'y'), (N'hectareas'), (N'ramsar'), (N'anp'), (N'cambio'), (N'eudr'), (N'riesgo'), (N'wkt');

    DECLARE constraint_cursor CURSOR LOCAL FAST_FORWARD FOR
        SELECT N'ALTER TABLE dbo.predios DROP CONSTRAINT ' + QUOTENAME(dc.name)
        FROM sys.default_constraints dc
        INNER JOIN sys.columns c
            ON dc.parent_object_id = c.object_id AND dc.parent_column_id = c.column_id
        INNER JOIN @columnsToDrop cols ON cols.name = c.name
        WHERE dc.parent_object_id = OBJECT_ID(N'dbo.predios')
        UNION
        SELECT N'ALTER TABLE dbo.predios DROP CONSTRAINT ' + QUOTENAME(cc.name)
        FROM sys.check_constraints cc
        INNER JOIN sys.columns c
            ON cc.parent_object_id = c.object_id AND cc.parent_column_id = c.column_id
        INNER JOIN @columnsToDrop cols ON cols.name = c.name
        WHERE cc.parent_object_id = OBJECT_ID(N'dbo.predios');

    OPEN constraint_cursor;
    FETCH NEXT FROM constraint_cursor INTO @dropConstraintSql;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC sp_executesql @dropConstraintSql;
        FETCH NEXT FROM constraint_cursor INTO @dropConstraintSql;
    END
    CLOSE constraint_cursor;
    DEALLOCATE constraint_cursor;
END

-- 6. Drop lote-level columns from predios
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'anio_plantacion')
    EXEC sp_executesql N'ALTER TABLE dbo.predios DROP COLUMN anio_plantacion';
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'etapa')
    EXEC sp_executesql N'ALTER TABLE dbo.predios DROP COLUMN etapa';
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'id_gis')
    EXEC sp_executesql N'ALTER TABLE dbo.predios DROP COLUMN id_gis';
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'latitud')
    EXEC sp_executesql N'ALTER TABLE dbo.predios DROP COLUMN latitud';
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'longitud')
    EXEC sp_executesql N'ALTER TABLE dbo.predios DROP COLUMN longitud';
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'x')
    EXEC sp_executesql N'ALTER TABLE dbo.predios DROP COLUMN x';
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'y')
    EXEC sp_executesql N'ALTER TABLE dbo.predios DROP COLUMN y';
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'hectareas')
    EXEC sp_executesql N'ALTER TABLE dbo.predios DROP COLUMN hectareas';
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'ramsar')
    EXEC sp_executesql N'ALTER TABLE dbo.predios DROP COLUMN ramsar';
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'anp')
    EXEC sp_executesql N'ALTER TABLE dbo.predios DROP COLUMN anp';
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'cambio')
    EXEC sp_executesql N'ALTER TABLE dbo.predios DROP COLUMN cambio';
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'eudr')
    EXEC sp_executesql N'ALTER TABLE dbo.predios DROP COLUMN eudr';
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'riesgo')
    EXEC sp_executesql N'ALTER TABLE dbo.predios DROP COLUMN riesgo';
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'wkt')
    EXEC sp_executesql N'ALTER TABLE dbo.predios DROP COLUMN wkt';

-- 7. Drop fiscal_id from productores
IF OBJECT_ID(N'dbo.productores', N'U') IS NOT NULL
    AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.productores') AND name = N'fiscal_id')
BEGIN
    DECLARE @dropProductorConstraintSql NVARCHAR(MAX);
    DECLARE productor_constraint_cursor CURSOR LOCAL FAST_FORWARD FOR
        SELECT N'ALTER TABLE dbo.productores DROP CONSTRAINT ' + QUOTENAME(dc.name)
        FROM sys.default_constraints dc
        INNER JOIN sys.columns c
            ON dc.parent_object_id = c.object_id AND dc.parent_column_id = c.column_id
        WHERE dc.parent_object_id = OBJECT_ID(N'dbo.productores') AND c.name = N'fiscal_id'
        UNION
        SELECT N'ALTER TABLE dbo.productores DROP CONSTRAINT ' + QUOTENAME(cc.name)
        FROM sys.check_constraints cc
        INNER JOIN sys.columns c
            ON cc.parent_object_id = c.object_id AND cc.parent_column_id = c.column_id
        WHERE cc.parent_object_id = OBJECT_ID(N'dbo.productores') AND c.name = N'fiscal_id';

    OPEN productor_constraint_cursor;
    FETCH NEXT FROM productor_constraint_cursor INTO @dropProductorConstraintSql;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC sp_executesql @dropProductorConstraintSql;
        FETCH NEXT FROM productor_constraint_cursor INTO @dropProductorConstraintSql;
    END
    CLOSE productor_constraint_cursor;
    DEALLOCATE productor_constraint_cursor;

    EXEC sp_executesql N'ALTER TABLE dbo.productores DROP COLUMN fiscal_id';
END

-- 8. Add lote_uuid to recepciones_fruta
IF OBJECT_ID(N'dbo.recepciones_fruta', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.recepciones_fruta') AND name = N'lote_uuid')
    EXEC sp_executesql N'ALTER TABLE dbo.recepciones_fruta ADD lote_uuid UNIQUEIDENTIFIER NULL';

IF OBJECT_ID(N'dbo.recepciones_fruta', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_recepciones_fruta_lote')
    EXEC sp_executesql N'ALTER TABLE dbo.recepciones_fruta ADD CONSTRAINT FK_recepciones_fruta_lote FOREIGN KEY (lote_uuid) REFERENCES dbo.lotes (uuid)';
