# Quickstart — Validación 019 Identidad del sistema

## Prerrequisitos

- SQL Server 2022 accesible con la cadena de conexión local (`.env` / user-secrets).
- .NET 10 SDK y Node.js instalados.

## 1. Aplicar migraciones y levantar el backend

```powershell
cd fornituras-api-dotnet
dotnet ef database update --project src/Fornituras.Api
dotnet run --project src/Fornituras.Api    # puerto 8080, path /sigefor
```

## 2. Levantar el frontend

```powershell
cd sigefor
npm start
```

## 3. Validaciones

| # | Escenario | Cómo validar | Resultado esperado |
|---|-----------|--------------|--------------------|
| 1 | Hero público (US1) | Abrir la landing sin sesión | Título: "Sistema Integral de Gestión de Fornituras" |
| 2 | Footer (US1) | Pie de la landing pública | "SIGEFOR · Sistema Integral de Gestión de Fornituras" |
| 3 | Pestaña del navegador (US1) | Título del documento en cualquier página | "SIGEFOR \| Sistema Integral de Gestión de Fornituras", sin "Gobierno de México" |
| 4 | Fallback (US1) | Con la API detenida, abrir la landing | Textos de respaldo con la nueva identidad |
| 5 | Migración sobre valor sembrado (US2) | BD con título antiguo → `dotnet ef database update` → `GET /sigefor/api/v1/landing/public` | El hero devuelve el nuevo título |
| 6 | Migración respeta ediciones (US2) | Poner un título personalizado en el hero, re-aplicar migración en BD limpia equivalente | El título personalizado no cambia |
| 7 | Menú ADMIN (US3) | Login como ADMIN → menú lateral | Entrada "Configurar landing" visible y funcional |
| 8 | Gating por rol (US3) | Login con rol no-ADMIN | La entrada no aparece; `/landing-admin` redirige a `/inicio` |

## 4. Tests automatizados

```powershell
cd fornituras-api-dotnet && dotnet test
cd sigefor && npm test
```

Verificar además que ningún test siga esperando los textos antiguos
("Sistema de Gestión de Blindajes", "Gobierno de México", "Contenido de bienvenida").
