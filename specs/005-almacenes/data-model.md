# Data Model: Almacenes (005)

> El almacén es una **entidad operativa / dato maestro**, no un catálogo plano. A diferencia de
> `sexo` o `motivo_baja` (solo `id + nombre`), un almacén es un **lugar físico real** con clave de
> negocio, clasificación, ubicación, responsable y cupo. Participa como origen/destino de traslados
> (**007**), guarda existencias (**001**) y alimenta reportes de ocupación (**011**).
>
> **Actualizado (ADR 0007):** la clasificación `tipo` dejó de ser un `enum` y es una **FK a
> `catalog_item`** del catálogo `TIPO_ALMACEN`; `municipio`/`estado` dejaron de ser FK y son **texto
> libre**. Migraciones: `V10__create_warehouse.sql` (creación con enum/municipio previos) +
> `V15__generic_catalog.sql` (repunta `tipo`→`tipo_item_id` y `municipio_id`→texto libre).

## Entidad `warehouse`

Hereda de `BaseEntity` (`id` Long IDENTITY, `createdAt`, `updatedAt`). Las referencias a tipo,
responsable (y antes municipio) se guardan **por id** (no asociaciones JPA) para mantener el módulo
desacoplado; la integridad la garantizan las FKs de la migración.

| Campo               | Tipo                     | Null | Notas                                                                 |
|---------------------|--------------------------|------|-----------------------------------------------------------------------|
| **Identidad**       |                          |      |                                                                       |
| `id`                | Long (PK)                | no   | IDENTITY (patrón del proyecto).                                        |
| `codigo`            | string(40) ÚNICO         | no   | Clave de negocio estable (p. ej. `ALM-01`). Para traslados/etiquetas/reportes. |
| `nombre`            | string(120)              | no   | Nombre legible mostrado al usuario.                                    |
| `nombre_normalizado`| string(120) ÚNICO        | no   | trim + colapso de espacios + casefold + sin acentos. Solo para unicidad. |
| **Clasificación**   |                          |      |                                                                       |
| `tipo_item_id`      | FK → `catalog_item`      | no   | Valor del catálogo `TIPO_ALMACEN` (spec **006**, ADR 0007). Antes enum `tipo`. |
| **Ubicación física**|                          |      |                                                                       |
| `municipio`         | string(120)              | sí   | **Texto libre** (ADR 0007). Antes `municipio_id` FK.                  |
| `estado`            | string(120)              | sí   | **Texto libre** (ADR 0007). Añadido en `V15`.                        |
| `direccion`         | string(255)              | sí   | Calle/número/colonia. **Campo sensible** (ubicación de armería).       |
| `cp`                | string(10)               | sí   | Código postal.                                                        |
| `latitud`           | DECIMAL(9,6)             | sí   | **Sensible.** Opcional, desaconsejada salvo necesidad.                |
| `longitud`          | DECIMAL(9,6)             | sí   | **Sensible.** Opcional, desaconsejada salvo necesidad.                |
| **Responsable y contacto** |                   |      |                                                                       |
| `responsable_id`    | FK → `user`              | sí   | Usuario a cargo del almacén. **Campo sensible.**                      |
| `telefono`          | string(30)               | sí   | Contacto **institucional** del almacén (no PII de una persona).        |
| `email_contacto`    | string(255)              | sí   | Contacto **institucional**.                                           |
| **Operativo**       |                          |      |                                                                       |
| `capacidad`         | int                      | sí   | Cupo máximo de fornituras. La **ocupación** se deriva por conteo.      |
| `observaciones`     | string(500)              | sí   |                                                                       |
| **Estado**          |                          |      |                                                                       |
| `active`            | bool                     | no   | Default `true`. Solo activos seleccionables en 001/007.               |

### Campos derivados (NO se almacenan)

- `ocupacion` = `count(equipment activos cuyo warehouse_id = este)` → reporte de existencias (011).
- `porcentaje_ocupacion` = `ocupacion / capacidad` (si `capacidad` no es null).

## Restricciones e índices

- `UNIQUE(codigo)` y `UNIQUE(nombre_normalizado)`.
- Índice por `active` (filtro frecuente del listado) e índice por `tipo_item_id`.
- FK `tipo_item_id → catalog_item(id)` (repunte en `V15`; el valor debe pertenecer a `TIPO_ALMACEN`,
  validado en servicio por `CatalogService.requireActiveItem(..., TIPO_ALMACEN)`).
- FK `responsable_id → users(id)`.
- `municipio`/`estado`: texto libre, sin FK.

## Reglas de integridad

- **No borrado en uso**: un almacén con fornituras (001) o traslados (007) asociados no se elimina;
  solo se **desactiva**. La verificación pasa por el puerto `WarehouseUsageQuery` (impl.
  `DefaultWarehouseUsageQuery`) que cuenta referencias.
- **Unicidad normalizada** de `codigo` y `nombre` (reutiliza el normalizador de `common/text`).
- El `tipo_item_id` debe resolver a un valor **activo** del catálogo `TIPO_ALMACEN`.

## Clasificación de sensibilidad (Principio I)

| Sensibilidad | Campos                                                        | Exposición                         |
|--------------|---------------------------------------------------------------|------------------------------------|
| Pública (operativa) | `id`, `codigo`, `nombre`, `tipo_item_id`, `active`     | Todos los roles autenticados.      |
| Restringida  | `direccion`, `cp`, `latitud`, `longitud`, `responsable_id`, `telefono`, `email_contacto`, `capacidad`, `observaciones`, `municipio`, `estado` | Solo ADMIN/almacén; lectura auditada. |

> No hay PII de elementos. El contacto es **institucional**; el responsable se referencia por
> `user_id`, sin duplicar datos personales. La dirección y geolocalización de la armería son
> sensibles operacionalmente; el RBAC aplica igual.

## DTOs (contrato)

- `WarehouseCreateRequest`: `codigo`, `nombre`, `tipoItemId`, `municipio?`, `estado?`, `direccion?`,
  `cp?`, `latitud?`, `longitud?`, `responsableId?`, `telefono?`, `emailContacto?`, `capacidad?`,
  `observaciones?`. Bean Validation (requeridos, longitudes, rango lat/long, formato de correo).
- `WarehouseSummary` (listado, campos no sensibles): `id`, `codigo`, `nombre`, `tipoItemId`, `active`,
  `ocupacion`.
- `WarehouseDetail` (ficha, incluye sensibles): todo lo anterior + ubicación, responsable, contacto,
  `capacidad`, `porcentajeOcupacion`, timestamps. Restringido a ADMIN/almacén.

## Endpoints

| Método | Ruta                          | Authz             | Notas                                  |
|--------|-------------------------------|-------------------|----------------------------------------|
| GET    | `/warehouses`                 | operativo+        | Paginado, filtro `active`/`tipoItemId`. Devuelve `WarehouseSummary`. |
| GET    | `/warehouses/{id}`            | ADMIN/almacén     | `WarehouseDetail` (campos sensibles).  |
| POST   | `/warehouses`                 | ADMIN/almacén     | 409 si `codigo`/`nombre` duplican.     |
| PUT    | `/warehouses/{id}`            | ADMIN/almacén     | Edición; mismas validaciones.          |
| PATCH  | `/warehouses/{id}/deactivate` | ADMIN/almacén     | Desactiva (no borra). Auditado.        |
