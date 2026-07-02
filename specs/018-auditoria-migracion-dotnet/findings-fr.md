# Hallazgos — Verificación requisito por requisito (FR) de las specs 001–017 en `fornituras-api-dotnet/`

**Fecha:** 2026-07-01 · **Método:** lectura de cada `spec.md` (Functional Requirements) y trazado de
cada FR contra el servicio/controlador/entidad/migración .NET. Complementa la auditoría estructural
de [findings.md](./findings.md). Solo lectura (SC-005).

## Resumen

De ~159 FR en las specs 001–017, la **gran mayoría está implementada** en el backend .NET. Se
identifican **4 huecos nuevos** a nivel FR (trazabilidad/formato, severidad Media/Baja) y varias
diferencias **intencionales** ya cubiertas por ADR. Ningún hueco es de núcleo funcional ni de PII.

| Nivel | Resultado |
|-------|-----------|
| Núcleo funcional (endpoints, entidades, reglas de negocio) | **Cumplido** en todas las specs |
| Seguridad de PII (cifrado, blind index, enmascaramiento, RBAC) | **Cumplido** (003, 004, 011) |
| Huecos nuevos FR-level | **4** (G-1..G-4), ver abajo |
| Diferencias intencionales (ADR) | MFA (013), firma QR (002), migración de datos (015) |

## Veredicto por spec (detalle de excepciones)

- **001 inventario** (12/12): FR-001..012 cumplidos. Código único (`CodigoNormalizado`), alta por
  lote con validación de duplicados, edición sin tocar identidad, **vigencia derivada**
  (`ExpiryCalculator`/`ComputeExpiry`), bloqueo de baja/traslado con asignación vigente
  (`AssertStatusChangeAllowed`), auditoría y RBAC (`WriteInventory`), coherencia de estado.
- **002 qr** (9/10): generación de códigos opacos consecutivos únicos, export PDF/ZIP, parámetros de
  impresión, listado/enumeración, auth (`WriteInventory`), resolución server-side, generación
  auditada (`GENERATE_QR_BATCH`). **FR-009** (verificar autenticidad/firma) sigue **abierto** (ADR
  0005, sin firma) — igual que Java. **FR-010** (SHOULD): la **exportación** PDF/ZIP no se audita
  (la generación sí) — menor.
- **003 elementos** (9/9): PII cifrada (`PiiCipher`) + blind index (`BlindIndexer`), enmascaramiento
  por rol, unicidad placa/CURP/RFC, auditoría de vista/alta (`VIEW_OFFICER`/`CREATE_OFFICER`).
- **004 asignación** (10/10 backend): solo disponible, cambio de estado, reasignar cerrando la
  previa, entrega/recepción (`AsignadoPor`/`RecibidoPor`), auditoría. FR-007 firma QR = abierto
  (ADR 0005). "Dos pasos" y "Limpiar" son de frontend.
- **005 almacenes** (8/8): CRUD + desactivar, unicidad `codigo`/`nombre`, bloqueo de borrado con
  fornituras/traslados (`CountUsageAsync`), solo activos ofrecidos, RBAC (`ManageConfig`).
- **006 tipos** (8/8): estructura genérica `catalog→catalog_item`, desactivación en vez de borrado,
  solo activos, dependientes vía `ParentItemId`, `IsSystem`, **semilla `TIPO_PRENDA`="Fornitura"**.
- **007 traslados** (7/7): origen≠destino, solo disponibles del origen, estado en-traslado→recepción,
  bloqueos, auditoría.
- **008 incidencias** (7/7): reporte/actualización de estado, alertas de vigencia
  (`AlertService`/`ExpiryCalculator`), cambio de estado del equipo, RBAC + auditoría.
- **009 bajas** (5/5): baja por código, bloqueo con asignación/traslado, no reactivable, listado,
  RBAC (`AuthorizeDecommission`) + auditoría.
- **010 dashboard** (5/5): totales agregados server-side (`GroupBy`), vigencia derivada, auth.
- **011 reportes** (4/6): totales, asignaciones activas con filtros + blind index, enmascaramiento
  PII, predefinidos. **G-1 (FR-003)** y **G-2 (FR-005)** — ver abajo.
- **012 auditoría** (5/6): registro automático de eventos sensibles, sin PII (por id/acción),
  **inmutable** (triggers B-2 ya remediados), consulta con filtros, retención (ADR 0012).
  **G-3 (FR-006)** — ver abajo.
