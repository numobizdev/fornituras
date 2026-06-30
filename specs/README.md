# Índice de especificaciones (SIGEFOR)

Mapa de las features spec-driven y su correspondencia con las pantallas de
[`Requerimientos.MD`](../Requerimientos.MD). Estado: **Draft** (especificación; aún sin
plan/tasks salvo donde se indique). Vocabulario: el producto es **SIGEFOR** y la unidad
controlada es la **fornitura** (chaleco, cinturón, casco…).

| # | Feature | Pantalla (`Requerimientos.MD`) | Nombre de menú |
|---|---------|-------------------------------|----------------|
| 001 | [inventario-equipos](./001-inventario-equipos/spec.md) — inventario de fornituras (alta individual, por lote, consulta, estados) | §3 Fornituras | Inventario de Fornituras |
| 002 | [qr-equipos](./002-qr-equipos/spec.md) — QR opaco firmado (generación, verificación, exportación) | §1/§3/§5 (QR) | — (transversal) |
| 003 | [elementos-padron](./003-elementos-padron/spec.md) — padrón de elementos policiales (PII) | §2 Elementos | Padrón de Elementos |
| 004 | [asignacion-resguardos](./004-asignacion-resguardos/spec.md) — asignación en 2 pasos, resguardos | §1 Captura y asignación | Asignación y Resguardos |
| 005 | [almacenes](./005-almacenes/spec.md) — CRUD de almacenes | §11 Almacenes | Almacenes |
| 006 | [tipos-fornitura](./006-tipos-fornitura/spec.md) — catálogo de tipos | §10 Tipo de fornituras | Catálogo de Tipos |
| 007 | [traslados](./007-traslados/spec.md) — traslados entre almacenes | §5 Traslados | Traslados |
| 008 | [incidencias](./008-incidencias/spec.md) — incidencias, mantenimiento y alertas | §4 Incidencias | Incidencias |
| 009 | [bajas](./009-bajas/spec.md) — bajas definitivas | §6 Bajas | Bajas Definitivas |
| 010 | [dashboard](./010-dashboard/spec.md) — tablero de indicadores | Dashboard (paleta) | Tablero |
| 011 | [reportes](./011-reportes/spec.md) — reportes y export Excel | §7 Reportes | Reportes y Estadística |
| 012 | [auditoria](./012-auditoria/spec.md) — bitácora ISO 27001 | §9 Auditoría | Bitácora de Auditoría |
| 013 | [usuarios](./013-usuarios/spec.md) — usuarios y roles (RBAC) | §8 Usuarios | Usuarios y Roles |
| 014 | [escaneo-qr](./014-escaneo-qr/spec.md) — captura QR (lector/cámara/manual) | §1/§3/§5 (captura) | — (componente) |

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
