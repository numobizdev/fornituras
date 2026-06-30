# Implementation Plan: QR opaco y firmado por equipo

**Branch**: `002-qr-equipos` | **Date**: 2026-06-29 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/002-qr-equipos/spec.md`

## Summary

Generar, por cada equipo del inventario, un **QR opaco firmado** (UUID v4 + HMAC-SHA256 con
versión de llave) apto para grabado/impresión, y poder **verificar** su autenticidad. La
resolución `QR → equipo` ocurre solo en el servidor tras authn+authz. El formato del payload
quedó fijado en [ADR 0002](../../docs/04-decisiones/0002-formato-del-qr.md) para permitir el
grabado definitivo sin retrabajo.

Enfoque técnico: servicio de dominio `qr` en el backend Spring Boot que (1) emite el
identificador opaco y la firma, (2) renderiza la imagen del QR, (3) verifica firmas; expuesto
por endpoints REST protegidos con Spring Security y auditados. El frontend Ionic/Angular añade
una acción para generar/exportar el QR de un equipo.

## Technical Context

**Language/Version**: Java 21 (LTS) — backend; TypeScript/Angular — frontend.

**Primary Dependencies**: Spring Boot 3.3.x (Web, Security, Validation, Data JPA), driver MSSQL
(`mssql-jdbc`), **ZXing** (`com.google.zxing:core` + `javatse`) para renderizar el QR (Apache-2.0,
mantenido). HMAC y UUID con la librería estándar de Java (sin dependencia extra). Frontend:
Ionic 8 + Angular (servicio HTTP).

**Storage**: SQL Server 2022. El identificador opaco y la versión de llave se persisten en la
tabla `equipment` (columnas `qr_opaque_id`, `qr_key_version`). La llave HMAC **no** se almacena
en BD: vive en el gestor de secretos / variable de entorno.

**Testing**: JUnit 5 + Spring Boot Test; **Testcontainers** (MSSQL) para integración;
pruebas de contrato sobre los endpoints. Frontend: pruebas de servicio con HttpTestingController.

**Target Platform**: API REST sobre Linux/contenedor; cliente Ionic (web + móvil vía Capacitor).

**Project Type**: Web (monorepo `backend/` + `frontend/`).

**Performance Goals**: generación y verificación de un QR en < 200 ms p95 (operación local de
CPU); export por lote de cientos de equipos sin bloquear la UI.

**Constraints**: el contenido del QR es **solo** `v<ver>.<b64url(uuid)>.<b64url(hmac)>` (sin
PII); la llave HMAC nunca toca el repo ni los logs; la verificación debe seguir funcionando
tras rotación de llave (selección por versión).

**Scale/Scope**: del orden de miles de equipos; un QR por equipo; baja concurrencia de
generación (operación administrativa).

## Constitution Check

*GATE: debe pasar antes de Phase 0 y re-verificarse tras Phase 1.*

| Principio | Cómo lo cumple este plan | Estado |
|-----------|--------------------------|--------|
| I. Seguridad primero | Diseño centrado en opacidad + firma; pruebas de no-fuga | ✅ |
| II. QR sin PII | Payload solo `uuid+hmac+ver`; test que verifica ausencia de PII (SC-002) | ✅ |
| III. Cero secretos | Llave HMAC desde env/secret manager; `.env.example` solo con el nombre | ✅ |
| IV. Mínimo privilegio | Endpoints de generación/reemisión/export requieren rol; verificación sin datos si no autz | ✅ |
| V. Auditoría sin fugas | Auditar generate/reissue/export (actor+timestamp+equipo_id, sin PII) | ✅ |
| VI. ADR / stack congelado | Formato fijado en ADR 0002; stack sin cambios | ✅ |

**Resultado del gate**: PASA. Sin violaciones que justificar (Complexity Tracking vacío).

## Project Structure

### Documentation (this feature)

```text
specs/002-qr-equipos/
├── plan.md              # Este archivo
├── research.md          # Phase 0: decisiones y alternativas
├── data-model.md        # Phase 1: entidades/columnas del QR
├── quickstart.md        # Phase 1: cómo correr y validar
├── contracts/
│   └── qr-api.md        # Phase 1: contrato REST de los endpoints de QR
└── tasks.md             # Phase 2: lo genera /speckit-tasks (no este comando)
```

### Source Code (repository root)

```text
backend/
└── src/
    ├── main/java/<base>/qr/
    │   ├── api/            # QrController (endpoints REST)
    │   ├── service/        # QrSigningService (HMAC), QrImageService (ZXing), QrService
    │   ├── domain/         # value objects: OpaqueId, QrPayload, KeyVersion
    │   └── config/         # carga de llave(s) HMAC desde entorno/secret manager
    ├── main/resources/db/migration/   # migración: columnas qr_* en equipment
    └── test/java/<base>/qr/            # unit + contract + integración (Testcontainers)

frontend/
└── src/app/
    ├── equipos/qr/         # acción "Generar/Exportar QR" en la ficha de equipo
    └── core/api/qr.service.ts
```

**Structure Decision**: Web app en monorepo. El grueso vive en `backend/` (emisión, firma,
render, verificación); el `frontend/` solo dispara y descarga/exporta. La feature de **escaneo**
(cámara/HID) es independiente y consumirá el endpoint de verificación/resolución.

## Phase 0 — Research

Ver [research.md](./research.md). Decisiones clave ya cerradas:
- Formato del QR → ADR 0002 (UUID v4 + HMAC-SHA256 + versión de llave).
- Librería de render → ZXing (licencia Apache-2.0, mantenida; justifica Principio VI).
- Rotación de llave → mapa `version → key`; el payload porta la versión; verificación elige.

Incógnitas restantes (no bloquean el plan, sí la implementación final):
- Gestor de secretos concreto (ADR pendiente) — entre tanto, variable de entorno.
- Tamaño/ECC del QR para grabado físico — se valida con prueba de lectura real (SC-006).

## Phase 1 — Design & Contracts

- **Data model**: [data-model.md](./data-model.md) — columnas `qr_opaque_id` (único) y
  `qr_key_version` en `equipment`; sin tabla nueva.
- **Contracts**: [contracts/qr-api.md](./contracts/qr-api.md) — `POST /equipment/{id}/qr`
  (generar), `GET /equipment/{id}/qr` (obtener imagen), `POST /equipment/{id}/qr:reissue`
  (reemitir, auditado), `POST /qr:verify` (verificar firma). Todos con authz + auditoría.
- **Quickstart**: [quickstart.md](./quickstart.md) — variables de entorno (llave HMAC),
  cómo generar un QR y cómo validar las pruebas de no-fuga y de firma.

Re-check Constitution tras diseño: el contrato no expone PII en ninguna respuesta de
`verify`; las respuestas de error son genéricas (FR-005). **Gate sigue en PASA.**

## Complexity Tracking

> Sin violaciones de la constitución. No aplica.
