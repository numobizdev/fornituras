# Feature Specification: Padrón de elementos policiales

**Feature Branch**: `003-elementos-padron`

**Created**: 2026-06-30

**Status**: Draft

**Input**: `Requerimientos.MD` §2 (Elementos) — listado con filtros, alta de elemento, foto.

> **DATO DE ALTA SENSIBILIDAD (PII de personal de seguridad pública).** Esta feature es la de
> mayor riesgo del sistema. Toda decisión aquí DEBE cumplir
> [`docs/02-seguridad.md`](../../docs/02-seguridad.md) y la Constitución (Principio I). Ver la
> sección **Decisión abierta: alcance de PII** más abajo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar el padrón de elementos (Priority: P1)

Un usuario autorizado consulta el padrón con **paginación**, busca por texto libre (nombre,
CURP, RFC o placa) y filtra por municipio y sexo. Ve una tabla con foto en miniatura, nombre
completo, identificador (placa), municipio, tipo de sangre y acciones.

**Why this priority**: Sin el padrón no se puede asignar una fornitura a un elemento (feature
**004**). Es el primer uso real de la pantalla al cargar.

**Independent Test**: Con varios elementos cargados, buscar por una placa devuelve el elemento
correcto; filtrar por municipio devuelve solo los de ese municipio; la respuesta llega
paginada.

**Acceptance Scenarios**:

1. **Given** elementos registrados, **When** el usuario escribe texto que coincide con nombre,
   CURP, RFC o placa, **Then** la tabla muestra las coincidencias paginadas.
2. **Given** elementos de varios municipios, **When** filtra por municipio y/o sexo, **Then**
   obtiene solo los que cumplen el filtro.
3. **Given** un usuario sin permiso de ver PII completa, **When** consulta el padrón, **Then**
   se le ocultan/enmascaran los campos sensibles (CURP/RFC) según su rol y el acceso queda
   auditado (Principio V).

---

### User Story 2 - Registrar un nuevo elemento (Priority: P1)

Un usuario autorizado da de alta un elemento capturando nombre, apellido paterno, apellido
materno, sexo (catálogo), tipo de sangre (catálogo), identificador (placa/serie), municipio y
estado (**texto libre**), los documentos que la política de PII autorice (ver decisión abierta) y
su fotografía.

**Why this priority**: Habilita la asignación; es parte del MVP junto con el inventario.

**Independent Test**: Dar de alta un elemento con placa nueva y verificar que aparece en el
padrón y que el sistema rechaza un segundo elemento con la misma placa.

**Acceptance Scenarios**:

1. **Given** un usuario autorizado, **When** registra un elemento con identificador (placa)
   nuevo y datos válidos, **Then** el elemento queda guardado y disponible para asignación.
2. **Given** un identificador (placa) ya existente, **When** se intenta registrar otro con el
   mismo, **Then** el sistema lo rechaza sin crear duplicado.
3. **Given** datos con formato inválido (CURP/RFC mal formados, si se capturan), **When** se
   envía el alta, **Then** el sistema valida en el borde y rechaza con mensaje claro.

---

### User Story 3 - Generar reporte del padrón (Priority: P3)

Un usuario autorizado genera un reporte del padrón (con los filtros aplicados) para control
interno, respetando el enmascaramiento de PII según rol.

**Why this priority**: Útil para control, pero secundario frente a consulta y alta.

**Independent Test**: Aplicar un filtro por municipio y generar el reporte; el archivo contiene
solo los elementos de ese municipio y respeta el enmascaramiento de PII.

### Edge Cases

- Foto del elemento: límites de tamaño/formato, almacenamiento cifrado y acceso autorizado (la
  foto es **PII biométrica indirecta**).
- Elemento dado de baja del cuerpo policial: ¿se conserva por historial de asignaciones o se
  anonimiza? (derechos ARCO / retención — ver decisión abierta).
- Búsqueda por texto que coincide parcialmente con CURP/RFC: no debe permitir enumeración de
  PII a roles sin permiso.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST listar elementos con **paginación** y permitir búsqueda por texto
  libre sobre nombre, CURP, RFC e identificador (placa), más filtros por municipio y sexo.
- **FR-002**: El sistema MUST permitir registrar un elemento con: nombre, apellido paterno,
  apellido materno, sexo (catálogo), tipo de sangre (catálogo), identificador (placa/serie),
  municipio y fotografía; y los documentos (CURP/RFC) que autorice la política de PII vigente.
- **FR-003**: El identificador (placa/serie) del elemento MUST ser **único**; el sistema rechaza
  duplicados normalizando antes de comparar.
- **FR-004**: El sistema MUST validar en el borde el formato de los datos capturados (incluido
  CURP/RFC si se recolectan).
