# SIGEFOR — Sistema Integral de Gestión de Fornituras

> **Qué es este documento.** Requerimientos funcionales por pantalla, redactados como contrato
> de producto. Es la visión del cliente ya depurada; el detalle ejecutable de cada módulo vive
> en `specs/` (spec-driven) y el mapeo pantalla → feature está en
> [`specs/README.md`](./specs/README.md). Si una norma cambia, se edita aquí y se refleja en la
> spec correspondiente.
>
> **Vocabulario.** El producto es **SIGEFOR**. La unidad controlada es la **fornitura**, la prenda
> de dotación del cuerpo. **"Fornitura" es un tipo de prenda** concreto (no una categoría con
> subtipos): el catálogo de tipos de prenda tiene hoy un único valor, "Fornitura". Un **elemento**
> es el personal policial al que se le asigna equipo. Una **asignación** vigente genera un
> **resguardo**.

---

## Convenciones generales (aplican a todas las pantallas)

Estas reglas evitan repetirlas en cada módulo. Son requisito transversal.

- **Paginación siempre.** Toda tabla/listado carga **por páginas** desde el servidor (nunca el
  conjunto completo de una vez) para mantener buen rendimiento con volúmenes grandes. Cada
  consulta paginada respeta los filtros activos.
- **Captura de código QR unificada.** Donde se pida un código QR, el campo **detecta
  automáticamente** el origen: **lector HID** (escáner físico que "teclea"), **cámara**
  (Capacitor en móvil; cámara de PC si está disponible, como mejor esfuerzo) o **tecleo
  manual**. Placeholder sugerido: *"Escanee con el lector o teclee el código"*. El detalle vive
  en la feature **014-escaneo-qr**.
- **El QR no contiene PII.** Es un identificador **opaco** ligado al número de serie; los datos
  reales se resuelven en el servidor tras autenticación y autorización (ver
  [`docs/02-seguridad.md`](./docs/02-seguridad.md) y la feature **002-qr-equipos**).
- **Búsqueda por texto libre.** Donde se ofrezca, busca contra **todas** las columnas relevantes
  (p. ej. nombre, CURP, RFC, placa) además de los filtros específicos.
- **Estados con color semántico.** Disponible (verde), Asignada (azul), En mantenimiento
  (amarillo), En traslado (gris/azul), Próxima a vencer ≤ 90 días (naranja), Caducada (rojo),
  Baja definitiva (gris). Catálogo y paleta en [`docs/05-ui-ux.md`](./docs/05-ui-ux.md).
- **Identidad institucional.** Apariencia profesional gob.mx (guinda `#611232` / dorado
  `#A57F2C`), barra superior con usuario/rol y menú lateral por permisos (mínimo privilegio).
- **Acceso por rol (RBAC).** Cada usuario ve solo su menú y sus acciones. La PII se enmascara
  salvo a roles autorizados. Roles implementados hoy: `ADMIN` y `CAPTURISTA`; expansión en la
  feature **013**.

### Convenciones de layout y UX

El *cómo* se distribuye y se siente cada pantalla vive en
[`docs/06-patrones-ux.md`](./docs/06-patrones-ux.md) (catálogo de patrones y componentes) y en
[`docs/05-ui-ux.md`](./docs/05-ui-ux.md) (identidad visual). Reglas transversales que aplican a
todas las pantallas:

- **Vista de datos responsiva (dualidad).** Los listados se presentan como **tabla** en escritorio
  (columnas ordenables/configurables, densidad conmutable, master–detail) y como **tarjetas** en
  móvil. Mismos datos, distinta presentación según el tamaño de pantalla.
- **Scan-first.** En toda pantalla con captura de QR, el campo de escaneo es el elemento
  protagonista y queda **enfocado al cargar**; el flujo se completa **sin ratón** (el lector HID
  es un teclado). Componente único reutilizable (feature **014**).
- **Cuatro estados, no dos.** Cada vista resuelve **cargando** (esqueleto, no spinner pelón),
  **con datos**, **vacío** (mensaje + acción para crear el primero) y **error** (mensaje claro +
  reintentar). El vacío distingue "no hay nada aún" de "sin coincidencias con el filtro".
