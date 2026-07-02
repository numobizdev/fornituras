# Quickstart — API .NET (017-migracion-api-dotnet)

## Prerrequisitos

- .NET SDK **10.0.202** (`dotnet --version`)
- **SQL Server 2022** local en ejecución
- Node.js + npm (para Ionic)

## 1. Base de datos

Crear BD vacía (puede dropear la usada por Java):

```sql
CREATE DATABASE fornituras;
```

Ajustar cadena de conexión en `src/Fornituras.Api/appsettings.json` o User Secrets:

```powershell
cd fornituras-api-dotnet/src/Fornituras.Api
dotnet user-secrets set "ConnectionStrings:DefaultConnection" "Server=localhost;Database=fornituras;Trusted_Connection=True;TrustServerCertificate=True"
```

## 2. Migraciones y arranque

```powershell
cd fornituras-api-dotnet
dotnet ef database update --project src/Fornituras.Api
dotnet run --project src/Fornituras.Api
```

- API: `http://localhost:8080/sigefor/api/v1`
- OpenAPI (dev): `http://localhost:8080/sigefor/openapi/v1.json`
- Health: `http://localhost:8080/sigefor/actuator/health`

## 3. Usuario seed (dev)

| Email | Password | Rol |
|-------|----------|-----|
| `admin@fornituras.local` | `Admin#2026` | ADMIN |

## 4. Probar login

```powershell
curl -X POST http://localhost:8080/sigefor/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d '{"email":"admin@fornituras.local","password":"Admin#2026"}'
```

## 5. Conectar Ionic

`sigefor/src/environments/environment.ts`:

```typescript
apiUrl: 'http://localhost:8080/sigefor/api/v1',
```

```powershell
cd sigefor
npm start
```

## 6. Smoke test manual (Ionic)

1. Login con admin seed
2. Almacenes → listar/crear
3. Tipos → listar (catálogo TIPO_PRENDA)
4. Fornituras → alta y consulta
5. Elementos → alta (PII cifrada)
6. Asignación → asignar y devolver

## Troubleshooting

| Problema | Solución |
|----------|----------|
| SQL Server no conecta | Verificar instancia local; ajustar connection string |
| CORS | `App:Cors:AllowedOrigins` incluye `:8100` y `:4200` |
| 401 | Token expirado; `expiresIn` en ms |
