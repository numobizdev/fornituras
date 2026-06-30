-- Catálogo de tipos de fornitura (chaleco antibala, cinturón táctico, casco…) y de tallas.
-- Sin PII: foto genérica del equipo. El eje es integridad referencial y unicidad de nombre.

CREATE TABLE equipment_type (
    id BIGINT IDENTITY(1,1) NOT NULL,
    nombre NVARCHAR(120) NOT NULL,
    nombre_normalizado NVARCHAR(120) NOT NULL,
    descripcion NVARCHAR(500) NULL,
    foto_url NVARCHAR(500) NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_equipment_type PRIMARY KEY (id),
    CONSTRAINT uk_equipment_type_nombre_norm UNIQUE (nombre_normalizado)
);

CREATE INDEX idx_equipment_type_active ON equipment_type (active);

CREATE TABLE size (
    id BIGINT IDENTITY(1,1) NOT NULL,
    etiqueta NVARCHAR(50) NOT NULL,
    equipment_type_id BIGINT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_size PRIMARY KEY (id),
    CONSTRAINT fk_size_equipment_type FOREIGN KEY (equipment_type_id)
        REFERENCES equipment_type(id)
);

CREATE INDEX idx_size_active ON size (active);
CREATE INDEX idx_size_equipment_type ON size (equipment_type_id);
