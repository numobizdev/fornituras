# Feature Specification: Inventario de fornituras

**Feature Branch**: `001-inventario-equipos`

**Created**: 2026-06-29

**Updated**: 2026-06-30 (merge con SIGEFOR — `Requerimientos.MD` y `Paleta de colores.MD`)

**Status**: Draft

**Input**: User description: "Inventario de fornituras (equipos de blindaje y equipamiento):
alta individual, alta por lote, consulta, edición y cambio de estado"

> **Vocabulario (SIGEFOR).** Una **fornitura** es la prenda de dotación controlada por el sistema.
> El término sustituye a "equipo" usado en versiones previas de esta spec. **"Fornitura" es un
> tipo de prenda concreto**, no una categoría con subtipos: el catálogo de **tipos de prenda**
> (`TIPO_PRENDA`, feature **006**) tiene hoy un único valor, "Fornitura". El producto se llama
> **SIGEFOR (Sistema Integral de Gestión de Fornituras)**. Ver `Requerimientos.MD`.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registrar una fornitura individual (Priority: P1)

Un usuario de almacén da de alta una fornitura capturando su **número de QR único** (por
lector, cámara o tecleo manual), su tipo, talla, almacén y vida útil. A partir de ese momento
la fornitura existe en el inventario y puede consultarse, asignarse y trasladarse.

**Why this priority**: Es el cimiento de todo el sistema. Sin inventario no hay nada que
asignar, ni a qué colgar el QR.

**Independent Test**: Dar de alta una fornitura con número de QR/serie y verificar que aparece
en la consulta y que el sistema rechaza una segunda con el mismo identificador único.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado con rol de alta, **When** registra una fornitura con
   identificador nuevo y atributos válidos (tipo, talla, almacén, vida útil), **Then** la
   fornitura queda guardada con estado inicial **"disponible"** y un identificador interno
   único.
2. **Given** ya existe una fornitura con cierto número de QR/serie, **When** se intenta
   registrar otra con el mismo, **Then** el sistema lo rechaza con un mensaje claro y no crea
   duplicado.
3. **Given** un usuario sin permiso de alta, **When** intenta registrar una fornitura, **Then**
   el sistema deniega la operación.

---

### User Story 2 - Alta por lote (Priority: P2)

Un usuario de almacén registra muchas fornituras que comparten datos generales (descripción,
tipo, talla, almacén, vida útil) y solo difieren en su código QR. Captura los datos del lote
una vez y luego agrega los códigos QR uno a uno (lector / cámara / manual), revisándolos en una
tabla antes de crear el lote completo.

**Why this priority**: Es el modo de carga real en bodega (cientos de piezas iguales). Reduce
drásticamente el tiempo de captura frente al alta individual.

**Independent Test**: Capturar datos de lote, agregar 3 códigos QR distintos y confirmar; los 3
quedan creados con los mismos datos generales y QR distintos; un código repetido dentro del
lote se rechaza antes de confirmar.

**Acceptance Scenarios**:

1. **Given** datos generales de lote válidos, **When** el usuario agrega varios códigos QR
   únicos y confirma, **Then** se crea una fornitura por cada código, todas en estado
   "disponible" y con los datos generales del lote.
2. **Given** un código QR ya presente en el lote o ya existente en el sistema, **When** se
   intenta agregar, **Then** el sistema lo rechaza y no lo añade a la tabla del lote.
3. **Given** un lote en captura, **When** el usuario cancela, **Then** no se crea ninguna
   fornitura.

---

### User Story 3 - Consultar y buscar fornituras (Priority: P2)

Un operador consulta el inventario con **paginación** y filtros (QR, descripción, tipo, talla,
almacén, estado) y ve la ficha de cada fornitura, sin datos personales del elemento salvo que
su rol lo permita.

**Why this priority**: La consulta es el uso más frecuente; la paginación es requisito de
desempeño (`Requerimientos.MD`: no cargar todos los datos).

**Independent Test**: Con varias fornituras cargadas, filtrar por estado "disponible" devuelve
solo las disponibles y la respuesta llega paginada; buscar por código QR devuelve la fornitura
correcta.

**Acceptance Scenarios**:

1. **Given** fornituras registradas, **When** el usuario busca por código QR/serie exacto,
   **Then** obtiene la ficha de esa fornitura.
2. **Given** fornituras en distintos estados, **When** el usuario filtra por un estado, **Then**
   obtiene solo las fornituras en ese estado, paginadas.
