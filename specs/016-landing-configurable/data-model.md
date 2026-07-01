# Data Model — Landing configurable

Phase 1 del plan. Deriva de las entidades de la spec (§Key Entities) y las reglas de los
requisitos funcionales. La persistencia de "tour visto" vive en el cliente (Capacitor
Preferences), no en base de datos.

## Entidad: `LandingSection`

Extiende `BaseEntity` (aporta `id`, `createdAt`, `updatedAt` con auditoría JPA). Tabla
`landing_section` (Flyway `V19`).

| Campo | Tipo | Reglas / Notas |
|-------|------|----------------|
| `id` | Long (PK, identity) | Heredado de `BaseEntity`. |
| `scope` | enum `LandingScope` | `PUBLIC` \| `HOME`. Requerido. Determina en qué cara se muestra. |
| `type` | enum `LandingSectionType` | `HERO` \| `ANNOUNCEMENT` \| `QUICK_LINKS` \| `RICH_TEXT`. Requerido. |
| `titulo` | String(160) | Requerido para `HERO`/`ANNOUNCEMENT`/`RICH_TEXT`; opcional para `QUICK_LINKS`. Texto plano. |
| `subtitulo` | String(240), null | Opcional. Texto plano. |
| `cuerpo` | String(2000), null | Cuerpo del aviso/texto. Texto plano. Opcional. |
| `imagenUrl` | String(512), null | URL de imagen. Valida esquema `http/https` o ruta interna relativa. |
| `ctaLabel` | String(80), null | Etiqueta del botón de acción (p. ej. "Acceder"). Texto plano. |
| `ctaUrl` | String(512), null | Destino del CTA. Valida esquema permitido; rutas internas relativas o `http/https`. |
| `orden` | int | Requerido. Orden ascendente de aparición dentro de su `scope`. Default 0. |
| `active` | boolean | Requerido. Baja lógica (nunca DELETE físico). Default true. |
| `configJson` | String (NVARCHAR MAX), null | Solo para `QUICK_LINKS`: JSON array de `QuickLinkItem`. Null en otros tipos. |

### Value object: `QuickLinkItem` (dentro de `configJson`)

| Campo | Tipo | Reglas |
|-------|------|--------|
| `label` | String(60) | Requerido. Texto plano. |
| `url` | String(512) | Requerido. Ruta interna relativa o `http/https`. |
| `icon` | String(60), null | Nombre de icono Ionicons (opcional). |

### Reglas de validación (en el borde — DTOs de request)

- `scope` y `type` obligatorios y dentro del enum.
- Longitudes máximas según la tabla; se rechaza el exceso (400).
- `titulo` requerido cuando `type ∈ {HERO, ANNOUNCEMENT, RICH_TEXT}`.
- `QUICK_LINKS` requiere `configJson` con al menos 1 `QuickLinkItem` válido; los demás tipos
  deben enviar `configJson = null`.
- `ctaLabel` y `ctaUrl` van juntos: si hay uno, se exige el otro.
- URLs (`imagenUrl`, `ctaUrl`, `QuickLinkItem.url`): esquema permitido `http`, `https` o ruta
  interna que empiece por `/`. Se rechazan `javascript:`, `data:` y otros.
- Todo campo de texto se almacena literal; el escape ocurre en el render (Angular).

## Reglas de negocio / consultas

- Lectura pública: `findByScopeAndActiveTrueOrderByOrdenAsc(PUBLIC)`.
- Lectura home: `findByScopeAndActiveTrueOrderByOrdenAsc(HOME)`.
- Editor admin: `findByScopeOrderByOrdenAsc(scope)` (incluye inactivas).
- Desactivar: set `active = false` (no DELETE).
- Reordenar: actualización por lote de `orden` para una lista de `{id, orden}`.
- Toda escritura (crear/editar/desactivar/reordenar) registra auditoría con el actor ADMIN.

## Seed inicial (migración V19)

Filas por defecto para que ninguna cara aparezca vacía al arrancar:

- `PUBLIC` / `HERO`: título institucional + subtítulo + CTA "Acceder" → `/login`.
- `HOME` / `HERO`: bienvenida al panel.
- `HOME` / `ANNOUNCEMENT`: aviso de ejemplo (activo).
- `HOME` / `QUICK_LINKS`: accesos rápidos a Elementos, Fornituras y Asignación.

## DTOs (proyecciones)

- `LandingSectionPublic` (para `/public` y `/home`): `type, titulo, subtitulo, cuerpo,
  imagenUrl, ctaLabel, ctaUrl, orden, quickLinks[]`. **Sin** `id` interno sensible ni banderas
  administrativas; **sin PII** por diseño.
- `LandingSectionAdmin` (para el editor): todos los campos + `id, scope, active, createdAt,
  updatedAt`.
- `LandingSectionCreateRequest` / `LandingSectionUpdateRequest`: campos editables con
  validación `@Valid`.
- `ReorderRequest`: lista de `{ id, orden }`.
