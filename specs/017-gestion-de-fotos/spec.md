# Feature Specification: Captura y almacenamiento seguro de fotos

**Feature Branch**: `017-gestion-de-fotos`

**Created**: 2026-07-01

**Status**: Draft

**Input**: User description: "Captura y almacenamiento de fotos para elementos, tipos de prenda y fornituras/equipos. Reemplazar el campo de URL de foto por tomar foto con la cámara o subir un archivo, con vista previa. Gestor de archivos propio que valide, cifre en reposo y sirva la imagen solo a usuarios autenticados y autorizados, con auditoría. La foto del elemento es PII de alta sensibilidad (personal policial en México)."

## User Scenarios & Testing *(mandatory)*

<!--
  Las historias están priorizadas. Cada una es un slice de valor testeable de forma
  independiente. Referencias de seguridad: constitución (principios I, IV, V) y
  docs/02-seguridad.md.
-->

### User Story 1 - Tomar o subir la foto de un equipo/tipo (Priority: P1)

Como responsable de inventario, al dar de alta o editar un **equipo** (fornitura) o un
**tipo de prenda**, quiero **tomar una foto con la cámara** del dispositivo o **elegir un
archivo de imagen** desde el equipo, y ver una **vista previa** antes de guardar, en lugar de
tener que pegar una URL de una imagen ya publicada en internet.

**Why this priority**: Es el flujo más frecuente y de menor sensibilidad (la foto de un
chaleco o de un tipo de prenda **no es dato personal**). Entrega el valor central —capturar la
imagen sin depender de un hosting externo— y puede liberarse sin resolver la base legal de la
PII, por lo que es el MVP.

**Independent Test**: Dar de alta un equipo tomando una foto (o subiendo un archivo),
guardar, reabrir la ficha y comprobar que la imagen se muestra. No requiere tocar el padrón de
elementos.

**Acceptance Scenarios**:

1. **Given** el formulario de alta de un equipo, **When** el usuario elige "Tomar foto" y
   captura una imagen, **Then** ve la vista previa y, al guardar, la ficha queda con esa foto
   asociada.
2. **Given** el formulario de alta de un equipo, **When** el usuario elige "Subir archivo" y
   selecciona una imagen `JPEG`/`PNG`/`WEBP` válida, **Then** ve la vista previa y puede
   guardar.
3. **Given** un equipo con foto ya guardada, **When** el usuario abre su ficha, **Then** la
   foto se muestra (solo tras estar autenticado).
4. **Given** una foto en vista previa, **When** el usuario la reemplaza por otra o la quita
   antes de guardar, **Then** se respeta la última elección al guardar.
5. **Given** un dispositivo sin cámara o un navegador de escritorio, **When** el usuario abre
   el selector de foto, **Then** puede al menos **subir un archivo** (la captura por cámara se
   ofrece solo si está disponible).

---

### User Story 2 - Foto del elemento policial con protección reforzada de PII (Priority: P1)

Como usuario autorizado del padrón, quiero **capturar/subir la foto de un elemento policial**
con los mismos medios (cámara o archivo), pero entendiendo que esa foto es **dato personal de
alta sensibilidad**: solo debe poder verla quien tenga rol autorizado, cada visualización,
subida y exportación debe quedar **auditada**, y por defecto la foto aparece **enmascarada**
para quien no está autorizado.

**Why this priority**: Es núcleo del padrón, pero su exposición mal controlada puede **poner
vidas en riesgo** (constitución, principio I). Tiene la misma prioridad de captura que US1,
pero su **habilitación en producción está condicionada** a que exista base legal y finalidad
documentada (ADR 0003); mientras tanto la capacidad se construye pero permanece restringida.

**Independent Test**: Con un rol autorizado, subir la foto de un elemento y verla en la ficha;
con un rol no autorizado, confirmar que la foto aparece enmascarada/oculta; verificar que
ambos accesos quedaron registrados en auditoría.

**Acceptance Scenarios**:

1. **Given** un usuario con rol autorizado, **When** captura o sube la foto de un elemento y
   guarda, **Then** la foto queda asociada al elemento y el evento de subida queda auditado.
2. **Given** un usuario con rol autorizado, **When** abre la ficha de un elemento con foto,
   **Then** ve la foto y el acceso queda registrado (quién, qué elemento, cuándo).
3. **Given** un usuario **sin** rol autorizado para ver PII, **When** abre la ficha de un
   elemento, **Then** la foto aparece **enmascarada/oculta** y no puede descargarla.
4. **Given** la foto de un elemento, **When** cualquier usuario la solicita, **Then** el acceso
   exige sesión válida y autorización; una petición sin autenticación es rechazada.
5. **Given** que la base legal para capturar foto de elementos **no** está confirmada (ADR
   0003), **When** se despliega la feature, **Then** la captura de foto de elemento permanece
   **deshabilitada/restringida** hasta que la base legal se confirme, sin bloquear US1.

