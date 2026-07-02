# Contrato REST — Consumido por sigefor (Ionic)

**Base URL (dev)**: `{host}/sigefor/api/v1`  
**Envelope**: `ApiResponse<T>` → `{ success: boolean, message: string, data: T }`  
**Auth**: `Authorization: Bearer {token}` (excepto rutas públicas de auth)

Referencia frontend: `sigefor/src/environments/environment.ts`, servicios en `sigefor/src/app/`.

## Auth (P1)

| Método | Path | Body | Response `data` |
|--------|------|------|-----------------|
| POST | `/auth/login` | `{ email, password }` | `AuthResponse` |
| POST | `/auth/forgot-password` | `{ email }` | `null` |
| POST | `/auth/reset-password` | `{ code, newPassword }` | `null` |

**AuthResponse**: `{ token, tokenType: "Bearer", expiresIn: number (ms), user: UserSummary }`  
**UserSummary**: `{ id, name, email, role: "ADMIN" \| "CAPTURISTA" }`

Rutas Java no cableadas en Ionic (paridad P2): `/auth/activate`, `/auth/change-password`.

## Warehouses (P1)

Base: `/warehouses`

| Método | Path | Query | Body |
|--------|------|-------|------|
| GET | `/warehouses` | `active`, `tipo`, `page`, `size` | — |
| GET | `/warehouses/{id}` | — | — |
| POST | `/warehouses` | — | `WarehouseCreateRequest` |
| PUT | `/warehouses/{id}` | — | `WarehouseCreateRequest` |
| PATCH | `/warehouses/{id}/deactivate` | — | `{}` |

## Equipment (P1)

Base: `/equipment`

| Método | Path | Query | Body |
|--------|------|-------|------|
| GET | `/equipment` | `q`, `status`, `equipmentTypeId`, `sizeId`, `warehouseId`, `page`, `size` | — |
| GET | `/equipment/{id}` | — | — |
| GET | `/equipment/by-codigo/{codigo}` | — | — |
| POST | `/equipment` | — | `EquipmentCreateRequest` |
| POST | `/equipment/batch` | — | `BatchCreateRequest` |
| PUT | `/equipment/{id}` | — | `EquipmentCreateRequest` |
| PATCH | `/equipment/{id}/status` | — | `{ status }` |

## Equipment types & sizes (P1)

| Método | Path | Query | Body |
|--------|------|-------|------|
| GET | `/equipment-types` | `active`, `page`, `size` | — |
| GET | `/equipment-types/{id}` | — | — |
| POST | `/equipment-types` | — | `EquipmentTypeCreateRequest` |
| PUT | `/equipment-types/{id}` | — | `EquipmentTypeCreateRequest` |
| PATCH | `/equipment-types/{id}/deactivate` | — | `{}` |
| GET | `/sizes` | `equipmentTypeId` | — |
| POST | `/sizes` | — | `SizeCreateRequest` |
| PATCH | `/sizes/{id}/deactivate` | — | `{}` |

## Officers & catalogs (P1)

| Método | Path | Query | Body |
|--------|------|-------|------|
| GET | `/officers` | `q`, `municipioId`, `sexoId`, `page`, `size` | — |
| GET | `/officers/{id}` | — | — |
| POST | `/officers` | — | `OfficerCreateRequest` |
| GET | `/sexos` | — | — |
| GET | `/tipos-sangre` | — | — |

## Assignments (P1)

Base: `/assignments`

| Método | Path | Body |
|--------|------|------|
| GET | `/assignments` | Query: `page`, `size` |
| POST | `/assignments` | `{ equipmentId, officerId, observaciones? }` |
| POST | `/assignments/{id}/return` | `{}` |
| POST | `/assignments/reassign` | `{ equipmentId, newOfficerId, observaciones? }` |

## Municipios (P1)

| Método | Path | Query | Body |
|--------|------|-------|------|
| GET | `/municipios` | `active`, `size` | — |
| POST | `/municipios` | — | `MunicipioCreateRequest` |

## Paginación

Query: `page` (0-based), `size`.

Response `Page<T>`:

```json
{
  "content": [],
  "totalElements": 0,
  "totalPages": 0,
  "number": 0,
  "size": 20
}
```

## Errores

- 400: `{ success: false, message: "...", data: { "field": "error" } }` (validación)
- 401: no autenticado
- 403: sin permiso
- 404: no encontrado
- 409: conflicto (p. ej. asignación concurrente)

## Modelos TypeScript de referencia

- `sigefor/src/app/core/models/api-response.model.ts`
- `sigefor/src/app/core/models/auth.model.ts`
- `sigefor/src/app/features/*/data/*.model.ts`

## Contratos detallados por feature (specs existentes)

- Officers: `specs/003-elementos-padron/contracts/officers-api.md`
- Assignments: `specs/004-asignacion-resguardos/contracts/assignments-api.md`
- QR (P3): `specs/002-qr-equipos/contracts/qr-api.md`