3. **Given** un usuario autenticado, **When** consulta la ficha de una fornitura, **Then** ve
   los datos conforme a su rol y queda registrado el acceso si incluye datos sensibles.

---

### User Story 4 - Editar y cambiar el estado de una fornitura (Priority: P3)

Un supervisor corrige atributos no identitarios o cambia el estado de una fornitura
(disponible, asignada, en mantenimiento, en traslado, extraviada, baja definitiva) reflejando
su situación real.

**Why this priority**: Mantiene el inventario fiel a la realidad; llega después de poder crear
y consultar.

**Independent Test**: Cambiar el estado de una fornitura de "disponible" a "en mantenimiento" y
verificar que la consulta refleja el nuevo estado y que el cambio queda auditado.

**Acceptance Scenarios**:

1. **Given** una fornitura existente, **When** un usuario autorizado edita un atributo no
   único, **Then** el cambio se guarda y queda registrado quién y cuándo lo hizo.
2. **Given** una fornitura "disponible", **When** se cambia su estado a "baja definitiva",
   **Then** deja de aparecer como asignable pero conserva su historial (ver feature
   **009-bajas**).
3. **Given** una fornitura asignada a un elemento, **When** se intenta darla de baja o
   trasladarla, **Then** el sistema advierte/impide la acción hasta resolver la asignación
   vigente.

### Edge Cases

- ¿Qué pasa si el número de serie/QR viene con espacios o distinto formato
  (mayúsculas/guiones)? Debe **normalizarse** para evitar duplicados "aparentemente distintos".
- ¿Qué pasa al editar el número de serie/QR de una fornitura que ya tiene QR grabado? Es ancla
  de identidad; cambiarlo debe estar restringido y auditado.
- ¿Cómo se maneja una fornitura cuya fecha de vencimiento está próxima o vencida? El sistema
  deriva los estados de vigencia "próxima a vencer" (≤ 90 días) y "caducada" (vencida) a partir
  de `fecha_vencimiento` y los señala con alertas (ver feature **008-incidencias** y
  `Paleta de colores.MD` §Alertas Inteligentes).
- ¿Qué ocurre al intentar dar de baja o trasladar una fornitura con asignación activa? (ver US4
  escenario 3).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir registrar una fornitura con un identificador **único**
  (número de QR/serie) y sus atributos: tipo (catálogo), talla (catálogo), almacén, vida útil,
  y los descriptivos aplicables (descripción, marca, modelo, nivel balístico, número de
  inventario, fechas de fabricación/adquisición/vencimiento, ubicación, observaciones, foto,
  documentación adjunta — ver `Paleta de colores.MD` §Gestión de Inventario).
- **FR-002**: El sistema MUST permitir el **alta por lote**: capturar datos generales una vez y
  agregar múltiples códigos QR (lector / cámara / manual), creando una fornitura por código.
- **FR-003**: El sistema MUST rechazar el alta o edición que produzca un identificador único
  duplicado, normalizando el valor antes de comparar, tanto entre fornituras existentes como
  dentro de un mismo lote en captura.
- **FR-004**: El sistema MUST asignar a cada fornitura un identificador interno único e
  inmutable, independiente del número de serie/QR.
- **FR-005**: El sistema MUST permitir consultar y buscar fornituras con **paginación** y
  filtrar por QR, descripción, tipo, talla, almacén y estado.
- **FR-006**: El sistema MUST permitir editar atributos no identitarios y cambiar el estado
  operativo entre: **disponible, asignada, en mantenimiento, en traslado, extraviada, baja
  definitiva** (catálogo controlado).
- **FR-007**: El sistema MUST derivar los **estados de vigencia** "próxima a vencer" (≤ 90 días
  para `fecha_vencimiento`) y "caducada" (vencida) sin requerir cambio manual de estado, para
  alimentar alertas y reportes.
- **FR-008**: El sistema MUST impedir (o advertir y bloquear) la baja o el traslado de una
  fornitura con asignación vigente.
- **FR-009**: Toda operación de alta, edición, cambio de estado y baja MUST quedar registrada
  en auditoría con el actor y la marca de tiempo (Constitución, Principio V).
- **FR-010**: Toda operación sobre el inventario MUST requerir usuario autenticado y autorizado
  según su rol; consulta, alta/edición y baja pueden requerir roles distintos (Principio IV).
- **FR-011**: El sistema MUST NOT exponer datos personales del elemento asignado a usuarios
  cuyo rol no lo permita, ni en las fichas de fornitura.
