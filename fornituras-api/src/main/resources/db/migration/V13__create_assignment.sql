-- Asignación de fornituras a elementos (004), el núcleo de SIGEFOR. Relación fornitura↔elemento en
-- el tiempo: fecha_devolucion NULL = vigente. El índice único filtrado garantiza UNA sola
-- asignación vigente por fornitura (garantía dura de concurrencia: dos asignaciones simultáneas de
-- la misma fornitura → solo una gana). Sin PII (referencias por id; el nombre se resuelve vía 003).

CREATE TABLE assignment (
    id BIGINT IDENTITY(1,1) NOT NULL,
    equipment_id BIGINT NOT NULL,
    officer_id BIGINT NOT NULL,
    fecha_asignacion DATETIME2 NOT NULL DEFAULT GETDATE(),
    fecha_devolucion DATETIME2 NULL,
    asignado_por BIGINT NULL,
    recibido_por BIGINT NULL,
    firma_url NVARCHAR(500) NULL,
    observaciones NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_assignment PRIMARY KEY (id),
    CONSTRAINT fk_assignment_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(id),
    CONSTRAINT fk_assignment_officer FOREIGN KEY (officer_id) REFERENCES officers(id),
    CONSTRAINT fk_assignment_asignado_por FOREIGN KEY (asignado_por) REFERENCES users(id),
    CONSTRAINT fk_assignment_recibido_por FOREIGN KEY (recibido_por) REFERENCES users(id)
);

-- Una sola asignación vigente por fornitura (fecha_devolucion IS NULL).
CREATE UNIQUE INDEX ux_assignment_vigente
    ON assignment (equipment_id) WHERE fecha_devolucion IS NULL;

CREATE INDEX idx_assignment_officer ON assignment (officer_id);
CREATE INDEX idx_assignment_fecha ON assignment (fecha_asignacion);
