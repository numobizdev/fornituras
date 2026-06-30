# Implementation Plan: Inventario de fornituras

**Branch**: `dev` (feature **001-inventario-equipos**) | **Date**: 2026-06-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-inventario-equipos/spec.md`

## Summary

Construir el **inventario de fornituras**: alta individual y por lote, consulta paginada con filtros,
edición y cambio de estado operativo, con **estados de vigencia derivados** de `fecha_vencimiento`.
Es el cimiento de SIGEFOR: sin fornitura no hay nada que asignar, trasladar o dar de baja. La
fornitura no contiene PII propia, pero **liga un código QR opaco** (`FOR-XXXXX`, módulo `qrcodes`,
ADR 0005) y debe proteger la consistencia con asignaciones (004) y traslados (007).

El reto central no es criptográfico sino de **integridad de estado**: un identificador único
inmutable + número de serie/QR único normalizado; un catálogo controlado de estados operativos; y la
regla de que no se da de baja/traslada una fornitura con asignación vigente. La vigencia (próxima a
vencer ≤ 90 días / caducada) se **deriva**, no se captura.

Enfoque: módulo backend `equipment` en Spring Boot, migración Flyway con `equipment` + estados, y la
feature `fornituras` (ya andamiada) en el frontend `sigefor/`, sobre tipos (006), tallas y almacenes
(005) ya existentes y el módulo `qrcodes` ya implementado.

## Technical Context

**Language/Version**: Java 25 (backend `fornituras-api/`); TypeScript + Angular/Ionic 8 (`sigefor/`).

**Primary Dependencies**: Spring Boot (Web MVC, Security, Validation, Data JPA), Flyway
(`flyway-sqlserver`), `mssql-jdbc`. Reusa el módulo `qrcodes` (resolución de existencia de `codigo`).
Captura de QR vía componente de **014-escaneo-qr**. Frontend: servicios HTTP + componentes Ionic.

**Storage**: SQL Server 2022. Tabla `equipment` (id interno opaco; `codigo_qr`/`numero_serie` único
normalizado; FK a `equipment_type`, `size`, `warehouse`; `fecha_fabricacion`, `vida_util_meses`,
`fecha_vencimiento` derivada y persistida; `status` operativo; descriptivos; `foto_url`). Catálogo
`equipment_status`. Sin Always Encrypted (la fornitura no es PII; la PII del elemento vive en 003).

**Testing**: JUnit 5 + Spring Boot Test; Testcontainers (MSSQL) para integración, migración y
**concurrencia de unicidad**; contrato; autorización; pruebas de derivación de vigencia. Frontend:
pruebas de servicio + del flujo de alta por lote.

**Target Platform**: API REST en contenedor Linux; cliente Ionic (web + móvil con cámara Capacitor).

**Project Type**: Web — monorepo `fornituras-api/` + `sigefor/`.

**Performance Goals**: alta individual < 1 min; lote de 50 < 10 min (SC-001); primera página del
listado < 2 s con decenas de miles de fornituras (SC-004).

**Constraints**: cero duplicados de identificador único (normalizado) — incluso bajo concurrencia
(SC-002); paginación del lado servidor; cambio de número de serie/QR restringido y auditado; baja/
traslado bloqueados con asignación vigente (FR-008); sin PII del elemento en fichas de fornitura
(FR-011); toda alta/edición/baja auditada (Principio V).

**Scale/Scope**: decenas de miles de fornituras; 3 pantallas (listado + alta individual/lote + ficha/
edición).

## Constitution Check

*GATE: debe pasar antes de Phase 0 y re-verificarse tras Phase 1.*

| Principio | Cómo lo cumple este plan | Estado |
|-----------|--------------------------|--------|
| I. Seguridad/privacidad primero | La fornitura no guarda PII; las fichas no exponen datos del elemento (eso vive en 003/004 con enmascaramiento) | ✅ |
| II. QR sin PII / resolución server-side | Liga un `codigo` opaco (ADR 0005); la resolución `codigo → fornitura` ocurre en servidor tras authn+authz | ✅ |
| III. Cero secretos | Sin secretos nuevos | ✅ |
| IV. Mínimo privilegio | Alta/edición/baja por rol (ADMIN/SUPERVISOR/ALMACEN); consulta a roles operativos; rechazo por defecto | ✅ |
| V. Auditoría sin fugas | Alta/edición/cambio de estado/baja auditados (actor, fornitura, cuándo) sin PII | ✅ |
| VI. ADR / stack congelado | Sin cambios de stack; QR según ADR 0005; lib PDF reutilizada de `qrcodes` si se imprime | ✅ |

**Resultado del gate**: PASA. La única "violación" aparente es depender de catálogos (005/006) y del
componente de captura (014); es **orden de implementación**, no violación de principio.

## Project Structure

### Documentation (this feature)

```text
specs/001-inventario-equipos/
├── plan.md              # Este archivo
└── tasks.md             # Phase 2: lo genera /speckit-tasks
```

> Feature central pero sin incógnitas criptográficas (a diferencia de 003): el diseño de datos y
> contrato se describe inline. Si surge una decisión de modelado relevante (p. ej. derivación de
> vigencia materializada vs calculada), se eleva a ADR.

### Source Code (repository root)

```text
fornituras-api/
└── src/
    ├── main/java/com/numobiz/solutions/fornituras/modules/equipment/
    │   ├── controller/     # EquipmentController (listado, alta, lote, ficha, edición, estado)
    │   ├── service/        # EquipmentService (normalización, unicidad, vigencia, reglas de estado), BatchService
    │   ├── repository/     # EquipmentRepository (+ specs de filtro/paginación)
    │   ├── entity/         # Equipment, EquipmentStatus
    │   ├── dto/            # EquipmentCreateRequest, BatchCreateRequest, EquipmentSummary, EquipmentDetail
    │   └── mapper/
    ├── main/resources/db/migration/   # V{n}__create_equipment.sql (Flyway)
    └── test/java/.../modules/equipment/

