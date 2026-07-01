# 0012. Inmutabilidad y retención de la bitácora de auditoría

- **Estado:** **Aceptado**
- **Fecha:** 2026-07-01
- **Feature:** [012-auditoria](../../specs/012-auditoria/) (tareas T003, T005, T021, T022)

## Contexto

La feature 012 implementa la bitácora de auditoría (Principio V, ISO/IEC 27001 A.12.4). El registro
debe ser **de difícil alteración** (FR-003/SC-003): ningún usuario de aplicación puede editar ni
borrar eventos. Además debe definirse una **política de retención** (FR-005) alineada al marco legal
mexicano (LGPDPPSO/LFPDPPP) e ISO 27001. El proyecto no tenía tabla de auditoría; hasta ahora el
`AuditWriter` solo escribía a SLF4J.

Opciones de inmutabilidad consideradas:

1. **Append-only por permisos de BD:** revocar UPDATE/DELETE al principal de la app. Simple, pero
   depende del nombre del principal (desconocido/variable por entorno) y no protege si la app usa un
   usuario con más privilegios.
2. **Triggers `INSTEAD OF UPDATE/DELETE`:** la BD rechaza cualquier modificación/borrado
   independientemente del principal. Robusto y portable dentro de SQL Server.
3. **Hashing encadenado (`prev_hash`):** cada fila referencia el hash de la anterior; permite
   *detectar* manipulación aunque no impedirla. Refuerzo complementario.
4. **WORM externo:** fuera de alcance por ahora (infra adicional).

## Decisión

1. **Append-only en dos capas:**
   - **Capa de aplicación:** el `AuditLogRepository` **no expone** operaciones de update/delete
     (extiende `Repository` + `JpaSpecificationExecutor`, con `save` de solo inserción). No hay
     endpoint de escritura/edición público; la escritura es interna vía el puerto `AuditWriter`.
   - **Capa de BD (SQL Server):** triggers `INSTEAD OF UPDATE, DELETE` sobre `audit_log` que lanzan
     error. Protegen incluso ante acceso directo con el principal de la app.
2. **Columna `prev_hash` reservada** para un futuro **hashing encadenado** (opción 3) como refuerzo
   de detección; su cálculo queda como mejora posterior (no MVP).
3. **Sin PII/secretos** en el log: se referencia por id y se **redacta** el detalle antes de
   persistir (defensa en profundidad, Principio V).
4. **Retención:** conservación mínima **24 meses** en línea; después, archivado/purga controlada
   conforme al marco legal aplicable e ISO 27001. La automatización de purga/archivado se implementa
   cuando exista el proceso operativo; hasta entonces la retención es "no purgar".

## Alternativas consideradas

- Solo permisos de BD (opción 1): descartada como mecanismo único por depender del principal.
- Hashing encadenado como mecanismo principal (opción 3): detecta pero no impide; se adopta como
  refuerzo futuro, no como control primario.

## Consecuencias

- **Positivas:** inmutabilidad efectiva en BD (triggers) + imposibilidad de borrar/editar desde la
  app (repositorio sin mutación); `prev_hash` deja la puerta abierta a verificación por cadena.
- **Límites:** los triggers son específicos de SQL Server (el perfil de test H2 no los ejecuta; la
  inmutabilidad en test se garantiza a nivel de tipo del repositorio). La retención automatizada
  queda pendiente. El hashing encadenado no está activo todavía.
