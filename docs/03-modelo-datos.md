# Modelo de datos (borrador)

> Borrador inicial para guiar el diseño. **No** es el esquema final; se refinará y se
> versionará con migraciones (Flyway/Liquibase) cuando empiece el backend. Las columnas con
> PII deben considerarse para **Always Encrypted** (ver `02-seguridad.md`).
>
> **Actualizado 2026-06-30** para el alcance SIGEFOR (`Requerimientos.MD`): "equipo" →
> **fornitura**, catálogos (tipo, talla, almacén), traslados, incidencias, bajas. Ver `specs/`.
>
> **Actualizado 2026-06-30 (spec 001, T043)**: la tabla `equipment` refleja ya el esquema
> **implementado** en la migración `V11__create_equipment.sql` (y sus FKs repuntadas por la
> reestructuración de catálogos, ADR 0007 / `V15`). Los catálogos `equipment_type`/`size` y el
> enum `warehouse.tipo` fueron **sustituidos** por el modelo genérico `catalog` + `catalog_item`.

## Entidades principales

### Fornitura (`equipment`) — spec 001 — **IMPLEMENTADO** (`V11`, FKs repuntadas en `V15`)
La prenda de dotación controlada. "Fornitura" es un **tipo de prenda** concreto (catálogo
`TIPO_PRENDA`), no una categoría con subtipos. Refleja la entidad `Equipment` y la migración `V11`.

| Campo             | Tipo (BD)              | Notas                                                        |
|-------------------|------------------------|-------------------------------------------------------------|
| id                | BIGINT IDENTITY (PK)   | Identificador interno opaco e inmutable (`BaseEntity`). No es UUID. |
| codigo_qr         | NVARCHAR(60)           | Código físico (QR/serie) tal cual se muestra; recortado y en mayúsculas. |
| codigo_normalizado| NVARCHAR(60) **único** | Forma normalizada (sin espacios/guiones, mayúsculas); garantiza unicidad física. |
| equipment_type_id | FK → catalog_item      | **Tipo de prenda** (catálogo `TIPO_PRENDA`; hoy único valor "Fornitura"). Antes `equipment_type`; repuntada en `V15`. |
| size_id           | FK → catalog_item (null)| Talla (catálogo `TALLA`, opcionalmente colgada del tipo). Antes `size`; repuntada en `V15`. |
| warehouse_id      | FK → warehouse         | Almacén actual.                                             |
| status            | NVARCHAR(20) + CHECK   | `DISPONIBLE`, `ASIGNADA`, `EN_MANTENIMIENTO`, `EN_TRASLADO`, `EXTRAVIADA`, `BAJA_DEFINITIVA` (enum `EquipmentStatus`). Default `DISPONIBLE`. |
| descripcion       | NVARCHAR(255) (null)   |                                                             |
| marca             | NVARCHAR(120) (null)   |                                                             |
| modelo            | NVARCHAR(120) (null)   |                                                             |
| nivel_balistico   | NVARCHAR(60) (null)    | p. ej. nivel balístico (NIJ).                               |
| numero_inventario | NVARCHAR(60) (null)    | Número de inventario institucional.                        |
| fecha_fabricacion | DATE (null)            |                                                             |
| fecha_adquisicion | DATE (null)            |                                                             |
| vida_util_meses   | INT (null)             | Duración; alimenta el cálculo de vencimiento.              |
| fecha_vencimiento | DATE (null)            | **Canónico** para alertas/vigencia (`= fecha_fabricacion + vida_util_meses`; ver spec 001). |
| observaciones     | NVARCHAR(500) (null)   |                                                             |
| foto_url          | NVARCHAR(500) (null)   | Foto de la fornitura (no PII).                             |
| created_at / updated_at | DATETIME2        | Timestamps de auditoría (`BaseEntity`).                    |

> **Índices**: `codigo_normalizado` (único), `status`, `equipment_type_id`, `warehouse_id`,
> `fecha_vencimiento`.
>
> **Estados de vigencia** (próxima a vencer ≤ 90 días / caducada) se **derivan** de
> `fecha_vencimiento` (`ExpiryCalculator` → `ExpiryStatus`); no son columnas de `status`.
>
> **Nota de identidad**: el `id` es interno; el código físico opaco `FOR-XXXXX` vive en
> `codigo_qr`/`codigo_normalizado` y se resuelve server-side (`GET /equipment/by-codigo/{codigo}`).
> No existe columna `serial_number` separada.

