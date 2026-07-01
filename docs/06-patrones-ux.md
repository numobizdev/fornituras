# Catálogo de patrones y componentes UX (SIGEFOR)

> **Propósito.** `05-ui-ux.md` define la **identidad visual** (paleta gob.mx, color semántico,
> estructura general). Este documento define **cómo se interactúa**: los patrones de interacción
> y la **librería de componentes reutilizables** que materializan esos patrones. Es la capa
> "LEGO" del frontend: piezas que se conectan en las 12 pantallas sin reescribir nada.
>
> **A quién aplica.** Toda feature de `specs/` que tenga interfaz. `Requerimientos.MD` describe
> *qué* hace cada pantalla; este doc describe *con qué piezas* se construye y *cómo se siente*.
>
> **Estado de adopción.** Hoy las pantallas implementadas (`elementos`, `fornituras`,
> `asignacion`, `tipos`, `almacenes`) usan componentes Ionic "de fábrica" (`ion-list` de
> `ion-item`, `ion-spinner`, pager manual). Este catálogo es el **objetivo** al que se migra de
> forma incremental; la §9 lista el orden de refactor sugerido. No reescribir lo que funciona de
> golpe: extraer componentes a medida que se tocan las pantallas.

---

## 1. Metodología: Atomic Design + Design Tokens

El frontend se organiza en capas, de lo más pequeño a lo más grande (encaja con el principio de
diseño desacoplado tipo LEGO):

```
Tokens  →  Átomos        →  Moléculas          →  Organismos        →  Plantillas
(CSS     (StatusBadge,     (FilterBar,           (DataView,           (CrudPage,
 vars)    PiiField,         SearchField,          AssignWizard,        DetailPage)
          Button)           Pager)                CommandPalette)
```

- **Tokens:** ya existen como variables CSS en `sigefor/src/theme/variables.scss` (paleta gob.mx)
  y en `05-ui-ux.md` §3 (`--status-*`). Se complementan con los tokens de **espaciado**,
  **tipografía** y **densidad** definidos abajo.
- **Átomos/moléculas/organismos:** componentes Angular **standalone** en
  `sigefor/src/app/shared/ui/` (ruta propuesta), sin lógica de negocio, configurables por
  `@Input`/`@Output` o signals. Se prueban en aislamiento (TDD).

### 1.1 Tokens de espaciado (rejilla de 8 pt)

```css
:root {
  --space-1: 4px;   --space-2: 8px;   --space-3: 12px;  --space-4: 16px;
  --space-5: 24px;  --space-6: 32px;  --space-7: 48px;  --space-8: 64px;
  --radius-sm: 4px; --radius-md: 8px; --radius-lg: 12px;
}
```

Todo margen/padding/gap se expresa en múltiplos de estos tokens. Nada de números mágicos.

### 1.2 Escala tipográfica

| Rol | Tamaño | Uso |
|-----|--------|-----|
| Display | 28–32 px / 700 | Título de pantalla (toolbar `size="large"`) |
| Title | 20–22 px / 600 | Encabezado de sección/tarjeta |
| Body | 14–16 px / 400 | Texto general (mínimo **14 px**, ver `05` §7) |
| Caption | 12–13 px / 400 | Metadatos, notas, ayudas |

Fuente institucional: `Noto Sans` (ya configurada en `--ion-font-family`).

### 1.3 Densidad

Sistema operativo de captura masiva → ofrecer **dos densidades** en vistas de tabla:
`cómoda` (default móvil) y `compacta` (más filas visibles, ideal escritorio/captura).
Se controla con una clase en el contenedor (`.density-compact`) que reduce `padding` vertical.

---

## 2. Componentes reutilizables (la librería)

Cada componente es independiente, sin dependencias del proyecto origen, testeable en aislamiento.

### 2.1 `PageHeader` — encabezado de pantalla
Toolbar superior consistente: título, subtítulo opcional, **slot de acciones** (botones
primarios a la derecha). Sustituye la repetición de `ion-header`/`ion-toolbar` en cada página.

### 2.2 `ScanInput` — captura de QR unificada ⭐ (núcleo)
El componente **más importante** del sistema. Encapsula la feature **014-escaneo-qr**:
- Detecta automáticamente el origen: **lector HID** (ráfaga de teclado terminada en Enter),
  **cámara** (Capacitor en móvil / cámara PC como mejor esfuerzo) o **tecleo manual**.