- **FR-012**: El estado de la fornitura MUST ser coherente con sus asignaciones (una "asignada"
  tiene asignación vigente; una "disponible" no) y con sus traslados (una "en traslado" tiene
  un traslado en curso).

### Key Entities *(include if feature involves data)*

- **Fornitura** (`equipment`): el equipo físico controlado. Atributos clave: identificador
  interno (único), número de serie/QR (único), tipo, talla, almacén, vida útil
  (`fecha_vencimiento`), estado operativo, y descriptivos. Se relaciona con asignaciones, su QR,
  traslados e incidencias. Ver [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
- **Estado operativo**: catálogo controlado (disponible, asignada, en mantenimiento, en
  traslado, extraviada, baja definitiva).
- **Estado de vigencia** (derivado): vigente / próxima a vencer / caducada, calculado desde
  `fecha_vencimiento`.
- **Tipo de prenda** y **Talla**: catálogos (ver feature **006-tipos-fornitura**). El tipo de
  prenda de toda fornitura es hoy el único valor `Fornitura` del catálogo `TIPO_PRENDA`.
- **Almacén**: ubicación de resguardo (ver feature **005-almacenes**).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un usuario autorizado puede dar de alta una fornitura individual en menos de 1
  minuto, y un lote de 50 piezas en menos de 10 minutos.
- **SC-002**: El 100% de los intentos de registrar un identificador duplicado son rechazados
  (cero duplicados en el inventario).
- **SC-003**: Una búsqueda por código QR/serie devuelve la fornitura correcta en una sola
  consulta, sin ambigüedad.
- **SC-004**: El listado responde paginado y la primera página carga en menos de 2 segundos con
  decenas de miles de fornituras.
- **SC-005**: El 100% de las operaciones de alta, edición y baja quedan auditadas con actor y
  fecha.
- **SC-006**: Ningún usuario sin autorización obtiene datos personales del elemento asignado.

## Assumptions

- **Vida útil — recomendación de modelado:** la fuente de verdad es una **fecha de vencimiento
  absoluta** (`fecha_vencimiento`, tipo `date`), porque elimina ambigüedad y permite derivar
  alertas y los estados de vigencia. Para el alta por lote conviene además capturar la vida útil
  como **duración en meses** (`vida_util_meses`) y/o `fecha_fabricacion`, de modo que el sistema
  calcule `fecha_vencimiento = fecha_fabricacion + vida_util_meses`. Recomendación: guardar
  ambas (`fecha_fabricacion` + `vida_util_meses` → `fecha_vencimiento` derivada y persistida),
  con `fecha_vencimiento` como dato canónico para consultas y alertas.
- El número de serie/QR lo provee el grabado/impresión de la fornitura y es la fuente de
  unicidad física; el identificador interno es opaco y solo del sistema.
- Los catálogos de tipo, talla y estado se definen como tablas de catálogo
  (features **006-tipos-fornitura** y `docs/03-modelo-datos.md`).
- Los códigos QR se **pregeneran por lotes** (código opaco `FOR-XXXXX`, sin firma; ver
  **002-qr-equipos** y [ADR 0005](../../docs/04-decisiones/0005-formato-qr-implementado.md)) y se
  imprimen/graban antes; el alta de fornitura **liga** un código ya existente al escanearlo. La
  lectura/escaneo se especifica en **014-escaneo-qr**.
- La asignación fornitura↔elemento se especifica en **004-asignacion-resguardos**; aquí solo se
  necesita conocer si una fornitura tiene asignación vigente para validar baja/traslado.
- Roles iniciales: ADMIN, SUPERVISOR (alta/edición/baja), ALMACEN (administración de
  inventario), OPERADOR/AUDITOR (consulta), conforme a `docs/02-seguridad.md` y feature
  **013-usuarios**.

## Dependencies

- Constitución: [`.specify/memory/constitution.md`](../../.specify/memory/constitution.md)
  (Principios IV, V, VI aplican directamente).
- Seguridad: [`docs/02-seguridad.md`](../../docs/02-seguridad.md).
- Modelo de datos: [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
- UI/UX y estados con color semántico: [`docs/05-ui-ux.md`](../../docs/05-ui-ux.md).
- Features relacionadas: **002-qr-equipos**, **004-asignacion-resguardos**, **005-almacenes**,
  **006-tipos-fornitura**, **007-traslados**, **008-incidencias**, **009-bajas**,
  **014-escaneo-qr**.
