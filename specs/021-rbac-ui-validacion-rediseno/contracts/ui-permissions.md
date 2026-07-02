# Contrato — Permisos de UI por pantalla (espejo de RolePolicy.cs)

Fuente de autoridad: `fornituras-api-dotnet/src/Fornituras.Api/Security/RolePolicy.cs`
(ADR 0013). Este contrato fija qué constante consume cada superficie de UI y qué endpoint
respalda cada acción. La UI NUNCA muestra una acción cuyo endpoint rechazaría al rol.

## Constantes (role-policy.ts ≡ RolePolicy.cs)

| Constante | Roles |
|---|---|
| WRITE_INVENTORY | ADMIN, ALMACEN, CAPTURISTA |
| WRITE_TRANSFERS | ADMIN, SUPERVISOR, ALMACEN, CAPTURISTA |
| WRITE_OPERATIONS | ADMIN, SUPERVISOR, CAPTURISTA |
| AUTHORIZE_DECOMMISSION | ADMIN, SUPERVISOR |
| WRITE_OFFICERS | ADMIN, SUPERVISOR, CAPTURISTA |
| MANAGE_CONFIG | ADMIN |
| MANAGE_LANDING | ADMIN |
| MANAGE_USERS | ADMIN |
| READ_AUDIT | ADMIN, AUDITOR |

## Menú lateral (`app-navigation.ts` + filtro en `app.component.ts`)

| Ítem | Regla | Cambio vs hoy |
|---|---|---|
| Inicio, Elementos, Fornituras, Asignación, Traslados, Incidencias, Bajas, Reportes, Catálogo de Tipos, Almacenes | sin `roles` (todo autenticado) | sin cambio |
| Bitácora de Auditoría | READ_AUDIT | **+AUDITOR** |
| Usuarios | MANAGE_USERS | sin cambio (pasa a constante) |
| Configurar landing | MANAGE_LANDING | sin cambio (pasa a constante) |

Además el encabezado del menú muestra `nombre`, `email` y **etiqueta del rol** (FR-005).

## Acciones por pantalla

| Pantalla (archivo .ts) | Flag actual | Flag nuevo (computed) | Endpoint respaldo | Cambio de roles |
|---|---|---|---|---|
| Fornituras (`fornituras.page.ts`) | `canWrite = ADMIN\|\|CAPTURISTA` | `hasAnyRole(WRITE_INVENTORY)` | POST/PATCH `/equipment`, `/qr` | **+ALMACEN** |
| Form fornitura / lote | (ruta accesible) | ídem WRITE_INVENTORY | ídem | +ALMACEN |
| Elementos (`elementos.page.ts`) | `canWrite = ADMIN\|\|CAPTURISTA` | `hasAnyRole(WRITE_OFFICERS)` | POST/PATCH `/officers` | **+SUPERVISOR** |
| Asignación (`asignacion.page.ts`) | `canAssign = ADMIN\|\|CAPTURISTA` | `hasAnyRole(WRITE_OPERATIONS)` | POST `/assignments/*` | **+SUPERVISOR** |
| Incidencias (`incidencias.page.ts`) | `canWrite = ADMIN\|\|CAPTURISTA` | `hasAnyRole(WRITE_OPERATIONS)` | POST/PATCH `/incidents` | **+SUPERVISOR** |
| Traslados (`traslados.page.ts`) | `canWrite = ADMIN\|\|CAPTURISTA` | `hasAnyRole(WRITE_TRANSFERS)` | POST `/transfers` | **+SUPERVISOR, +ALMACEN** |
| Bajas (`bajas.page.ts`) | `canWrite = ADMIN\|\|CAPTURISTA` | `hasAnyRole(AUTHORIZE_DECOMMISSION)` | POST `/decommissions` | **−CAPTURISTA, +SUPERVISOR** (hoy CAPTURISTA ve un botón que el servidor rechaza con 403) |
| Almacenes (`almacenes.page.ts`) | `isAdmin = ADMIN` | `hasAnyRole(MANAGE_CONFIG)` | POST/PATCH `/warehouses` | sin cambio |
| Tipos (`tipos.page.ts`) | `isAdmin = ADMIN` | `hasAnyRole(MANAGE_CONFIG)` | POST/PATCH `/catalog/*` | sin cambio |
| Usuarios (`usuarios.page.ts`) | route guard admin | `hasAnyRole(MANAGE_USERS)` (+guard) | `/users/*` | sin cambio |
| Configurar landing | route guard admin | `hasAnyRole(MANAGE_LANDING)` (+guard) | `/landing/*` | sin cambio |

## Componente de errores de campo (contrato de uso)

```
<app-field-errors [control]="form.controls.campo" [patternMessage]="'…'" />
```

- Muestra el primer error cuando el control está `touched || dirty`.
- `role="alert"`/`aria-live="polite"` para accesibilidad.
- Sin control o control válido → no renderiza nada.

## Matriz de verificación por rol (para tests y QA manual)

| Superficie | ADMIN | SUPERVISOR | ALMACEN | AUDITOR | CAPTURISTA |
|---|---|---|---|---|---|
| Menú: ítems visibles | 13 | 10 | 10 | 11 (10+Auditoría) | 10 |
| FAB Fornituras (alta/lote) | ✔ | ✖ | ✔ | ✖ | ✔ |
| FAB Elementos | ✔ | ✔ | ✖ | ✖ | ✔ |
| Asistente Asignación | ✔ | ✔ | ✖ | ✖ | ✔ |
| Botón Incidencias | ✔ | ✔ | ✖ | ✖ | ✔ |
| Botón Traslados | ✔ | ✔ | ✔ | ✖ | ✔ |
| Botón Bajas | ✔ | ✔ | ✖ | ✖ | ✖ |
| FAB Almacenes / Tipos | ✔ | ✖ | ✖ | ✖ | ✖ |
| Menú Usuarios / Landing | ✔ | ✖ | ✖ | ✖ | ✖ |
| Menú Bitácora de Auditoría | ✔ | ✖ | ✖ | ✔ | ✖ |
| Rol visible en menú | ✔ | ✔ | ✔ | ✔ | ✔ |
