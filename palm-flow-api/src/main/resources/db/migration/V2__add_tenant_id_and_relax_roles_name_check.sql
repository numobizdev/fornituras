-- SQL Server: add nullable tenant_id, backfill, then NOT NULL (Hibernate cannot do this safely on populated tables).
-- Legacy CHECK on roles.name may not include current RoleName enum values.
--
-- 1) Flyway may split this file on ';' at the script level — each EXEC below is one statement, so order is preserved.
-- 2) Do NOT put ALTER ADD + UPDATE tenant_id in one sp_executesql string: SQL Server compiles the whole batch at once,
--    so UPDATE ... tenant_id fails with "Invalid column name" until a separate batch runs after ADD.

DECLARE @drops nvarchar(max);
SELECT @drops = STUFF((
                            SELECT N';' + N'ALTER TABLE dbo.roles DROP CONSTRAINT ' + QUOTENAME(cc.name)
                            FROM sys.check_constraints cc
                            WHERE cc.parent_object_id = OBJECT_ID(N'dbo.roles')
                            ORDER BY cc.name
                            FOR XML PATH(N''), TYPE).value(N'.', N'nvarchar(max)'), 1, 1, N'');
IF @drops IS NOT NULL AND LEN(@drops) > 0
    EXEC sp_executesql @drops;

-- dbo.users
IF OBJECT_ID(N'dbo.users', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.columns c
                             INNER JOIN sys.tables t ON c.object_id = t.object_id
                             INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                    WHERE s.name = N'dbo' AND t.name = N'users' AND c.name = N'tenant_id')
    EXEC sp_executesql N'ALTER TABLE dbo.users ADD tenant_id VARCHAR(50) NULL';

IF OBJECT_ID(N'dbo.users', N'U') IS NOT NULL
    AND EXISTS (SELECT 1
                FROM sys.columns c
                         INNER JOIN sys.tables t ON c.object_id = t.object_id
                         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = N'dbo' AND t.name = N'users' AND c.name = N'tenant_id')
    EXEC sp_executesql N'UPDATE dbo.users SET tenant_id = N''uumbal'' WHERE tenant_id IS NULL';

IF OBJECT_ID(N'dbo.users', N'U') IS NOT NULL
    AND EXISTS (SELECT 1
                FROM sys.columns c
                         INNER JOIN sys.tables t ON c.object_id = t.object_id
                         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = N'dbo' AND t.name = N'users' AND c.name = N'tenant_id' AND c.is_nullable = 1)
    EXEC sp_executesql N'ALTER TABLE dbo.users ALTER COLUMN tenant_id VARCHAR(50) NOT NULL';

-- dbo.verification_tokens
IF OBJECT_ID(N'dbo.verification_tokens', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.columns c
                             INNER JOIN sys.tables t ON c.object_id = t.object_id
                             INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                    WHERE s.name = N'dbo' AND t.name = N'verification_tokens' AND c.name = N'tenant_id')
    EXEC sp_executesql N'ALTER TABLE dbo.verification_tokens ADD tenant_id VARCHAR(50) NULL';

IF OBJECT_ID(N'dbo.verification_tokens', N'U') IS NOT NULL
    AND EXISTS (SELECT 1
                FROM sys.columns c
                         INNER JOIN sys.tables t ON c.object_id = t.object_id
                         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = N'dbo' AND t.name = N'verification_tokens' AND c.name = N'tenant_id')
    EXEC sp_executesql N'UPDATE dbo.verification_tokens SET tenant_id = N''uumbal'' WHERE tenant_id IS NULL';

IF OBJECT_ID(N'dbo.verification_tokens', N'U') IS NOT NULL
    AND EXISTS (SELECT 1
                FROM sys.columns c
                         INNER JOIN sys.tables t ON c.object_id = t.object_id
                         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = N'dbo' AND t.name = N'verification_tokens' AND c.name = N'tenant_id' AND c.is_nullable = 1)
    EXEC sp_executesql N'ALTER TABLE dbo.verification_tokens ALTER COLUMN tenant_id VARCHAR(50) NOT NULL';

