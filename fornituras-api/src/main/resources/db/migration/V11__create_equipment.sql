-- Inventario de fornituras (001). El equipo físico controlado: identificador interno opaco (id)
-- e inmutable, código físico (QR/serie) único por su forma normalizada, FKs a los catálogos de
-- tipo/talla (006) y almacén (005), y vida útil como fecha de vencimiento canónica de la que se
-- derivan vigencia y alertas. Sin PII del elemento (la asignación vive en 004).

CREATE TABLE equipment (
    id BIGINT IDENTITY(1,1) NOT NULL,
    codigo_qr NVARCHAR(60) NOT NULL,
    codigo_normalizado NVARCHAR(60) NOT NULL,
    equipment_type_id BIGINT NOT NULL,
    size_id BIGINT NULL,
    warehouse_id BIGINT NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
    descripcion NVARCHAR(255) NULL,
    marca NVARCHAR(120) NULL,
    modelo NVARCHAR(120) NULL,
    nivel_balistico NVARCHAR(60) NULL,
    numero_inventario NVARCHAR(60) NULL,
    fecha_fabricacion DATE NULL,
    fecha_adquisicion DATE NULL,
    vida_util_meses INT NULL,
    fecha_vencimiento DATE NULL,
    observaciones NVARCHAR(500) NULL,
    foto_url NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_equipment PRIMARY KEY (id),
    CONSTRAINT uk_equipment_codigo_norm UNIQUE (codigo_normalizado),
    CONSTRAINT fk_equipment_type FOREIGN KEY (equipment_type_id) REFERENCES equipment_type(id),
    CONSTRAINT fk_equipment_size FOREIGN KEY (size_id) REFERENCES size(id),
    CONSTRAINT fk_equipment_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
    CONSTRAINT ck_equipment_status CHECK (status IN (
        'DISPONIBLE', 'ASIGNADA', 'EN_MANTENIMIENTO', 'EN_TRASLADO', 'EXTRAVIADA', 'BAJA_DEFINITIVA'))
);

CREATE INDEX idx_equipment_status ON equipment (status);
CREATE INDEX idx_equipment_type ON equipment (equipment_type_id);
CREATE INDEX idx_equipment_warehouse ON equipment (warehouse_id);
CREATE INDEX idx_equipment_vencimiento ON equipment (fecha_vencimiento);
