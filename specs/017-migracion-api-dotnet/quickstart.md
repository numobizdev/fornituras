# Quickstart — API .NET (017-migracion-api-dotnet)

Guía para levantar `fornituras-api-dotnet/` en desarrollo local. Completar tras Phase 0–1 de
implementación.

## Prerrequisitos

- .NET SDK **10.0.202** (`dotnet --version`)
- SQL Server 2022 local
- Node.js + npm (para Ionic, opcional en esta fase)

## 1. Base de datos

Crear BD vacía (puede dropear la usada por Java):

```sql
CREATE DATABASE fornituras;
```

## 2. Secretos locales

Desde `fornituras-api-dotnet/src/Fornituras.Api/`:

```powershell
dotnet user-secrets init
dotnet user-secrets set "ConnectionStrings:Default" "Server=localhost;Database=fornituras;TrustServerCertificate=True;User Id=sa;Password=TU_PASSWORD"
dotnet user-secrets set "App:Jwt:Secret" "TU_CLAVE_SECRETA_MIN_32_BYTES_BASE64_O_HEX"
dotnet user-secrets set "App:Seed:Admin:Password" "TuPasswordDev#123"
dotnet user-secrets set "App:Pii:EncryptionKey" "TU_CLAVE_AES_256"
dotnet user-secrets set "App:Pii:BlindIndexKey" "TU_CLAVE_HMAC"
```

> Nunca commitees estos valores. Ver `docs/02-seguridad.md`.

## 3. Migraciones y arranque

```powershell
cd fornituras-api-dotnet
dotnet ef database update --project src/Fornituras.Infrastructure --startup-project src/Fornituras.Api
dotnet run --project src/Fornituras.Api
```

API esperada: `http://localhost:{puerto}/sigefor/api/v1`  
Swagger: `http://localhost:{puerto}/sigefor/swagger`

## 4. Probar login (curl)

```powershell
curl -X POST http://localhost:8080/sigefor/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d '{"email":"admin@example.com","password":"TuPasswordDev#123"}'
```

Respuesta esperada: `{ "success": true, "data": { "token", "tokenType": "Bearer", "expiresIn", "user" } }`.

## 5. Conectar Ionic

En `sigefor/src/environments/environment.ts`:

```typescript
apiUrl: 'http://localhost:8080/sigefor/api/v1',
```

Ajustar host/puerto si .NET no usa 8080.

```powershell
cd sigefor
npm start
```

Login con credenciales del seed admin.

## 6. Referencia Java (comparación)

Para contrastar respuestas durante el port:

```powershell
cd fornituras-api
.\mvnw.cmd spring-boot:run
```

Solo uno de los dos backends debe usar el mismo puerto a la vez, o configurar puertos distintos y
actualizar `apiUrl`.

## Troubleshooting

| Problema | Solución |
|----------|----------|
| CORS error desde Ionic | Verificar `App:Cors:AllowedOrigins` incluye `http://localhost:8100` |
| 401 inmediato | Token expirado; revisar `expiresIn` en ms |
| EF migration fail | BD existe y usuario SQL tiene permisos DDL |
| JSON shape distinto | Comparar con contrato en `contracts/ionic-api-contract.md` |