-- dbo.password_reset_tokens
IF OBJECT_ID(N'dbo.password_reset_tokens', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.columns c
                             INNER JOIN sys.tables t ON c.object_id = t.object_id
                             INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                    WHERE s.name = N'dbo' AND t.name = N'password_reset_tokens' AND c.name = N'tenant_id')
    EXEC sp_executesql N'ALTER TABLE dbo.password_reset_tokens ADD tenant_id VARCHAR(50) NULL';

IF OBJECT_ID(N'dbo.password_reset_tokens', N'U') IS NOT NULL
    AND EXISTS (SELECT 1
                FROM sys.columns c
                         INNER JOIN sys.tables t ON c.object_id = t.object_id
                         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = N'dbo' AND t.name = N'password_reset_tokens' AND c.name = N'tenant_id')
    EXEC sp_executesql N'UPDATE dbo.password_reset_tokens SET tenant_id = N''uumbal'' WHERE tenant_id IS NULL';

IF OBJECT_ID(N'dbo.password_reset_tokens', N'U') IS NOT NULL
    AND EXISTS (SELECT 1
                FROM sys.columns c
                         INNER JOIN sys.tables t ON c.object_id = t.object_id
                         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = N'dbo' AND t.name = N'password_reset_tokens' AND c.name = N'tenant_id' AND c.is_nullable = 1)
    EXEC sp_executesql N'ALTER TABLE dbo.password_reset_tokens ALTER COLUMN tenant_id VARCHAR(50) NOT NULL';

-- dbo.productores
IF OBJECT_ID(N'dbo.productores', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.columns c
                             INNER JOIN sys.tables t ON c.object_id = t.object_id
                             INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                    WHERE s.name = N'dbo' AND t.name = N'productores' AND c.name = N'tenant_id')
    EXEC sp_executesql N'ALTER TABLE dbo.productores ADD tenant_id VARCHAR(50) NULL';

IF OBJECT_ID(N'dbo.productores', N'U') IS NOT NULL
    AND EXISTS (SELECT 1
                FROM sys.columns c
                         INNER JOIN sys.tables t ON c.object_id = t.object_id
                         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = N'dbo' AND t.name = N'productores' AND c.name = N'tenant_id')
    EXEC sp_executesql N'UPDATE dbo.productores SET tenant_id = N''uumbal'' WHERE tenant_id IS NULL';

IF OBJECT_ID(N'dbo.productores', N'U') IS NOT NULL
    AND EXISTS (SELECT 1
                FROM sys.columns c
                         INNER JOIN sys.tables t ON c.object_id = t.object_id
                         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = N'dbo' AND t.name = N'productores' AND c.name = N'tenant_id' AND c.is_nullable = 1)
    EXEC sp_executesql N'ALTER TABLE dbo.productores ALTER COLUMN tenant_id VARCHAR(50) NOT NULL';

-- dbo.usuarios
IF OBJECT_ID(N'dbo.usuarios', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1
                    FROM sys.columns c
                             INNER JOIN sys.tables t ON c.object_id = t.object_id
                             INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                    WHERE s.name = N'dbo' AND t.name = N'usuarios' AND c.name = N'tenant_id')
    EXEC sp_executesql N'ALTER TABLE dbo.usuarios ADD tenant_id VARCHAR(50) NULL';

IF OBJECT_ID(N'dbo.usuarios', N'U') IS NOT NULL
    AND EXISTS (SELECT 1
                FROM sys.columns c
                         INNER JOIN sys.tables t ON c.object_id = t.object_id
                         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = N'dbo' AND t.name = N'usuarios' AND c.name = N'tenant_id')
    EXEC sp_executesql N'UPDATE dbo.usuarios SET tenant_id = N''uumbal'' WHERE tenant_id IS NULL';

IF OBJECT_ID(N'dbo.usuarios', N'U') IS NOT NULL
    AND EXISTS (SELECT 1
                FROM sys.columns c
                         INNER JOIN sys.tables t ON c.object_id = t.object_id
                         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = N'dbo' AND t.name = N'usuarios' AND c.name = N'tenant_id' AND c.is_nullable = 1)
    EXEC sp_executesql N'ALTER TABLE dbo.usuarios ALTER COLUMN tenant_id VARCHAR(50) NOT NULL';