### QR — `lote_qr` y `codigo_qr` (**IMPLEMENTADO**, ver ADR 0005)
Los códigos QR se generan **por lotes** e independientes de la fornitura; el enlace ocurre al dar
de alta/asignar (la fornitura guarda el `codigo`). El código es opaco `FOR-XXXXX`, **sin firma**.

**`lote_qr`**: `id`, `cantidad`, `descripcion`, `qr_size_cm` (DECIMAL), `padding_cm` (DECIMAL),
`label_position` (NONE/TOP/BOTTOM), `mostrar_bordes` (bool), timestamps.

**`codigo_qr`**: `id`, `codigo` (`FOR-XXXXX`, **único**), `lote_qr_id` (FK → `lote_qr`),
timestamps. Índice por `lote_qr_id`.

> El ADR 0002 (UUID + HMAC) fue **reemplazado** por el 0005; ya no aplican columnas
> `qr_opaque_id`/`qr_key_version`.

### Elemento (`officer`) — **contiene PII (alta sensibilidad)**
El personal policial.

| Campo            | Tipo            | Notas                                              |
|------------------|-----------------|----------------------------------------------------|
| id               | UUID (PK)        |                                                    |
| placa            | string (único)   | Identificador (placa/serie). **PII**               |
| nombre           | string           | **PII** — Always Encrypted.                        |
| apellido_paterno | string           | **PII** — Always Encrypted.                        |
| apellido_materno | string (null)    | **PII** — Always Encrypted.                        |
| sexo_id          | FK → sexo        | Catálogo.                                          |
| tipo_sangre_id   | FK → tipo_sangre | Catálogo. **PII sensible.**                        |
| municipio        | string (null)    | **Texto libre** (`NVARCHAR(120)`). Antes `municipio_id` FK; retirado en `V15` (ADR 0007). |
| estado           | string (null)    | **Texto libre** (`NVARCHAR(120)`); añadido en `V15`.  |
| curp             | string (null)    | **PII — [PENDIENTE ADR 0003]** Always Encrypted.   |
| rfc              | string (null)    | **PII — [PENDIENTE ADR 0003]** Always Encrypted.   |
| foto_url         | string (null)    | **PII biométrica indirecta** — storage cifrado.    |
| adscripcion      | string (null)    | Unidad/área.                                       |
| status           | enum             | Activo, baja, etc.                                 |

> **Minimización (Principio I).** `curp`, `rfc` y `foto` están sujetos a la decisión abierta de
> alcance de PII (spec **003** + ADR pendiente `0003-pii-elementos`). Mientras no exista el ADR,
> se contemplan en el esquema pero su captura permanece restringida. Guardar solo lo necesario
> para la finalidad declarada.

### Catálogos — **modelo genérico `catalog` + `catalog_item`** (ADR 0007, `V15`)
La reestructuración de catálogos (ADR 0007) sustituyó las tablas tipadas `equipment_type` y `size`
(y el enum `warehouse.tipo`) por un par genérico. Cada **`catalog`** es una lista controlada; cada
**`catalog_item`** es un valor de esa lista, con jerarquía opcional (`parent_item_id`) para modelar
p. ej. tallas colgadas de un tipo.

- **`catalog`**: `id` (PK), `code` (único, p. ej. `TIPO_PRENDA`/`TALLA`/`TIPO_ALMACEN`), `nombre`,
  `descripcion`, `is_system` (bit), `active` (bit), timestamps.
- **`catalog_item`**: `id` (PK), `catalog_id` (FK → `catalog`), `code` (null), `nombre`,
  `nombre_normalizado`, `descripcion`, `foto_url`, `parent_item_id` (FK → `catalog_item`, null),
  `orden`, `active` (bit), timestamps. **Único** `nombre_normalizado` por catálogo (distinguiendo por
  padre para permitir el mismo valor —p. ej. talla "M"— bajo tipos distintos).

Catálogos semilla del sistema (`is_system = 1`): `TIPO_PRENDA` (**tipo de prenda**; semilla con el
único valor "Fornitura" — una fornitura es un tipo de prenda, no una categoría con subtipos),
`TALLA` (tallas, opcionalmente ligadas a un tipo de prenda vía `parent_item_id`), `TIPO_ALMACEN`
(CENTRAL/REGIONAL/MOVIL/TEMPORAL). Las FKs de `equipment` (tipo de prenda/talla) y `warehouse` (tipo)
apuntan a `catalog_item`.

