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

Configurables en `appsettings.json` → `App:Seed:Admin`. En cada arranque el seeder
**garantiza** que la cuenta admin configurada tenga rol `ADMIN` y esté habilitada (021).

## Configuración local y secretos (021)

`appsettings.Development.json` **NO se versiona** (contiene cadena de conexión y credenciales;
principio III de la constitución). Para configurar tu entorno:

1. Copia la plantilla y rellena los valores:

   ```powershell
   Copy-Item src/Fornituras.Api/appsettings.Development.json.example src/Fornituras.Api/appsettings.Development.json
   ```

2. Alternativa sin archivo: `dotnet user-secrets` (proyecto `src/Fornituras.Api`):

   ```powershell
   dotnet user-secrets set "ConnectionStrings:Default" "<cadena>" --project src/Fornituras.Api
   dotnet user-secrets set "App:Seed:Admin:Email" "<correo>" --project src/Fornituras.Api
   dotnet user-secrets set "App:Seed:Admin:Password" "<contraseña>" --project src/Fornituras.Api
   ```

Claves esperadas (solo nombres; los valores nunca se documentan):
`ConnectionStrings:Default`, `App:Seed:Admin:{Enabled,Name,Email,Password}`,
`App:Cors:AllowedOrigins`.

> ⚠️ **Rotación pendiente**: versiones anteriores del repositorio incluyeron
> `appsettings.Development.json` con la contraseña de la BD de desarrollo remota y la del
> admin sembrado. Un secreto commiteado se considera comprometido: **rotar la contraseña de
> la BD (panel del proveedor) y la del admin** en cuanto sea posible.

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
