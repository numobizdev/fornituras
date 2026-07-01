# Índice de especificaciones (SIGEFOR)

Mapa de las features spec-driven y su correspondencia con las pantallas de
[`Requerimientos.MD`](../Requerimientos.MD). Estado: **todas con `spec` + `plan` + `tasks`**
(listas para `/speckit-implement`); la implementación aún no ha comenzado salvo lo ya
existente (auth, QR). Vocabulario: el producto es **SIGEFOR** y la unidad controlada es la
**fornitura**, un **tipo de prenda** concreto (no una categoría con subtipos); el catálogo de tipos
de prenda tiene hoy un único valor, "Fornitura".

| # | Feature | Pantalla (`Requerimientos.MD`) | Nombre de menú |
|---|---------|-------------------------------|----------------|
| 001 | [inventario-equipos](./001-inventario-equipos/spec.md) — inventario de fornituras (alta individual, por lote, consulta, estados) | §3 Fornituras | Inventario de Fornituras |
| 002 | [qr-equipos](./002-qr-equipos/spec.md) — **QR implementado**: código `FOR-XXXXX` por lotes + export PDF/ZIP | §1/§3/§5 (QR) | — (transversal) |
| 003 | [elementos-padron](./003-elementos-padron/spec.md) — padrón de elementos policiales (PII) | §2 Elementos | Padrón de Elementos |
| 004 | [asignacion-resguardos](./004-asignacion-resguardos/spec.md) — asignación en 2 pasos, resguardos | §1 Captura y asignación | Asignación y Resguardos |
| 005 | [almacenes](./005-almacenes/spec.md) — CRUD de almacenes | §11 Almacenes | Almacenes |
| 006 | [tipos-fornitura](./006-tipos-fornitura/spec.md) — **catálogos genéricos** (`catalog → catalog_item`): tipo de prenda (hoy: Fornitura), tallas, tipo de almacén | §10 Tipo de fornituras | Catálogos |
| 007 | [traslados](./007-traslados/spec.md) — traslados entre almacenes | §5 Traslados | Traslados |
| 008 | [incidencias](./008-incidencias/spec.md) — incidencias, mantenimiento y alertas | §4 Incidencias | Incidencias |
| 009 | [bajas](./009-bajas/spec.md) — bajas definitivas | §6 Bajas | Bajas Definitivas |
| 010 | [dashboard](./010-dashboard/spec.md) — tablero de indicadores | Dashboard (paleta) | Tablero |
| 011 | [reportes](./011-reportes/spec.md) — reportes y export Excel | §7 Reportes | Reportes y Estadística |
| 012 | [auditoria](./012-auditoria/spec.md) — bitácora ISO 27001 | §9 Auditoría | Bitácora de Auditoría |
| 013 | [usuarios](./013-usuarios/spec.md) — usuarios y roles (RBAC) | §8 Usuarios | Usuarios y Roles |
| 014 | [escaneo-qr](./014-escaneo-qr/spec.md) — captura QR (lector/cámara/manual) | §1/§3/§5 (captura) | — (componente) |
| 015 | [catalogos-sexo-sangre](./015-catalogos-sexo-sangre/spec.md) — migrar `SEXO`/`TIPO_SANGRE` a la estructura genérica (ADR 0007) | — (deuda técnica) | — (parte de Catálogos) |
| 017 | [migracion-api-dotnet](./017-migracion-api-dotnet/spec.md) — **migración backend** Spring Boot → ASP.NET Core Web API (.NET 10); contrato transparente para Ionic | — (infraestructura) | — |

## Orden de implementación recomendado

> **Migración de stack (017):** la spec [017-migracion-api-dotnet](./017-migracion-api-dotnet/spec.md)
> es transversal y **prioriza** reimplementar en .NET lo ya consumido por `sigefor/` antes de
> continuar features nuevas en Java. Ver [tasks.md](./017-migracion-api-dotnet/tasks.md).

Derivado de las dependencias declaradas en cada `plan.md` (los puertos permiten desarrollar y
testear antes de que existan las features de las que se depende):

1. **Catálogos base:** 006-tipos-fornitura, 015-catalogos-sexo-sangre (migración temprana, antes de sumar consumidores), 005-almacenes (prerequisito de 001/007).
2. **Núcleo de inventario:** 001-inventario-equipos (cimiento; expone resolución `codigo → fornitura`).
3. **Transversales tempranos:** 012-auditoria (puerto `AuditWriter` que todas consumen), 014-escaneo-qr (componente de captura).
4. **PII y núcleo operativo:** 003-elementos-padron (ya con tasks), 004-asignacion-resguardos.
5. **Operación de equipo:** 007-traslados, 008-incidencias, 009-bajas.
6. **Lectura/control:** 010-dashboard, 011-reportes.
7. **Gobierno de acceso:** 013-usuarios (extiende la auth; expansión de roles y MFA **gated por ADR**).
8. **Deuda gestionada:** 002-qr-equipos (verificación + cierre de brechas; firma abierta en ADR 0005).

> No es un orden estricto: las features con puertos (p. ej. 004 sobre 001/003, 009 sobre 004/007)
> pueden avanzarse en paralelo y cerrar su integración real al existir la dependencia.

## Referencias transversales

- **Constitución:** [`.specify/memory/constitution.md`](../.specify/memory/constitution.md)
- **Seguridad:** [`docs/02-seguridad.md`](../docs/02-seguridad.md)
- **Modelo de datos:** [`docs/03-modelo-datos.md`](../docs/03-modelo-datos.md)
- **UI/UX e identidad visual (paleta):** [`docs/05-ui-ux.md`](../docs/05-ui-ux.md)
- **Decisión abierta de PII:** [`docs/04-decisiones/0003-pii-elementos.md`](../docs/04-decisiones/0003-pii-elementos.md)

## Notas de merge (2026-06-30)

- Las features **001** y **002** existían antes con vocabulario "equipo/blindaje"; se
  **ajustaron** a "fornitura" y al alcance SIGEFOR. Sus carpetas conservan el nombre de rama
  original (`001-inventario-equipos`, `002-qr-equipos`) por continuidad histórica.
- El frontend vive en [`sigefor/`](../sigefor/) (Ionic 8 + Angular), con páginas andamiadas para
  Inicio, Elementos, Fornituras y Asignación, y la paleta ya aplicada en `theme/variables.scss`.
  El resto de módulos se añadirá conforme se implementen las features.
- La decisión de **alcance de PII** (CURP/RFC/foto) quedó abierta como ADR **0003 (Propuesto)**.
- **Autenticación ya implementada** (backend + `sigefor/`): login por **email**, JWT, guards,
  interceptor y recuperación de contraseña por código. Roles actuales: **`ADMIN`** y
  **`CAPTURISTA`** (la spec **013** detalla el estado y la expansión propuesta de roles).
- **QR ya implementado** (módulo `qrcodes`): código corto opaco **`FOR-XXXXX`** generado **por
  lotes** con parámetros de impresión y export **PDF/ZIP** (`/api/v1/qr/lotes...`). **Sin firma**
  (diverge del Principio II) → [ADR 0005](../docs/04-decisiones/0005-formato-qr-implementado.md),
  que reemplaza al 0002. La spec **002** se ajustó a esta realidad.