- `placeholder` estándar: *"Escanee con el lector o teclee el código"*.
- **Autofocus** y re-focus tras cada lectura (el capturista no debe tocar el ratón).
- Feedback inmediato: estado `escaneando → resuelto/no encontrado` con color y, opcionalmente,
  sonido/vibración.
- `@Output` emite el código ya normalizado. Reutilizado en: Asignación, Inventario (alta y
  lote), Traslados, Bajas.

### 2.3 `DataView` — vista de datos responsiva ⭐
Resuelve la tensión móvil↔escritorio. **Mismos datos, dos presentaciones** según breakpoint:
- **Escritorio (≥ md):** **tabla** real — columnas configurables, ordenable, densidad
  conmutable, fila clicable, columna de acciones fija.
- **Móvil (< md):** **lista de tarjetas** (el patrón `ion-item` actual).
- Recibe la definición de columnas + el page de datos; delega la paginación a `Pager`.
- Estados de carga/vacío/error gestionados internamente (ver §4).

### 2.4 `FilterBar` — barra de filtros
- **Sticky** bajo el header. Combina búsqueda de texto libre (contra todas las columnas
  relevantes) + filtros específicos (selects/segments).
- Los filtros activos se muestran como **chips removibles** ("Municipio: Centro ✕").
- Estado de filtros **persistido en la URL** (query params) → enlaces compartibles y back/forward
  coherentes.

### 2.5 `StatusBadge` — estado con color semántico
Encapsula la semántica de `05-ui-ux.md` §3/§4 (`--status-*`). Entra el código de estado, sale el
badge con color y etiqueta correctos. Una sola fuente de verdad para todo estado de fornitura.

### 2.6 `Stepper` / `Wizard` — flujos multipaso
Para procesos guiados (asignación de 2 pasos, alta por lote). Muestra progreso, valida cada paso
antes de habilitar el siguiente, permite retroceder. Sustituye la lista inline actual de
`asignacion.page.html`.

### 2.7 `PiiField` — dato sensible con revelado bajo demanda
PII (CURP, RFC, foto) **enmascarada por defecto** (`CURP: ••••••••`); se descubre con un clic que
**queda auditado** (feature **012**) y solo si el rol está autorizado. Centraliza el cumplimiento
de `02-seguridad.md` y los ADR 0003/0004/0006: ninguna pantalla pinta PII en claro por su cuenta.

### 2.8 `Pager` — paginación de servidor
Paginación contra el backend (nunca el set completo; ver `Requerimientos.MD` §convenciones).
Muestra "Página X de Y · N registros", respeta los filtros activos. Reutiliza el patrón ya
presente en `elementos.page.html`, extraído a componente.

### 2.9 Estados: `SkeletonLoader`, `EmptyState`, `ErrorState`, `Toast`
Ver §4 (matriz de estados). Sustituyen el `ion-spinner` suelto por una experiencia con intención.

### 2.10 `ConfirmDialog` — confirmación de acciones sensibles
Para destructivas o de alto impacto (baja definitiva, devolver resguardo, revelar PII). Texto
claro de consecuencia, acción primaria diferenciada, requisito de motivo cuando aplica (bajas).

### 2.11 `CommandPalette` (Ctrl+K) — opcional, power users
Barra global de comando/escaneo: el capturista abre con `Ctrl+K`, escanea o busca y salta a la
ficha sin navegar el menú. Acelera el trabajo repetitivo. Adopción posterior (no MVP).

---

## 3. Patrones de interacción

- **Scan-first.** En toda pantalla con flujo de QR, `ScanInput` es el elemento protagonista,
  enfocado al cargar. El teclado manda (el lector HID *es* un teclado): todo el flujo debe poder
  completarse sin ratón.
- **Master–detail (split view).** En escritorio, Padrón e Inventario abren el detalle en un panel
  lateral en vez de navegar a otra ruta; en móvil, navegación normal a la ficha.
- **UI optimista + toast.** Las mutaciones (asignar, devolver, dar de baja) reflejan el resultado
  de inmediato y confirman con un toast; si el servidor falla, se revierte y se avisa.
- **Acciones masivas (bulk).** Alta por lote y traslados acumulan ítems en una tabla de revisión,
  con eliminación individual y rechazo de duplicados **antes** de confirmar.
- **Filtros vivos.** Buscar/filtrar actualiza la vista con *debounce*; los filtros activos son
  visibles (chips) y reversibles de un clic.

---

## 4. Matriz de estados de una vista

Toda vista de datos contempla los cuatro estados (no solo "cargando" y "listo"):

