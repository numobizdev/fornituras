---
description: "Task list — Usuarios y roles (013)"
---

# Tasks: Usuarios y roles

**Input**: Design documents from `specs/013-usuarios/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md)

> **Se EXTIENDE la auth existente, no se reescribe** (AGENTS.md §7). El backend ya tiene `User`, enum
> `Role` (`ADMIN`/`CAPTURISTA`), login JWT, recuperación por código y seed de admin; el frontend ya tiene
> login/guards/interceptor. Estas tareas **añaden** administración de usuarios, RBAC reforzado y dejan la
> expansión de roles y el MFA **gated por ADR**.

**Tests**: incluidos. Pruebas de autorización (rechazo por defecto), unicidad de email, "no dejar sin
admin", hashing y bloqueo por fuerza bruta.

**Organization**: por user story; algunas tareas son **gated por ADR** y no se implementan sin él.

## Path Conventions

- **Backend**: `<be>/users/` y `<be>/auth/` (módulos existentes) =
  `fornituras-api/src/main/java/com/numobiz/solutions/fornituras/modules/{users,auth}/`; migraciones en
  `fornituras-api/src/main/resources/db/migration/`; pruebas en `<bet>/users/`.
- **Frontend**: `<fe>/usuarios/` = `sigefor/src/app/features/usuarios/` (reusa `core/auth`).

---

## Phase 1: Setup

- [X] T001 Revisar el módulo `users`/`auth` existente y anotar puntos de extensión (controller, service, enum `Role`, config de Security) sin romper lo actual
- [X] T002 [P] Preparar la feature frontend `<fe>/usuarios/` (`pages/usuarios/`, `pages/usuario-form/`, `data/`)
- [X] T003 [P] Abrir **ADR de expansión de roles** (mapa rol→permisos para Supervisor/Almacén/Auditor/Consulta) en `docs/04-decisiones/` — hecho: `0013-expansion-de-roles.md` (**Aceptado**), desbloqueó e implementó T020
- [X] T004 [P] Abrir **ADR de estrategia MFA** (TOTP vs OTP por correo; almacenamiento cifrado del secreto; dependencia, Principio VI) en `docs/04-decisiones/` — hecho: `0014-estrategia-mfa.md` (**Propuesto**, propone TOTP); T022/T023 siguen gated hasta que se **acepte**

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: las historias de CRUD/RBAC pueden empezar; MFA/roles nuevos esperan su ADR.

- [X] T005 [P] Definir DTOs `UserCreateRequest`, `UserSummary`, `UserUpdateRequest` (sin exponer hash ni token) en `<be>/users/dto/` — se reusan `UserRequestDTO`/`UserResponseDTO` existentes y se añaden `UserUpdateRequest`, `RoleUpdateRequest`, `EnabledUpdateRequest`
- [X] T006 [P] Verificar/confirmar el **hashing fuerte** (Argon2id/bcrypt) del backend y documentar parámetros (FR-003, SC-001) en `<be>/auth/` o `<be>/users/service/` — confirmado `BCryptPasswordEncoder` en `SecurityConfig`
- [X] T007 Reusar el escritor de **auditoría** (012) para `CREATE/UPDATE/ENABLE/DISABLE/ROLE_CHANGE_USER` (sin volcar hash/token); si 012 no existe, escritor mínimo a `audit_log`

**Checkpoint**: fundamento listo para el CRUD admin.

---

## Phase 3: User Story 1 - Administrar usuarios del sistema (Priority: P1) 🎯 MVP

**Goal**: alta/consulta/edición/activar-desactivar usuarios y asignación de rol, restringido a admin, con
contraseñas hasheadas y auditoría.

**Independent Test**: crear usuario rol CAPTURISTA → inicia sesión y solo accede a lo permitido; email
duplicado → rechazado; desactivar al último admin → bloqueado.

### Tests for User Story 1

- [X] T008 [P] [US1] Test de contrato `GET/POST/PUT /users` + `PATCH .../enabled` + `PATCH .../role` (admin only; 409 email duplicado) en `<bet>/users/UserAdminContractTest.java`
- [X] T009 [P] [US1] Test de integración: alta con hash (nunca texto plano); **no dejar el sistema sin admin** (desactivar/degradar último admin → bloqueado, FR-007) en `<bet>/users/UserAdminIntegrationTest.java`
- [X] T010 [P] [US1] Test de autorización: rol no-admin no accede al CRUD de usuarios (rechazo por defecto) y queda auditado en `<bet>/users/UserAdminAuthTest.java`

### Implementation for User Story 1

- [X] T011 [US1] Extender `UserService` (alta con unicidad de email, edición, activar/desactivar, cambio de rol, **regla de último admin**, auditoría) en `<be>/users/service/`
- [X] T012 [US1] Extender `UserController` con el CRUD admin (`GET` paginado, `POST`, `PUT`, `PATCH /enabled`, `PATCH /role`), restringido a admin, en `<be>/users/controller/`
- [X] T013 [US1] Añadir **Bean Validation** a los DTOs (email válido/único, rol válido del enum actual, nombre requerido) en `<be>/users/dto/`
- [X] T014 [P] [US1] Frontend: `users.service.ts` (list/create/update/enable/disable/role) en `<fe>/usuarios/data/`
- [X] T015 [US1] Frontend: página de listado (paginada, admin) en `<fe>/usuarios/pages/usuarios/`
- [X] T016 [US1] Frontend: página `usuario-form` (datos + rol + activar/desactivar) reusando `core/auth` en `<fe>/usuarios/pages/usuario-form/`

**Checkpoint**: administración de usuarios operativa (MVP).

---

## Phase 4: User Story 2 - Control de acceso basado en roles (RBAC) (Priority: P1)

**Goal**: cada rol ve/hace solo lo necesario (mínimo privilegio, rechazo por defecto); acciones fuera de
alcance denegadas y auditadas.

**Independent Test**: un usuario "Consulta" no ve CURP/RFC ni el botón de asignar; un "Auditor" solo lee y
consulta la bitácora.

### Tests for User Story 2

- [X] T017 [P] [US2] Test de RBAC: acción fuera del alcance del rol → denegada (rechazo por defecto) y auditada (SC-002) en `<bet>/users/RbacTest.java`

### Implementation for User Story 2

- [X] T018 [US2] Consolidar la **matriz de autorización por rol** en la config de Spring Security (con los roles existentes `ADMIN`/`CAPTURISTA`) y exponer `hasRole` consistente al frontend en `<be>/auth/` — CRUD de usuarios `@PreAuthorize hasRole('ADMIN')`; documentado en `UserController`
- [X] T019 [US2] Frontend: aplicar guards/permita-según-rol en menús y acciones (ocultar/inhabilitar lo no permitido) reusando `AuthService.hasRole` en `<fe>/usuarios/` y menús compartidos — nuevo `adminGuard` en rutas + entrada de menú `roles: ['ADMIN']`
- [X] T020 [US2] **[ADR 0013 ACEPTADO]** Ampliar el enum `Role` a cinco valores (`ADMIN`/`SUPERVISOR`/`ALMACEN`/`AUDITOR`/`CAPTURISTA`) y propagar la matriz de permisos a todas las features — enum en `<be>/users/entity/Role.java`; matriz centralizada en `<be>/security/RolePolicy.java` (constantes `@PreAuthorize` + `canViewFullPii` para la regla 3) reutilizada por cada controlador; PII visible para ADMIN/SUPERVISOR/AUDITOR (regla 3); auditoría legible por ADMIN/AUDITOR (regla 4); roles expuestos al frontend en `role-options.ts`/`auth.model.ts`. Sin migración de datos (los usuarios existentes conservan su rol). Tests de autorización por rol añadidos (`RolePolicyTest`, `EquipmentAuthTest`, `DecommissionAuthTest`, `AuditQueryAuthTest`, `TransferAuthTest`)

**Checkpoint**: RBAC consistente con la matriz ampliada del ADR 0013 (cinco roles, mínimo privilegio).

---

## Phase 5: Endurecimiento de autenticación (objetivos)

- [X] T021 [US-sec] Implementar **anti-fuerza-bruta** (rate limiting + bloqueo temporal por intentos fallidos; columnas `failed_attempts`/`locked_until` vía nueva migración Flyway) en `<be>/auth/` — `LoginAttemptService` + `LoginLockProperties` + migración `V22`
- [ ] T022 [US-sec] **(DIFERIDO)** **[GATED por ADR T004]** Implementar **MFA** para roles administrativos (TOTP/OTP según ADR; secreto cifrado; recuperación) — solo tras aprobar el ADR, en `<be>/auth/`
- [ ] T023 [P] [US-sec] **(DIFERIDO)** Frontend: flujo de MFA en login para roles administrativos (gated por T022) en `<fe>/usuarios/`/`core/auth`

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T024 [P] Asegurar que ninguna respuesta/login expone hash, secreto MFA ni token de más; errores que no filtran si el email existe — `UserResponseDTO` sin password (test lo verifica); login/lock con mensajes genéricos
- [X] T025 [P] Tests unitarios de la regla "último admin" y de unicidad de email en `<bet>/users/` — `UserServiceTest`
- [X] T026 Validar el quickstart (crear/editar/rol/activar, bloqueo de último admin, anti-bruteforce) y registrar resultados — cubierto por tests automáticos (contract/integration/lockout); suite completa 215/215 verde

---

## Dependencies & Execution Order

- **Setup → Foundational (BLOQUEA) → US1 (P1, MVP) → US2 (P1) → Endurecimiento → Polish.**
- **Gated por ADR:** T020 (roles) espera T003; T022/T023 (MFA) esperan T004. No implementar antes.
- Se apoya en **012** (auditoría de cambios) y es **transversal** al resto (define los roles que todas usan).

### Parallel Opportunities

- Setup: T002–T004 en paralelo. Foundational: T005, T006 en paralelo.
- US1: tests T008–T010 en paralelo; T014 con backend. US2: T017 antes de T018/T019.

---

## Notes

- [P] = archivos distintos, sin dependencias.
- **Extender, no reescribir** la auth existente; nunca exponer hash/secreto/token.
- Expansión de roles y MFA **requieren ADR** antes de tocar autorización (Principio VI).
- Commit por tarea o grupo lógico; TDD (tests en rojo antes de implementar).
