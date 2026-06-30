# Módulo Centros de Acopio

**Package:** `mx.uumbal.solutions.palm_flow.modules.centros_acopio`

Manages **regions** and **collection centers** where fresh fruit is received and weighed.

## Domain model

```
Region (1) ──< CentroAcopio (N)
                    │
                    ├── FK: estado, municipio, comunidad (geografía)
                    └── coordinates: x, y, latitud, longitud
```

A **Region** groups multiple collection centers. Each **CentroAcopio** belongs to exactly one region and has geographic location data.

## Entities

### Region

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | Auto-increment PK |
| `nombre` | String | Region name |

### CentroAcopio

| Field | Type | Notes |
|-------|------|-------|
| `uuid` | UUID | PK, auto-generated |
| `nombre` | String | Center name |
| `region` | FK → Region | Required |
| `activo` | Enum | `SI`, `NO`, `EVENTUAL` |
| `x`, `y` | BigDecimal | Projected coordinates (UTM-like) |
| `latitud`, `longitud` | BigDecimal | WGS84 |
| `encargado` | String | Person in charge |
| `estado`, `municipio`, `comunidad` | FK | Geographic location |
| `distanciaKm` | BigDecimal | Distance metric |
| `alias` | String | Short alias |

## API endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET/POST/PUT/DELETE | `/api/v1/regiones` | Region CRUD |
| GET | `/api/v1/centros-acopio?regionId=` | List centers (optional region filter) |
| GET/POST/PUT/DELETE | `/api/v1/centros-acopio/{uuid}` | Center CRUD |

## Business rules

- Geographic FKs must be consistent: municipio ∈ estado, comunidad ∈ municipio
- `CentroAcopioService.validateGeografia()` enforces this on create/update
- Referenced by `Predio.centroAcopio` and `RecepcionFruta.centroAcopio`

## Enum: CentroAcopioActivo

- `SI` — Active center
- `NO` — Inactive
- `EVENTUAL` — Occasionally used

## Key files

- `entity/Region.java`, `CentroAcopio.java`, `CentroAcopioActivo.java`
- `service/RegionService.java`, `CentroAcopioService.java`
- `controller/RegionController.java`, `CentroAcopioController.java`

## Ionic app context

Collection center staff (encargado) use the mobile app at these centers. The center UUID is the anchor for fruit reception (`RecepcionFruta`) and predio assignment.
