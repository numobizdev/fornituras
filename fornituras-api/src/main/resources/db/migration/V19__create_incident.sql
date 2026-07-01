-- Incidencias y mantenimiento sobre fornituras (008). Cada incidencia (daño, falla, extravío,
-- mantenimiento) referencia una fornitura y su ciclo de estado (abierta → en proceso →
-- resuelta/cerrada). Reportarla puede retirar la fornitura (en mantenimiento/extraviada) y
-- resolverla devolverla a disponible; esos cambios los hace el servicio, transaccionalmente. Sin
-- PII: referencias por id. Las alertas de vigencia NO se materializan aquí: se derivan de
-- equipment.fecha_vencimiento con el mismo criterio (≤ 90 días) que 001/010.

CREATE TABLE incident (
    id BIGINT IDENTITY(1,1) NOT NULL,
    equipment_id BIGINT NOT NULL,
    tipo NVARCHAR(20) NOT NULL,
    descripcion NVARCHAR(500) NOT NULL,
    estado NVARCHAR(20) NOT NULL DEFAULT 'ABIERTA',
    fecha_reporte DATETIME2 NOT NULL DEFAULT GETDATE(),
    fecha_resolucion DATETIME2 NULL,
    reportado_por BIGINT NULL,
    actualizado_por BIGINT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_incident PRIMARY KEY (id),
    CONSTRAINT fk_incident_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(id),
    CONSTRAINT fk_incident_reportado_por FOREIGN KEY (reportado_por) REFERENCES users(id),
    CONSTRAINT fk_incident_actualizado_por FOREIGN KEY (actualizado_por) REFERENCES users(id),
    CONSTRAINT ck_incident_tipo CHECK (tipo IN ('DANO', 'FALLA', 'EXTRAVIO', 'MANTENIMIENTO')),
    CONSTRAINT ck_incident_estado CHECK (estado IN ('ABIERTA', 'EN_PROCESO', 'RESUELTA', 'CERRADA'))
);

CREATE INDEX idx_incident_estado ON incident (estado);
CREATE INDEX idx_incident_equipment ON incident (equipment_id);
