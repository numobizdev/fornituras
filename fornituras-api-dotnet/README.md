# fornituras-api-dotnet — SIGEFOR REST API

Backend **ASP.NET Core Web API** (.NET 10) para SIGEFOR. Reemplaza `fornituras-api/` (Java).

## Requisitos

- .NET SDK **10.0.202** (ver `global.json`)
- SQL Server 2022 local

## Arranque rápido

```powershell
cd fornituras-api-dotnet
dotnet restore
dotnet ef database update --project src/Fornituras.Api
dotnet run --project src/Fornituras.Api
```

- API: `http://localhost:8080/sigefor/api/v1`
- OpenAPI (dev): `http://localhost:8080/sigefor/openapi/v1.json`
- Health: `http://localhost:8080/sigefor/actuator/health`

### Usuario seed (dev)

| Email | Password | Rol |
|-------|----------|-----|
| `admin@fornituras.local` | `Admin#2026` | ADMIN |
| `responsable.prueba@fornituras.local` | `Prueba#2026` | ALMACEN |

Configurables en `appsettings.json` → `App:Seed:Admin`.

### Ionic

En `sigefor/src/environments/environment.ts`:

```typescript
apiUrl: 'http://localhost:8080/sigefor/api/v1',
```

## Estructura

```
src/Fornituras.Api/
├── Controllers/     # REST /api/v1/*
├── Services/        # Lógica de negocio
├── Data/            # EF Core, entidades, migraciones, seeder
├── Security/        # JWT, RBAC (RolePolicy)
├── Common/          # ApiResponse, crypto PII, utilidades
└── Dto/             # Contratos JSON
```

## Tests

```powershell
dotnet test fornituras-api-dotnet/Fornituras.Api.sln
```

## Documentación

- Migración: [`specs/017-migracion-api-dotnet/`](../specs/017-migracion-api-dotnet/)
- ADR stack: [`docs/04-decisiones/0016-backend-aspnetcore.md`](../docs/04-decisiones/0016-backend-aspnetcore.md)
