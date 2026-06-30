-- Align centro_acopio_precios_kg with JPA entity (no audit columns).
-- V9 created DATETIME2 audit columns that Hibernate ddl-auto cannot alter due to defaults.
-- Idempotent: safe to re-run if partially applied.

IF OBJECT_ID(N'dbo.centro_acopio_precios_kg', N'U') IS NOT NULL
BEGIN
    DECLARE @dropDefaults nvarchar(max);
    SELECT @dropDefaults = STUFF((
        SELECT N'; ALTER TABLE dbo.centro_acopio_precios_kg DROP CONSTRAINT ' + QUOTENAME(dc.name)
        FROM sys.default_constraints dc
        WHERE dc.parent_object_id = OBJECT_ID(N'dbo.centro_acopio_precios_kg')
          AND dc.parent_column_id IN (
              SELECT c.column_id
              FROM sys.columns c
              WHERE c.object_id = OBJECT_ID(N'dbo.centro_acopio_precios_kg')
                AND c.name IN (N'created_at', N'updated_at')
          )
        FOR XML PATH(N''), TYPE).value(N'.', N'nvarchar(max)'), 1, 1, N'');
    IF @dropDefaults IS NOT NULL AND LEN(@dropDefaults) > 0
        EXEC sp_executesql @dropDefaults;

    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.centro_acopio_precios_kg') AND name = N'created_at')
        EXEC sp_executesql N'ALTER TABLE dbo.centro_acopio_precios_kg DROP COLUMN created_at';

    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.centro_acopio_precios_kg') AND name = N'updated_at')
        EXEC sp_executesql N'ALTER TABLE dbo.centro_acopio_precios_kg DROP COLUMN updated_at';

    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.centro_acopio_precios_kg') AND name = N'created_by')
        EXEC sp_executesql N'ALTER TABLE dbo.centro_acopio_precios_kg DROP COLUMN created_by';

    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.centro_acopio_precios_kg') AND name = N'updated_by')
        EXEC sp_executesql N'ALTER TABLE dbo.centro_acopio_precios_kg DROP COLUMN updated_by';
END
