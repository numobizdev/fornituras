# Feature Specification: QR opaco y firmado por fornitura

**Feature Branch**: `002-qr-equipos`

**Created**: 2026-06-29

**Updated**: 2026-06-30 (merge SIGEFOR: "equipo" → "fornitura"; lectura/escaneo movida a **014**)

**Status**: Draft

**Input**: User description: "Generación de QR opaco y firmado por fornitura para grabado o impresión"

> **Vocabulario (SIGEFOR).** "Equipo" se renombra a **fornitura** (chaleco, cinturón, casco…).
> La **lectura/escaneo** del QR (cámara Capacitor, lector HID, tecleo manual) y la pantalla de
> ficha al escanear se especifican en la feature **014-escaneo-qr**; aquí solo se cubren
> generación, verificación de firma y exportación para grabado.

> **Nota de prioridad operativa:** el cliente necesita comenzar a **generar QR cuanto antes**
> porque se graban/imprimen sobre el equipo físico. Por eso el formato del payload del QR es
> una decisión **bloqueante** (ver Assumptions y la pregunta de clarificación): un QR grabado
> con un formato provisional sería irreversible y costoso de rehacer.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generar el QR de un equipo (Priority: P1)

Un supervisor genera el QR de un equipo ya registrado en el inventario. El sistema produce un
código imprimible/grabable, único para ese equipo, que **no contiene ningún dato personal ni
explotable**: solo un identificador opaco acompañado de una firma que prueba que lo emitió el
sistema.

**Why this priority**: Es la funcionalidad urgente y el habilitador de la trazabilidad física
del equipo. Una vez fijado el formato, los QR pueden grabarse de forma definitiva.

**Independent Test**: Generar el QR de un equipo y verificar que (a) el contenido no incluye
número de serie, nombre ni datos del elemento; (b) dos equipos distintos producen QR
distintos; (c) el mismo equipo produce un QR estable/reproducible.

**Acceptance Scenarios**:

1. **Given** un equipo registrado sin QR, **When** un usuario autorizado solicita generar su
   QR, **Then** el sistema crea un identificador opaco firmado, lo liga al equipo y entrega el
   código en un formato apto para impresión/grabado.
2. **Given** un equipo que ya tiene QR, **When** se solicita generarlo de nuevo, **Then** el
   sistema reutiliza el QR existente (no genera un identificador nuevo) salvo una acción
   explícita de reemisión autorizada y auditada.
3. **Given** el contenido de cualquier QR generado, **When** se inspecciona sin estar
   autenticado, **Then** no revela número de serie, nombre, adscripción ni dato alguno del
   elemento (Principio II de la constitución).

---

### User Story 2 - Verificar la autenticidad de un QR (Priority: P2)

Cuando un QR se presenta al sistema (escaneado por cámara o lector manual en una feature
posterior), el sistema **verifica su firma** antes de resolver cualquier información, de modo
que un QR inventado o alterado se rechaza.

**Why this priority**: La firma es lo que hace al QR confiable; sin verificación, el opaco-id
sería falsificable. Es necesaria, pero la UX de escaneo se aborda en otra feature.

**Independent Test**: Tomar un QR válido y verificar que el sistema lo acepta; alterar un
carácter del contenido y verificar que el sistema lo rechaza por firma inválida.

**Acceptance Scenarios**:

1. **Given** un QR emitido por el sistema, **When** se valida su contenido, **Then** la firma
   verifica correctamente y el identificador se reconoce como auténtico.
2. **Given** un QR con el contenido alterado o una firma incorrecta, **When** se valida,
   **Then** el sistema lo rechaza sin revelar información.
3. **Given** un QR cuyo identificador no corresponde a ningún equipo, **When** se valida,
   **Then** el sistema responde "no encontrado" sin filtrar detalles internos.

---

### User Story 3 - Exportar QR para grabado/impresión (Priority: P3)

Un supervisor exporta el/los QR en un formato listo para enviar a grabado láser o impresión
de etiquetas, individualmente o por lote.

**Why this priority**: Cierra el ciclo físico (llevar el QR al equipo). Depende de P1.

**Independent Test**: Exportar el QR de un equipo y verificar que el archivo resultante es
escaneable y corresponde al identificador opaco de ese equipo.

**Acceptance Scenarios**:

1. **Given** equipos con QR generado, **When** el usuario exporta un lote, **Then** obtiene un
   archivo por equipo (o un consolidado) escaneable y correctamente asociado.
2. **Given** un QR exportado, **When** se escanea con un lector estándar, **Then** entrega
   exactamente el contenido opaco firmado, sin pérdida.

### Edge Cases

- ¿Qué pasa si se intenta generar un QR para un equipo inexistente o dado de baja? Debe
  rechazarse.
- ¿Qué pasa si se rota la llave de firma después de haber grabado QR? Los QR ya grabados deben
  seguir verificando (estrategia de versionado/rotación de llaves — ver Assumptions).
