# Guía UI/UX e identidad visual (SIGEFOR)

> Fuente: [`Paleta de colores.MD`](../Paleta%20de%20colores.MD). Este documento la convierte en
> referencia viva para las features de `specs/` y para `frontend/sigefor/`. La paleta ya está
> aplicada en `sigefor/src/theme/variables.scss` (tema institucional gob.mx).

## 1. Principios de diseño

- Apariencia **institucional y profesional**, alineada a organismos de gobierno de México.
- Priorizar legibilidad y accesibilidad; reducir la carga cognitiva.
- Resaltar información crítica mediante **color semántico**.
- Consistencia visual entre todos los módulos.

## 2. Paleta institucional

| Elemento | Color | HEX | Variable |
|----------|-------|-----|----------|
| Primario (Guinda) | Guinda | `#611232` | `--ion-color-primary` |
| Secundario (Dorado) | Dorado | `#A57F2C` | `--ion-color-secondary` |
| Fondo principal | Blanco | `#FFFFFF` | `--ion-background-color` |
| Fondo secundario | Gris claro | `#F5F5F5` | `--gobmx-fondo-suave` |
| Texto principal | Gris oscuro | `#333333` / `#545454` | `--ion-text-color` |
| Bordes | Gris suave | `#D1D5DB` / `#DDC9A3` | `--ion-border-color` |

> **Nota de consistencia.** `Paleta de colores.MD` (guía UI) y el tema gob.mx aplicado en
> `variables.scss` usan tonos ligeramente distintos para algunos estados (p. ej. éxito/peligro).
> El tema gob.mx es la **fuente de verdad de implementación**; la tabla de colores de estado de
> abajo es la **intención semántica**. Si se requiere fidelidad exacta a la guía UI para los
> badges de estado, definir variables CSS dedicadas por estado (no reusar los `--ion-color-*`).

## 3. Colores de estado (semántica)

Estos colores aplican a badges/indicadores de estado de fornituras en inventario, tablero
(**010**), incidencias (**008**) y reportes (**011**).

| Estado | Color | HEX |
|--------|-------|-----|
| Disponible | Verde | `#198754` |
| Asignado | Azul | `#0D6EFD` |
| En mantenimiento | Amarillo | `#FFC107` |
| Próximo a vencer | Naranja | `#FD7E14` |
| Caducado | Rojo | `#DC3545` |
| Baja definitiva / inactivo | Gris | `#6C757D` |

Variables CSS sugeridas (independientes del rol Ionic para no contaminar la semántica):

```css
:root {
  --status-disponible: #198754;
  --status-asignado:   #0D6EFD;
  --status-mantenimiento: #FFC107;
  --status-proximo-vencer: #FD7E14;
  --status-caducado:   #DC3545;
  --status-inactivo:   #6C757D;
}
```

## 4. Estados de la fornitura (catálogo canónico)

El catálogo operativo de estados (ver feature **001**) y su mapeo de color:

- **Disponible** (verde) · **Asignada** (azul) · **En mantenimiento** (amarillo) ·
  **En traslado** (gris/azul) · **Extraviada** (gris) · **Baja definitiva** (gris).
- **Estados de vigencia derivados** de `fecha_vencimiento`: **Próxima a vencer** (naranja, ≤ 90
  días) y **Caducada** (rojo, vencida). Son una capa de alerta sobre el estado operativo, no un
  estado operativo en sí.

## 5. Estructura de la aplicación

- **Barra superior** (guinda `#611232`): logotipo institucional, nombre del sistema, usuario
  autenticado, rol, notificaciones, cerrar sesión.
- **Menú lateral** (fondo blanco, opción activa en guinda, íconos consistentes, colapsable).

### Navegación — nombres de módulo (profesionales)

`Requerimientos.MD` pidió mejorar los nombres. Mapeo propuesto pantalla → nombre de menú →
feature:

| Pantalla (`Requerimientos.MD`) | Nombre de menú | Feature |
|-------------------------------|----------------|---------|
| Inicio / Dashboard | **Tablero** | 010-dashboard |
| Elementos | **Padrón de Elementos** | 003-elementos-padron |
| Fornituras | **Inventario de Fornituras** | 001-inventario-equipos |
| Captura y asignación | **Asignación y Resguardos** | 004-asignacion-resguardos |
| Traslados | **Traslados** | 007-traslados |
| Incidencias | **Incidencias** | 008-incidencias |
| Bajas | **Bajas Definitivas** | 009-bajas |
| Reportes | **Reportes y Estadística** | 011-reportes |
| Auditoría | **Bitácora de Auditoría** | 012-auditoria |
| Tipo de fornituras | **Catálogo de Tipos** | 006-tipos-fornitura |
| Almacenes | **Almacenes** | 005-almacenes |
| Usuarios | **Usuarios y Roles** | 013-usuarios |

> El menú actual en `sigefor/src/app/core/constants/app-navigation.ts` solo tiene 4 entradas
> (Inicio, Elementos, Fornituras, Asignación). Al implementar cada feature se irá ampliando este
> menú con los nombres de la tabla, agrupando catálogos (Tipos, Almacenes) y administración
> (Usuarios, Auditoría) según el rol.

## 6. Roles (RBAC) — ver feature 013 y `02-seguridad.md`

**Implementados hoy:** `ADMIN` y `CAPTURISTA` (login por email + JWT, ya operativo).
**Expansión propuesta** (pendiente de ADR): Administrador · Supervisor · Almacén · Auditor ·
Consulta. Cada rol ve solo su menú y sus acciones (mínimo privilegio). La PII se enmascara salvo
a roles autorizados.

## 7. Accesibilidad

- Contraste mínimo **AA**; tamaño de fuente mínimo **14 px**.
- Compatible con móvil y con navegación por teclado (clave para el flujo con lector HID).
- Soporte de **modo oscuro** (definir variantes de las variables anteriores).

## 8. Tokens de espaciado, tipografía y densidad

Además de la paleta (§2), la identidad se apoya en estos tokens de base. El detalle de uso y la
librería de componentes que los consume están en
[`06-patrones-ux.md`](./06-patrones-ux.md).

- **Espaciado — rejilla de 8 pt.** Todo margen/padding/gap es múltiplo de `4 px`
  (`--space-1: 4px` … `--space-8: 64px`). Sin números mágicos.
- **Tipografía — `Noto Sans`** (`--ion-font-family`). Escala: Display 28–32 / Title 20–22 /
  Body 14–16 (mínimo 14) / Caption 12–13.
- **Densidad.** Las vistas de tabla ofrecen densidad `cómoda` (default) y `compacta` (más filas,
  para captura masiva en escritorio).

## 9. Patrones de interacción y componentes

La capa de **interacción** (cómo se comporta y se construye la UI: vista de datos responsiva,
scan-first, estados de carga/vacío/error, filtros con chips, PII con revelado, wizard, etc.) y la
**librería de componentes reutilizables** viven en
[`06-patrones-ux.md`](./06-patrones-ux.md). Este documento (`05`) define la **identidad
visual**; `06` define **cómo se interactúa**. No se duplican: `06` consume los colores y tokens
de aquí.

## 10. Relación con las features

Toda feature que muestre estados de fornitura o indicadores DEBE usar la semántica de color de
este documento (§3/§4). Las features que lo referencian: **001**, **008**, **010**, **011**.