- **013 usuarios** (6/7): CRUD, RBAC mínimo privilegio, hashing BCrypt, **lockout** de login,
  auditoría de cambios, **impide quedarse sin admin** (`WouldLeaveSystemWithoutAdmin`). **FR-004**
  (MFA) sigue **no implementado** por diseño (SHOULD, gated ADR 0014).
- **014 escaneo-qr** (FE): componente de captura es **frontend**; el backend provee resolución
  autenticada por código (`equipment/by-codigo`). Sin parte de backend pendiente.
- **015 catálogos sexo/sangre** (5/6 backend): SEXO/TIPO_SANGRE como catálogos de sistema
  (semilla), resolución en alta de elemento. **FR-002/003** (migrar datos y **dropear** tablas
  `sexo`/`tipo_sangre`) **N/A en .NET**: el esquema nace ya consolidado (`InitialCreate`), no había
  tablas legadas que migrar — decisión de migración. FR-006 (frontend) = FE.
- **016 landing** (backend cumplido): pública anónima + home + CRUD/reorder/activate (ManageLanding),
  **auditoría de todas las mutaciones**, **rate limit público** (B-1 remediado), quick links.
  FR-011..016 (tour guiado, estado vacío, redirecciones, shell) son **frontend**.
- **017 fotos** (15/16): captura/cifrado/EXIF/RBAC/gating/auditoría. **G-4 (FR-016)** limpieza de
  huérfanas — ver abajo.

## Huecos nuevos (FR-level)

### G-1 — 🟡 Media · 011 FR-003: la exportación no es Excel real
`ReportService.ExportActiveAssignmentsAsync`/`ExportPredefinedReportAsync` generan **CSV**
(`text/...`), no un `.xlsx`. La spec 011 pide "exportar a **Excel**" y **ADR 0011** eligió Apache POI
(en Java). Funciona (Excel abre CSV) pero no cumple el formato decidido.
- **Remediación:** generar `.xlsx` con una librería .NET (p. ej. ClosedXML/OpenXML, evaluar licencia
  por Principio VI) o registrar un ADR que acepte CSV como formato.

### G-2 — 🟡 Media · 011 FR-005: las exportaciones no se auditan
Ni `ExportActiveAssignmentsAsync` ni `ExportPredefinedReportAsync` (ni el controller) registran
auditoría. FR-005 exige que **toda exportación** quede auditada (quién/qué/cuándo) — Principio V.
- **Remediación:** añadir `audit.RecordEventAsync("EXPORT_REPORT", ...)` en los endpoints de export.

### G-3 — 🟡 Media · 012 FR-006: los accesos denegados no se auditan
`AuthService.LoginAsync` audita el **login exitoso** (`LOGIN`) e incrementa el contador de fallos
(`OnFailedAttemptAsync`), pero **no registra en auditoría** los intentos fallidos/denegados. FR-006
pide registrarlos "igual que los exitosos".
- **Remediación:** auditar `LOGIN_FAILED`/`ACCESS_DENIED` (login fallido y, si aplica, 403).

### G-4 — 🟢 Baja · 017 FR-016: limpieza de imágenes huérfanas
El picker sube la foto antes de guardar la ficha; no hay purga de `media_asset` sin asociar. Ya
estaba identificado como pendiente (**T034** de la spec 017).
- **Remediación:** tarea de limpieza de huérfanas / borrado en el flujo de guardado (spec 017 T034).

## Diferencias intencionales (no huecos)

- **013 FR-004 (MFA):** SHOULD, gated por ADR 0014 (Propuesto). No implementado a propósito.
- **002 FR-009 (firma QR):** punto abierto de ADR 0005; ni Java ni .NET firman el QR.
- **015 FR-002/003 (migración de datos sexo/sangre):** N/A en .NET; el esquema nace consolidado.

## Conclusión

La migración a .NET **aplica las specs 001–017 a nivel de requisito**, salvo cuatro huecos de
**trazabilidad/formato** (G-1..G-4), ninguno de núcleo funcional ni de protección de PII, y todos
con remediación acotada. Sumados a B-1/B-2 (ya remediados), constituyen el backlog para dar la
migración por 100% cerrada a nivel FR. Cada remediación va en la rama de la spec correspondiente
(011 para G-1/G-2, 012/013 para G-3, 017 para G-4).
