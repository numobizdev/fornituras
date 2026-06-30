# Palm Flow API — Module Documentation

This folder documents domain modules for AI agents and developers working on `palm-flow-api`.

## Module index

| Module | Package | Purpose |
|--------|---------|---------|
| [Geografía](./geografia.md) | `modules.geografia` | Shared geographic catalog: Estado → Municipio → Comunidad |
| [Centros de Acopio](./centros-acopio.md) | `modules.centros_acopio` | Regions and collection centers |
| [Productores](./productores.md) | `modules.productores` | Producers and their land parcels (Predios) |
| [Recepción de Fruta](./recepcion-fruta.md) | `modules.recepcion_fruta` | Fresh fruit reception transactions at collection centers |

## Cross-cutting conventions

- **Base URL:** `/palmFlowApi/api/v1/...`
- **Auth:** JWT Bearer token; roles `ADMINISTRADOR`, `ANALISTA` for domain CRUD
- **Multitenancy:** All domain entities use `@TenantId` on `tenant_id`; Hibernate injects from JWT via `TenantContext`
- **Responses:** Wrapped in `ApiResponse<T>` (`success`, `message`, `data`)
- **IDs:** Long auto-increment for catalogs; UUID for CentroAcopio, Predio, RecepcionFruta
- **Schema:** Hibernate `ddl-auto: update` in dev/prod; add Flyway migrations for production data migrations

## Entity relationship overview

```
Estado ──< Municipio ──< Comunidad
Region ──< CentroAcopio ──> Estado, Municipio, Comunidad
Productor ──< Predio ──> CentroAcopio, Estado, Municipio, Comunidad
CentroAcopio + Productor + User ──< RecepcionFruta
```

## When extending these modules

1. Follow the layered structure: `entity` → `repository` → `service` → `controller`, plus `dto` and `mapper`
2. Validate geographic FK consistency (municipio ∈ estado, comunidad ∈ municipio) in services
3. Never expose JPA entities from controllers — use DTOs
4. Add OpenAPI `@Tag` and `@Operation` on new endpoints