---

### User Story 3 - Reemplazo del campo "URL de foto" y datos existentes (Priority: P2)

Como usuario, ya no quiero ver un campo donde pegar una URL de foto; quiero el nuevo selector
(cámara/archivo con vista previa) en los tres formularios (elemento, tipo, equipo). Las fichas
que hoy pudieran tener una URL externa guardada deben seguir mostrándose sin errores durante la
transición.

**Why this priority**: Mejora de usabilidad y consistencia que depende de US1/US2; no es
bloqueante para el valor central, pero cierra la experiencia y elimina la práctica insegura de
apuntar a imágenes externas arbitrarias.

**Independent Test**: Abrir los tres formularios y confirmar que ninguno pide una URL de texto;
abrir una ficha que tuviera una URL previa y confirmar que no rompe la pantalla.

**Acceptance Scenarios**:

1. **Given** cualquiera de los tres formularios (elemento, tipo, equipo), **When** el usuario
   lo abre, **Then** ve el selector de foto (cámara/archivo con vista previa) y **no** un campo
   de texto para URL.
2. **Given** una ficha con una URL de foto capturada antes de esta feature, **When** el usuario
   la abre, **Then** la pantalla se muestra sin error (la imagen previa se muestra si es
   accesible, o se indica que no hay foto).

---

### Edge Cases

- **Archivo no-imagen renombrado** (p. ej. un `.exe` renombrado a `.jpg`): debe rechazarse por
  contenido, no solo por extensión.
- **Imagen vectorial `SVG`**: rechazada por riesgo de contenido activo (XSS).
- **Imagen con metadatos EXIF de ubicación (GPS)**: los metadatos deben eliminarse antes de
  almacenar, para no filtrar dónde se tomó la foto.
- **Imagen demasiado grande** (peso o dimensiones fuera de límite): rechazada con mensaje claro.
- **Pérdida de sesión durante la subida** (token expirado): la subida falla de forma controlada
  y el usuario puede reintentar sin perder el resto del formulario.
- **Foto huérfana**: una imagen subida cuyo alta/edición no llega a completarse no debe quedar
  accesible ni acumularse indefinidamente.
- **Permisos de cámara denegados** por el usuario: se ofrece la alternativa de subir archivo.
- **Baja/depuración de un elemento o equipo**: su foto debe poder eliminarse conforme a la
  política de retención (derechos ARCO para PII).
- **El QR no cambia**: sigue sin contener datos personales ni referencia explotable a la foto
  (constitución, principio II).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir asociar una foto a un **equipo**, a un **tipo de prenda**
  y a un **elemento**, tanto en el alta como en la edición.
- **FR-002**: El usuario DEBE poder proporcionar la foto de dos formas: **capturándola con la
  cámara** del dispositivo o **subiendo un archivo de imagen** desde el equipo/galería.
- **FR-003**: El sistema DEBE mostrar una **vista previa** de la foto seleccionada antes de
  guardar, y permitir **reemplazarla o quitarla**.
- **FR-004**: Cuando la cámara no esté disponible (dispositivo/entorno sin cámara o permiso
  denegado), el sistema DEBE permitir al menos **subir un archivo**.
- **FR-005**: El sistema DEBE **validar** que el archivo es una imagen real y de un tipo
  permitido (`JPEG`, `PNG`, `WEBP`), **rechazando** otros contenidos —incluido `SVG`— aunque la
  extensión sugiera lo contrario.
- **FR-006**: El sistema DEBE **rechazar** imágenes que excedan un **límite de peso** y de
  **dimensiones** configurables, con un mensaje comprensible.
- **FR-007**: El sistema DEBE **eliminar los metadatos** de la imagen (incluida la ubicación
  GPS de EXIF) antes de almacenarla.
- **FR-008**: El sistema DEBE **almacenar las fotos cifradas en reposo**; ninguna foto queda en
  claro en el almacenamiento (constitución, principio I; `docs/02-seguridad.md`).
- **FR-009**: El sistema DEBE servir cada foto **únicamente a usuarios autenticados y
  autorizados**; ninguna foto es accesible de forma anónima ni por URL adivinable.
- **FR-010**: Para la foto de **elemento** (PII), el sistema DEBE aplicar **control de acceso
  por rol** con **enmascaramiento por defecto**: quien no esté autorizado no ve la foto.
- **FR-011**: El sistema DEBE **registrar en auditoría** cada **subida, visualización y
  exportación** de la foto de un elemento (quién, qué elemento, cuándo), sin escribir PII en los
  logs de aplicación (constitución, principio V).
- **FR-012**: El sistema NO DEBE incluir datos personales ni la foto en el **QR**; el QR sigue
  conteniendo solo un identificador opaco firmado (constitución, principio II).
