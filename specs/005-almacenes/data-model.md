# Data Model: Almacenes (005)

> El almacén es una **entidad operativa / dato maestro**, no un catálogo plano. A diferencia de
> `sexo` o `motivo_baja` (solo `id + nombre`), un almacén es un **lugar físico real** con clave de
> negocio, clasificación, ubicación, responsable y cupo. Participa como origen/destino de traslados
> (**007**), guarda existencias (**001**) y alimenta reportes de ocupación (**011**).

## Entidad `warehouse`

Hereda de `BaseEntity` (`id` Long IDENTITY, `createdAt`, `updatedAt`).

| Campo               | Tipo                | Null | Notas                                                                 |
|---------------------|---------------------|------|-----------------------------------------------------------------------|
| **Identidad**       |                     |      |                                                                       |
| `id`                | Long (PK)           | no   | IDENTITY (patrón del proyecto).                                        |
| `codigo`            | string(40) ÚNICO    | no   | Clave de negocio estable (p. ej. `ALM-01`). Para traslados/etiquetas/reportes. |
| `nombre`            | string(120)         | no   | Nombre legible mostrado al usuario.                                    |
| `nombre_normalizado`| string(120) ÚNICO   | no   | trim + colapso de espacios + casefold. Solo para unicidad; no se muestra. |
| **Clasificación**   |                     |      |                                                                       |
| `tipo`              | enum                | no   | `CENTRAL` / `REGIONAL` / `MOVIL` / `TEMPORAL`.                         |
| **Ubicación física**|                     |      |                                                                       |
| `municipio_id`      | FK → `municipio`    | sí*  | Reutiliza el catálogo geográfico de `officer` (003). *FK nullable hasta que exista 003. |
| `direccion`         | string(255)         | sí   | Calle/número/colonia. **Campo sensible** (ubicación de armería).       |
| `cp`                | string(10)          | sí   | Código postal.                                                        |
| `latitud`           | DECIMAL(9,6)        | sí   | **Sensible.** Opcional, desaconsejada salvo necesidad.                |
| `longitud`          | DECIMAL(9,6)        | sí   | **Sensible.** Opcional, desaconsejada salvo necesidad.                |
| **Responsable y contacto** |              |      |                                                                       |
| `responsable_id`    | FK → `user`         | sí   | Usuario a cargo del almacén. **Campo sensible.**                      |
| `telefono`          | string(30)          | sí   | Contacto **institucional** del almacén (no PII de una persona).        |
| `email_contacto`    | string(255)         | sí   | Contacto **institucional**.                                           |
| **Operativo**       |                     |      |                                                                       |
| `capacidad`         | int                 | sí   | Cupo máximo de fornituras. La **ocupación** se deriva por conteo.      |
| `observaciones`     | string(500)         | sí   |                                                                       |
| **Estado**          |                     |      |                                                                       |
| `active`            | bool                | no   | Default `true`. Solo activos seleccionables en 001/007.               |

### Campos derivados (NO se almacenan)

- `ocupacion` = `count(equipment activos cuyo warehouse_id = este)` → reporte de existencias (011).
- `porcentaje_ocupacion` = `ocupacion / capacidad` (si `capacidad` no es null).

## Restricciones e índices

- `UNIQUE(codigo)` y `UNIQUE(nombre_normalizado)`.
- Índice por `active` (filtro frecuente del listado).
- FK `municipio_id → municipio(id)` (se añade al existir 003; ver Assumptions de la spec).
- FK `responsable_id → users(id)`.
- `tipo` validado contra el enum en el borde (Bean Validation) y, opcionalmente, `CHECK` en BD.

## Reglas de integridad

- **No borrado en uso**: un almacén con fornituras (001) o traslados (007) asociados no se elimina;
  solo se **desactiva**. La verificación pasa por un puerto `WarehouseUsageQuery` que devuelve 0
  hasta que esas tablas existan (mismo patrón que el plan original).
- **Unicidad normalizada** de `codigo` y `nombre` (reutiliza `NameNormalizer` del módulo
  `equipmenttypes`).

## Clasificación de sensibilidad (Principio I)

| Sensibilidad | Campos                                                        | Exposición                         |
|--------------|---------------------------------------------------------------|------------------------------------|
| Pública (operativa) | `id`, `codigo`, `nombre`, `tipo`, `active`             | Todos los roles autenticados.      |
| Restringida  | `direccion`, `cp`, `latitud`, `longitud`, `responsable_id`, `telefono`, `email_contacto`, `capacidad`, `observaciones` | Solo ADMIN/almacén; lectura auditada. |

> No hay PII de elementos. El contacto es **institucional**; el responsable se referencia por
> `user_id`, sin duplicar datos personales. La dirección y geolocalización de la armería son
> sensibles operacionalmente, no por LFPDPPP — pero el RBAC aplica igual.

## DTOs (contrato)

- `WarehouseCreateRequest`: `codigo`, `nombre`, `tipo`, `municipioId?`, `direccion?`, `cp?`,
  `latitud?`, `longitud?`, `responsableId?`, `telefono?`, `emailContacto?`, `capacidad?`,
  `observaciones?`. Bean Validation (requeridos, longitudes, rango lat/long).
- `WarehouseSummary` (listado, campos no sensibles): `id`, `codigo`, `nombre`, `tipo`, `active`,
  `ocupacion`.
- `WarehouseDetail` (ficha, incluye sensibles): todo lo anterior + ubicación, responsable, contacto,
  `capacidad`, `porcentajeOcupacion`, timestamps. Filtrado por rol.

## Endpoints

| Método | Ruta                          | Authz             | Notas                                  |
|--------|-------------------------------|-------------------|----------------------------------------|
| GET    | `/warehouses`                 | operativo+        | Paginado, filtro `active`/`tipo`. Devuelve `WarehouseSummary`. |
| GET    | `/warehouses/{id}`            | ADMIN/almacén     | `WarehouseDetail` (campos sensibles).  |
| POST   | `/warehouses`                 | ADMIN/almacén     | 409 si `codigo`/`nombre` duplican.     |
| PUT    | `/warehouses/{id}`            | ADMIN/almacén     | Edición; mismas validaciones.          |
| PATCH  | `/warehouses/{id}/deactivate` | ADMIN/almacén     | Desactiva (no borra). Auditado.        |
