-- Almacenes: entidad operativa (no catálogo plano). Ubicación física de resguardo de fornituras,
-- con clave de negocio única, clasificación, ubicación, responsable y cupo. Sin PII de elementos;
-- los campos sensibles (dirección/geo/responsable/contacto) se protegen por autorización, no por
-- cifrado de columna. FKs garantizan integridad referencial con municipio (005/003) y users.

CREATE TABLE warehouse (
    id BIGINT IDENTITY(1,1) NOT NULL,
    codigo NVARCHAR(40) NOT NULL,
    nombre NVARCHAR(120) NOT NULL,
    nombre_normalizado NVARCHAR(120) NOT NULL,
    tipo NVARCHAR(20) NOT NULL,
    municipio_id BIGINT NULL,
    direccion NVARCHAR(255) NULL,
    cp NVARCHAR(10) NULL,
    latitud DECIMAL(9,6) NULL,
    longitud DECIMAL(9,6) NULL,
    responsable_id BIGINT NULL,
    telefono NVARCHAR(30) NULL,
    email_contacto NVARCHAR(255) NULL,
    capacidad INT NULL,
    observaciones NVARCHAR(500) NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_warehouse PRIMARY KEY (id),
    CONSTRAINT uk_warehouse_codigo UNIQUE (codigo),
    CONSTRAINT uk_warehouse_nombre_norm UNIQUE (nombre_normalizado),
    CONSTRAINT fk_warehouse_municipio FOREIGN KEY (municipio_id) REFERENCES municipio(id),
    CONSTRAINT fk_warehouse_responsable FOREIGN KEY (responsable_id) REFERENCES users(id),
    CONSTRAINT ck_warehouse_tipo CHECK (tipo IN ('CENTRAL', 'REGIONAL', 'MOVIL', 'TEMPORAL'))
);

CREATE INDEX idx_warehouse_active ON warehouse (active);
CREATE INDEX idx_warehouse_tipo ON warehouse (tipo);
CREATE INDEX idx_warehouse_municipio ON warehouse (municipio_id);
