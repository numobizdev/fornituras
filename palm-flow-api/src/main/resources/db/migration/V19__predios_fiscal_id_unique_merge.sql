-- Merge duplicate predios that share the same fiscal_id (per tenant), then enforce uniqueness.
-- Root cause: V17 copied productor.fiscal_id onto every predio of that productor.

-- 1. Normalize fiscal_id values
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL
BEGIN
    UPDATE dbo.predios
    SET fiscal_id = LTRIM(RTRIM(fiscal_id))
    WHERE fiscal_id IS NOT NULL;

    UPDATE dbo.predios
    SET fiscal_id = NULL
    WHERE fiscal_id IS NOT NULL AND fiscal_id = N'';
END;

-- 2. Reassign lotes and recepciones from duplicate predios to the canonical predio (oldest per fiscal_id)
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL
    AND EXISTS (
        SELECT 1
        FROM dbo.predios
        WHERE fiscal_id IS NOT NULL
        GROUP BY tenant_id, LOWER(fiscal_id)
        HAVING COUNT(*) > 1
    )
BEGIN
    ;WITH ranked AS (
        SELECT
            p.uuid,
            FIRST_VALUE(p.uuid) OVER (
                PARTITION BY p.tenant_id, LOWER(p.fiscal_id)
                ORDER BY p.created_at ASC, p.uuid ASC
            ) AS keeper_uuid
        FROM dbo.predios p
        WHERE p.fiscal_id IS NOT NULL
    )
    UPDATE l
    SET l.predio_uuid = r.keeper_uuid
    FROM dbo.lotes l
    INNER JOIN ranked r ON r.uuid = l.predio_uuid
    WHERE r.uuid <> r.keeper_uuid;

    IF OBJECT_ID(N'dbo.recepciones_fruta', N'U') IS NOT NULL
    BEGIN
        ;WITH ranked AS (
            SELECT
                p.uuid,
                FIRST_VALUE(p.uuid) OVER (
                    PARTITION BY p.tenant_id, LOWER(p.fiscal_id)
                    ORDER BY p.created_at ASC, p.uuid ASC
                ) AS keeper_uuid
            FROM dbo.predios p
            WHERE p.fiscal_id IS NOT NULL
        )
        UPDATE rf
        SET rf.predio_uuid = r.keeper_uuid
        FROM dbo.recepciones_fruta rf
        INNER JOIN ranked r ON r.uuid = rf.predio_uuid
        WHERE r.uuid <> r.keeper_uuid;
    END;

    ;WITH ranked AS (
        SELECT
            p.uuid,
            ROW_NUMBER() OVER (
                PARTITION BY p.tenant_id, LOWER(p.fiscal_id)
                ORDER BY p.created_at ASC, p.uuid ASC
            ) AS rn
        FROM dbo.predios p
        WHERE p.fiscal_id IS NOT NULL
    )
    DELETE p
    FROM dbo.predios p
    INNER JOIN ranked r ON r.uuid = p.uuid
    WHERE r.rn > 1;
END;

-- 3. Unique fiscal_id per tenant (when fiscal_id is present)
IF OBJECT_ID(N'dbo.predios', N'U') IS NOT NULL
    AND NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE name = N'uq_predios_tenant_fiscal_id'
          AND object_id = OBJECT_ID(N'dbo.predios')
    )
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX uq_predios_tenant_fiscal_id
        ON dbo.predios (tenant_id, fiscal_id)
        WHERE fiscal_id IS NOT NULL;
END;