- **Filtros visibles y reversibles.** La barra de filtros es *sticky*; los filtros activos se
  muestran como **chips removibles** y su estado se refleja en la URL (enlaces compartibles).
- **Confirmación de acciones sensibles.** Bajas, devolución de resguardo y revelado de PII piden
  confirmación que nombra la consecuencia; el revelado de PII queda auditado (feature **012**).
- **Encabezado y componentes consistentes.** Toda pantalla reusa las mismas piezas
  (`PageHeader`, `DataView`, `FilterBar`, `ScanInput`, `StatusBadge`, `Pager`, `PiiField`)
  descritas en `06-patrones-ux.md`; no se construyen layouts ad hoc.

### Nombres de módulo (navegación)

Se profesionalizaron los nombres del borrador original para una presentación institucional:

| # | Pantalla (borrador original) | Nombre de menú | Feature |
|---|------------------------------|----------------|---------|
| 0 | Inicio / Dashboard | **Tablero** | 010-dashboard |
| 1 | Captura y asignación | **Asignación y Resguardos** | 004-asignacion-resguardos |
| 2 | Elementos | **Padrón de Elementos** | 003-elementos-padron |
| 3 | Fornituras | **Inventario de Fornituras** | 001-inventario-equipos |
| 4 | Incidencias | **Incidencias** | 008-incidencias |
| 5 | Traslados | **Traslados** | 007-traslados |
| 6 | Bajas | **Bajas Definitivas** | 009-bajas |
| 7 | Reportes | **Reportes y Estadística** | 011-reportes |
| 8 | Usuarios | **Usuarios y Roles** | 013-usuarios |
| 9 | Auditoría | **Bitácora de Auditoría** | 012-auditoria |
| 10 | Tipo de fornituras | **Catálogo de Tipos** | 006-tipos-fornitura |
| 11 | Almacenes | **Almacenes** | 005-almacenes |

---

## 1. Asignación y Resguardos

**Feature:** `004-asignacion-resguardos`

Pantalla central del sistema: liga una fornitura con el elemento que la resguarda.

**Al cargar:** muestra las **asignaciones vigentes** (qué fornitura tiene asignada cada
elemento), paginadas, y un botón **"Asignar"** que abre el flujo de captura en dos pasos.

**Flujo de asignación (2 pasos):**

1. **Identificar la fornitura.** Campo de código QR (detección automática lector/cámara/manual)
   con botón **"Buscar"**. Al encontrarla, debajo se muestra su **descripción** y si está
   **disponible**. Si no está disponible (asignada, en mantenimiento, en traslado, baja), se
   indica y **no** se permite continuar.
2. **Identificar al elemento.** Campo de búsqueda por **nombre, placa, CURP o RFC** con botón
   buscar. Al encontrarlo, se muestra su **nombre, descripción y foto**.

Con ambos pasos completos, se ofrecen dos botones: **"Asignar fornitura"** y **"Limpiar"**.

**Layout (ver [`06-patrones-ux.md`](./docs/06-patrones-ux.md)).** Pantalla de **dos zonas**:

- **Zona de acción (arriba):** el flujo de 2 pasos se presenta como **`Stepper`** (paso 1
  fornitura → paso 2 elemento), no como lista inline. El paso 1 usa **`ScanInput`** (protagonista,
  enfocado al cargar). La disponibilidad de la fornitura se muestra con **`StatusBadge`**; si no
  está disponible, el paso 2 permanece bloqueado. El elemento encontrado se previsualiza en una
  **tarjeta de confirmación** (foto, nombre, placa) con su PII vía **`PiiField`**.
- **Zona de consulta (abajo):** **`DataView`** de **asignaciones vigentes** (tabla en escritorio,
  tarjetas en móvil) con `Pager` de servidor y acción **"Devolver"** por fila (con
  `ConfirmDialog`). En escritorio, ambas zonas conviven a dos columnas; en móvil se apilan.

---

## 2. Padrón de Elementos

**Feature:** `003-elementos-padron` · **Dato de ALTA SENSIBILIDAD (PII policial).**

