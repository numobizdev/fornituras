# Research — Fase 0 (018)

La investigación de la auditoría (Fase A) está consolidada en [findings.md](./findings.md). Aquí se
registran las **decisiones de remediación** de las dos brechas Alta.

## Decisión R-1 — Rate limiting (remedia B-1)

- **Decisión:** usar el **rate limiter nativo de ASP.NET Core** (`AddRateLimiter` +
  `UseRateLimiter`, ventana fija) con dos políticas: `by-codigo` (partición por actor autenticado,
  por defecto 30 req/60 s, igual que Java) y `public` (partición por IP, para `landing/public`).
  Rechazo con **429** envuelto en `ApiResponse`. Límites configurables en `App:RateLimit`.
- **Rationale:** reemplaza a `Bucket4jRateLimiter` (ADR 0010) sin traer una dependencia nueva
  (Principio VI); el limitador nativo cubre el caso token-bucket/ventana. El login ya está
  protegido por *lockout* (`LoginAttemptService`), así que el foco es `by-codigo` y el endpoint
  público, que era exactamente el alcance de Bucket4j en Java.
- **Alternativas descartadas:** (a) portar Bucket4j vía una lib .NET equivalente → dependencia
  innecesaria; (b) middleware propio → reinventa lo que el framework ya ofrece.

## Decisión R-2 — Inmutabilidad de la bitácora (remedia B-2)

- **Decisión:** añadir una **migración EF Core** con `migrationBuilder.Sql(...)` que recree los
  triggers `trg_audit_log_no_update` y `trg_audit_log_no_delete` (`INSTEAD OF UPDATE/DELETE`) sobre
  `audit_log`, idénticos en intención a `V21` del backend Java. `Down` los elimina.
- **Rationale:** EF no traslada SQL crudo/triggers automáticamente (origen de la brecha); recrearlos
  restaura la garantía append-only a nivel de BD (ADR 0012, ISO 27001) sin cambiar el esquema.
- **Alternativas descartadas:** (a) confiar solo en que la app no expone update/delete → no protege
  ante acceso directo a BD; (b) tabla con versión de fila/temporal → mayor complejidad que el
  requisito (rechazo duro de UPDATE/DELETE) ya resuelto por triggers.

## Nota sobre pruebas

Ni el rate limiter (requiere host/varias peticiones) ni los triggers (requieren SQL Server real; el
proveedor InMemory de EF no ejecuta SQL crudo ni triggers) son verificables con la infra de test
actual (xUnit unitario + EF InMemory). Se cubre por unidad lo aislable (opciones/partición del
limitador, presencia de la migración) y se documenta la **validación manual** en quickstart.
Montar un host de integración (WebApplicationFactory + SQL Server/Testcontainers) queda como mejora
de tooling fuera del alcance de esta remediación.
