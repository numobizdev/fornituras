-- Bitácora de auditoría (012). Registro append-only del Principio V (ISO/IEC 27001 A.12.4): quién
-- (usuario_id/actor) hizo qué (accion) sobre qué (entidad + entidad_id), cuándo (occurred_at), desde
-- dónde (ip) y con qué evidencia redactada. NO contiene PII ni secretos en claro: las entidades se
-- referencian por id y el detalle se redacta en la aplicación. prev_hash queda reservado para el
-- encadenamiento por hash (ADR 0012). Inmutabilidad: la app no expone update/delete y, además, los
-- triggers de abajo rechazan cualquier UPDATE/DELETE a nivel de BD.

CREATE TABLE audit_log (
    id BIGINT IDENTITY(1,1) NOT NULL,
    usuario_id BIGINT NULL,
    actor NVARCHAR(150) NULL,
    accion NVARCHAR(80) NOT NULL,
    entidad NVARCHAR(60) NULL,
    entidad_id BIGINT NULL,
    occurred_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    ip NVARCHAR(45) NULL,
    evidencia NVARCHAR(1000) NULL,
    prev_hash NVARCHAR(64) NULL,
    CONSTRAINT pk_audit_log PRIMARY KEY (id)
);

-- Índices para la consulta filtrada (SC-004): por usuario, acción, entidad y tiempo.
CREATE INDEX idx_audit_usuario ON audit_log (usuario_id);
CREATE INDEX idx_audit_accion ON audit_log (accion);
CREATE INDEX idx_audit_entidad ON audit_log (entidad, entidad_id);
CREATE INDEX idx_audit_occurred_at ON audit_log (occurred_at);

-- Inmutabilidad a nivel de BD (FR-003/SC-003): rechazar cualquier modificación o borrado,
-- independientemente del principal que se conecte.
GO
CREATE TRIGGER trg_audit_log_no_update ON audit_log INSTEAD OF UPDATE AS
BEGIN
    RAISERROR('audit_log es append-only: UPDATE no permitido.', 16, 1);
END;
GO
CREATE TRIGGER trg_audit_log_no_delete ON audit_log INSTEAD OF DELETE AS
BEGIN
    RAISERROR('audit_log es append-only: DELETE no permitido.', 16, 1);
END;
GO