**Pantalla principal.** Encabezado con botón **"Reporte"** y botón **"Nuevo elemento"**. Panel
de filtros: campo de texto que busca por **todas** las columnas (nombre, CURP, RFC, placa) más
filtros por **municipio** y **sexo**, con botón **"Buscar"**. Debajo, tabla paginada con
columnas: **foto (miniatura), nombre completo, CURP, ID (placa), municipio, tipo de sangre,
acciones**.

**Pantalla "Nuevo elemento".** Captura de datos del elemento policial:

- **Identidad:** nombre, apellido paterno, apellido materno, sexo (catálogo), tipo de sangre
  (catálogo).
- **Documentos:** CURP, RFC, identificador (placa/serie), municipio.
- **Foto** del elemento.

> La PII (CURP, RFC, foto) se trata bajo cifrado y enmascaramiento según
> [`docs/02-seguridad.md`](./docs/02-seguridad.md) y los ADR
> [0003](./docs/04-decisiones/0003-pii-elementos.md),
> [0004](./docs/04-decisiones/0004-busqueda-pii-cifrada.md) y
> [0006](./docs/04-decisiones/0006-cifrado-pii-nivel-aplicacion.md).

**Layout (ver [`06-patrones-ux.md`](./docs/06-patrones-ux.md)).** `PageHeader` con acciones
**"Reporte"** y **"Nuevo elemento"** (botón en la barra en escritorio; **FAB** en móvil).
**`FilterBar`** *sticky*: texto libre (busca en todas las columnas) + selects de **municipio** y
**sexo**, con chips removibles. **`DataView`** con las columnas indicadas (foto miniatura, nombre,
CURP, placa, municipio, tipo de sangre, acciones): **tabla** en escritorio con **master–detail**
(la ficha del elemento abre en panel lateral, no navega fuera) y **tarjetas** en móvil. La PII
(CURP, RFC, foto) se pinta con **`PiiField`** (enmascarada por defecto, revelado auditado según
los ADR citados). Cuatro estados resueltos (esqueleto / datos / vacío con CTA / error).

---

## 3. Inventario de Fornituras

**Feature:** `001-inventario-equipos`

**Pantalla principal.** Botón **"Nueva fornitura"** y, a su lado, **"Alta por lote"**. Panel de
filtros: **QR, descripción, estado, almacén**. Debajo, tabla paginada con columnas: **QR,
descripción, tipo (cinturón, chaleco…), talla, almacén, estado, vida útil, acciones**.

### 3.1 Alta de fornitura individual

Formulario para registrar una pieza:

- **Número de QR (único)** — capturado por lector, cámara o tecleo manual.
- **Descripción, talla** (catálogo), **tipo** (catálogo: cinturón, chaleco…), **almacén**,
  **vida útil**.

### 3.2 Alta de fornituras por lote

Para cargar muchas piezas que comparten datos generales:

1. **Datos generales del lote:** descripción, talla (catálogo), tipo (catálogo), almacén, vida
   útil.
2. **Captura de códigos QR** uno a uno (lector / cámara / manual): campo con placeholder
   *"Escanee con el lector o teclee el código"* y botón **"Agregar"**. Cada código se añade a
   una **tabla de fornituras del lote** para revisión (se rechazan duplicados antes de
   confirmar).
3. Botones **"Crear lote"** y **"Cancelar"**.

> **Modelado de "vida útil" (adoptado en la spec 001).** El dato canónico es una **fecha de
> vencimiento absoluta** (`fecha_vencimiento`), porque elimina ambigüedad y permite derivar
> alertas y estados de vigencia (vigente / próxima a vencer ≤ 90 días / caducada). En el alta se
> captura la **vida útil en meses** (`vida_util_meses`) junto con `fecha_fabricacion`/adquisición
> y el sistema calcula `fecha_vencimiento = fecha_fabricacion + vida_util_meses`. Así el usuario
> piensa en "meses/años" y el sistema razona con una fecha concreta.

