-- Bajas definitivas de fornituras (009). Una baja registra la fornitura, el motivo (catálogo
-- decommission_reason), la fecha, el responsable (por id) y observaciones. El cambio de estado de la
-- fornitura a 'BAJA_DEFINITIVA' lo hace el servicio, transaccionalmente, tras validar que no tenga
-- asignación vigente ni traslado en curso (consistente con 001/004/007). Una baja NO se revierte: un
-- error se corrige con un ajuste auditado. Sin PII: referencias por id.

CREATE TABLE decommission_reason (
    id BIGINT IDENTITY(1,1) NOT NULL,
    nombre NVARCHAR(100) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_decommission_reason PRIMARY KEY (id),
    CONSTRAINT uq_decommission_reason_nombre UNIQUE (nombre)
);

-- Motivos base sembrados; el catálogo es extensible desde la administración.
INSERT INTO decommission_reason (nombre, active) VALUES
    ('Caducidad', 1),
    ('Daño', 1),
    ('Extravío', 1),
    ('Obsolescencia', 1);

CREATE TABLE decommission (
    id BIGINT IDENTITY(1,1) NOT NULL,
    equipment_id BIGINT NOT NULL,
    motivo_id BIGINT NOT NULL,
    fecha DATE NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    responsable BIGINT NULL,
    observaciones NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_decommission PRIMARY KEY (id),
    CONSTRAINT fk_decommission_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(id),
    CONSTRAINT fk_decommission_motivo FOREIGN KEY (motivo_id) REFERENCES decommission_reason(id),
    CONSTRAINT fk_decommission_responsable FOREIGN KEY (responsable) REFERENCES users(id)
);

CREATE INDEX idx_decommission_fecha ON decommission (fecha);
CREATE INDEX idx_decommission_motivo ON decommission (motivo_id);
CREATE INDEX idx_decommission_equipment ON decommission (equipment_id);
