# 0007. Catálogos genéricos (`catalog → catalog_item`) y ubicación como texto libre

- **Estado:** **Aceptado**
- **Fecha:** 2026-06-30

## Contexto

Los catálogos administrables del sistema (tipos de fornitura, tallas, tipo de almacén, y a
futuro marca/material/color) se venían modelando como **tablas tipadas independientes**, cada
una con su propia entidad, repositorio, servicio, controller, DTOs y CRUD. En el backend actual
esto se materializó en `modules/equipmenttypes` (`equipment_type`, `size`) y en un **enum**
`warehouse_type` embebido en `modules/warehouses`.

Este enfoque **duplica CRUD** por cada catálogo nuevo y contradice el principio LEGO
(piezas reutilizables, bajo acoplamiento) que rige el proyecto: agregar un catálogo "marca"
obligaba a crear una tabla y una pila de clases más.

En paralelo, **municipio** se había planteado como catálogo geográfico compartido (spec 003) y
como FK (`municipio_id`) desde `warehouse` (spec 005). En la práctica es un dato de captura
simple que no requiere integridad referencial ni administración.

## Decisión

1. **Estructura genérica única** para todos los catálogos planos:
   - `catalog` (cabecera): `code` (único, estable — p. ej. `TIPO_FORNITURA`, `TALLA`,
     `TIPO_ALMACEN`), `nombre`, `descripcion?`, `system` (bool, no borrable si `true`), `active`.
   - `catalog_item` (valores): `catalog_id` (FK → `catalog`), `code?` (único dentro del catálogo),
     `nombre`, `nombre_normalizado` (trim + colapso de espacios + casefold; único dentro del
     catálogo), `descripcion?`, `foto_url?`, `parent_item_id?` (FK self → jerarquía item→item),
     `orden?`, `active`.
   - **Un único CRUD genérico** (`CatalogService`/`CatalogController`) sirve a todos los catálogos.
2. **Catálogos en alcance inicial:** `TIPO_FORNITURA`, `TALLA` (depende de `TIPO_FORNITURA` vía
   `parent_item_id`) y `TIPO_ALMACEN` (migra el `enum warehouse_type`; semilla CENTRAL/REGIONAL/
   MOVIL/TEMPORAL). Candidatos a migrar después: `SEXO`, `TIPO_SANGRE` (spec 003), `MARCA`,
   `MATERIAL`, `COLOR`.
3. **Municipio y estado dejan de ser catálogo:** se capturan como **texto libre** en `officer`
   (spec 003) y `warehouse` (spec 005). Se elimina la FK `municipio_id` y la dependencia de un
   catálogo geográfico.
4. **Almacén y elemento siguen siendo entidades/maestros**, no catálogos; solo sus clasificaciones
   (tipo de almacén, sexo, tipo de sangre) son catálogos.

## Alternativas consideradas

- **Mantener tablas tipadas por catálogo** (patrón `equipment_type → size` replicado): máxima
  integridad por FK y tipado fuerte, pero duplica CRUD por catálogo y no escala al agregar
  catálogos. Descartada por costo de mantenimiento y acoplamiento.
- **Municipio como catálogo/FK geográfica:** aporta consistencia de nombres, pero exige poblar y
  mantener un catálogo geográfico completo para un dato de captura secundario. Descartada por
  sobrediseño frente al valor.

## Consecuencias

- (+) Agregar un catálogo nuevo no requiere tabla ni CRUD nuevos: basta un registro en `catalog`.
- (+) Un solo módulo que mantener, probar y auditar (menos superficie).
- (+) Jerarquías (talla-por-tipo) se cubren con `parent_item_id` sin tablas extra.
- (−) Se pierde integridad **tipada por FK** por catálogo: la coherencia (que un `catalog_item`
  pertenezca al catálogo correcto donde se consume) se valida en la capa de servicio, no por el
  esquema. Mitigación: validar `catalog.code` esperado al resolver cada referencia.
- (−) Municipio/estado como texto libre admite variantes ("CDMX" vs "Ciudad de México"); aceptable
  para el caso de uso (no se agrega ni se cruza por municipio con exactitud).
- (−) Regeneración pendiente de `plan.md`/`tasks.md`/`data-model.md` de las specs 003/005/006
  (los `spec.md` ya están ajustados; el código ya migró).

## Estado de implementación (2026-06-30)

- **Hecho:** módulo `modules/catalog` (entidades, repos, servicio y CRUD genérico); migración
  Flyway `V15__generic_catalog.sql` (crea `catalog`/`catalog_item`, siembra `TIPO_FORNITURA`,
  `TALLA`, `TIPO_ALMACEN`, migra datos desde `equipment_type`/`size` y el enum `warehouse_type`, y
  añade columnas `municipio`/`estado` de texto libre en `warehouse`/`officers`); repunte de
  `equipment`, `warehouse` y `officer`; retiro de `modules/equipmenttypes` y `modules/municipios`.
  Frontend: `core/catalog` (servicio/modelo genérico) consumido por tipos, almacenes y elementos.
- **Pendiente:** regenerar `plan.md`/`tasks.md`/`data-model.md` de 003/005/006 (`/speckit-plan`,
  `/speckit-tasks`); migrar a futuro `SEXO`/`TIPO_SANGRE` a la estructura genérica.
