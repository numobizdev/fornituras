-- Migrar los catálogos SEXO y TIPO_SANGRE (tablas planas) a la estructura genérica
-- catalog/catalog_item (ADR 0007, spec 015). Migración con preservación de datos: se copian los
-- valores, se repuntan las FKs de officers y se retiran las tablas planas.
-- Se usa GO como separador de lote (SQL Server) porque hay DDL seguido de uso en el mismo script.

-- 1. Cabeceras de catálogo (de sistema) y sus valores, conservando nombre/estado y orden original.
INSERT INTO catalog (code, nombre, descripcion, is_system) VALUES
    ('SEXO', 'Sexo', 'Sexo del elemento policial.', 1),
    ('TIPO_SANGRE', 'Tipo de sangre', 'Tipo de sangre del elemento policial.', 1);

INSERT INTO catalog_item (catalog_id, nombre, nombre_normalizado, orden, active)
SELECT (SELECT id FROM catalog WHERE code = 'SEXO'),
       s.nombre, LOWER(LTRIM(RTRIM(s.nombre))), s.id, s.active
FROM sexo s;

INSERT INTO catalog_item (catalog_id, nombre, nombre_normalizado, orden, active)
SELECT (SELECT id FROM catalog WHERE code = 'TIPO_SANGRE'),
       t.etiqueta, LOWER(LTRIM(RTRIM(t.etiqueta))), t.id, t.active
FROM tipo_sangre t;
GO

-- 2. Repuntar officers.sexo_id / tipo_sangre_id de las tablas planas a catalog_item.
ALTER TABLE officers DROP CONSTRAINT fk_officers_sexo;
ALTER TABLE officers DROP CONSTRAINT fk_officers_tipo_sangre;
DROP INDEX idx_officers_sexo ON officers;

UPDATE o SET o.sexo_id = ci.id
FROM officers o
JOIN sexo s ON s.id = o.sexo_id
JOIN catalog_item ci ON ci.catalog_id = (SELECT id FROM catalog WHERE code = 'SEXO')
                    AND ci.nombre = s.nombre;

UPDATE o SET o.tipo_sangre_id = ci.id
FROM officers o
JOIN tipo_sangre t ON t.id = o.tipo_sangre_id
JOIN catalog_item ci ON ci.catalog_id = (SELECT id FROM catalog WHERE code = 'TIPO_SANGRE')
                    AND ci.nombre = t.etiqueta
WHERE o.tipo_sangre_id IS NOT NULL;

ALTER TABLE officers ADD CONSTRAINT fk_officers_sexo_item FOREIGN KEY (sexo_id) REFERENCES catalog_item(id);
ALTER TABLE officers ADD CONSTRAINT fk_officers_tipo_sangre_item FOREIGN KEY (tipo_sangre_id) REFERENCES catalog_item(id);
CREATE INDEX idx_officers_sexo ON officers (sexo_id);
GO

-- 3. Retirar las tablas planas ya migradas.
DROP TABLE sexo;
DROP TABLE tipo_sangre;
GO
