# Modelo de datos (borrador)

> Borrador inicial para guiar el diseño. **No** es el esquema final; se refinará y se
> versionará con migraciones (Flyway/Liquibase) cuando empiece el backend. Las columnas con
> PII deben considerarse para **Always Encrypted** (ver `02-seguridad.md`).
>
> **Actualizado 2026-06-30** para el alcance SIGEFOR (`Requerimientos.MD`): "equipo" →
> **fornitura**, catálogos (tipo, talla, almacén), traslados, incidencias, bajas. Ver `specs/`.

## Entidades principales

### Fornitura (`equipment`)
El equipo de dotación controlado (chaleco antibala, cinturón táctico, casco, etc.).

| Campo            | Tipo            | Notas                                              |
|------------------|-----------------|----------------------------------------------------|
| id               | UUID (PK)        | Identificador interno.                             |
| serial_number    | string (único)   | Número de serie/QR único de la fornitura.          |
| codigo_qr        | string (FK/único)| Código `FOR-XXXXX` escaneado (→ `codigo_qr.codigo`); se liga en el alta. |
| numero_inventario| string           | Número de inventario institucional.                |
| type_id          | FK → equipment_type | Tipo (cinturón, chaleco, casco…).               |
| size_id          | FK → size (null) | Talla (catálogo).                                  |
| warehouse_id     | FK → warehouse   | Almacén actual.                                    |
| marca            | string (null)    |                                                    |
| modelo           | string (null)    |                                                    |
| nivel_proteccion | string (null)    | p. ej. nivel balístico (NIJ).                      |
| status           | enum             | disponible, asignada, mantenimiento, en_traslado, extraviada, baja. |
| fecha_fabricacion| date (null)      |                                                    |
| fecha_adquisicion| date (null)      |                                                    |
| vida_util_meses  | int (null)       | Duración; alimenta el cálculo de vencimiento.      |
| fecha_vencimiento| date (null)      | **Canónico** para alertas/vigencia (ver spec 001). |
| fecha_alta       | datetime         |                                                    |
| ubicacion        | string (null)    | Ubicación dentro del almacén.                      |
| observaciones    | string (null)    |                                                    |
| foto_url         | string (null)    | Foto de la fornitura (no PII).                     |

> **Estados de vigencia** (próxima a vencer ≤ 90 días / caducada) se **derivan** de
> `fecha_vencimiento`; no son columnas de `status`.

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
| municipio_id     | FK → municipio   | Catálogo.                                          |
| curp             | string (null)    | **PII — [PENDIENTE ADR 0003]** Always Encrypted.   |
| rfc              | string (null)    | **PII — [PENDIENTE ADR 0003]** Always Encrypted.   |
| foto_url         | string (null)    | **PII biométrica indirecta** — storage cifrado.    |
| adscripcion      | string (null)    | Unidad/área.                                       |
| status           | enum             | Activo, baja, etc.                                 |

> **Minimización (Principio I).** `curp`, `rfc` y `foto` están sujetos a la decisión abierta de
> alcance de PII (spec **003** + ADR pendiente `0003-pii-elementos`). Mientras no exista el ADR,
> se contemplan en el esquema pero su captura permanece restringida. Guardar solo lo necesario
> para la finalidad declarada.

### Catálogos
Tablas de catálogo controladas (activo/inactivo, no se eliminan si están en uso):

- **`equipment_type`** (spec 006): `id`, `nombre` (único), `descripcion`, `foto_url`, `activo`.
- **`size`** (talla): `id`, `nombre`, `equipment_type_id` (null = global), `activo`.
- **`sexo`**, **`tipo_sangre`**, **`municipio`**: `id`, `nombre`.
- **`motivo_baja`**: `id`, `nombre`.

> **`warehouse` (almacén) NO es un catálogo plano.** Es una **entidad operativa** (lugar físico con
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
| tipo             | enum               | CENTRAL, REGIONAL, MOVIL, TEMPORAL.                  |
| municipio_id     | FK → municipio (null) | Reutiliza el catálogo geográfico de `officer`.    |
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

- `equipment` N—1 `equipment_type`, `size` (catálogos), `warehouse` (entidad operativa).
- `warehouse` N—1 `municipio` (catálogo); `warehouse` N—1 `user` (responsable).
- `equipment` 1—N `assignment` N—1 `officer` (historial de asignaciones / resguardos).
- `equipment` 1—N `incident`; `equipment` 1—N `decommission` (baja definitiva).
- `transfer` 1—N `transfer_item` N—1 `equipment`; `transfer` N—1 `warehouse` (origen/destino).
- `officer` N—1 `sexo`, `tipo_sangre`, `municipio` (catálogos).
- `assignment.asignado_por` / `recibido_por` → `user` (trazabilidad).
- `audit_log.user_id` → `user`.

## Pendientes

- **Alcance de PII del elemento** (CURP/RFC/foto): decisión abierta → ADR `0003-pii-elementos`
  (ver spec **003**).
- Definir qué columnas exactas usan Always Encrypted (mínimo: nombre/apellidos, CURP, RFC, tipo
  de sangre; foto en storage cifrado).
- Talla: ¿catálogo global o por tipo de fornitura? (ver spec 006).
- Inmutabilidad y retención de `audit_log` (ISO 27001) → ADR (ver spec **012**).
- Política de retención y baja de datos (derechos ARCO).