> **Deuda de sincronización (código).** `V15` y el backend aún usan el `code` `TIPO_FORNITURA` con
> semilla de subtipos (chaleco/cinturón/casco). Pendiente: migración que renombre el catálogo a
> `TIPO_PRENDA` y reemplace la semilla por el único valor "Fornitura" (ADR 0007, spec 006).

Catálogos aún **planos** (fuera del modelo genérico, pendientes de valorar su migración):

- **`sexo`**, **`tipo_sangre`**: `id`, `nombre` (usados por `officer`).
- **`motivo_baja`**: `id`, `nombre` (spec 009).

> **`municipio` dejó de ser catálogo.** Con ADR 0007 se retiró la tabla `municipio` y su FK; ahora
> `warehouse` y `officer` guardan `municipio`/`estado` como **texto libre** (`NVARCHAR(120)`).
>
> **`warehouse` (almacén) NO es un catálogo.** Es una **entidad operativa** (lugar físico con
> responsable y cupo); ver su tabla propia abajo y `specs/005-almacenes/data-model.md`.

### Almacén (`warehouse`) — spec 005 — **entidad operativa (no catálogo)**
Ubicación física de resguardo de fornituras. Origen/destino de traslados (007), guarda existencias
(001) y alimenta reportes de ocupación (011). Sin PII de elementos, pero ubicación/responsable de una
armería es **sensible** (RBAC por campo).

| Campo            | Tipo               | Notas                                                |
|------------------|--------------------|------------------------------------------------------|
| id               | Long (PK)          | IDENTITY.                                             |
| codigo           | string (único)     | Clave de negocio estable (`ALM-01`); traslados/etiquetas. |
| nombre           | string             | Legible (con `nombre_normalizado` único).            |
| tipo_item_id     | FK → catalog_item  | Tipo de almacén (catálogo `TIPO_ALMACEN`). Antes enum `tipo`; migrado en `V15` (ADR 0007). |
| municipio        | string (null)      | **Texto libre** (`NVARCHAR(120)`). Antes `municipio_id` FK; retirado en `V15`. |
| estado           | string (null)      | **Texto libre** (`NVARCHAR(120)`); añadido en `V15`. |
| direccion        | string (null)      | **Sensible** (ubicación de armería).                 |
| cp               | string (null)      |                                                      |
| latitud/longitud | decimal (null)     | **Sensible**; opcional, desaconsejada salvo necesidad. |
| responsable_id   | FK → user (null)   | Usuario a cargo. **Sensible.**                       |
| telefono         | string (null)      | Contacto **institucional** (no PII).                 |
| email_contacto   | string (null)      | Contacto **institucional**.                          |
| capacidad        | int (null)         | Cupo; la ocupación se **deriva** por conteo.         |
| observaciones    | string (null)      |                                                      |
| active           | bool               | Solo activos seleccionables en 001/007.              |

### Traslado (`transfer`) — spec 007
Movimiento de fornituras entre almacenes.

| Campo            | Tipo            | Notas                                              |
|------------------|-----------------|----------------------------------------------------|
| id               | UUID (PK)        |                                                    |
| origen_id        | FK → warehouse   |                                                    |
| destino_id       | FK → warehouse   |                                                    |
| status           | enum             | enviado, recibido, cancelado.                      |
| fecha_envio      | datetime         |                                                    |
| fecha_recepcion  | datetime (null)  |                                                    |
| creado_por       | FK → user        |                                                    |
| recibido_por     | FK → user (null) |                                                    |

- **`transfer_item`**: `id`, `transfer_id` (FK), `equipment_id` (FK).

### Incidencia (`incident`) — spec 008
Problema reportado sobre una fornitura.

| Campo            | Tipo            | Notas                                              |
|------------------|-----------------|----------------------------------------------------|
| id               | UUID (PK)        |                                                    |
| equipment_id     | FK → equipment   |                                                    |
| tipo             | enum             | daño, falla, extravío, mantenimiento.              |
| problema         | string           | Descripción.                                       |
| status           | enum             | abierta, en_proceso, resuelta, cerrada.            |
| fecha_reportada  | datetime         |                                                    |
| fecha_resolucion | datetime (null)  |                                                    |
| reportado_por    | FK → user        |                                                    |

### Baja (`decommission`) — spec 009
Baja definitiva de una fornitura.

| Campo            | Tipo            | Notas                                              |
|------------------|-----------------|----------------------------------------------------|
| id               | UUID (PK)        |                                                    |
| equipment_id     | FK → equipment   |                                                    |
| motivo_id        | FK → motivo_baja |                                                    |
| fecha            | datetime         |                                                    |
| responsable_id   | FK → user        |                                                    |
| observaciones    | string (null)    |                                                    |

