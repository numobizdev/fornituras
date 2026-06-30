# Módulo Productores

**Package:** `mx.uumbal.solutions.palm_flow.modules.productores`

Manages **producers** (productores) and their **land parcels** (predios).

## Domain model

```
Productor (1) ──< Predio (N)
                    │
                    ├── FK: centroAcopio (assigned collection center)
                    └── FK: estado, municipio, comunidad
```

A producer can have multiple predios. Each predio is linked to a collection center and stores geospatial polygon data (WKT).

## Entity: Productor

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | Auto-increment PK |
| `nombre`, `nombre2` | String | Primary and secondary name |
| `genero` | Enum | `M`, `F` |
| `telefono` | String | Phone |
| `correoElectronico` | String | Email |
| `rfc` | String | Tax ID |
| `tipoPersona` | Enum | `FISICA`, `MORAL` |
| `activo` | boolean | Active flag |
| `frecuente` | Enum | `F` (frecuente), `NF`, `PDTE` |
| `fiscalId` | String | Fiscal identifier |
| `typeCert` | String | Certification type |
| `idAkk` | String | External AKK identifier |
| `nivelRspo` | Enum | `NINGUNO`, `NIVEL_INICIAL`, `HITO_A_PROGRESO`, `HITO_B_CUMPLIMIENTO_TOTAL` |

## Entity: Predio

| Field | Type | Notes |
|-------|------|-------|
| `uuid` | UUID | PK |
| `nombre` | String | Parcel name |
| `anioPlantacion` | Integer | Planting year |
| `productor` | FK | Owner producer |
| `centroAcopio` | FK (UUID) | Assigned collection center |
| `estado`, `municipio`, `comunidad` | FK | Location |
| `etapa` | Enum | `PRODUCTIVA`, `PREPRODUCTIVA` |
| `tipoPredio` | Enum | `EJIDAL`, `PEQUENA_PROPIEDAD` |
| `actividadPrimaria` | Enum | `COMPLEMENTARIA`, `PRIMARIA` |
| `idGis` | String | GIS identifier |
| `latitud`, `longitud`, `x`, `y` | BigDecimal | Coordinates |
| `hectareas` | BigDecimal | Area |
| `ramsar`, `anp`, `cambio` | boolean | Environmental flags |
| `eudr` | Integer | 0, 1, or 2 (EUDR compliance) |
| `riesgo` | Integer | 0–4 risk level |
| `wkt` | String (LOB) | WKT polygon for map rendering |

### WKT field

Stores Well-Known Text polygon geometry (e.g. `POLYGON ((lon lat, ...))`) for drawing parcel boundaries in Google Maps, Google Earth, or similar libraries. Returned in API responses for UI consumption.

## API endpoints

### Productores

| Method | Path | Description |
|--------|------|-------------|
| GET/POST | `/api/v1/productores` | List / Create |
| GET/PUT/DELETE | `/api/v1/productores/{id}` | Read / Update / Delete |

### Predios

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/predios?productorId=` | List (optional producer filter) |
| GET/POST/PUT/DELETE | `/api/v1/predios/{uuid}` | CRUD |

## Business rules

- Geographic FK consistency validated in `PredioService` (same as Centros de Acopio)
- `eudr` constrained to 0–2, `riesgo` to 0–4 via DTO validation
- Producers referenced by `RecepcionFruta.productor`

## Key files

- `entity/Productor.java`, `Predio.java`, enums in `entity/`
- `service/ProductorService.java`, `PredioService.java`
- `controller/ProductorController.java`, `PredioController.java`
