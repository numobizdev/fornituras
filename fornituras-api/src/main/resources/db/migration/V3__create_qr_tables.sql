CREATE TABLE lote_qr (
    id BIGINT IDENTITY(1,1) NOT NULL,
    cantidad INT NOT NULL,
    qr_size_cm DECIMAL(5,2) NOT NULL,
    padding_cm DECIMAL(5,2) NOT NULL,
    label_position NVARCHAR(10) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_lote_qr PRIMARY KEY (id)
);

CREATE TABLE codigo_qr (
    id BIGINT IDENTITY(1,1) NOT NULL,
    codigo NVARCHAR(10) NOT NULL,
    lote_qr_id BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_codigo_qr PRIMARY KEY (id),
    CONSTRAINT uk_codigo_qr_codigo UNIQUE (codigo),
    CONSTRAINT fk_codigo_qr_lote FOREIGN KEY (lote_qr_id) REFERENCES lote_qr(id)
);

CREATE INDEX idx_codigo_qr_lote ON codigo_qr (lote_qr_id);