| Estado | Qué se muestra | Componente |
|--------|----------------|------------|
| **Cargando** | Esqueleto con la forma del contenido (no spinner pelón) | `SkeletonLoader` |
| **Con datos** | Tabla/tarjetas + paginación | `DataView` + `Pager` |
| **Vacío** | Mensaje + ilustración/ícono + **CTA** ("Crear el primero") | `EmptyState` |
| **Error** | Mensaje claro + acción **"Reintentar"**, sin tecnicismos | `ErrorState` |

El estado vacío distingue *"no hay nada todavía"* de *"no hay coincidencias con el filtro"*
(este último ofrece "Limpiar filtros").

---

## 5. Responsividad

Breakpoints Ionic estándar. Regla mental: **móvil = una columna y tarjetas; escritorio = rejilla
y tablas**.

| Rango | Layout |
|-------|--------|
| `< md` (móvil) | Una columna, `DataView` en modo tarjetas, menú lateral overlay, FAB para "Nuevo". |
| `≥ md` (tablet/escritorio) | `ion-split-pane` con menú fijo, `DataView` en modo tabla, master–detail, acciones en la barra (no FAB). |

El menú lateral ya usa `ion-split-pane` (`app.component.html`); falta llevar las **vistas de
contenido** a su versión de escritorio (hoy se quedan en modo móvil en pantallas grandes).

---

## 6. Accesibilidad y teclado

- Contraste mínimo **AA**; fuente mínima **14 px** (ver `05` §7).
- **Teclado primero**: navegación completa por `Tab`/`Enter`/`Esc`; foco visible. Es funcional,
  no cosmético — el lector HID inyecta teclas.
- `aria-label` en todo botón de solo ícono (ya presente en varias pantallas; mantenerlo).
- **Modo oscuro**: definir variantes de los tokens de §1 y de `--status-*`.

---

## 7. Microcopy

- Español neutro, institucional, en presente. Verbos de acción claros en botones
  ("Asignar fornitura", "Crear lote", "Dar de baja").
- Mensajes de error orientados a la solución, sin jerga técnica ni códigos crudos.
- Confirmaciones que nombran la consecuencia ("Esta fornitura quedará dada de baja
  definitivamente").

---

## 8. Checklist por pantalla nueva

Antes de dar por terminada una pantalla, verificar:

- [ ] Usa `PageHeader` (no `ion-header` ad hoc).
- [ ] Listados con `DataView` (tabla en escritorio, tarjetas en móvil) + `Pager` de servidor.
- [ ] Búsqueda/filtros con `FilterBar` (chips removibles, estado en URL).
- [ ] Captura de QR con `ScanInput` (autofocus, detección HID/cámara/manual).
- [ ] Estados de carga/vacío/error resueltos (§4), no solo el camino feliz.
- [ ] Estados de fornitura con `StatusBadge` (semántica de `05` §3/§4).
- [ ] PII con `PiiField` (enmascarada, revelado auditado) si la pantalla la toca.
- [ ] Acciones sensibles con `ConfirmDialog`.
- [ ] Navegable por teclado; `aria-label` en botones de ícono; contraste AA.

---

## 9. Roadmap de adopción (refactor incremental)

No se reescribe todo de golpe. Orden sugerido, de mayor a menor reutilización:

1. **`StatusBadge`** y tokens de §1 — base barata, alto reuso inmediato.
2. **`ScanInput`** — núcleo; desbloquea Asignación, Inventario, Traslados, Bajas con una sola
   pieza bien hecha.
3. **`DataView` + `Pager` + `FilterBar`** — el trío que transforma Padrón e Inventario en
   escritorio (es donde más se nota el salto de calidad).
4. **`SkeletonLoader`/`EmptyState`/`ErrorState`** — pulido de percepción de calidad.
5. **`Stepper`** para el wizard de Asignación; **`PiiField`** en Padrón.
6. **Master–detail** y **`CommandPalette`** — mejoras de productividad posteriores.

Cada extracción se acompaña de su test (TDD) y mantiene el componente sin dependencias del
proyecto origen, candidato a la librería personal de utilidades cuando sea genérico.

---

## 10. Relación con otros documentos

- `05-ui-ux.md` — identidad visual, paleta, color semántico de estados. **Este doc no la
  duplica**: la consume.
- `02-seguridad.md` y ADR 0003/0004/0006 — reglas de PII que `PiiField` materializa.
- `Requerimientos.MD` — contrato por pantalla; su sección "Convenciones de layout y UX" apunta
  aquí, y las pantallas insignia incluyen una subsección **Layout** que referencia estos
  componentes.
- `specs/` — cada feature detalla su uso concreto de estos componentes.