- ¿Qué pasa si dos solicitudes concurrentes intentan generar el QR del mismo equipo? Debe
  resultar en un único identificador opaco, no dos.
- ¿Qué densidad/tamaño mínimo de QR resiste el grabado sobre tela/placa sin perder lectura?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST generar, para un equipo registrado, un **identificador opaco
  único** (sin significado ni datos derivables) y ligarlo a ese equipo.
- **FR-002**: El sistema MUST firmar el contenido del QR de forma que pueda verificarse su
  autenticidad e integridad posteriormente.
- **FR-003**: El contenido del QR MUST NOT incluir número de serie, nombre, adscripción ni
  ningún dato personal o explotable del elemento o del equipo (Principio II).
- **FR-004**: El sistema MUST verificar la firma de un QR presentado **antes** de resolver o
  devolver cualquier información asociada.
- **FR-005**: Ante un QR con firma inválida, alterado o desconocido, el sistema MUST rechazarlo
  sin filtrar detalles internos ni datos.
- **FR-006**: El sistema MUST garantizar que cada equipo tenga a lo sumo **un** identificador
  opaco vigente; la reemisión MUST ser una acción explícita, autorizada y auditada.
- **FR-007**: El sistema MUST entregar el QR en un formato apto para impresión y grabado
  (imagen escaneable por cámara y por lector manual estándar).
- **FR-008**: La generación, reemisión y exportación de QR MUST requerir autorización por rol
  y quedar registradas en auditoría (Principios IV y V).
- **FR-009**: El esquema de firma MUST soportar **rotación de llaves** de modo que los QR ya
  grabados sigan verificando tras rotar (identificación de versión de llave).
- **FR-010**: La resolución `QR → equipo/asignación` MUST ocurrir solo en el servidor y solo
  tras autenticación + autorización (la UX de escaneo se especifica en una feature posterior).

### Key Entities *(include if feature involves data)*

- **Identificador opaco del QR**: valor único sin significado, ligado 1:1 a un equipo. Es lo
  único (junto con la firma) que viaja en el QR.
- **Firma del QR**: prueba criptográfica de que el sistema emitió el identificador; incluye
  referencia a la versión de llave usada (para rotación).
- **Equipo**: definido en la feature **001-inventario-equipos**; el QR cuelga de su
  identificador interno. Ver [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los QR generados son únicos por equipo (cero colisiones).
- **SC-002**: El 100% del contenido de los QR está libre de datos personales o explotables,
  verificable inspeccionando el contenido crudo.
- **SC-003**: Un QR alterado en un solo carácter es rechazado el 100% de las veces.
- **SC-004**: Tras una rotación de llave, el 100% de los QR previamente grabados siguen
  verificando correctamente.
- **SC-005**: Un usuario autorizado puede generar y exportar el QR de un equipo en menos de 1
  minuto.
- **SC-006**: Un QR grabado/impreso con el tamaño definido se lee correctamente por cámara y
  por lector manual en el primer intento en condiciones normales.

## Assumptions

- **Formato del QR (bloqueante, a fijar por ADR antes de grabar en serie):** propuesta por
  defecto = identificador opaco **UUID v4** + **firma HMAC-SHA256** con llave desde gestor de
  secretos, codificados de forma compacta (p. ej. `base64url(uuid).base64url(firma).version`).
  Esta es la decisión que el cliente necesita confirmar para empezar a grabar sin retrabajo.
- El equipo debe existir en el inventario (feature 001) antes de generar su QR.
- La lectura del QR por cámara (Capacitor) y por escáner manual (HID), y la pantalla de ficha
  al escanear, se especifican en una **feature de escaneo** aparte.
- La llave de firma nunca se versiona en el repositorio (Principio III) y se rota
  periódicamente (Principio I / `docs/02-seguridad.md`).
- El tamaño/densidad físico del QR para grabado se validará con una prueba de lectura real.

## Clarifications

### Question 1: Estrategia del contenido del QR — RESUELTA

**Context**: FR-001/FR-002/FR-003 y la nota de prioridad: el QR se graba de forma permanente.

**Decisión**: **Opción A — UUID opaco (v4) + firma HMAC-SHA256 con versión de llave**, payload
`v<version>.<base64url(uuid)>.<base64url(hmac)>`. Resolución solo en servidor. Registrada en
[`docs/04-decisiones/0002-formato-del-qr.md`](../../docs/04-decisiones/0002-formato-del-qr.md).
La opción B (token autocontenido) se descartó por exponer más superficie y contradecir la
minimización (Principio II).

## Dependencies

- Constitución: [`.specify/memory/constitution.md`](../../.specify/memory/constitution.md)
  (Principio II es el eje de esta feature; III, IV, V aplican).
- Seguridad: [`docs/02-seguridad.md`](../../docs/02-seguridad.md) §2 (Principio rector del QR).
- Arquitectura — flujo del QR: [`docs/01-arquitectura.md`](../../docs/01-arquitectura.md).
- Feature **001-inventario-equipos** (el QR cuelga de un equipo existente).
