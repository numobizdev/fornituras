# Implementation Plan: Catálogo de tipos de fornitura

**Branch**: `dev` (feature **006-tipos-fornitura**) | **Date**: 2026-06-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/006-tipos-fornitura/spec.md`

## Summary

Construir el **catálogo de tipos de fornitura** (chaleco antibala, cinturón táctico, casco…) y el
**catálogo de tallas**, ambos prerequisito del alta de fornituras (**001**). CRUD con nombre único,
descripción, foto representativa y **desactivación** (no borrado) cuando el tipo está en uso. No hay
PII: la foto es genérica del equipo, así que el eje no es cifrado sino **integridad referencial**
(no romper fornituras que referencian un tipo) y **autorización + auditoría**.

Enfoque: módulo backend `equipmenttypes` en Spring Boot (controller/service/repository/entity/dto +
mapper), migración Flyway con `equipment_type` y `size`, y feature `tipos` en el frontend `sigefor/`
(listado + formulario) sobre la auth existente.

## Technical Context

**Language/Version**: Java 25 (backend `fornituras-api/`); TypeScript + Angular/Ionic 8 (`sigefor/`).

**Primary Dependencies**: Spring Boot (Web MVC, Security, Validation, Data JPA), Flyway
(`flyway-sqlserver`), `mssql-jdbc`. Almacenamiento de foto: blob/objeto (no cifrado — imagen genérica
del equipo, no PII); la fila guarda `foto_url`/referencia. Frontend: servicios HTTP + componentes
standalone Ionic.

**Storage**: SQL Server 2022. Tablas `equipment_type` (nombre único, descripción, foto_url, estado) y
`size` (catálogo de tallas, opcionalmente asociado por tipo). Sin Always Encrypted (no hay PII).

**Testing**: JUnit 5 + Spring Boot Test; Testcontainers (MSSQL) para integración y migración Flyway;
pruebas de contrato y de autorización. Frontend: pruebas de servicio con `HttpTestingController`.

**Target Platform**: API REST en contenedor Linux; cliente Ionic (web + móvil vía Capacitor).

**Project Type**: Web — monorepo `fornituras-api/` + `sigefor/`.

**Performance Goals**: alta/edición de tipo en < 1 min (SC-001); listado paginado < 2 s.

**Constraints**: nombre de tipo único (normalizado); no eliminar tipos/tallas en uso (solo
desactivar); solo activos seleccionables en el alta de **001**; toda operación autorizada y auditada
(Principios IV, V).

**Scale/Scope**: catálogos pequeños (decenas–cientos de filas); 2 pantallas (listado + formulario).

## Constitution Check

*GATE: debe pasar antes de Phase 0 y re-verificarse tras Phase 1.*

| Principio | Cómo lo cumple este plan | Estado |
|-----------|--------------------------|--------|
| I. Seguridad/privacidad primero | Sin PII; foto genérica del equipo. Validación de tipo/tamaño de imagen en el borde | ✅ (N/A PII) |
| II. QR sin PII | No genera ni resuelve QR | ✅ (N/A) |
| III. Cero secretos | Sin secretos nuevos | ✅ |
| IV. Mínimo privilegio | CRUD restringido a rol (ADMIN/almacén); consulta abierta a roles operativos; rechazo por defecto | ✅ |
| V. Auditoría sin fugas | Alta/edición/desactivación auditadas (actor, tipo, cuándo) | ✅ |
| VI. ADR / stack congelado | Sin cambios de stack ni dependencias nuevas | ✅ |

**Resultado del gate**: PASA sin decisiones abiertas. Es un catálogo de soporte de baja
sensibilidad; la disciplina está en integridad referencial y autorización.

## Project Structure

### Documentation (this feature)

```text
specs/006-tipos-fornitura/
├── plan.md              # Este archivo
└── tasks.md             # Phase 2: lo genera /speckit-tasks
```

> Catálogo simple: el diseño de datos y contrato se describe inline en este plan (sin research/
> data-model/contracts separados, que se reservan para features con incógnitas — p. ej. 003/004).

### Source Code (repository root)

```text
fornituras-api/
└── src/
    ├── main/java/com/numobiz/solutions/fornituras/modules/equipmenttypes/
    │   ├── controller/     # EquipmentTypeController, SizeController
    │   ├── service/        # EquipmentTypeService, SizeService (desactivación, unicidad)
    │   ├── repository/     # EquipmentTypeRepository, SizeRepository
    │   ├── entity/         # EquipmentType, Size
    │   ├── dto/            # *CreateRequest, *Summary, *Detail
    │   └── mapper/
    ├── main/resources/db/migration/   # V8__create_equipment_type_and_size.sql (Flyway)
    └── test/java/.../modules/equipmenttypes/

sigefor/
└── src/app/features/tipos/
    ├── pages/tipos/             # listado paginado de tipos + tallas
    ├── pages/tipo-form/         # alta/edición de tipo (con foto)
    └── data/equipment-types.service.ts
```

**Structure Decision**: módulo backend `equipmenttypes/` siguiendo el patrón existente (`auth`,
`users`, `qrcodes`). Tallas en el mismo módulo (catálogo hermano) para no fragmentar un dominio
pequeño. La verificación "en uso" consulta `equipment` (001); mientras 001 no exista, la regla se
implementa pero su test de integración usa un doble/condición sobre la futura FK.

## Phase 0 — Research

Sin incógnitas técnicas relevantes. Decisiones tomadas inline:
- **Tallas**: catálogo independiente `size`, opcionalmente asociado por tipo (un chaleco y un
  cinturón no comparten tallas) — recomendación de la spec (Assumptions). Se modela `size` con FK
  nullable a `equipment_type` para permitir tallas globales o por tipo.
- **Borrado vs desactivación**: nunca borrado físico de un tipo/talla en uso; columna `active`.
- **Foto**: imagen genérica (no PII); validación de tipo MIME y tamaño máximo; almacenamiento por
  referencia (`foto_url`).

## Phase 1 — Design & Contracts

- **Data model** (inline): `equipment_type(id, nombre UNIQUE normalizado, descripcion, foto_url,
  active, created_at/by)`; `size(id, etiqueta, equipment_type_id NULL, active)`.
- **Contract** (inline): `GET /equipment-types` (paginado + filtro `active`), `POST /equipment-types`,
  `PUT /equipment-types/{id}`, `PATCH /equipment-types/{id}/deactivate`; análogos `/sizes`. Todos con
  authn (JWT) + authz por rol. `POST/PUT` validan unicidad de nombre normalizado (409 si duplica).
- **Quickstart** (inline): sembrar tipos base, intentar eliminar uno en uso → bloqueado, desactivar →
  desaparece del alta de 001.

Re-check Constitution tras diseño: catálogo sin PII; integridad referencial y autorización cubiertas.
**Gate sigue en PASA.**

## Complexity Tracking

> Sin violaciones de la constitución. Catálogo de soporte; no introduce complejidad estructural.
