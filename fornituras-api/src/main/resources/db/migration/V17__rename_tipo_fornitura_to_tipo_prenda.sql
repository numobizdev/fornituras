-- Aclaración de dominio (ADR 0007): "fornitura" es un TIPO DE PRENDA concreto, no una categoría
-- con subtipos. El catálogo TIPO_FORNITURA (que modelaba subtipos como chaleco/cinturón/casco) se
-- renombra a TIPO_PRENDA y se deja con un único valor de sistema: "Fornitura". V8 no sembró
-- subtipos, así que en el estado esperado el catálogo está vacío; los pasos de repunte son una red
-- de seguridad para datos creados en tiempo de ejecución.

DECLARE @catalogId BIGINT = (SELECT id FROM catalog WHERE code = 'TIPO_FORNITURA');

-- 1. Renombrar la cabecera del catálogo (el id es estable; no afecta a @catalogId).
UPDATE catalog
   SET code = 'TIPO_PRENDA',
       nombre = 'Tipo de prenda',
       descripcion = 'Tipo de prenda controlada por el sistema. Hoy: Fornitura.'
 WHERE code = 'TIPO_FORNITURA';

-- 2. Asegurar el único valor de sistema "Fornitura".
IF NOT EXISTS (SELECT 1 FROM catalog_item WHERE catalog_id = @catalogId AND nombre_normalizado = 'fornitura')
    INSERT INTO catalog_item (catalog_id, nombre, nombre_normalizado, descripcion, orden, active)
    VALUES (@catalogId, 'Fornitura', 'fornitura', 'Prenda de dotación controlada.', 1, 1);

DECLARE @fornituraItemId BIGINT =
    (SELECT id FROM catalog_item WHERE catalog_id = @catalogId AND nombre_normalizado = 'fornitura');

-- 3. Repuntar cualquier fornitura al único tipo de prenda "Fornitura".
UPDATE equipment
   SET equipment_type_id = @fornituraItemId
 WHERE equipment_type_id IN (SELECT id FROM catalog_item WHERE catalog_id = @catalogId)
   AND equipment_type_id <> @fornituraItemId;

-- 4. Colgar del tipo "Fornitura" las tallas que colgaban de tipos anteriores, cuando no colisionen
--    con una talla ya existente bajo "Fornitura".
UPDATE ci
   SET parent_item_id = @fornituraItemId
  FROM catalog_item ci
 WHERE ci.parent_item_id IN (SELECT id FROM catalog_item WHERE catalog_id = @catalogId AND id <> @fornituraItemId)
   AND NOT EXISTS (SELECT 1 FROM catalog_item sib
                    WHERE sib.catalog_id = ci.catalog_id
                      AND sib.parent_item_id = @fornituraItemId
                      AND sib.nombre_normalizado = ci.nombre_normalizado);

-- 5. Las tallas que aún colgaran de un tipo anterior (colisión) se retiran para poder borrar los
--    tipos; no ocurre en el estado actual (V8 no sembró tallas).
DELETE FROM catalog_item
 WHERE parent_item_id IN (SELECT id FROM catalog_item WHERE catalog_id = @catalogId AND id <> @fornituraItemId);

-- 6. Retirar los tipos anteriores (ya sin equipment ni tallas que los referencien).
DELETE FROM catalog_item
 WHERE catalog_id = @catalogId AND id <> @fornituraItemId;