sigefor/
└── src/app/features/fornituras/        # ya andamiada
    ├── pages/fornituras/                # listado: filtros + tabla paginada
    ├── pages/fornitura-form/            # alta individual / edición / cambio de estado
    ├── pages/fornitura-lote/            # alta por lote (captura QR + tabla previa)
    └── data/equipment.service.ts
```

**Structure Decision**: módulo backend `equipment/` siguiendo el patrón existente. Reusa el componente
de captura de QR (**014**) para teclear/escanear el `codigo` y el módulo `qrcodes` para verificar que
el código existe y no está ya ligado. La vigencia se **deriva en servidor**; el frontend solo pinta el
color semántico (`docs/05-ui-ux.md`).

## Phase 0 — Research

Decisiones inline (sin incógnitas bloqueantes):
- **Identidad**: `id` interno opaco (UUID) inmutable + `codigo_qr`/`numero_serie` único **normalizado**
  (trim/upper/sin guiones) para evitar duplicados "aparentemente distintos" (Edge Cases, FR-003).
- **Vida útil / vigencia**: guardar `fecha_fabricacion` + `vida_util_meses` y **persistir**
  `fecha_vencimiento = fecha_fabricacion + vida_util_meses` como dato canónico (Assumptions). Los
  estados de vigencia (próxima ≤ 90 días / caducada) se **derivan** al consultar; alimentan 008/010.
- **Concurrencia de unicidad**: `UNIQUE` sobre el código normalizado + manejo de violación → 409;
  test de inserción concurrente.
- **Regla baja/traslado con asignación vigente**: el servicio consulta `assignment` (004) y `transfer`
  (007) vía puerto; mientras no existan, el puerto devuelve "sin asignación" y el test se completa al
  integrarlas.

## Phase 1 — Design & Contracts

- **Data model** (inline): `equipment(id, codigo_qr UNIQUE normalizado, equipment_type_id, size_id,
  warehouse_id, fecha_fabricacion, vida_util_meses, fecha_vencimiento, status, marca/modelo/nivel/
  inventario/observaciones, foto_url, created_at/by, updated_at/by)`; catálogo `equipment_status`
  (disponible/asignada/en_mantenimiento/en_traslado/extraviada/baja_definitiva).
- **Contract** (inline): `GET /equipment` (paginado + filtros qr/descr/tipo/talla/almacén/estado),
  `POST /equipment` (alta), `POST /equipment/batch` (lote), `GET /equipment/{id}` (ficha),
  `GET /equipment/by-codigo/{codigo}` (**resolución server-side**, consumida por 004/007/009),
  `PUT /equipment/{id}` (edición no identitaria), `PATCH /equipment/{id}/status` (cambio de estado).
  Todos con authn (JWT) + authz por rol.
- **Quickstart** (inline): sembrar catálogos, alta individual y lote, búsqueda por código, derivación
  de vigencia, intento de baja con asignación vigente (bloqueado), auditoría.

Re-check Constitution tras diseño: fichas sin PII del elemento; resolución de código server-side;
unicidad y reglas de estado garantizadas. **Gate sigue en PASA.**

## Complexity Tracking

> Sin violaciones de la constitución. Las dependencias de 004/007 para la regla baja/traslado se
> aíslan tras un puerto; no añaden complejidad estructural permanente.
