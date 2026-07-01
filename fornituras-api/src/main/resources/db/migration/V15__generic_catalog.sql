-- Catálogos genéricos (ADR 0007). Un par catalog + catalog_item sustituye a las tablas tipadas
-- equipment_type/size y al enum warehouse_type; municipio pasa a TEXTO LIBRE en warehouse y officers.
-- Migración con preservación de datos: se copian los valores existentes y se repuntan las FKs.
-- Se usa GO como separador de lote (SQL Server) porque hay ADD COLUMN seguido de uso en el mismo script.

-- 1. Estructura -------------------------------------------------------------------------------
CREATE TABLE catalog (
    id BIGINT IDENTITY(1,1) NOT NULL,
    code NVARCHAR(40) NOT NULL,
    nombre NVARCHAR(120) NOT NULL,
    descripcion NVARCHAR(500) NULL,
    is_system BIT NOT NULL DEFAULT 0,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_catalog PRIMARY KEY (id),
    CONSTRAINT uk_catalog_code UNIQUE (code)
);

CREATE TABLE catalog_item (
    id BIGINT IDENTITY(1,1) NOT NULL,
    catalog_id BIGINT NOT NULL,
    code NVARCHAR(40) NULL,
    nombre NVARCHAR(120) NOT NULL,
    nombre_normalizado NVARCHAR(120) NOT NULL,
    descripcion NVARCHAR(500) NULL,
    foto_url NVARCHAR(500) NULL,
    parent_item_id BIGINT NULL,
    orden INT NULL,
    active BIT NOT NULL DEFAULT 1,
    -- Columnas temporales para repuntar FKs de datos migrados (se eliminan al final del script).
    legacy_equipment_type_id BIGINT NULL,
    legacy_size_id BIGINT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_catalog_item PRIMARY KEY (id),
    CONSTRAINT fk_catalog_item_catalog FOREIGN KEY (catalog_id) REFERENCES catalog(id),
    CONSTRAINT fk_catalog_item_parent FOREIGN KEY (parent_item_id) REFERENCES catalog_item(id)
);

CREATE INDEX idx_catalog_item_catalog ON catalog_item (catalog_id);
CREATE INDEX idx_catalog_item_active ON catalog_item (active);
CREATE INDEX idx_catalog_item_parent ON catalog_item (parent_item_id);

-- Unicidad de nombre dentro del catálogo. Se distingue por padre para permitir el mismo valor
-- (p. ej. talla "M") colgando de tipos distintos, y valores globales únicos cuando no hay padre.
CREATE UNIQUE INDEX uk_catalog_item_named ON catalog_item (catalog_id, nombre_normalizado)
    WHERE parent_item_id IS NULL;
CREATE UNIQUE INDEX uk_catalog_item_named_child ON catalog_item (catalog_id, parent_item_id, nombre_normalizado)
    WHERE parent_item_id IS NOT NULL;
GO

-- 2. Semilla de catálogos y migración de valores existentes -----------------------------------
INSERT INTO catalog (code, nombre, descripcion, is_system) VALUES
    ('TIPO_FORNITURA', 'Tipos de fornitura', 'Clasificación de las fornituras (chaleco, cinturón, casco…).', 1),
    ('TALLA', 'Tallas', 'Tallas de fornitura, opcionalmente ligadas a un tipo.', 1),
    ('TIPO_ALMACEN', 'Tipos de almacén', 'Clasificación operativa de los almacenes.', 1);

-- 2a. equipment_type -> catalog_item (TIPO_FORNITURA)
INSERT INTO catalog_item (catalog_id, nombre, nombre_normalizado, descripcion, foto_url, active, legacy_equipment_type_id)
SELECT (SELECT id FROM catalog WHERE code = 'TIPO_FORNITURA'),
       nombre, nombre_normalizado, descripcion, foto_url, active, id
FROM equipment_type;

-- 2b. size -> catalog_item (TALLA), enlazando el padre por el tipo de origen (talla-por-tipo)
INSERT INTO catalog_item (catalog_id, nombre, nombre_normalizado, parent_item_id, active, legacy_size_id)
SELECT (SELECT id FROM catalog WHERE code = 'TALLA'),
       s.etiqueta, LOWER(LTRIM(RTRIM(s.etiqueta))), pt.id, s.active, s.id
FROM size s
LEFT JOIN catalog_item pt
       ON pt.legacy_equipment_type_id = s.equipment_type_id
      AND pt.catalog_id = (SELECT id FROM catalog WHERE code = 'TIPO_FORNITURA');

