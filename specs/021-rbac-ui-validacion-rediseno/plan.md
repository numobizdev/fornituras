# Implementation Plan: Visibilidad coherente por rol, validación visible y rediseño de Login/Asignación

**Branch**: `021-rbac-ui-validacion-rediseno` | **Date**: 2026-07-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/021-rbac-ui-validacion-rediseno/spec.md`

## Summary

El frontend decide visibilidad con reglas de 2 roles cuando el backend aplica 5 (ADR 0013), y
el seeder no garantiza el rol de la cuenta admin: el administrador quedó sin botones ni
módulos. Solución: (1) `DataSeeder` en modo *ensure* para el admin configurado; (2) matriz de
permisos **espejo** de `RolePolicy.cs` en el frontend (`core/security/role-policy.ts`) +
`hasAnyRole` en `AuthService`, consumida por menú y páginas con flags `computed()`; (3)
componente reutilizable de errores de campo aplicado a todos los formularios (incidencia
migra a Reactive Forms); (4) rediseño del login (split-screen institucional mobile-first,
escudo SVG provisional) y de Asignación (secciones en tarjetas conservando lo del PR #5);
(5) remediación de secretos: `appsettings.Development.json` sale del control de versiones.

## Technical Context

**Language/Version**: Backend C# / .NET 10 (ASP.NET Core Web API); Frontend TypeScript,
Angular 20.3 + Ionic 8 (componentes standalone, signals, control-flow `@if/@for`).

**Primary Dependencies**: Frontend: `@ionic/angular/standalone`, Reactive Forms,
`@capacitor/preferences`; sin dependencias nuevas. Backend: EF Core, BCrypt; sin dependencias
nuevas.

**Storage**: SQL Server 2022 (sin cambios de esquema; solo corrección de datos vía seeder).

**Testing**: Backend `dotnet test` (`tests/Fornituras.Api.Tests`); Frontend `ng test`
(Karma/Jasmine, specs junto al código).

**Target Platform**: Web + móvil (Capacitor); usuarios mayoritariamente en teléfonos.

**Project Type**: Monorepo web app — `fornituras-api-dotnet/` (API) + `sigefor/` (Ionic).

**Performance Goals**: N/A (sin rutas nuevas de datos; UI y autorización visual).

**Constraints**: La matriz RBAC del backend NO se modifica (decisión del usuario). Conservar
intactas las piezas del PR #5 (banner de escaneo, selección de cámara). No romper
forgot/reset-password (comparten `auth-page.scss`). Modo claro forzado, paleta gobmx.

**Scale/Scope**: 13 módulos, ~10 formularios, 3 pantallas auth, 1 seeder; ~25 archivos
frontend y 2-3 backend.

## Constitution Check

*GATE: evaluado contra la constitución v1.0.1 antes de Phase 0 y re-evaluado tras Phase 1.*

| Principio | Cumplimiento |
|---|---|
| I. Seguridad y privacidad primero | ✅ La feature ES una corrección de seguridad/UX de autorización; no toca QR ni PII nueva. El log del seeder no registra credenciales ni PII (solo email del admin configurado, ya presente en config). |
| II. QR sin datos personales | ✅ No se toca el contenido del QR; el componente de escaneo se reutiliza tal cual. |
| III. Cero secretos en el repo | ✅ **Esta feature remedia una violación existente**: `appsettings.Development.json` versionado con credenciales. Sale de git, se ignora, y se crea `.env.example`-equivalente (`appsettings.Development.json.example` con nombres/placeholder). Rotación documentada. |
| IV. Mínimo privilegio y authz en cada acceso | ✅ La autorización del servidor no se relaja; la UI se alinea a ella (rechazo por defecto para roles no reconocidos, FR-007). Ocultar en UI no sustituye la authz del servidor, que permanece. |
| V. Trazabilidad sin fugas | ✅ La corrección del seeder se loguea sin contraseñas. Sin PII en logs nuevos. |
| VI. ADR y stack congelado | ✅ Sin cambios de stack ni dependencias nuevas. No requiere ADR nuevo: aplica ADR 0013 (matriz) y ADR 0020 (identidad) existentes. |

**Nota constitución**: la constitución menciona roles `ADMIN/SUPERVISOR/OPERADOR` (texto
anterior al ADR 0013); la fuente vigente de roles es el ADR 0013 (5 roles), que esta feature
aplica. No hay conflicto de principios.

**Post-diseño (Phase 1)**: sin violaciones nuevas; ver `research.md` para decisiones.

## Project Structure

### Documentation (this feature)

```text
specs/021-rbac-ui-validacion-rediseno/
├── plan.md              # Este archivo
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   └── ui-permissions.md  # Contrato capacidad→roles (espejo RolePolicy.cs) y por pantalla
└── tasks.md             # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
fornituras-api-dotnet/
├── src/Fornituras.Api/
│   ├── Data/DataSeeder.cs                  # MODIFICAR: SeedAdminAsync → ensure rol/enabled
│   ├── appsettings.Development.json        # RETIRAR de git (queda local); crear .example
│   └── (sin otros cambios)
└── tests/Fornituras.Api.Tests/             # AÑADIR: tests del seeder ensure

