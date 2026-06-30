DELETE FROM codigo_qr;
DELETE FROM lote_qr;
GO

DROP TABLE codigo_qr;
GO

ALTER TABLE lote_qr ADD consecutivo_inicial INT NOT NULL;
ALTER TABLE lote_qr ADD consecutivo_final INT NOT NULL;
GO

ALTER TABLE lote_qr ADD CONSTRAINT ck_lote_qr_consecutivo_range
    CHECK (consecutivo_final >= consecutivo_inicial);
GO
