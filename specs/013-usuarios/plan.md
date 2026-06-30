# Implementation Plan: Usuarios y roles

**Branch**: `dev` (feature **013-usuarios**) | **Date**: 2026-06-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/013-usuarios/spec.md`

## Summary

**Extender** (no reescribir) la autenticación ya implementada para ofrecer **administración de usuarios
desde SIGEFOR** (CRUD + activar/desactivar + asignar rol) y reforzar el **RBAC**. El backend ya tiene
`User` (email único, hash, `role`, `enabled`), enum `Role` con `ADMIN`/`CAPTURISTA`, login JWT,
recuperación por código y seeding de admin; el frontend ya tiene login/guards/interceptor. Esta feature
**añade**: pantalla de gestión de usuarios, la **expansión de roles** (Supervisor/Almacén/Auditor/
Consulta) — que por afectar autorización **requiere ADR** antes de ampliar el enum —, **MFA** para roles
administrativos y **protección reforzada contra fuerza bruta**. La regla dura: **nunca dejar el sistema
sin un administrador activo**.

Enfoque: extender el módulo `users` existente (no crear uno nuevo) y la feature `auth`/nueva `usuarios`
en `sigefor/`. Toda ampliación del enum `Role` pasa por ADR (Principio VI) porque cambia el modelo de
autorización del sistema completo.

## Technical Context

**Language/Version**: Java 25 (backend `fornituras-api/`, módulos `auth`/`users` existentes); TypeScript
+ Angular/Ionic 8 (`sigefor/`, auth existente).

**Primary Dependencies**: Spring Boot (Web MVC, **Security** ya configurado, Validation, Data JPA),
Flyway (`flyway-sqlserver`), `mssql-jdbc`. Hashing fuerte ya presente (verificar Argon2id/bcrypt). Para
**MFA**: evaluar TOTP (p. ej. `java-otp`/equivalente) — **introducir dependencia requiere ADR**
(Principio VI). Frontend: extiende `AuthService`/guards existentes.

**Storage**: SQL Server 2022. Tabla `users` existente (migraciones `V1`/`V2` ya aplicadas: creación +
roles + seed admin). Cambios nuevos vía **nueva migración** Flyway (p. ej. columnas MFA, intentos
fallidos), nunca editando migraciones aplicadas.

**Testing**: JUnit 5 + Spring Boot Test; Testcontainers (MSSQL); pruebas de autorización por rol (rechazo
por defecto), de "no dejar sin admin", de hashing, y de bloqueo por fuerza bruta. Frontend: pruebas de
servicio + guards.

**Target Platform**: API REST en contenedor Linux; cliente Ionic.

**Project Type**: Web — monorepo `fornituras-api/` + `sigefor/`.

**Performance Goals**: listado de usuarios paginado < 2 s; verificación de rol sin penalización notable.

**Constraints**: contraseñas siempre con hashing fuerte, nunca reversibles (FR-003, SC-001 — ya
cumplido); RBAC con mínimo privilegio y rechazo por defecto (FR-002); cambios auditados (FR-006); no
dejar el sistema sin admin (FR-007); MFA y anti-fuerza-bruta como objetivos (FR-004/FR-005).

**Scale/Scope**: usuarios del sistema (no elementos); 2 pantallas (listado + formulario) sobre la auth
existente.

## Constitution Check

*GATE: debe pasar antes de Phase 0 y re-verificarse tras Phase 1.*

| Principio | Cómo lo cumple este plan | Estado |
|-----------|--------------------------|--------|
| I. Seguridad/privacidad primero | Usuarios del sistema (no PII de elementos); contraseñas hasheadas; sin secretos en respuestas | ✅ |
| II. QR sin PII | N/A | ✅ (N/A) |
| III. Cero secretos | Hash/JWT/claves desde entorno; nunca en repo ni en respuestas | ✅ |
| IV. Mínimo privilegio | **Es** el núcleo del RBAC: rechazo por defecto, roles acotados, CRUD solo admin | ✅ |
| V. Auditoría sin fugas | Alta/edición/cambio de rol/activación auditados (sin volcar hash ni token) | ✅ |
| VI. ADR / stack congelado | **Ampliar el enum `Role`** y **añadir MFA/dependencia** → ADR antes de implementar | ⚠️ ver research |

**Resultado del gate**: PASA con **ADR requerido** para (a) la expansión de roles y (b) la estrategia de
MFA. No es violación: la spec ya exige registrar estas decisiones antes de tocar autorización.

## Project Structure

### Documentation (this feature)

```text
specs/013-usuarios/
├── plan.md              # Este archivo
└── tasks.md             # Phase 2: lo genera /speckit-tasks
```

> Diseño inline. **Dos ADR** a abrir: expansión de roles y estrategia MFA (ambos afectan autorización).

### Source Code (repository root)

```text
fornituras-api/
└── src/
    ├── main/java/com/numobiz/solutions/fornituras/modules/users/   # EXISTENTE — se extiende
    │   ├── controller/     # UserController: añadir CRUD admin (list/create/update/enable/disable/role)
    │   ├── service/        # UserService: reglas (último admin, unicidad email, cambio de rol)
    │   ├── repository/     # UserRepository (existente)
    │   ├── entity/         # User, Role (enum) — ampliar Role SOLO tras ADR
    │   └── dto/            # UserCreateRequest, UserSummary, UserUpdateRequest
    ├── main/java/.../modules/auth/    # EXISTENTE — MFA/anti-bruteforce se integran aquí
    ├── main/resources/db/migration/   # V{n}__users_admin_and_security.sql (MFA/intentos) — nueva
    └── test/java/.../modules/users/

