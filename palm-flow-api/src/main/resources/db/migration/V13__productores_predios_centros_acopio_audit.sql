-- Audit columns for productores, predios, and centros_acopio.
-- Columns are nullable (no defaults) so Hibernate ddl-auto can manage them safely.
-- Idempotent: safe to re-run if partially applied.

IF OBJECT_ID(N'dbo.productores', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.productores') AND name = N'created_at')
    EXEC sp_executesql N'ALTER TABLE dbo.productores ADD created_at DATETIME2 NULL';

IF OBJECT_ID(N'dbo.productores', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.productores') AND name = N'updated_at')
    EXEC sp_executesql N'ALTER TABLE dbo.productores ADD updated_at DATETIME2 NULL';

IF OBJECT_ID(N'dbo.productores', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.productores') AND name = N'created_by_user_id')
    EXEC sp_executesql N'ALTER TABLE dbo.productores ADD created_by_user_id BIGINT NULL';

IF OBJECT_ID(N'dbo.productores', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.productores') AND name = N'updated_by_user_id')
    EXEC sp_executesql N'ALTER TABLE dbo.productores ADD updated_by_user_id BIGINT NULL';

IF OBJECT_ID(N'dbo.productores', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_productores_created_by_user')
    EXEC sp_executesql N'ALTER TABLE dbo.productores ADD CONSTRAINT fk_productores_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES dbo.users (id)';

IF OBJECT_ID(N'dbo.productores', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_productores_updated_by_user')
    EXEC sp_executesql N'ALTER TABLE dbo.productores ADD CONSTRAINT fk_productores_updated_by_user FOREIGN KEY (updated_by_user_id) REFERENCES dbo.users (id)';

IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'created_at')
    EXEC sp_executesql N'ALTER TABLE dbo.predios ADD created_at DATETIME2 NULL';

IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'updated_at')
    EXEC sp_executesql N'ALTER TABLE dbo.predios ADD updated_at DATETIME2 NULL';

IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'created_by_user_id')
    EXEC sp_executesql N'ALTER TABLE dbo.predios ADD created_by_user_id BIGINT NULL';

IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.predios') AND name = N'updated_by_user_id')
    EXEC sp_executesql N'ALTER TABLE dbo.predios ADD updated_by_user_id BIGINT NULL';

IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_predios_created_by_user')
    EXEC sp_executesql N'ALTER TABLE dbo.predios ADD CONSTRAINT fk_predios_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES dbo.users (id)';

IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_predios_updated_by_user')
    EXEC sp_executesql N'ALTER TABLE dbo.predios ADD CONSTRAINT fk_predios_updated_by_user FOREIGN KEY (updated_by_user_id) REFERENCES dbo.users (id)';

IF OBJECT_ID(N'dbo.centros_acopio', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.centros_acopio') AND name = N'created_at')
    EXEC sp_executesql N'ALTER TABLE dbo.centros_acopio ADD created_at DATETIME2 NULL';

IF OBJECT_ID(N'dbo.centros_acopio', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.centros_acopio') AND name = N'updated_at')
    EXEC sp_executesql N'ALTER TABLE dbo.centros_acopio ADD updated_at DATETIME2 NULL';

IF OBJECT_ID(N'dbo.centros_acopio', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.centros_acopio') AND name = N'created_by_user_id')
    EXEC sp_executesql N'ALTER TABLE dbo.centros_acopio ADD created_by_user_id BIGINT NULL';

IF OBJECT_ID(N'dbo.centros_acopio', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.centros_acopio') AND name = N'updated_by_user_id')
    EXEC sp_executesql N'ALTER TABLE dbo.centros_acopio ADD updated_by_user_id BIGINT NULL';

IF OBJECT_ID(N'dbo.centros_acopio', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_centros_acopio_created_by_user')
    EXEC sp_executesql N'ALTER TABLE dbo.centros_acopio ADD CONSTRAINT fk_centros_acopio_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES dbo.users (id)';

IF OBJECT_ID(N'dbo.centros_acopio', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_centros_acopio_updated_by_user')
    EXEC sp_executesql N'ALTER TABLE dbo.centros_acopio ADD CONSTRAINT fk_centros_acopio_updated_by_user FOREIGN KEY (updated_by_user_id) REFERENCES dbo.users (id)';
