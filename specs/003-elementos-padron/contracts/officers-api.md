# Phase 1 — Contrato REST: Padrón de elementos

Endpoints del módulo `officers`. Todos requieren **JWT válido** (auth existente) y aplican
**autorización por rol**. Las respuestas siguen el envoltorio `ApiResponse<T>` ya usado en el
backend. Ninguna respuesta incluye PII no autorizada; ningún parámetro de PII viaja en la URL.

## Convenciones

- Base: `/api` (según configuración existente). Autenticación: `Authorization: Bearer <jwt>`.
- Errores: estructura genérica de `ApiResponse` (sin filtrar detalles internos ni PII).
- Paginación: `page` (0-based), `size`; respuesta con `content`, `totalElements`, `totalPages`.

## `GET /officers` — listado paginado con búsqueda y filtros

Roles: `ADMIN`, `CAPTURISTA` (lectura). PII enmascarada según rol.

**Query params:**
- `q` (opcional): texto libre. El servidor decide la estrategia: igualdad por blind index si
  parece CURP/RFC, o igualdad/parcial por `placa`. La búsqueda por **nombre parcial no está
  disponible** (PII cifrada a nivel app, no determinista — ADR 0006).
- `municipio` (opcional, **texto libre**, `LIKE`), `sexoId` (opcional, catálogo): filtros.
- `page`, `size`: paginación.

**200** → `ApiResponse<Page<OfficerSummary>>`

```jsonc
// OfficerSummary (campos no sensibles + foto miniatura si el rol lo permite)
{
  "id": 123,
  "nombreCompleto": "GARCÍA LÓPEZ, JUAN",   // o enmascarado según rol
  "placa": "PM-1042",
  "municipio": "Centro",                       // texto libre
  "estado": "Tabasco",                         // texto libre
  "tipoSangre": "O+",                          // null/enmascarado si no autorizado
  "fotoThumbUrl": "/api/officers/123/foto?variant=thumb" // requiere authz; null si no
}
```

> El `q` **nunca** se registra crudo en logs si pudo contener PII (se omite o se hashea).

## `POST /officers` — alta de elemento

Roles: `ADMIN`, `CAPTURISTA` (alta). Validación en el borde.

**Body** `OfficerCreateRequest`:

```jsonc
{
  "placa": "PM-1042",            // requerido, único (normalizado)
  "nombre": "JUAN",             // requerido
  "apellidoPaterno": "GARCÍA",  // requerido
  "apellidoMaterno": "LÓPEZ",   // opcional
  "sexoId": 1,                   // requerido (catálogo plano)
  "tipoSangreId": 3,             // opcional (catálogo plano)
  "municipio": "Centro",         // opcional (texto libre)
  "estado": "Tabasco",           // opcional (texto libre)
  "curp": "GALJ900101HDFXXX01",  // [PENDIENTE ADR 0003] — solo si habilitado
  "rfc": "GALJ900101AB1"         // [PENDIENTE ADR 0003] — solo si habilitado
  // la foto se sube por endpoint aparte (multipart), ver abajo
}
```

**201** → `ApiResponse<OfficerSummary>`
**409** si `placa` (o CURP/RFC normalizados) ya existen.
**422/400** si la validación de formato falla.

Efecto: crea el elemento, calcula blind index de CURP/RFC si aplica, **audita** `CREATE_OFFICER`.

## `GET /officers/{id}` — ficha (auditada)

Roles: `ADMIN` (completa), `CAPTURISTA` (operativa, CURP/RFC enmascarados).

**200** → `ApiResponse<OfficerDetail>` — superset de `OfficerSummary` con los campos que el rol
pueda ver (CURP/RFC en claro **solo** para rol autorizado; enmascarados o ausentes para el resto).

> **Side effect obligatorio:** emite registro de auditoría `VIEW_OFFICER` (actor, id, ts, IP).
> Cumple SC-002.

## `PUT /officers/{id}` — edición

Roles: `ADMIN` (y `CAPTURISTA` para campos no identitarios, según política). Mismas validaciones
que el alta; `placa`/CURP/RFC como ancla de identidad → edición restringida y auditada
(`UPDATE_OFFICER`).

## `POST /officers/{id}/foto` y `GET /officers/{id}/foto` — foto

- **Subida** (`multipart/form-data`): valida tipo/tamaño; guarda en storage cifrado; actualiza
  `foto_url`. Roles con permiso de alta/edición.
- **Descarga**: el backend verifica rol y **audita** el acceso; nunca es una URL pública/cacheable.
  `[PENDIENTE ADR 0003]` mientras la captura de foto esté restringida.

## Autorización — resumen

| Endpoint | ADMIN | CAPTURISTA | Notas |
|----------|:-----:|:----------:|-------|
| GET /officers | ✅ | ✅ (enmascarado) | PII sensible oculta a CAPTURISTA |
| POST /officers | ✅ | ✅ | validación en el borde |
| GET /officers/{id} | ✅ (completa) | ✅ (enmascarada) | **auditado** |
| PUT /officers/{id} | ✅ | ⚠️ parcial | identidad restringida |
| foto (subir/ver) | ✅ | ⚠️ según política | acceso auditado |

> La expansión a roles Supervisor/Almacén/Auditor/Consulta (spec 013) ajustará esta matriz vía
> ADR antes de ampliar el enum `Role`.