### Asignación (`assignment`) — spec 004
Relación fornitura ↔ elemento en el tiempo (historial / resguardo).

| Campo           | Tipo            | Notas                                               |
|-----------------|-----------------|-----------------------------------------------------|
| id              | UUID (PK)        |                                                     |
| equipment_id    | UUID (FK)        | → equipment.id                                      |
| officer_id      | UUID (FK)        | → officer.id                                        |
| fecha_asignacion| datetime         |                                                     |
| fecha_devolucion| datetime (null)  | Null mientras esté asignado (vigente).              |
| asignado_por    | UUID (FK)        | → user.id (auditoría).                              |
| recibido_por    | UUID (FK, null)  | → user.id (devolución).                             |
| firma_url       | string (null)    | Firma electrónica del resguardo, si aplica.         |

### Usuario del sistema (`users`) — spec 013 — **IMPLEMENTADO**
Quien opera la aplicación (no confundir con `officer`). Refleja la entidad `User` real del
backend `fornituras-api/`.

| Campo         | Tipo            | Notas                                                  |
|---------------|-----------------|--------------------------------------------------------|
| id            | (BaseEntity PK)  | Identificador interno.                                 |
| name          | string(100)      | Nombre del usuario.                                    |
| email         | string(255) único| **Identidad de login.**                               |
| password      | string(255)      | Hash — nunca texto plano.                              |
| role          | enum (`Role`)    | **`ADMIN` / `CAPTURISTA`** (implementados). Expansión a SUPERVISOR/ALMACEN/AUDITOR pendiente de ADR. |
| enabled       | bool             | Activo / inactivo.                                     |

> **MFA** (`mfa_enabled`) y la expansión de roles son objetivos de la spec 013, **no**
> implementados aún. La sesión es **JWT** (`token`, `tokenType`, `expiresIn`); la recuperación
> de contraseña es por **código**.

### Auditoría (`audit_log`)
Quién accedió/modificó qué y cuándo.

| Campo       | Tipo      | Notas                                       |
|-------------|-----------|---------------------------------------------|
| id          | UUID (PK) |                                             |
| user_id     | UUID (FK) | Actor.                                      |
| accion      | string    | LOGIN, VIEW_OFFICER, ASSIGN, TRANSFER, EXPORT, etc. |
| entidad     | string    | Tipo de entidad afectada.                   |
| entidad_id  | UUID      | Sin incluir PII en el log.                  |
| ip          | string    | Dirección IP del actor.                     |
| evidencia   | string/json | Diff/evidencia del cambio (sin PII en claro). |
| timestamp   | datetime  |                                             |

## Relaciones

- `equipment` N—1 `catalog_item` (tipo de prenda `TIPO_PRENDA` y talla `TALLA`), `warehouse` (entidad operativa).
- `warehouse` N—1 `catalog_item` (tipo `TIPO_ALMACEN`); `municipio`/`estado` son texto libre; `warehouse` N—1 `user` (responsable).
- `catalog` 1—N `catalog_item`; `catalog_item` 1—N `catalog_item` (jerarquía `parent_item_id`, p. ej. talla→tipo).
- `equipment` 1—N `assignment` N—1 `officer` (historial de asignaciones / resguardos).
- `equipment` 1—N `incident`; `equipment` 1—N `decommission` (baja definitiva).
- `transfer` 1—N `transfer_item` N—1 `equipment`; `transfer` N—1 `warehouse` (origen/destino).
- `officer` N—1 `sexo`, `tipo_sangre` (catálogos); `municipio`/`estado` son texto libre.
- `assignment.asignado_por` / `recibido_por` → `user` (trazabilidad).
- `audit_log.user_id` → `user`.

## Pendientes

- **Alcance de PII del elemento** (CURP/RFC/foto): decisión abierta → ADR `0003-pii-elementos`
  (ver spec **003**).
- Definir qué columnas exactas usan Always Encrypted (mínimo: nombre/apellidos, CURP, RFC, tipo
  de sangre; foto en storage cifrado).
- ~~Talla: ¿catálogo global o por tipo de fornitura?~~ **Resuelto (ADR 0007)**: `catalog_item`
  `TALLA` con `parent_item_id` opcional → talla global (sin padre) o colgada de un tipo.
- Inmutabilidad y retención de `audit_log` (ISO 27001) → ADR (ver spec **012**).
- Política de retención y baja de datos (derechos ARCO).
