-- Numeración secuencial de lotes QR: se elimina la tabla de códigos individuales
-- (codigo_qr) y se añade el rango de consecutivos al lote. Escrita de forma idempotente
-- para reconciliar entornos donde el cambio ya se aplicó manualmente fuera de Flyway.

IF OBJECT_ID('codigo_qr', 'U') IS NOT NULL
    DROP TABLE codigo_qr;
GO

IF COL_LENGTH('lote_qr', 'consecutivo_inicial') IS NULL
BEGIN
    -- El esquema de QR cambió: los lotes previos no son válidos bajo numeración secuencial.
    DELETE FROM lote_qr;
    ALTER TABLE lote_qr ADD consecutivo_inicial INT NOT NULL;
    ALTER TABLE lote_qr ADD consecutivo_final INT NOT NULL;
END
GO

IF OBJECT_ID('ck_lote_qr_consecutivo_range', 'C') IS NULL
    ALTER TABLE lote_qr ADD CONSTRAINT ck_lote_qr_consecutivo_range
        CHECK (consecutivo_final >= consecutivo_inicial);
GO