- **FR-013**: El sistema DEBE **reemplazar** el campo de "URL de foto" en los tres formularios
  por el nuevo selector, y DEBE **seguir mostrando sin error** las fichas que tuvieran una URL
  capturada antes de esta feature.
- **FR-014**: El sistema DEBE permitir **eliminar** la foto asociada a un equipo, tipo o
  elemento, y aplicar la **política de retención/baja** correspondiente (derechos ARCO para la
  foto de elemento).
- **FR-015**: La **captura de foto de elemento** DEBE poder mantenerse **deshabilitada o
  restringida** mientras la base legal y la finalidad no estén confirmadas por ADR 0003, sin
  impedir el uso de la foto para equipos y tipos.
- **FR-016**: El sistema DEBE evitar la acumulación de **imágenes huérfanas** (subidas que no se
  asocian a ninguna ficha).

### Key Entities *(include if feature involves data)*

- **Foto (activo de imagen)**: representa una imagen almacenada de forma segura. Atributos
  conceptuales: tipo de imagen, tamaño, huella/identificador único opaco, indicador de si es
  **PII**, quién la subió y cuándo. No contiene datos personales en sí misma más allá del propio
  contenido visual.
- **Equipo**: fornitura/blindaje que puede tener **una** foto asociada (no PII).
- **Tipo de prenda**: catálogo que puede tener **una** foto ilustrativa (no PII).
- **Elemento**: persona del padrón policial que puede tener **una** foto asociada, tratada como
  **dato personal de alta sensibilidad**.
- **Registro de auditoría**: evidencia de acceso/subida/exportación de fotos de elementos.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un usuario puede asociar una foto a un equipo (capturando o subiendo) y verla en
  la ficha en **menos de 1 minuto**, sin salir de la aplicación ni usar servicios externos.
- **SC-002**: El **100%** de los archivos que no son imágenes permitidas (incluido `SVG` y
  archivos renombrados) son **rechazados** antes de almacenarse.
- **SC-003**: El **100%** de las fotos almacenadas quedan **cifradas en reposo** y **sin
  metadatos de ubicación** (EXIF/GPS) verificables.
- **SC-004**: **Ninguna** foto es accesible sin autenticación y autorización; el **100%** de los
  accesos a fotos de elementos quedan registrados en auditoría.
- **SC-005**: Un usuario sin rol autorizado **nunca** ve la foto de un elemento (enmascarada u
  oculta en el **100%** de los casos).
- **SC-006**: Tras la feature, **ninguno** de los tres formularios solicita una URL de imagen de
  texto, y las fichas con datos previos siguen abriéndose sin error.

## Assumptions

- La foto de **equipo** y de **tipo de prenda** **no** es dato personal; recibe controles de
  acceso estándar (autenticación) pero no el régimen reforzado de PII.
- La foto de **elemento** es PII de alta sensibilidad y su **habilitación** depende de la base
  legal/finalidad que resuelva **ADR 0003**; hasta entonces la capacidad se construye pero
  permanece restringida (FR-015).
- Cada equipo, tipo y elemento tiene **como máximo una** foto (no galería múltiple) en esta
  versión.
- Se reutiliza la **autenticación y los roles existentes** (`ADMIN`, `SUPERVISOR`, `OPERADOR`)
  y el módulo de **auditoría** ya presente en el sistema.
- El almacenamiento seguro de las fotos vive en el **servidor** (no en un servicio externo de
  terceros en esta versión); la decisión concreta de almacenamiento se registra en un ADR.
- Formatos de imagen soportados de entrada: `JPEG`, `PNG`, `WEBP`. Otros formatos quedan fuera
  de alcance de esta versión.
- La captura por cámara en móvil usa las capacidades nativas del dispositivo; en navegador de
  escritorio el flujo principal es subir archivo (con captura por cámara web si está disponible).

## Dependencies

- **Constitución** del proyecto: principios I (seguridad/privacidad), II (QR sin PII), IV
  (mínimo privilegio y autorización), V (auditoría sin fugas).
- **[ADR 0003](../../docs/04-decisiones/0003-pii-elementos.md)** — alcance de PII de elementos;
  define que la foto va en storage cifrado con acceso autorizado y condiciona su captura a base
  legal.
- **[ADR 0006](../../docs/04-decisiones/0006-cifrado-pii-nivel-aplicacion.md)** — cifrado de PII
  a nivel de aplicación; la foto quedaba pendiente hasta resolver su almacenamiento cifrado.
- **ADR nuevo (pendiente)** — decisión de **almacenamiento de fotos** (dónde y cómo se guardan y
  sirven de forma cifrada y auditada). Se redacta junto con esta feature.
- **`docs/02-seguridad.md`** — checklist de seguridad obligatorio (§8) para cambios que tocan
  datos, autenticación o almacenamiento.
- Padrón de **elementos** (spec 003), inventario de **equipos** (spec 001) y catálogo de
  **tipos de prenda** (specs 006/015) — entidades que reciben la foto.
