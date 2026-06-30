# Implementation Plan: Traslados entre almacenes

**Branch**: `dev` (feature **007-traslados**) | **Date**: 2026-06-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/007-traslados/spec.md`

## Summary

Implementar el **movimiento de fornituras entre almacenes**: crear un traslado origen→destino,
agregar fornituras por código QR (lector/cámara/manual), enviarlo (las fornituras pasan a "en
traslado") y recibirlo en destino (vuelven a "disponible" bajo el almacén destino). Incluye consulta
paginada con filtros y cancelación (revierte al origen). El eje es la **consistencia de estado**: una
fornitura "en traslado" no puede asignarse ni darse de baja, y solo se agregan fornituras
**disponibles** ubicadas en el almacén origen.

Enfoque: módulo backend `transfers` en Spring Boot, migración Flyway con `transfer` + `transfer_item`,
y feature `traslados` en el frontend `sigefor/`. Reusa el componente de captura (**014**), el módulo
`equipment` (**001**, estado/ubicación) y `warehouses` (**005**).

## Technical Context

**Language/Version**: Java 25 (backend `fornituras-api/`); TypeScript + Angular/Ionic 8 (`sigefor/`).

**Primary Dependencies**: Spring Boot (Web MVC, Security, Validation, Data JPA), Flyway
(`flyway-sqlserver`), `mssql-jdbc`. Captura de QR vía componente **014**. Frontend: servicios HTTP +
componentes Ionic.

**Storage**: SQL Server 2022. Tablas `transfer` (origen, destino, estado, fechas, creado_por,
recibido_por) y `transfer_item` (transfer ↔ equipment). Lee/actualiza `equipment` (estado y almacén).

**Testing**: JUnit 5 + Spring Boot Test; Testcontainers (MSSQL) para integración (transición de estados,
bloqueo de asignación de fornituras "en traslado", cancelación); contrato; autorización; auditoría.
Frontend: pruebas de servicio + del flujo de creación.

**Target Platform**: API REST en contenedor Linux; cliente Ionic (web + móvil con cámara Capacitor).

**Project Type**: Web — monorepo `fornituras-api/` + `sigefor/`.

**Performance Goals**: traslado de 10 fornituras en < 3 min (SC-001); listado paginado < 2 s.

**Constraints**: solo fornituras **disponibles** en el almacén origen pueden agregarse (FR-003); "en
traslado" bloquea asignación/baja (FR-006, SC-002); trazabilidad envío/recepción (SC-003); operaciones
autorizadas y auditadas (FR-007).

**Scale/Scope**: 3 pantallas/flujos (listado, nuevo traslado, recibir); volumen moderado.

## Constitution Check

*GATE: debe pasar antes de Phase 0 y re-verificarse tras Phase 1.*

| Principio | Cómo lo cumple este plan | Estado |
|-----------|--------------------------|--------|
| I. Seguridad/privacidad primero | El traslado no maneja PII (solo fornituras y almacenes) | ✅ (N/A PII) |
| II. QR sin PII / resolución server-side | Las fornituras se agregan resolviendo el código en servidor (001) | ✅ |
| III. Cero secretos | Sin secretos nuevos | ✅ |
| IV. Mínimo privilegio | Crear/recibir/cancelar por rol (ALMACEN/SUPERVISOR/ADMIN); consulta a roles operativos | ✅ |
| V. Auditoría sin fugas | Creación/recepción/cancelación auditadas (actor, traslado, fornituras por id) | ✅ |
| VI. ADR / stack congelado | Sin cambios de stack; recepción parcial diferida (Assumptions) → ADR si se adopta | ✅ |

**Resultado del gate**: PASA. Dependencia de 001/005/014 = **orden de implementación**, no violación.

## Project Structure

### Documentation (this feature)

```text
specs/007-traslados/
├── plan.md              # Este archivo
└── tasks.md             # Phase 2: lo genera /speckit-tasks
```

> Diseño de datos y contrato inline. La recepción parcial (mejora posible) se eleva a ADR si se adopta.

### Source Code (repository root)

```text
fornituras-api/
└── src/
    ├── main/java/com/numobiz/solutions/fornituras/modules/transfers/
    │   ├── controller/     # TransferController (listar, crear, recibir, cancelar)
    │   ├── service/        # TransferService (transición de estados, validación de origen/disponibilidad)
    │   ├── repository/     # TransferRepository, TransferItemRepository
    │   ├── entity/         # Transfer, TransferItem
    │   ├── dto/            # TransferCreateRequest, TransferSummary, TransferDetail
    │   └── mapper/
    ├── main/resources/db/migration/   # V{n}__create_transfer.sql (Flyway)
    └── test/java/.../modules/transfers/

sigefor/
└── src/app/features/traslados/
    ├── pages/traslados/         # listado paginado + filtros
    ├── pages/traslado-form/     # nuevo traslado (origen/destino + agregar por QR)
    └── data/transfers.service.ts
```

**Structure Decision**: módulo backend `transfers/` siguiendo el patrón existente. El cambio de estado
de la fornitura ("disponible"↔"en traslado") se hace a través del servicio de **001** (o un puerto a
él) para no duplicar reglas de estado. La captura de fornituras reusa el componente **014**.

## Phase 0 — Research

Decisiones inline:
- **Transición de estados**: crear traslado → fornituras "en traslado"; recibir → "disponible" en
  destino + actualizar `warehouse_id`; cancelar → revertir a "disponible" en origen. Todo transaccional.
- **Validación de agregado**: solo `equipment` con estado "disponible" y `warehouse_id == origen`
  (FR-003); el resto se bloquea con motivo.
- **Bloqueo cruzado**: una fornitura "en traslado" no es asignable/baja → lo garantiza 001 vía el
  catálogo de estado y el puerto `EquipmentLifecycleQuery`.
- **Recepción parcial**: por defecto recepción completa; parcial = mejora futura (ADR si se adopta).

## Phase 1 — Design & Contracts

- **Data model** (inline): `transfer(id, origen_id, destino_id, estado[enviado/recibido/cancelado],
  fecha_envio, fecha_recepcion, creado_por, recibido_por)`; `transfer_item(id, transfer_id, equipment_id)`.
- **Contract** (inline): `GET /transfers` (paginado + filtros origen/destino/estado),
  `POST /transfers` (crear → "enviado"), `POST /transfers/{id}/receive` (recibir → "recibido"),
  `POST /transfers/{id}/cancel` (cancelar → revertir). Todos con authn + authz por rol.
- **Quickstart** (inline): crear traslado con 2 fornituras → "en traslado"; intentar asignar una →
  bloqueado; recibir → "disponible" en destino; cancelar otro → revierte a origen.

Re-check Constitution tras diseño: sin PII; transiciones consistentes; resolución de código server-side.
**Gate sigue en PASA.**

## Complexity Tracking

> Sin violaciones. Las dependencias (001/005/014) son orden de implementación; la recepción parcial se
> mantiene fuera del MVP para no añadir complejidad.
