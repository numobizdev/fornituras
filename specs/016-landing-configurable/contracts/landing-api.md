# Contract — API de Landing (`/api/v1/landing`)

Phase 1 del plan. Contrato REST del módulo `landing`. Todas las respuestas usan el envoltorio
estándar del proyecto `ApiResponse<T>` (`{ success, message, data }`). Roles reales:
`ADMIN`, `CAPTURISTA`.

## Convenciones

- Autenticación: JWT Bearer (salvo el endpoint público).
- Errores: formato de error estándar del proyecto, sin filtrar detalles internos.
- Estados: 200 OK, 201 Created, 400 Validación, 401 No autenticado, 403 Sin permiso,
  404 No encontrado, 429 Rate limit excedido.

## Endpoints de lectura

### GET `/api/v1/landing/public`  — público (permitAll + rate limit)

Devuelve las secciones `PUBLIC` activas, ordenadas. **Nunca incluye PII.**

- Auth: ninguna. Rate limiting Bucket4j (como `by-codigo`). 429 si se excede.
- 200 → `data: LandingSectionPublic[]` (puede ser `[]` → el front muestra estado por defecto).

```json
{
  "success": true,
  "message": "OK",
  "data": [
    {
      "type": "HERO",
      "titulo": "Sistema de Gestión de Blindajes",
      "subtitulo": "Acceso institucional",
      "cuerpo": null,
      "imagenUrl": "/assets/branding/hero.svg",
      "ctaLabel": "Acceder",
      "ctaUrl": "/login",
      "orden": 0,
      "quickLinks": []
    }
  ]
}
```

### GET `/api/v1/landing/home`  — autenticado (`isAuthenticated()`)

Secciones `HOME` activas, ordenadas. 401 si no hay sesión.

- 200 → `data: LandingSectionPublic[]`.

## Endpoints de administración (solo `ADMIN`)

### GET `/api/v1/landing/sections?scope={PUBLIC|HOME}`  — `hasRole('ADMIN')`

Lista completa (incluye inactivas) para el editor. 403 si no es ADMIN.

- 200 → `data: LandingSectionAdmin[]`.

### POST `/api/v1/landing/sections`  — `hasRole('ADMIN')`

Crea una sección. Body `LandingSectionCreateRequest`. Valida reglas de `data-model.md`.

- 201 → `data: LandingSectionAdmin`. 400 si falla validación. Registra auditoría.

```json
{
  "scope": "HOME",
  "type": "ANNOUNCEMENT",
  "titulo": "Mantenimiento programado",
  "subtitulo": null,
  "cuerpo": "El sistema estará en mantenimiento el domingo.",
  "imagenUrl": null,
  "ctaLabel": null,
  "ctaUrl": null,
  "orden": 2,
  "configJson": null
}
```

### PUT `/api/v1/landing/sections/{id}`  — `hasRole('ADMIN')`

Edita una sección existente. Body `LandingSectionUpdateRequest`.

- 200 → `data: LandingSectionAdmin`. 404 si no existe. 400 validación. Auditoría.

### PATCH `/api/v1/landing/sections/{id}/deactivate`  — `hasRole('ADMIN')`

Baja lógica (`active = false`). Idempotente.

- 200 → `data: LandingSectionAdmin`. 404 si no existe. Auditoría.

### PATCH `/api/v1/landing/sections/reorder`  — `hasRole('ADMIN')`

Actualiza el `orden` por lote. Body `ReorderRequest`.

- 200 → `data: LandingSectionAdmin[]` (nuevo orden). 400 si algún id no existe. Auditoría.

```json
{ "items": [ { "id": 10, "orden": 0 }, { "id": 7, "orden": 1 } ] }
```

## Matriz de autorización

| Endpoint | Anónimo | CAPTURISTA | ADMIN |
|----------|:------:|:----------:|:-----:|
| GET `/public` | ✅ (rate-limited) | ✅ | ✅ |
| GET `/home` | ❌ 401 | ✅ | ✅ |
| GET `/sections` | ❌ 401 | ❌ 403 | ✅ |
| POST/PUT/PATCH `/sections*` | ❌ 401 | ❌ 403 | ✅ |

## Seguridad del contrato

- `LandingSectionPublic` excluye `id`, `active`, `scope` y timestamps: solo lo necesario para
  render. Cero PII por diseño.
- Validación de URLs (esquema permitido) en todos los requests de escritura.
- El endpoint público añadido a `permitAll` en `SecurityConfig`; el resto cae en `/api/v1/**`
  autenticado.
