---
description: "Task list — Generación e impresión de QR por lotes (002)"
---

# Tasks: Generación e impresión de QR por lotes

**Input**: Design documents from `specs/002-qr-equipos/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/qr-api.md](./contracts/qr-api.md)

> **ESTADO: IMPLEMENTADO (parcial).** El módulo `fornituras-api/.../modules/qrcodes/` ya existe:
> generación de códigos opacos `FOR-XXXXX` por lotes, parámetros de impresión y export PDF/ZIP. Estas
> tareas **no reimplementan** lo existente: **verifican** el comportamiento contra la spec, cierran las
> brechas conocidas (autorización fina, auditoría) y dejan abierta la firma (ADR 0005). No romper lo
> que ya funciona (Principio de respeto al backend existente, AGENTS.md §7).

**Tests**: incluidos como **caracterización** de lo implementado (cubrir lo que existe sin reescribir)
y como guía de las brechas a cerrar.

**Organization**: por user story; las de generación/export/consulta ya están implementadas (tareas de
verificación); la de autenticidad (firma) queda como deuda gestionada por ADR.

## Path Conventions

- **Backend**: `<be>/qrcodes/` = `fornituras-api/src/main/java/com/numobiz/solutions/fornituras/modules/qrcodes/`;
  pruebas en `<bet>/qrcodes/`.
- **Frontend**: la API REST puede consumirse desde `sigefor/` (hoy existe además UI Thymeleaf `QrWebController`).

---

## Phase 1: Caracterización del módulo existente

**Purpose**: fijar el comportamiento actual con tests antes de tocar nada.

- [ ] T001 Inventariar el módulo `qrcodes` existente (controllers REST `/api/v1/qr/lotes...`, `QrPdfService`, generador `SecureRandom`, entidades `lote_qr`/`codigo_qr`) y anotar en [research.md](./research.md) lo que diverge de la spec
- [ ] T002 [P] Test de caracterización de **opacidad** (FR-003/SC-002): el contenido crudo de un código no contiene PII ni datos derivables en `<bet>/qrcodes/QrOpacityTest.java`
- [ ] T003 [P] Test de caracterización de **unicidad** (FR-002/SC-001): generar un lote → cero colisiones contra BD y contra el lote en curso en `<bet>/qrcodes/QrUniquenessTest.java`

---

## Phase 2: User Story 1 - Generar un lote de QR (P1) — IMPLEMENTADO (verificar)

**Independent Test**: generar un lote de 10 con tamaño 3 cm → 10 códigos `FOR-XXXXX` únicos ligados al lote; cantidad fuera de rango (≤0 o > `app.qr.maxBatchSize`) → rechazada.

- [ ] T004 [P] [US1] Test de contrato `POST /api/v1/qr/lotes` (crea N códigos; valida `cantidad`, `qrSizeCm`, `paddingCm`, `labelPosition`, `mostrarBordes`; rechazo fuera de rango) en `<bet>/qrcodes/LoteCreateContractTest.java`
- [ ] T005 [US1] Verificar/ajustar la validación de rangos (`app.qr.*`) y mensajes claros sin filtrar internals en `<be>/qrcodes/` (solo si el test detecta brecha)

---

## Phase 3: User Story 2 - Exportar el lote (P1) — IMPLEMENTADO (verificar)

**Independent Test**: descargar PDF/ZIP de un lote → un QR escaneable por código con la disposición elegida; re-exportar con otros ajustes (`ReprintQrForm`) → mismos códigos.

- [ ] T006 [P] [US2] Test de export PDF/ZIP (FR-004/SC-003/SC-004): el archivo contiene un QR por código y **re-exportar no cambia los códigos** en `<bet>/qrcodes/QrExportTest.java`
- [ ] T007 [US2] Verificar que la reimpresión usa ajustes personalizados sin regenerar códigos (idempotencia de códigos) en `<be>/qrcodes/`

---

## Phase 4: User Story 3 - Consultar lotes y códigos (P2) — IMPLEMENTADO (verificar)

**Independent Test**: listar lotes (más nuevos primero); consultar los códigos de un lote.

- [ ] T008 [P] [US3] Test de contrato de listado de lotes (orden por fecha desc) y enumeración de códigos de un lote en `<bet>/qrcodes/LoteQueryTest.java`

---

## Phase 5: Cierre de brechas de seguridad (transversal)

**Purpose**: autorización fina y auditoría (FR-007/FR-010) sin alterar el formato (ADR 0005).

- [ ] T009 [US-sec] Añadir **autorización por rol** a los endpoints `/api/v1/qr/**` (hoy solo autenticación) — alineado con la expansión de roles (spec 013); rechazo por defecto en `<be>/qrcodes/controller/`
- [ ] T010 [P] [US-sec] Auditar **generación y exportación** de lotes (actor, lote, cantidad, cuándo) reutilizando el escritor de la feature **012** en `<be>/qrcodes/service/`
- [ ] T011 [P] [US-sec] Añadir **rate limiting** a la resolución/consulta de códigos para mitigar enumeración (riesgo conocido sin firma, ADR 0005) en `<be>/qrcodes/`

---

## Phase 6: User Story 4 - Verificar autenticidad (P3) — ABIERTO (no implementar sin ADR)

**Goal**: decidir si se añade firma (HMAC) o se acepta el riesgo con mitigaciones. **No** implementar
hasta resolver el ADR.

- [ ] T012 [US4] Documentar en [ADR 0005](../../docs/04-decisiones/0005-formato-qr-implementado.md) la decisión final (aceptar riesgo + mitigaciones T009–T011, o introducir firma con migración de formato) y su impacto en 014/004
- [ ] T013 [US4] *(condicional, solo si el ADR elige firma)* Diseñar la migración de formato firmado compatible con los códigos ya impresos (no invalidar etiquetas existentes) — nueva tarea derivada del ADR

---

## Dependencies & Execution Order

- **Phase 1 (caracterización)** primero: congelar el comportamiento actual.
- **Phases 2–4** son verificación de lo implementado (pueden ir en paralelo entre sí).
- **Phase 5** (seguridad) depende de que exista el escritor de auditoría (012) y de la decisión de roles (013).
- **Phase 6** es deuda gestionada por ADR; bloqueante de nada en el MVP.

### Parallel Opportunities

- T002, T003 en paralelo; T004, T006, T008 en paralelo; T010, T011 en paralelo.

---

## Notes

- [P] = archivos distintos, sin dependencias.
- **No reescribir** el módulo `qrcodes`: caracterizar, verificar y cerrar brechas.
- La divergencia de firma es conocida y **gestionada por ADR 0005**; no introducir firma sin esa decisión.
- Commit por tarea o grupo lógico.
