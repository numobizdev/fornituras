# Módulo Geografía

**Package:** `mx.uumbal.solutions.palm_flow.modules.geografia`

Shared geographic catalog reused by **Centros de Acopio**, **Productores (Predios)**, and any future modules needing location data.

## Hierarchy

```
Estado (1) ──< Municipio (N) ──< Comunidad (N)
```

Each level is tenant-scoped (`@TenantId`).

## Entities

| Entity | Table | PK | Fields |
|--------|-------|-----|--------|
| `Estado` | `estados` | `Long id` | `nombre` |
| `Municipio` | `municipios` | `Long id` | `nombre`, FK `estado_id` |
| `Comunidad` | `comunidades` | `Long id` | `nombre`, FK `municipio_id` |

## API endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/estados` | Paginated list |
| GET/POST/PUT/DELETE | `/api/v1/estados/{id}` | CRUD |
| GET | `/api/v1/municipios?estadoId=` | List (optional filter by estado) |
| GET/POST/PUT/DELETE | `/api/v1/municipios/{id}` | CRUD |
| GET | `/api/v1/comunidades?municipioId=` | List (optional filter by municipio) |
| GET/POST/PUT/DELETE | `/api/v1/comunidades/{id}` | CRUD |

## Business rules

- Creating/updating `Municipio` requires a valid `estadoId`
- Creating/updating `Comunidad` requires a valid `municipioId`
- Downstream modules (CentroAcopio, Predio) additionally validate that municipio belongs to estado and comunidad belongs to municipio

## Typical UI flow

1. Load estados for dropdown
2. On estado selection, load municipios filtered by `estadoId`
3. On municipio selection, load comunidades filtered by `municipioId`

## Key files

- Entities: `entity/Estado.java`, `Municipio.java`, `Comunidad.java`
- Services: `service/EstadoService.java`, `MunicipioService.java`, `ComunidadService.java`
- Controllers: `controller/EstadoController.java`, `MunicipioController.java`, `ComunidadController.java`
