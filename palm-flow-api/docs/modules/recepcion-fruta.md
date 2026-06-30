# Módulo Recepción de Fruta Fresca

**Package:** `mx.uumbal.solutions.palm_flow.modules.recepcion_fruta`

Transactional module for **fresh fruit reception** at collection centers. When a producer arrives with a vehicle load, the center operator registers the reception via the Ionic app, capturing vehicle data and scale weights.

## Domain model

```
RecepcionFruta
    ├── centroAcopio (where reception happens)
    ├── productor (who delivered the fruit)
    └── usuario (User who registered the reception)
```

## Entity: RecepcionFruta

| Field | Type | Notes |
|-------|------|-------|
| `uuid` | UUID | PK |
| `folio` | String | Auto-generated, e.g. `RF202600001` |
| `fecha` | Instant | Reception timestamp |
| `centroAcopio` | FK (UUID) | Collection center |
| `productor` | FK | Delivering producer |
| `placa`, `modelo`, `marca` | String | Vehicle data |
| `tipoColor` | String | Vehicle color/type |
| `propietario` | String | Vehicle owner |
| `pesoTara`, `pesoNeto` | BigDecimal | Weights in kg |
| `origenPeso` | Enum | `MANUAL`, `DISPOSITIVO`, `IMAGEN` |
| `usuario` | FK → User | Operator who registered |
| `registroOffline` | boolean | Created offline on mobile |
| `fechaSync` | Instant | When offline record synced to server |

## Folio generation

`FolioGeneratorService` generates sequential folios per calendar year:

- Format: `RF` + `YYYY` + 6-digit sequence
- Example: `RF202600001`, `RF202600002`
- Thread-safe via `synchronized` + `@Transactional`

Folio is assigned automatically on create; clients must not send it.

## API endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/recepciones-fruta?centroAcopioUuid=&productorId=` | List with optional filters |
| GET | `/api/v1/recepciones-fruta/{uuid}` | Get by UUID |
| POST | `/api/v1/recepciones-fruta` | Register reception (auto folio) |
| PUT | `/api/v1/recepciones-fruta/{uuid}` | Update |
| PATCH | `/api/v1/recepciones-fruta/{uuid}/sync` | Mark as synced (sets `fechaSync`) |
| DELETE | `/api/v1/recepciones-fruta/{uuid}` | Delete |

## Offline workflow (Ionic app)

1. Operator registers reception with `registroOffline: true` when no connectivity
2. Record stored locally on device with provisional data
3. When online, POST to API or PATCH `/sync` to set `fechaSync`
4. On server create with `registroOffline: true`, `fechaSync` defaults to `Instant.now()` if not provided

## Enum: OrigenPeso

| Value | Meaning |
|-------|---------|
| `MANUAL` | Weight entered manually |
| `DISPOSITIVO` | Read from scale/device |
| `IMAGEN` | Derived from image recognition |

## Authorization

Requires `ADMINISTRADOR` or `ANALISTA` role. Collection center operators typically have `ANALISTA`.

## Key files

- `entity/RecepcionFruta.java`, `OrigenPeso.java`
- `service/RecepcionFrutaService.java`, `FolioGeneratorService.java`
- `controller/RecepcionFrutaController.java`
- `repository/RecepcionFrutaRepository.java`

## Related modules

- **Centros de Acopio:** `centroAcopioUuid` references collection center
- **Productores:** `productorId` references delivering producer
- **Users:** `usuarioId` references the authenticated operator (`modules.users.entity.User`)
