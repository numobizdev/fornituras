# Feature Specification: Migración backend Spring Boot → ASP.NET Core Web API

**Feature Branch**: `017-migracion-api-dotnet`

**Created**: 2026-07-01

**Status**: Draft

**Input**: Reemplazar `fornituras-api/` (Java Spring Boot) por una API **ASP.NET Core Web API**
(.NET 10) que preserve el contrato HTTP consumido por `sigefor/` (Ionic 8 + Angular). Entorno
único: desarrollo local. Datos y usuarios actuales son desechables (solo desarrollo).

> **Alcance de esta spec:** migración **completa** del backend REST necesario para paridad con
> Java y operación de las pantallas Ionic ya cableadas. El **primer hito** (auth + seed + login
> Ionic) es la puerta de entrada de la Fase 1, no el límite del proyecto.

## Contexto

### Situación actual

| Componente | Ubicación | Estado |
|------------|-----------|--------|
| Backend Java | `fornituras-api/` | Implementado: auth, usuarios, QR, inventario, elementos, asignaciones, catálogos |
| Frontend Ionic | `sigefor/` | Auth + pantallas core consumiendo REST |
| Base de datos | SQL Server 2022 local | Esquema vía Flyway (14 migraciones Java) |
| Contrato API | `/sigefor/api/v1/**` | Envelope `ApiResponse<T>`, JWT Bearer |

### Objetivo

Crear **`fornituras-api-dotnet/`** — ASP.NET Core Web API en **.NET 10** — que:

1. Exponga los **mismos endpoints y contratos JSON** que el frontend ya usa.
2. Permita **cero o mínimos cambios** en `sigefor/` (idealmente solo `environment.apiUrl`).
3. Use **base de datos nueva o recreada** (sin migrar usuarios, contraseñas ni PII cifrada existente).
4. Mantenga los **principios de seguridad** del proyecto (`docs/02-seguridad.md`), implementados de nuevo en C#.

### Fuera de alcance (v1 de la migración)

- Compatibilidad con hashes BCrypt, JWT secrets o ciphertext PII del backend Java.
- Entornos staging/producción (solo local por ahora).
- Port del UI Thymeleaf `/qr/**` (admin web Java) — fase posterior opcional con Razor Pages.
- Features spec-driven aún no implementadas en Java ni consumidas por Ionic (dashboard, reportes,
  traslados, incidencias, etc.).

### Prerrequisito de gobernanza

Registrar **ADR** en `docs/04-decisiones/` que documente el cambio de stack (Spring Boot → ASP.NET
Core Web API). Actualizar `AGENTS.md`, `README.md` y `CLAUDE.md` al completar la migración.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Desarrollador levanta la API .NET localmente (Priority: P1)

Un desarrollador clona el repo, configura secretos locales y ejecuta la API .NET contra SQL Server.

**Why this priority**: Sin infraestructura local no hay migración.

**Independent Test**: `dotnet run` + health check responde; EF aplica migraciones; admin seed existe.

**Acceptance Scenarios**:

1. **Given** SQL Server local y .NET SDK 10, **When** ejecuto la API .NET, **Then** escucha con path
   base `/sigefor`.
2. **Given** BD vacía, **When** la API arranca, **Then** EF Core aplica migraciones y crea usuario admin.
3. **Given** entorno Development, **When** abro Swagger, **Then** veo endpoints bajo `/api/v1/**`.

---

### User Story 2 - Usuario inicia sesión desde Ionic sin cambios de código (Priority: P1)

El flujo de login de `sigefor/` funciona contra la nueva API cambiando solo la URL base.

**Why this priority**: Criterio principal de transparencia para el frontend.

**Independent Test**: Apuntar `environment.apiUrl` a .NET; login con admin seed; sesión persiste.

**Acceptance Scenarios**:

1. **Given** `apiUrl` apunta a la API .NET, **When** POST `/auth/login`, **Then** respuesta
   `{ success, data: { token, tokenType: "Bearer", expiresIn, user } }`.
2. **Given** sesión activa, **When** request autenticado, **Then** `Authorization: Bearer` es aceptado.
3. **Given** token inválido, **When** request protegido, **Then** HTTP 401 (Ionic hace logout).

---

### User Story 3 - Operador usa flujos core de Ionic contra API .NET (Priority: P1)

Las pantallas ya cableadas (almacenes, tipos, fornituras, elementos, asignación) operan con paridad.

**Why this priority**: Validar que la migración no rompe el producto en desarrollo.

**Independent Test**: Smoke test manual de las 5 áreas core tras login.

**Acceptance Scenarios**:

1. **Given** usuario CAPTURISTA, **When** lista/crea fornituras y elementos, **Then** paginación y
   filtros funcionan.
2. **Given** fornitura disponible, **When** POST `/assignments`, **Then** asignación creada y estado
   actualizado.
3. **Given** código manual, **When** GET `/equipment/by-codigo/{codigo}`, **Then** detalle o 404.

---

### User Story 4 - Administrador gestiona catálogos y almacenes (Priority: P2)

CRUD de almacenes, tipos de prenda, tallas y municipios con RBAC ADMIN.

**Independent Test**: Usuario ADMIN crea almacén y tipo; CAPTURISTA no puede desactivar catálogos.

**Acceptance Scenarios**:

