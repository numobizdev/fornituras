-- Traslados de fornituras entre almacenes (007). Un traslado mueve N fornituras de un almacén
-- origen a uno destino; al crearlo quedan "en traslado" (bloqueadas para asignación/baja) y el
-- traslado "enviado"; al recibirlo pasan a "disponible" bajo el destino; al cancelarlo vuelven al
-- origen. Sin PII: referencias por id. Consistencia de estado garantizada por transacciones.

CREATE TABLE transfer (
    id BIGINT IDENTITY(1,1) NOT NULL,
    origen_id BIGINT NOT NULL,
    destino_id BIGINT NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'ENVIADO',
    fecha_envio DATETIME2 NOT NULL DEFAULT GETDATE(),
    fecha_recepcion DATETIME2 NULL,
    creado_por BIGINT NULL,
    recibido_por BIGINT NULL,
    observaciones NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_transfer PRIMARY KEY (id),
    CONSTRAINT fk_transfer_origen FOREIGN KEY (origen_id) REFERENCES warehouse(id),
    CONSTRAINT fk_transfer_destino FOREIGN KEY (destino_id) REFERENCES warehouse(id),
    CONSTRAINT fk_transfer_creado_por FOREIGN KEY (creado_por) REFERENCES users(id),
    CONSTRAINT fk_transfer_recibido_por FOREIGN KEY (recibido_por) REFERENCES users(id),
    CONSTRAINT ck_transfer_status CHECK (status IN ('ENVIADO', 'RECIBIDO', 'CANCELADO')),
    CONSTRAINT ck_transfer_distinct CHECK (origen_id <> destino_id)
);

CREATE INDEX idx_transfer_status ON transfer (status);
CREATE INDEX idx_transfer_origen ON transfer (origen_id);
CREATE INDEX idx_transfer_destino ON transfer (destino_id);

CREATE TABLE transfer_item (
    id BIGINT IDENTITY(1,1) NOT NULL,
    transfer_id BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_transfer_item PRIMARY KEY (id),
    CONSTRAINT fk_transfer_item_transfer FOREIGN KEY (transfer_id) REFERENCES transfer(id),
    CONSTRAINT fk_transfer_item_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(id)
);

CREATE INDEX idx_transfer_item_transfer ON transfer_item (transfer_id);
CREATE INDEX idx_transfer_item_equipment ON transfer_item (equipment_id);