**Layout (ver [`06-patrones-ux.md`](./docs/06-patrones-ux.md)).** `PageHeader` con acciones
**"Nueva fornitura"** y **"Alta por lote"**. **`FilterBar`** *sticky* con QR, descripción, estado
y almacén (chips removibles). **`DataView`** con las columnas indicadas (QR, descripción, tipo,
talla, almacén, estado, vida útil, acciones): **tabla** ordenable en escritorio, **tarjetas** en
móvil. La columna **estado** usa **`StatusBadge`** y la **vida útil** muestra el indicador de
vigencia derivado (vigente / próxima a vencer ≤ 90 días / caducada) con su color semántico. El
campo **QR** de las altas (individual y lote) usa **`ScanInput`**; el alta por lote acumula los
códigos en una **tabla de revisión** con rechazo de duplicados antes de confirmar (patrón de
acciones masivas). Cuatro estados resueltos.

---

## 4. Incidencias

**Feature:** `008-incidencias`

Registro de problemas, mantenimiento y alertas sobre fornituras.

**Encabezado:** combo/filtro por **estado**. Tabla paginada con columnas: **fornitura, problema,
reportada (fecha), estado, actualizar (acción)**.

---

## 5. Traslados

**Feature:** `007-traslados`

Movimiento de fornituras entre almacenes.

**Al cargar:** lista los traslados realizados (con filtros) y un botón **"Nuevo traslado"**.

**Nuevo traslado:** capturar **almacén origen** y **almacén destino**; luego capturar el código
QR de cada fornitura (lector / cámara / manual) con un campo de búsqueda y botón **"Agregar"**.
Debajo, **tabla de fornituras a trasladar**.

**Traslados registrados:** tabla con columnas **origen, destino, estado, enviado (fecha),
recibido (fecha), acción**.

---

## 6. Bajas Definitivas

**Feature:** `009-bajas`

Retiro definitivo de una fornitura. Permite dar de baja buscando la fornitura **por QR**, y
muestra una **tabla paginada de fornituras dadas de baja** con la información relevante (QR,
descripción, motivo, fecha de baja, responsable).

---

## 7. Reportes y Estadística

**Feature:** `011-reportes`

- **Exportación a Excel.**
- **Resumen general:** total de fornituras y desglose por estado (**disponibles, asignadas, en
  incidencia, baja**) y total de **elementos**.
- **Asignaciones activas:** tabla con filtro por **QR de fornitura, nombre, RFC, placa, CURP** y
  filtro por **municipio**.
- Tabla de detalle con columnas relevantes (fornitura, elemento y las que se consideren
  útiles).

---

## 8. Usuarios y Roles

**Feature:** `013-usuarios`

Gestión de usuarios del sistema y sus roles (RBAC). Hoy existen `ADMIN` y `CAPTURISTA` (login
por email + JWT, ya operativo); la expansión de roles y MFA está propuesta en la spec **013**
(pendiente de ADR).

---

## 9. Bitácora de Auditoría

**Feature:** `012-auditoria`

Registro de las transacciones realizadas: **fecha, usuario, acción** y datos relevantes,
tomando como referencia **ISO/IEC 27001**. Solo consulta y filtros; los registros son
inmutables.

---

## 10. Catálogo de Tipos

**Feature:** `006-tipos-fornitura`

CRUD del catálogo de **tipos de prenda**: **nombre, foto, descripción**. Operaciones de alta,
edición, baja y consulta. Una **fornitura** es un tipo de prenda concreto; el catálogo arranca con
un único valor ("Fornitura") y se conserva administrable por si más adelante se dan de alta otras
prendas.

---

## 11. Almacenes

**Feature:** `005-almacenes`

CRUD de almacenes (ubicaciones físicas donde residen las fornituras). Prerequisito de inventario
y traslados.

---

## 0. Tablero (mejora propuesta)

**Feature:** `010-dashboard`

No estaba en el borrador original como pantalla numerada, pero se recomienda como **inicio** del
sistema: tablero de indicadores (totales por estado, próximas a vencer, incidencias abiertas,
asignaciones recientes) usando la semántica de color de `docs/05-ui-ux.md`. Da contexto operativo
de un vistazo.