- **FR-005**: El sistema MUST aplicar **autorización por rol** sobre la PII: los campos
  sensibles (CURP/RFC, foto) se muestran solo a roles autorizados; al resto se ocultan o
  enmascaran.
- **FR-006**: Todo acceso a la ficha completa de un elemento y toda alta/edición MUST quedar
  **auditados** (quién, qué elemento, cuándo) sin escribir PII en el log (Principio V).
- **FR-007**: Las columnas con PII (nombre completo, CURP, RFC, foto, tipo de sangre) MUST
  almacenarse con **Always Encrypted** y la foto en almacenamiento cifrado con acceso
  autorizado (Constitución §Cifrado en reposo).
- **FR-008**: El sistema MUST NOT exponer PII en URLs, parámetros de búsqueda cacheables, ni en
  el código QR de ninguna fornitura asignada (Principio II).
- **FR-009**: El sistema MUST soportar la **finalidad declarada** y la **minimización**: no se
  recolecta PII sin justificación de finalidad registrada (decisión abierta / ADR).

### Key Entities *(include if feature involves data)*

- **Elemento** (`officer`): personal policial. Atributos: identificador interno, nombre y
  apellidos, sexo, tipo de sangre, identificador (placa/serie), municipio, fotografía y
  (sujeto a política de PII) CURP/RFC. **Contiene PII de alta sensibilidad.** Ver
  [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
- **Sexo**, **Tipo de sangre**: catálogos (modelados con la estructura genérica `catalog →
  catalog_item` de la spec **006**; `code` = `SEXO`, `TIPO_SANGRE`).
- **Municipio**, **Estado**: **texto libre** (decisión 2026-06-30; ya no son catálogo ni FK).

## Decisión abierta: alcance de PII *(NEEDS CLARIFICATION → ADR pendiente)*

`Requerimientos.MD` pide capturar **CURP, RFC, placa, tipo de sangre, foto y municipio**. La
Constitución (Principio I) y `docs/02-seguridad.md` exigen **minimizar PII**. Hay dos posturas:

- **Postura A — Capturar todo con controles.** Recolectar el conjunto completo, marcando
  CURP/RFC/nombre/tipo de sangre/foto como **Always Encrypted**, con RBAC estricto, auditoría
  de acceso y política de retención/ARCO. Cumple el requerimiento; asume el riesgo con
  controles fuertes.
- **Postura B — Minimizar.** Recolectar solo lo imprescindible para identificar y asignar
  (identificador/placa, nombre, municipio, foto), y **diferir CURP/RFC** hasta justificar su
  finalidad concreta. Cumple mejor el principio de minimización; choca con el requerimiento.

**Recomendación (a confirmar por el responsable y registrar como ADR `0003-pii-elementos`):**
adoptar una **Postura A acotada**: capturar CURP/RFC/foto **solo si existe finalidad declarada
y base legal** (control de dotación + identificación inequívoca del resguardatario), con
**Always Encrypted**, RBAC, enmascaramiento por defecto, auditoría de todo acceso a la ficha
completa, y política de retención/ARCO. Mientras no exista el ADR, estos campos se tratan como
**[PENDIENTE]**: el esquema los contempla pero su captura permanece deshabilitada o restringida
a un único rol. La decisión final corresponde al área legal del cliente (LFPDPPP / LGPDPPSO).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Una búsqueda por placa/CURP/RFC/nombre devuelve el elemento correcto, paginada y
  en menos de 2 segundos con decenas de miles de registros.
- **SC-002**: El 100% de los accesos a la ficha completa de un elemento quedan auditados.
- **SC-003**: El 100% de los campos PII están cifrados en reposo (Always Encrypted / storage
  cifrado para la foto).
- **SC-004**: Ningún rol sin autorización visualiza CURP/RFC ni descarga la foto.
- **SC-005**: Cero PII en logs, URLs o el contenido de cualquier QR.

## Assumptions

- Los catálogos **sexo** y **tipo de sangre** se modelan con la estructura genérica `catalog →
  catalog_item` (spec **006**). **Municipio** y **estado** se capturan como **texto libre** (ya no
  son catálogo).
- La asignación de fornituras a elementos se especifica en **004-asignacion-resguardos**.
- La política exacta de PII (qué se captura) queda sujeta al ADR `0003-pii-elementos`.

## Dependencies

- Constitución (Principios I, II, IV, V) y `docs/02-seguridad.md`.
- Modelo de datos: [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
- ADR pendiente: `docs/04-decisiones/0003-pii-elementos.md`.
- Features: **004-asignacion-resguardos**, **011-reportes**, **012-auditoria**, **013-usuarios**.