sigefor/src/
├── theme/variables.scss                    # (solo lectura: paleta)
├── assets/img/escudo-sigefor.svg           # NUEVO: emblema provisional
└── app/
    ├── app.component.ts/.html              # MODIFICAR: mostrar rol; menú ya reactivo
    ├── core/
    │   ├── security/role-policy.ts         # NUEVO: matriz espejo de RolePolicy.cs
    │   ├── security/role-policy.spec.ts    # NUEVO
    │   ├── services/auth.service.ts        # MODIFICAR: hasAnyRole()
    │   ├── constants/app-navigation.ts     # MODIFICAR: Auditoría += AUDITOR
    │   ├── constants/role-labels.ts        # NUEVO: etiqueta es-MX por rol
    │   └── forms/field-errors.component.ts # NUEVO (+ .spec.ts): errores de campo
    ├── features/
    │   ├── auth/pages/login/               # REDISEÑAR (split-screen)
    │   ├── auth/styles/auth-page.scss      # EXTENDER sin romper forgot/reset
    │   ├── asignacion/pages/asignacion/    # REDISEÑAR (secciones) + rol reactivo
    │   ├── fornituras/ elementos/ almacenes/ tipos/ usuarios/
    │   │   traslados/ bajas/ incidencias/  # MODIFICAR: flags → computed + matriz;
    │   │   │                               #   forms → <app-field-errors>
    │   │   └── incidencias/pages/incidencia-form/  # MIGRAR a Reactive Forms
    │   └── landing/                        # (sin cambios)
    └── global.scss                         # MODIFICAR: .form-actions/.state-message compartidos