sigefor/
└── src/app/features/usuarios/
    ├── pages/usuarios/          # listado paginado (admin)
    ├── pages/usuario-form/      # alta/edición + rol + activar/desactivar
    └── data/users.service.ts
# Reutiliza core/auth existente (AuthService, guards, interceptor).
```

**Structure Decision**: se **extiende** el módulo `users` existente (AGENTS.md §7: respetar/extender, no
reescribir). El RBAC se apoya en la config de Spring Security ya presente; cada feature declara sus
requisitos de rol. La **ampliación del enum `Role`** se hace en una sola tarea, **gated por ADR**, y se
propaga a las autorizaciones de las demás features. MFA y anti-fuerza-bruta se integran en `auth`.

## Phase 0 — Research

Decisiones / incógnitas (ambas → ADR):
- **Expansión de roles** (Supervisor/Almacén/Auditor/Consulta): hoy solo `ADMIN`/`CAPTURISTA`. Ampliar el
  enum cambia la autorización de todo el sistema → **ADR** que defina el mapa rol→permisos antes de tocar
  código. Hasta entonces, las demás features autorizan con los dos roles existentes.
- **MFA** para roles administrativos: estrategia (TOTP vs email/OTP), almacenamiento del secreto MFA
  (cifrado), recuperación → **ADR** (incluye la dependencia, Principio VI).
- **Anti-fuerza-bruta**: rate limiting + bloqueo temporal por intentos fallidos (contador en `users` o
  store); umbral configurable.
- **Hashing**: verificar que el backend usa Argon2id/bcrypt (FR-003 ya cumplido); documentar parámetros.

## Phase 1 — Design & Contracts

- **Data model** (inline): `users` existente; **nuevas columnas** (vía migración nueva) para MFA
  (`mfa_enabled`, `mfa_secret` cifrado) e intentos fallidos/bloqueo (`failed_attempts`, `locked_until`).
- **Contract** (inline): `GET /users` (paginado, admin), `POST /users` (alta), `PUT /users/{id}`
  (edición), `PATCH /users/{id}/enabled`, `PATCH /users/{id}/role`. Reglas: unicidad de email (409); no
  desactivar/cambiar de rol al **último admin** (FR-007). Endpoints de login existentes se refuerzan
  (MFA/anti-bruteforce). Todos con authz de administrador.
- **Quickstart** (inline): crear usuario con rol → inicia sesión y solo accede a lo permitido; email
  duplicado → rechazado; intentar desactivar al último admin → bloqueado; cambio de rol → auditado.

Re-check Constitution tras diseño: respuestas sin hash/token; RBAC con rechazo por defecto; cambios
auditados; ADRs de roles/MFA registrados antes de implementarlos. **Gate sigue en PASA** (con los ADR).

## Complexity Tracking

> Sin violaciones. La expansión de roles y MFA se gestionan por ADR (Principio VI). Se **extiende** el
> backend existente, no se reescribe, evitando complejidad y regresiones.
