# ADR 0016: Backend ASP.NET Core Web API (.NET 10)

**Estado:** Aceptado  
**Fecha:** 2026-07-01  
**Reemplaza:** decisión implícita de Spring Boot en `AGENTS.md` §3

## Contexto

El cliente solicitó migrar el backend REST de **Java Spring Boot** a **ASP.NET Core Web API**
(.NET 10), manteniendo el contrato HTTP consumido por `sigefor/` (Ionic 8 + Angular). Los datos
y usuarios de desarrollo son desechables; no hay entornos productivos aún.

## Decisión

1. El backend oficial del monorepo pasa a ser **`fornituras-api-dotnet/`** (ASP.NET Core Web API,
   .NET 10, EF Core, SQL Server 2022).
2. Se preserva el path base **`/sigefor`**, el envelope **`ApiResponse<T>`**, JWT Bearer y la
   paginación compatible con Spring Data.
3. **`fornituras-api/`** (Java) queda **obsoleto** como referencia histórica; no recibe nuevas
   features salvo correcciones críticas durante la transición.
4. El frontend **`sigefor/`** no cambia de stack; solo requiere apuntar `environment.apiUrl` al
   host .NET (mismo puerto/path si es posible).

## Consecuencias

### Positivas

- Alineación con stack .NET del cliente.
- Contrato REST estable para Ionic.
- Esquema de BD gestionado con EF Core migrations en el nuevo proyecto.

### Negativas / riesgos

- Duplicación temporal de documentación y código Java hasta retirarlo por completo.
- Reimplementación manual de reglas de negocio (PII, asignaciones, QR secuencial).
- Dependencia de SQL Server local para desarrollo.

## Alternativas consideradas

- **Mantener Spring Boot:** rechazada por requerimiento del cliente.
- **Traducción automática Java→C#:** inviable para cifrado PII, RBAC y reglas de dominio.
- **API Gateway dual** (Java + .NET en paralelo): innecesario en fase dev-only.

## Referencias

- Spec: [`specs/017-migracion-api-dotnet/spec.md`](../../specs/017-migracion-api-dotnet/spec.md)
- Seguridad: [`docs/02-seguridad.md`](../02-seguridad.md)
- Catálogos genéricos: [ADR 0007](./0007-catalogos-genericos.md)