1. **Given** ADMIN, **When** CRUD almacenes, **Then** operaciones auditables y paginadas.
2. **Given** CAPTURISTA, **When** intenta POST `/equipment-types`, **Then** 403.

---

### User Story 5 - Recuperación de contraseña (Priority: P2)

Flujos forgot/reset password compatibles con pantallas Ionic existentes.

**Independent Test**: Solicitar reset, usar código, login con nueva contraseña.

**Acceptance Scenarios**:

1. **Given** email registrado, **When** POST `/auth/forgot-password`, **Then** respuesta genérica OK.
2. **Given** código válido, **When** POST `/auth/reset-password`, **Then** contraseña actualizada.

---

### User Story 6 - Generación QR por lotes (Priority: P3)

Paridad con módulo Java `qrcodes` para operación admin (Ionic aún no consume REST QR).

**Independent Test**: Crear lote, descargar PDF y ZIP.

**Acceptance Scenarios**:

1. **Given** ADMIN/CAPTURISTA, **When** POST `/qr/lotes`, **Then** lote con códigos `FOR-XXXXXX`.
2. **Given** lote existente, **When** GET `/qr/lotes/{id}/pdf`, **Then** PDF válido.

---

### Edge Cases

- Validación de entrada → 400 con `ApiResponse` y mapa de errores por campo.
- Conflicto de asignación concurrente → 409.
- Rol insuficiente → 403.
- Recurso inexistente → 404.
- CORS desde `http://localhost:8100`, `http://localhost:4200`, `capacitor://localhost`.
- Paginación Spring-compatible: `page` (0-based), `size`, respuesta con `content`, `totalElements`,
  `totalPages`, `number`, `size`.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La API MUST usar path base `/sigefor` (equivalente al `context-path` Java).
- **FR-002**: Todas las respuestas JSON REST MUST usar `ApiResponse<T>`: `{ success, message, data }`.
- **FR-003**: MUST existir manejo global de excepciones alineado con `extractApiErrorMessage` del frontend.
- **FR-004**: MUST habilitar CORS para orígenes Ionic/Capacitor en desarrollo.
- **FR-005**: MUST exponer health check.
- **FR-006**: MUST documentar OpenAPI/Swagger en Development.
- **FR-007**: Secretos (JWT, PII, DB) MUST ir en User Secrets / variables de entorno; nunca en el repo.
- **FR-010**: Auth: login, forgot-password, reset-password, change-password, activate (paridad Java).
- **FR-011**: Contraseñas MUST hashearse con BCrypt (hashes nuevos; sin compatibilidad con Java).
- **FR-012**: JWT Bearer stateless; `expiresIn` en **milisegundos** en respuesta login.
- **FR-013**: Roles `ADMIN` y `CAPTURISTA` con autorización por endpoint equivalente a Java.
- **FR-020**: Portar módulos: auth, users, warehouses, equipment-types, sizes, municipios, equipment,
  officers, catalogs (sexos, tipos-sangre), assignments, qr (P3).
- **FR-030**: PII de elementos: cifrado en reposo, blind index, enmascaramiento por rol (ADR 0006).
- **FR-040**: QR: códigos `FOR-` + 6 dígitos; sin datos personales en el código.
- **FR-050**: Seed configurable de usuario ADMIN en desarrollo.

### Key Entities

Ver [data-model.md](./data-model.md). Entidades principales: `User`, `VerificationToken`,
`PasswordResetToken`, `EquipmentType`, `Size`, `Municipio`, `Warehouse`, `Equipment`, `Officer`,
`Sexo`, `TipoSangre`, `Assignment`, `LoteQr`.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Ionic funciona contra API .NET cambiando solo `apiUrl` (o puerto con mismo path).
- **SC-002**: 100% de endpoints P1 del [contrato Ionic](./contracts/ionic-api-contract.md) responden
  con shape JSON compatible.
- **SC-003**: Login → flujo completo de asignación completable en smoke test manual (< 15 min).
- **SC-004**: Ningún secreto en git; build y run documentados en [quickstart.md](./quickstart.md).
- **SC-005**: PII de elementos cifrada en BD; QR sin datos personales.
- **SC-006**: ADR y `AGENTS.md` reflejan el nuevo stack al cierre.

---

## Assumptions

- SDK **.NET 10.0.202** disponible en máquinas de desarrollo.
- SQL Server 2022 local; BD puede recrearse (`fornituras` o nombre configurable).
- No hay usuarios reales ni datos de producción.
- El cliente acepta pausa de nuevas features Java durante el port P1.
- Ionic permanece en Ionic 8 + Angular; sin cambios de arquitectura frontend.
- Email en desarrollo puede ser log-only hasta configurar SMTP.
- Java (`fornituras-api/`) permanece como referencia hasta completar paridad P1.

---

## Referencias

- Contrato Ionic: [contracts/ionic-api-contract.md](./contracts/ionic-api-contract.md)
- Plan técnico: [plan.md](./plan.md)
- Tareas: [tasks.md](./tasks.md)
- Quickstart: [quickstart.md](./quickstart.md)
- Seguridad: [`docs/02-seguridad.md`](../../docs/02-seguridad.md)
- Backend Java (referencia): [`fornituras-api/`](../../fornituras-api/)
- Frontend: [`sigefor/`](../../sigefor/)
