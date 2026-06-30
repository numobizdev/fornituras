# Feature Specification: Usuarios y roles

**Feature Branch**: `013-usuarios`

**Created**: 2026-06-30

**Status**: Draft

**Input**: `Requerimientos.MD` §8 (Usuarios) + `Paleta de colores.MD` §Seguridad (roles
sugeridos). RBAC del sistema, alineado a `docs/02-seguridad.md` §4.

> **Nota.** Esta spec describe el QUÉ del módulo de administración de usuarios y roles desde
> SIGEFOR; la implementación existente se respeta y se extiende, no se reescribe.

## Estado de implementación (2026-06-30) — **autenticación YA IMPLEMENTADA**

La **autenticación** está implementada de extremo a extremo y esta spec se ajusta a esa
realidad (no se rediseña):

- **Backend** (`fornituras-api/`): entidad `User` (`name`, `email` único, `password` con hash,
  `role`, `enabled`), enum `Role` con **`ADMIN`** y **`CAPTURISTA`**, seeding de admin inicial,
  endpoints `auth/login`, `auth/forgot-password`, `auth/reset-password` y gestión de usuarios.
- **Frontend** (`sigefor/`): páginas `login`, `forgot-password`, `reset-password`;
  `AuthService` (login, recuperación, restauración de sesión, `logout`, `hasRole`),
  `TokenStorageService`, `authGuard`/`guestGuard`, `auth.interceptor` (adjunta el token).
- **Identidad:** el login es por **email + contraseña**; la sesión es un **JWT** (`token`,
  `tokenType`, `expiresIn`). La recuperación de contraseña es por **código** (`reset-password`
  con `code` + `newPassword`).

Lo que esta spec **añade/pendiente**: administración de usuarios (CRUD) desde la UI, expansión
de roles (ver tabla), **MFA** para roles administrativos y protección reforzada contra fuerza
bruta. Estos puntos NO están implementados aún.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Administrar usuarios del sistema (Priority: P1)

Un administrador da de alta, consulta (paginado), edita, activa/desactiva usuarios y les asigna
un **rol**. Las contraseñas se almacenan con hashing fuerte; nunca en claro.

**Why this priority**: Sin gestión de usuarios y roles no hay control de acceso real al resto del
sistema.

**Independent Test**: Crear un usuario con rol OPERADOR; puede iniciar sesión y solo accede a lo
permitido por su rol; un username duplicado se rechaza.

**Acceptance Scenarios**:

1. **Given** un administrador, **When** crea un usuario con username único y rol, **Then** el
   usuario queda activo y puede autenticarse según su rol.
2. **Given** un usuario existente, **When** el admin cambia su rol o lo desactiva, **Then** sus
   permisos cambian de inmediato y queda auditado.
3. **Given** un intento de guardar contraseña, **When** se persiste, **Then** se almacena con
   **Argon2id/bcrypt**, nunca en texto plano ni reversible.

---

### User Story 2 - Control de acceso basado en roles (RBAC) (Priority: P1)

Cada rol ve y hace solo lo necesario (mínimo privilegio).

**Roles implementados hoy** (enum `Role` del backend):

| Rol implementado | Alcance actual |
|------------------|----------------|
| `ADMIN` | Control total, gestión de usuarios. |
| `CAPTURISTA` | Captura y operación (alta/consulta/asignación). |

**Expansión propuesta** (de `Paleta de colores.MD` §Seguridad; requiere ampliar el enum `Role`
y, por afectar autorización, registrarse como **decisión/ADR** antes de implementar):

| Rol propuesto | Equivalente | Alcance |
|---------------|-------------|---------|
| Administrador | `ADMIN` *(existe)* | Control total, gestión de usuarios. |
| Supervisor | `SUPERVISOR` | Consulta, autorización, alta/baja y asignaciones. |
| Almacén | `ALMACEN` | Administración de inventario, alta y traslados. |
| Auditor | `AUDITOR` | Solo lectura y reportes/auditoría. |
| Consulta/Capturista | `CAPTURISTA`/`OPERADOR` *(existe)* | Operación limitada, sin PII completa. |

> Hasta que el enum se amplíe, los permisos finos de las demás pantallas se resuelven con los
> dos roles existentes (`ADMIN` / `CAPTURISTA`). La granularidad de 5 roles es un objetivo, no
> el estado actual.

**Why this priority**: Es el corazón de la seguridad de acceso; condiciona cada pantalla.

**Independent Test**: Un usuario "Consulta" no ve CURP/RFC ni el botón de asignar; un "Auditor"
solo lee y consulta la bitácora.

**Acceptance Scenarios**:

1. **Given** un usuario con rol limitado, **When** intenta una acción fuera de su alcance,
   **Then** el sistema la deniega (rechazo por defecto) y lo audita.
2. **Given** roles administrativos, **When** inician sesión, **Then** se les exige **MFA**
   (`docs/02-seguridad.md` §4) — *objetivo; aún no implementado.*

### Edge Cases

- Último administrador: no puede desactivarse a sí mismo si dejaría el sistema sin admin.
- Reseteo de contraseña / activación de cuenta: ya existente en backend; se integra aquí.
- Bloqueo por intentos fallidos (protección contra fuerza bruta).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir CRUD de usuarios (alta, consulta paginada, edición,
  activar/desactivar) restringido a rol administrador.
- **FR-002**: El sistema MUST aplicar RBAC con **mínimo privilegio** y **rechazo por defecto**.
  Roles **implementados**: `ADMIN`, `CAPTURISTA`. La expansión a Supervisor/Almacén/Auditor/
  Consulta MUST registrarse como decisión/ADR antes de ampliar el enum `Role`.
- **FR-003**: Las contraseñas MUST almacenarse con hashing fuerte (Argon2id/bcrypt), nunca en
  claro ni reversible. *(Implementado en backend.)*
- **FR-004**: Los roles administrativos SHOULD requerir **MFA** (pendiente; no implementado).
- **FR-005**: El login MUST tener protección contra fuerza bruta (rate limiting / bloqueo)
  (pendiente de reforzar).
- **FR-006**: Todo cambio de usuario o rol MUST quedar auditado (Principio V).
- **FR-007**: El sistema MUST impedir dejar el sistema sin ningún administrador activo.

### Key Entities

- **Usuario del sistema** (`user`/`users`): `name`, `email` (único, identidad de login),
  `password` (hash), `role` (`ADMIN`/`CAPTURISTA`), `enabled`. Distinto de **Elemento** (**003**).
  Ver [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
- **Rol** (`Role`): enum con su conjunto de permisos.
- **Sesión:** JWT (`token`, `tokenType`, `expiresIn`); recuperación de contraseña por `code`.

## Success Criteria *(mandatory)*

- **SC-001**: Cero contraseñas en texto plano o reversibles. *(Cumplido por el backend.)*
- **SC-002**: El 100% de acciones fuera del alcance del rol son denegadas y auditadas.
- **SC-003**: *(Objetivo)* Los roles administrativos no pueden operar sin MFA una vez
  implementada.

## Dependencies

- Constitución (Principios IV, V); `docs/02-seguridad.md` §4.
- Backend existente `fornituras-api/` (auth/usuarios) — se extiende, no se reescribe.
- Features: **012-auditoria** (registro de cambios), transversal al resto.
- Modelo de datos: [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