```

**Structure Decision**: monorepo existente; los cambios viven en las capas ya establecidas
(`core/` para piezas transversales nuevas, `features/*` para pantallas). La matriz de
permisos es una **constante tipada** en `core/security/` (no un servicio) porque es un espejo
estático de `RolePolicy.cs` — misma forma que `RolePolicy` en el backend (LEGO: una pieza,
dos lados del cable).

## Diseño por área

### A. Backend — seeder *ensure* (FR-001)

`SeedAdminAsync` pasa de "crear si no existe" a "asegurar estado": si la cuenta configurada
existe con `Role != Role.ADMIN` o `Enabled == false`, se corrige, se guarda y se loguea
`LogWarning("Seed admin corrected: role/enabled ensured for configured admin")` (sin
contraseña). Si no existe, se crea como hoy. Si `Seed.Admin.Enabled == false` o email vacío,
no hace nada (edge case cubierto). No toca ninguna otra cuenta. Tests: existente-con-rol-menor
→ corregido; existente-deshabilitado → habilitado; existente-correcto → sin cambios (no
`UpdatedAt` bump innecesario); inexistente → creado; seed deshabilitado → no-op.

### B. Frontend — matriz espejo y visibilidad (FR-002..FR-007)

`core/security/role-policy.ts` exporta constantes `ReadonlyArray<UserRole>` con los MISMOS
nombres que el backend para trazabilidad 1:1:

```
WRITE_INVENTORY   = ['ADMIN','ALMACEN','CAPTURISTA']
WRITE_TRANSFERS   = ['ADMIN','SUPERVISOR','ALMACEN','CAPTURISTA']
WRITE_OPERATIONS  = ['ADMIN','SUPERVISOR','CAPTURISTA']
AUTHORIZE_DECOMMISSION = ['ADMIN','SUPERVISOR']
WRITE_OFFICERS    = ['ADMIN','SUPERVISOR','CAPTURISTA']
MANAGE_CONFIG = MANAGE_LANDING = MANAGE_USERS = ['ADMIN']
READ_AUDIT        = ['ADMIN','AUDITOR']
```

`AuthService.hasAnyRole(roles)` = `role != null && roles.includes(role)` (rechazo por
defecto). Cada página reemplaza su flag por `computed(() => auth.hasAnyRole(POLICY.X))`
(FR-006). Mapa página→constante en `contracts/ui-permissions.md`. Correcciones visibles:
Bajas usa `AUTHORIZE_DECOMMISSION` (hoy muestra el botón a CAPTURISTA y el servidor lo
rechaza), Fornituras suma ALMACEN, Traslados suma SUPERVISOR+ALMACEN, Elementos/Asignación/
Incidencias suman SUPERVISOR, menú Auditoría suma AUDITOR. `app-navigation.ts` deriva
`roles` de estas constantes en lugar de arreglos literales.

Rol visible (FR-005): `core/constants/role-labels.ts` mapea enum→etiqueta es-MX
(ADMIN→"Administrador", ALMACEN→"Almacén", …); `app.component.html` la muestra bajo el
nombre/correo del usuario.

### C. Frontend — validación uniforme (FR-008..FR-011)

`core/forms/field-errors.component.ts` (standalone, OnPush): recibe `control: AbstractControl`
y `label?: string`; muestra el primer error activo cuando `touched || dirty`, con mensajes
es-MX centralizados (`required`, `email`, `minlength`, `maxlength`, `pattern` con override por
input `patternMessage`). Presentación reutiliza el patrón visual de `.auth-page__field-error`
(color danger, texto pequeño) movido a estilo global `.field-error`. Cada formulario añade
`<app-field-errors [control]="form.controls.x" />` bajo el campo; `onSubmit` conserva
`markAllAsTouched()`. Incidencia migra a `FormBuilder.nonNullable.group` con
`codigo (required + resuelto)`, `tipo (required)`, `descripcion (required, maxLength 500)`.
CURP/RFC ganan `patternMessage` descriptivo. `.form-actions`/`.state-message` se mueven a
`global.scss` y se eliminan de los ~15 SCSS que los duplican.

### D. Login split-screen (FR-012) — con skill ui-ux-pro-max

Layout: contenedor flex; panel institucional (guinda `--gobmx-guinda`, escudo SVG provisional
`assets/img/escudo-sigefor.svg` en dorado/blanco, "SIGEFOR" en fuente Patria + nombre
completo + barra dorada) y panel de formulario. `@media (min-width: 900px)`: dos columnas
50/50 a pantalla completa. Móvil (default, mobile-first): panel colapsa a cabecera compacta
(~200px) sobre el formulario. Los estilos nuevos viven en una clase específica del login
(`.login-split`) DENTRO de `auth-page.scss` o en SCSS propio del login, sin alterar las
clases que forgot/reset consumen. La lógica del formulario (validators, toasts, spinner) no
cambia.

### E. Asignación por secciones (FR-013) — con skill ui-ux-pro-max

Reestructura del template: el asistente sale de la `ion-list` única y se organiza en dos
`ion-card` numeradas ("1 · Identificar fornitura" con `<app-qr-scan>` + banner/spinner/
detalle del equipo tal cual; "2 · Seleccionar elemento" con el `ion-searchbar` fuera de
lista, resultados y seleccionado) + bloque de acciones al pie del asistente; "Asignaciones
vigentes" queda en su `ion-list` con encabezado propio y separación visual. Paso 2 se
muestra deshabilitado/atenuado hasta que hay fornitura disponible (guía de flujo). Sin tocar
`qr-scan` ni servicios. Flag `canAssign` → `computed` con `WRITE_OPERATIONS`.

### F. Secretos (FR-014, FR-015)

`git rm --cached` de `appsettings.Development.json` + entrada en `.gitignore`; se crea
`appsettings.Development.json.example` con placeholders (`__SET_ME__`) y nombres de claves;
`fornituras-api-dotnet/README` (o docs) documenta cómo configurar (archivo local o
`dotnet user-secrets`) y la **recomendación de rotar** la contraseña de la BD remota y la del
admin sembrado (ya expuestas en el historial). El archivo local del desarrollador se conserva
en disco (git deja de rastrearlo), por lo que su entorno sigue arrancando.

## Complexity Tracking

Sin violaciones a la constitución; tabla no aplicable.
