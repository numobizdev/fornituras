# Phase 1 — Data model: Asignación de fornituras y resguardos

Refina, para esta feature, la entidad `assignment` de
[`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md). Lee `equipment` (001) y `officers`
(003); no las redefine.

## `assignment` — relación fornitura ↔ elemento en el tiempo

| Columna           | Tipo            | Notas                                              |
|-------------------|-----------------|----------------------------------------------------|
| id                | (PK BaseEntity)  | Identificador interno.                             |
| equipment_id      | FK → equipment   | Fornitura asignada (001).                          |
| officer_id        | FK → officer     | Elemento resguardatario (003).                     |
| fecha_asignacion  | DATETIME2        | Inicio del resguardo.                              |
| fecha_devolucion  | DATETIME2 (null) | **NULL = vigente**; con valor = histórico.         |
| asignado_por      | FK → users       | Usuario que asignó (auditoría).                    |
| recibido_por      | FK → users (null)| Usuario que registró la devolución.               |
| firma_url         | NVARCHAR (null)  | Referencia a la firma electrónica, si se capturó.  |
| observaciones     | NVARCHAR (null)  | Entrega/recepción.                                 |
| (auditoría base)  | created/updated  | De `BaseEntity`.                                   |

**Índice de integridad (concurrencia, ver research §2):**

```sql
CREATE UNIQUE INDEX ux_assignment_vigente
  ON assignment(equipment_id) WHERE fecha_devolucion IS NULL;
```

Garantiza **una sola asignación vigente por fornitura**. Índices adicionales por `officer_id` y
`fecha_asignacion` para historial y listados.

## Lecturas de otras features (no se crean aquí)

- **`equipment`** (001): se consulta por `codigo_qr` (`GET /equipment/by-codigo/{codigo}`) para el
  paso 1; debe exponer `status` (solo `disponible` es asignable) y datos de descripción.
- **`codigo_qr`** (módulo `qrcodes`, existente): el código `FOR-XXXXX` escaneado resuelve a
  `equipment` vía la FK que 001 define (`equipment.codigo_qr` → `codigo_qr.codigo`).
- **`officers`** (003): búsqueda por nombre/placa/CURP/RFC con enmascaramiento por rol (paso 2).

## Transiciones de estado de la fornitura (efecto de esta feature)

- `disponible` → `asignada` al crear asignación vigente.
- `asignada` → `disponible` al registrar devolución (salvo que pase a otro estado por incidencia).
- Reasignar = cerrar vigente + abrir nueva (transacción única), la fornitura sigue `asignada`.
- Bloqueos: una fornitura `en_traslado`, `mantenimiento`, `baja` **no** es asignable.

## Resguardo

No es tabla obligatoria: se genera el **PDF al vuelo** desde la asignación. Si se requiere
trazabilidad del documento emitido, persistir metadatos (`assignment_id`, `emitido_en`,
`con_firma`) — decisión de implementación.

## Reglas de validación (en el borde)

- `equipment_id` debe existir y estar **disponible**; si no, rechazo (409/422).
- `officer_id` debe existir y estar activo.
- No permitir crear una asignación si ya hay vigente para esa fornitura (lo refuerza el índice).
- Devolución solo sobre una asignación **vigente**.