-- 2c. Semilla TIPO_ALMACEN (conserva los valores del enum previo)
INSERT INTO catalog_item (catalog_id, code, nombre, nombre_normalizado, orden, active) VALUES
    ((SELECT id FROM catalog WHERE code = 'TIPO_ALMACEN'), 'CENTRAL',  'Central',  'central',  1, 1),
    ((SELECT id FROM catalog WHERE code = 'TIPO_ALMACEN'), 'REGIONAL', 'Regional', 'regional', 2, 1),
    ((SELECT id FROM catalog WHERE code = 'TIPO_ALMACEN'), 'MOVIL',    'Móvil',    'movil',    3, 1),
    ((SELECT id FROM catalog WHERE code = 'TIPO_ALMACEN'), 'TEMPORAL', 'Temporal', 'temporal', 4, 1);
GO

-- 3. Repuntar equipment a catalog_item --------------------------------------------------------
ALTER TABLE equipment DROP CONSTRAINT fk_equipment_type;
ALTER TABLE equipment DROP CONSTRAINT fk_equipment_size;
DROP INDEX idx_equipment_type ON equipment;

UPDATE e SET e.equipment_type_id = ci.id
FROM equipment e
JOIN catalog_item ci ON ci.legacy_equipment_type_id = e.equipment_type_id
                    AND ci.catalog_id = (SELECT id FROM catalog WHERE code = 'TIPO_FORNITURA');

UPDATE e SET e.size_id = ci.id
FROM equipment e
JOIN catalog_item ci ON ci.legacy_size_id = e.size_id
                    AND ci.catalog_id = (SELECT id FROM catalog WHERE code = 'TALLA')
WHERE e.size_id IS NOT NULL;

ALTER TABLE equipment ADD CONSTRAINT fk_equipment_type_item FOREIGN KEY (equipment_type_id) REFERENCES catalog_item(id);
ALTER TABLE equipment ADD CONSTRAINT fk_equipment_size_item FOREIGN KEY (size_id) REFERENCES catalog_item(id);
CREATE INDEX idx_equipment_type ON equipment (equipment_type_id);
GO

-- 4. Warehouse: nuevas columnas (tipo_item_id, municipio/estado texto libre) -------------------
ALTER TABLE warehouse ADD tipo_item_id BIGINT NULL;
ALTER TABLE warehouse ADD municipio NVARCHAR(120) NULL;
ALTER TABLE warehouse ADD estado NVARCHAR(120) NULL;
GO

-- 5. Warehouse: migrar datos y retirar lo viejo ----------------------------------------------
UPDATE w SET w.tipo_item_id = ci.id
FROM warehouse w
JOIN catalog_item ci ON ci.code = w.tipo
                    AND ci.catalog_id = (SELECT id FROM catalog WHERE code = 'TIPO_ALMACEN');

UPDATE w SET w.municipio = m.nombre
FROM warehouse w
JOIN municipio m ON m.id = w.municipio_id;

ALTER TABLE warehouse DROP CONSTRAINT fk_warehouse_municipio;
ALTER TABLE warehouse DROP CONSTRAINT ck_warehouse_tipo;
DROP INDEX idx_warehouse_municipio ON warehouse;
DROP INDEX idx_warehouse_tipo ON warehouse;
ALTER TABLE warehouse DROP COLUMN municipio_id;
ALTER TABLE warehouse DROP COLUMN tipo;

ALTER TABLE warehouse ALTER COLUMN tipo_item_id BIGINT NOT NULL;
ALTER TABLE warehouse ADD CONSTRAINT fk_warehouse_tipo_item FOREIGN KEY (tipo_item_id) REFERENCES catalog_item(id);
CREATE INDEX idx_warehouse_tipo ON warehouse (tipo_item_id);
GO

-- 6. Officers: municipio/estado como texto libre ---------------------------------------------
ALTER TABLE officers ADD municipio NVARCHAR(120) NULL;
ALTER TABLE officers ADD estado NVARCHAR(120) NULL;
GO

-- 7. Officers: migrar y retirar la FK a municipio --------------------------------------------
UPDATE o SET o.municipio = m.nombre
FROM officers o
JOIN municipio m ON m.id = o.municipio_id;

ALTER TABLE officers DROP CONSTRAINT fk_officers_municipio;
DROP INDEX idx_officers_municipio ON officers;
ALTER TABLE officers DROP COLUMN municipio_id;
GO

-- 8. Limpieza: retirar tablas migradas y columnas temporales ----------------------------------
DROP TABLE size;
DROP TABLE equipment_type;
DROP TABLE municipio;
ALTER TABLE catalog_item DROP COLUMN legacy_equipment_type_id;
ALTER TABLE catalog_item DROP COLUMN legacy_size_id;
GO
