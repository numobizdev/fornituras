-- Audit columns for recepciones_fruta and centro_acopio_precios_kg.
-- Columns are nullable (no defaults) so Hibernate ddl-auto can manage them safely.
-- Idempotent: safe to re-run if partially applied.

IF OBJECT_ID(N'dbo.recepciones_fruta', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.recepciones_fruta') AND name = N'created_at')
    EXEC sp_executesql N'ALTER TABLE dbo.recepciones_fruta ADD created_at DATETIME2 NULL';

IF OBJECT_ID(N'dbo.recepciones_fruta', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.recepciones_fruta') AND name = N'updated_at')
    EXEC sp_executesql N'ALTER TABLE dbo.recepciones_fruta ADD updated_at DATETIME2 NULL';

IF OBJECT_ID(N'dbo.centro_acopio_precios_kg', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.centro_acopio_precios_kg') AND name = N'created_at')
    EXEC sp_executesql N'ALTER TABLE dbo.centro_acopio_precios_kg ADD created_at DATETIME2 NULL';

IF OBJECT_ID(N'dbo.centro_acopio_precios_kg', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.centro_acopio_precios_kg') AND name = N'updated_at')
    EXEC sp_executesql N'ALTER TABLE dbo.centro_acopio_precios_kg ADD updated_at DATETIME2 NULL';

IF OBJECT_ID(N'dbo.centro_acopio_precios_kg', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.centro_acopio_precios_kg') AND name = N'created_by_user_id')
    EXEC sp_executesql N'ALTER TABLE dbo.centro_acopio_precios_kg ADD created_by_user_id BIGINT NULL';

IF OBJECT_ID(N'dbo.centro_acopio_precios_kg', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.centro_acopio_precios_kg') AND name = N'updated_by_user_id')
    EXEC sp_executesql N'ALTER TABLE dbo.centro_acopio_precios_kg ADD updated_by_user_id BIGINT NULL';

IF OBJECT_ID(N'dbo.centro_acopio_precios_kg', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_capk_created_by_user')
    EXEC sp_executesql N'ALTER TABLE dbo.centro_acopio_precios_kg ADD CONSTRAINT fk_capk_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES dbo.users (id)';

IF OBJECT_ID(N'dbo.centro_acopio_precios_kg', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_capk_updated_by_user')
    EXEC sp_executesql N'ALTER TABLE dbo.centro_acopio_precios_kg ADD CONSTRAINT fk_capk_updated_by_user FOREIGN KEY (updated_by_user_id) REFERENCES dbo.users (id)';
