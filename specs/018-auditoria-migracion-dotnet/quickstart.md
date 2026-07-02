# Quickstart / Validación (018)

Guía para validar la auditoría y las remediaciones. Diseño en [plan.md](./plan.md) y
[research.md](./research.md); resultado de la auditoría en [findings.md](./findings.md).

## A. Validar la auditoría (Fase A — hecha)

1. Abrir [findings.md](./findings.md) y [checklists/cobertura-migracion.md](./checklists/cobertura-migracion.md).
2. **Esperado:** cada spec 001–017 con estado (C/P/A/FE) y evidencia; brechas B-1/B-2 con severidad
   y remediación; mejora M-1 documentada; `git diff fornituras-api-dotnet/src` = 0 durante la
   auditoría (SC-005).

## B. Validar la remediación de B-1 (rate limiting)

Prerrequisito: backend levantado (`dotnet run --project src/Fornituras.Api`) y un token válido.

1. Repetir `GET /sigefor/api/v1/equipment/by-codigo/{codigo}` por encima del límite configurado
   (`App:RateLimit:ByCodigo`, por defecto 30/60 s) con el mismo usuario.
2. **Esperado:** tras superar el límite, respuesta **429** con `ApiResponse` (`success:false`);
   al pasar la ventana, vuelve a responder 200.
3. Golpear `GET /sigefor/api/v1/landing/public` por IP por encima de `App:RateLimit:Public`.
4. **Esperado:** **429** al exceder; el resto de endpoints no se ven afectados.

## C. Validar la remediación de B-2 (inmutabilidad de auditoría)

Prerrequisito: BD SQL Server con la migración aplicada (`dotnet ef database update`).

1. Generar un evento de auditoría (p. ej. ver un elemento) para tener una fila en `audit_log`.
2. Intentar `UPDATE audit_log SET accion='x' WHERE id=...` y `DELETE FROM audit_log WHERE id=...`
   directamente en la BD.
3. **Esperado:** ambos fallan (triggers `INSTEAD OF UPDATE/DELETE` rechazan la operación); la fila
   permanece intacta. Los `INSERT` siguen permitidos.

## D. Regresión

1. `dotnet test` desde `fornituras-api-dotnet/` → verde (los 14 tests previos + los nuevos aislables).
2. Humo del contrato Ionic: login, listar equipos/elementos, subir foto — sin cambios de forma.
