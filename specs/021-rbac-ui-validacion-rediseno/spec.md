# Feature Specification: Visibilidad coherente por rol, validación visible y rediseño de Login/Asignación

**Feature Branch**: `021-rbac-ui-validacion-rediseno`

**Created**: 2026-07-01

**Status**: Draft

**Input**: User description: "Corrección de visibilidad por rol (RBAC) en la UI, rol garantizado del admin sembrado, validación visible en todos los formularios, y rediseño de las pantallas de Login y Asignación."

## Contexto del problema

El administrador del sistema (la cuenta administradora inicial) inicia sesión y **no ve los
botones de agregar** (por ejemplo en Almacenes) **ni todos los módulos del menú**. La
investigación demostró que nada fue eliminado: la interfaz oculta acciones y módulos según el
rol del usuario, pero:

1. El proceso que garantiza la existencia de la cuenta administradora inicial **no garantiza
   su rol**: si la cuenta ya existía con un rol menor, se queda así indefinidamente y nadie
   puede notarlo porque **la interfaz nunca muestra el rol del usuario conectado**.
2. La interfaz quedó **desalineada de la matriz de permisos oficial** (ADR 0013) cuando el
   sistema pasó de 2 a 5 roles: las pantallas siguen decidiendo visibilidad con las reglas
   antiguas, por lo que los roles Supervisor, Almacén y Auditor no ven acciones y módulos que
   el servidor sí les permite (p. ej. el Auditor no ve la Bitácora de Auditoría en el menú).

Adicionalmente: ningún formulario operativo muestra mensajes de error de validación (el
usuario no sabe qué campo obligatorio le falta), la pantalla de inicio de sesión carece de la
identidad institucional del sistema, la pantalla de Asignación presenta secciones visualmente
encimadas y confusas, y se detectó que **credenciales reales están versionadas en el
repositorio**, en contra del principio III de la constitución.

## Clarifications

### Session 2026-07-01

- Q: ¿Qué se usa como emblema institucional en el login, dado que el proyecto no tiene
  ningún logo en sus assets? → A: Un escudo vectorial provisional (dorado/blanco, coherente
  con la paleta institucional), fácilmente reemplazable cuando la corporación entregue el
  emblema oficial.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El administrador recupera el control total (Priority: P1)

Como administrador del sistema, al iniciar sesión con la cuenta administradora inicial quiero
ver todos los módulos del menú y todos los botones de acción (agregar, editar, deshabilitar),
para poder operar y configurar el sistema completo.

**Why this priority**: es el bloqueo operativo reportado — hoy el administrador no puede dar
de alta almacenes ni acceder a módulos administrativos; sin esto el sistema es inoperable
para su responsable.

**Independent Test**: iniciar sesión con la cuenta administradora inicial y verificar que el
menú muestra los 13 módulos y que cada pantalla de listado muestra su botón de agregar.

**Acceptance Scenarios**:

1. **Given** la cuenta administradora inicial existe con un rol distinto de Administrador (o
   deshabilitada) por datos históricos, **When** el sistema arranca, **Then** la cuenta queda
   corregida automáticamente a rol Administrador y habilitada, y la corrección queda
   registrada en el registro de eventos del sistema (sin exponer datos sensibles).
2. **Given** el administrador inicia sesión, **When** se muestra el menú lateral, **Then**
   aparecen los 13 módulos, incluidos Bitácora de Auditoría, Usuarios y Configurar landing.
3. **Given** el administrador navega a Almacenes o Catálogo de Tipos, **When** carga el
   listado, **Then** el botón de agregar es visible y funcional.
4. **Given** cualquier usuario autenticado, **When** abre el menú lateral, **Then** ve su
   nombre y **su rol** claramente identificado.

---

### User Story 2 - Cada rol ve exactamente lo que puede hacer (Priority: P1)

Como usuario con rol Supervisor, Almacén, Auditor o Capturista, quiero que el menú y los
botones de acción reflejen exactamente lo que el servidor me permite hacer, para no
encontrarme con acciones ocultas que sí tengo permitidas ni con botones que al usarlos el
servidor rechaza.

**Why this priority**: la desalineación actual deja a roles completos sin poder trabajar
(p. ej. Almacén no ve el botón para agregar fornituras aunque el servidor se lo permite) y es
la otra mitad de la causa raíz del reporte.

**Independent Test**: iniciar sesión con un usuario de cada rol y comparar, módulo por
módulo, la visibilidad de menú y acciones contra la matriz oficial de permisos (ADR 0013).

**Acceptance Scenarios**:

1. **Given** un usuario con rol Almacén, **When** navega por el sistema, **Then** puede
   agregar/editar fornituras y registrar traslados, pero NO ve acciones de alta/edición en
   Almacenes, Tipos, Usuarios ni Configurar landing, ni la pantalla de asignación en modo
   escritura.
2. **Given** un usuario con rol Auditor, **When** abre el menú, **Then** ve la Bitácora de
   Auditoría (además de los módulos de consulta) y NO ve ningún botón de escritura en ninguna
   pantalla.
3. **Given** un usuario con rol Supervisor, **When** navega por el sistema, **Then** puede
   operar asignaciones, traslados, bajas, incidencias y elementos, pero no gestiona usuarios
   ni configuración.
4. **Given** un usuario con rol Capturista, **When** navega por el sistema, **Then** conserva
   exactamente las capacidades que la matriz le otorga (captura de inventario, elementos,
   asignaciones, incidencias, traslados) sin pérdidas respecto a hoy.
5. **Given** cualquier rol, **When** se compara cada acción visible contra la matriz oficial,
   **Then** no existe ninguna acción visible que el servidor rechace ni ninguna acción
   permitida por el servidor que la interfaz oculte.

---

### User Story 3 - Cero credenciales en el repositorio (Priority: P1)

Como responsable de seguridad del proyecto, quiero que ninguna credencial real (contraseñas
de base de datos o de cuentas) viva en archivos versionados, para cumplir el principio III de
la constitución y reducir el riesgo de compromiso de datos de personal policial.

**Why this priority**: hallazgo de seguridad activo — hay credenciales reales versionadas hoy;
la constitución lo marca como no negociable y el dato protegido es PII de alta sensibilidad.

**Independent Test**: revisar el árbol versionado del repositorio y confirmar que no contiene
valores de credenciales; el entorno de desarrollo arranca tomando las credenciales de una
fuente local no versionada documentada solo por nombres de variables.

**Acceptance Scenarios**:

1. **Given** el repositorio tras el cambio, **When** se inspeccionan los archivos versionados,
   **Then** no existe ninguna contraseña, cadena de conexión con credenciales ni secreto con
   valor real; solo se documentan los **nombres** de las variables requeridas.
2. **Given** un desarrollador con su configuración local no versionada, **When** levanta el
   entorno de desarrollo, **Then** el sistema arranca normalmente.
3. **Given** las credenciales que ya quedaron expuestas en el historial, **When** se cierra
   esta feature, **Then** queda documentada la instrucción de rotarlas (la rotación en sí es
   una operación del responsable del entorno, fuera del alcance).

---

### User Story 4 - Sé qué me falta al llenar un formulario (Priority: P2)

Como usuario operativo, cuando intento guardar un formulario con datos faltantes o inválidos,
quiero ver mensajes claros en español debajo de cada campo con problema, para corregirlo sin
adivinar.

**Why this priority**: hoy el usuario oprime guardar y no pasa nada visible (o solo se
deshabilita el botón), lo que genera confusión y captura incompleta; afecta a todos los
módulos de captura diariamente.

**Independent Test**: en cada formulario del sistema, intentar guardar vacío y verificar que
cada campo obligatorio muestra su mensaje; llenar un campo con formato inválido (correo,
CURP, RFC) y verificar el mensaje específico.

**Acceptance Scenarios**:

1. **Given** cualquier formulario de alta/edición (fornitura, lote, elemento, almacén, tipo,
   usuario, traslado, baja, incidencia), **When** el usuario intenta guardar con campos
   obligatorios vacíos, **Then** cada campo con problema muestra un mensaje en español junto
   al campo y el formulario no se envía.
2. **Given** un campo con formato específico (correo, CURP de 18 caracteres, RFC de 12-13),
   **When** el usuario captura un valor con formato inválido y sale del campo, **Then** ve un
   mensaje que describe el formato esperado.
3. **Given** el formulario de incidencia (que hoy no valida), **When** el usuario intenta
   guardar sin fornitura resuelta, tipo o descripción, **Then** recibe la misma
   retroalimentación visual que el resto de los formularios.
4. **Given** los campos obligatorios de cada entidad, **When** se comparan con las reglas del
   servidor, **Then** coinciden (lo que el servidor rechaza como faltante, la interfaz lo
   marca como obligatorio).

---

### User Story 5 - Entiendo la pantalla de Asignación (Priority: P2)

Como capturista o supervisor, quiero que la pantalla de Asignación presente sus pasos como
secciones visualmente separadas y sin elementos encimados, para completar una asignación sin
confusión.

**Why this priority**: la pantalla es funcional pero confusa (elementos visualmente
encimados, pasos mezclados con el listado); es una operación central del negocio.

**Independent Test**: realizar una asignación completa (escanear/capturar código → buscar
elemento → confirmar) verificando que cada paso es una sección visual distinguible y que el
listado de asignaciones vigentes se distingue claramente del asistente.

**Acceptance Scenarios**:

1. **Given** un usuario con permiso de asignar, **When** abre la pantalla, **Then** distingue
   visualmente: Paso 1 (identificar fornitura), Paso 2 (identificar elemento), acciones de
   confirmación, y la sección separada de "Asignaciones vigentes", sin solapes visuales.
2. **Given** el flujo de asignación, **When** el usuario escanea o captura un código, **Then**
   conserva la retroalimentación existente (mensaje de resultado con estados de éxito/aviso/
   error, indicador de búsqueda y selección de cámara) sin regresiones.
3. **Given** un usuario sin permiso de asignar, **When** abre la pantalla, **Then** ve solo el
   listado de asignaciones vigentes en modo consulta.
4. **Given** una asignación completada o el botón limpiar, **When** termina la operación,
   **Then** el asistente regresa a su estado inicial y el listado de vigentes se actualiza.

---

### User Story 6 - El login refleja la identidad institucional (Priority: P3)

Como usuario del sistema (mayoritariamente desde teléfono móvil), quiero una pantalla de
inicio de sesión con la identidad institucional completa del sistema, para confiar en que
estoy en la aplicación oficial.

**Why this priority**: mejora de percepción y confianza; no bloquea operación, pero es la
cara del sistema y hoy es "demasiado simple" (solo el acrónimo, sin identidad).

**Independent Test**: abrir la pantalla de inicio de sesión en pantalla ancha (dos paneles) y
en un teléfono (~390px de ancho, panel colapsado a cabecera) y verificar identidad completa,
legibilidad y funcionamiento del formulario.

**Acceptance Scenarios**:

1. **Given** la pantalla de inicio de sesión en pantalla ancha, **When** se muestra, **Then**
   presenta un panel institucional (color guinda, emblema, nombre completo "SIGEFOR — Sistema
   Integral de Gestión de Fornituras", acento dorado) junto al formulario.
2. **Given** la pantalla en un teléfono, **When** se muestra, **Then** el panel institucional
   colapsa a una cabecera compacta y el formulario permanece completo y usable sin
   desplazamiento excesivo.
3. **Given** las pantallas de recuperación y restablecimiento de contraseña, **When** se
   muestran tras el cambio, **Then** conservan un estilo coherente y siguen funcionando (sin
   regresiones visuales ni funcionales).
4. **Given** credenciales inválidas o campos vacíos, **When** el usuario intenta entrar,
   **Then** conserva los mensajes de error actuales por campo y el aviso de error general.

---

### Edge Cases

- Usuario cuyo rol almacenado no coincide con ningún rol reconocido (dato corrupto o sesión
  de una versión anterior): la interfaz lo trata como sin permisos de escritura y sin módulos
  restringidos (rechazo por defecto), sin romper la navegación.
- Sesión guardada de una versión anterior de la aplicación (formato distinto): el usuario
  puede cerrar sesión y volver a entrar sin quedar en estado inconsistente.
- La cuenta administradora inicial no existe en el entorno (siembra deshabilitada): el
  arranque no falla y no se crea ni modifica nada.
- Cambio de rol de un usuario mientras tiene sesión activa: al restablecer o renovar la
  sesión, la visibilidad refleja el rol vigente.
- Formulario con campo opcional que tiene formato (CURP/RFC/correo): vacío es válido; con
  valor, se valida el formato.
- Búsqueda de elemento en Asignación sin resultados: se comunica "sin resultados" en lugar de
  quedar en silencio.
- Fornitura escaneada no disponible (ya asignada o de baja): el Paso 2 no se habilita y el
  motivo es visible (comportamiento actual que se conserva).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE garantizar en cada arranque que la cuenta administradora
  inicial configurada exista con rol Administrador y habilitada; si existe con otro rol o
  deshabilitada, DEBE corregirla y registrar el evento (sin exponer credenciales ni PII).
- **FR-002**: La visibilidad de módulos del menú y de acciones de escritura en cada pantalla
  DEBE derivarse de una **única definición de permisos por capacidad** alineada con la matriz
  oficial (ADR 0013), eliminando reglas dispersas por pantalla.
- **FR-003**: La matriz aplicada por la interfaz DEBE ser: escritura de inventario
  (Administrador, Almacén, Capturista); traslados (Administrador, Supervisor, Almacén,
  Capturista); asignaciones e incidencias (Administrador, Supervisor, Capturista); autorizar
  bajas (Administrador, Supervisor); elementos (Administrador, Supervisor, Capturista);
  almacenes, tipos, usuarios y landing (solo Administrador); bitácora de auditoría
  (Administrador y Auditor).
- **FR-004**: El módulo "Bitácora de Auditoría" DEBE aparecer en el menú para el rol Auditor
  (además del Administrador).
- **FR-005**: El menú lateral DEBE mostrar el rol del usuario conectado junto a su identidad.
- **FR-006**: La visibilidad por rol DEBE reaccionar al estado vigente de la sesión (no
  quedar congelada al momento de abrir una pantalla).
- **FR-007**: Un rol no reconocido o ausente DEBE tratarse como sin permisos de escritura y
  sin acceso a módulos restringidos (rechazo por defecto).
- **FR-008**: Todos los formularios de alta/edición DEBEN mostrar mensajes de validación en
  español junto a cada campo con problema al intentar guardar o al abandonar el campo, con
  una presentación **uniforme** en todo el sistema.
- **FR-009**: Los campos marcados como obligatorios en la interfaz DEBEN coincidir con los
  que el servidor exige por entidad: fornitura (código, tipo, almacén), elemento (nombre,
  apellido paterno, placa, sexo), usuario (nombre, correo, rol), almacén (código, nombre,
  tipo), traslado (origen y destino), baja (motivo), asignación (fornitura y elemento),
  incidencia (fornitura, tipo, descripción).
- **FR-010**: Los campos con formato específico DEBEN validar y comunicar el formato: correo
  válido, CURP de 18 caracteres alfanuméricos, RFC de 12-13 caracteres (ambos opcionales pero
  validados si se capturan).
- **FR-011**: El formulario de incidencia DEBE incorporarse al mismo esquema de validación
  que el resto de los formularios.
- **FR-012**: La pantalla de inicio de sesión DEBE presentar la identidad institucional
  completa (emblema de escudo provisional reemplazable, "SIGEFOR — Sistema Integral de
  Gestión de Fornituras", paleta institucional guinda/dorado) en un diseño de dos paneles en pantallas anchas que colapsa a
  cabecera compacta en teléfonos, sin degradar la validación ni los mensajes de error
  existentes, y sin romper las pantallas de recuperación/restablecimiento de contraseña.
- **FR-013**: La pantalla de Asignación DEBE presentar el asistente en secciones visualmente
  separadas (identificar fornitura, identificar elemento, confirmar) y el listado de
  asignaciones vigentes como sección claramente distinguible, sin elementos encimados,
  conservando la retroalimentación de escaneo existente (banner de resultado, indicador de
  búsqueda, selección de cámara).
- **FR-014**: El repositorio NO DEBE contener credenciales con valor real en ningún archivo
  versionado; la configuración local de desarrollo DEBE tomarse de una fuente no versionada y
  el repositorio DEBE documentar únicamente los nombres de las variables requeridas.
- **FR-015**: DEBE quedar documentada la recomendación de rotar las credenciales ya expuestas
  en el historial del repositorio (base de datos remota y cuenta administradora sembrada).

### Key Entities

- **Rol**: nivel de acceso de un usuario (Administrador, Supervisor, Almacén, Auditor,
  Capturista); determina módulos visibles y acciones disponibles.
- **Matriz de permisos**: relación oficial capacidad → roles autorizados (ADR 0013); fuente
  única de verdad para servidor e interfaz; en esta feature NO se modifica, solo se refleja.
- **Cuenta administradora inicial**: usuario sembrado al arranque a partir de configuración;
  debe garantizarse su rol y habilitación.
- **Formulario de captura**: cualquier pantalla de alta/edición; tiene campos obligatorios y
  campos con formato que deben validarse de manera uniforme.
- **Asignación**: vínculo vigente entre una fornitura disponible y un elemento policial
  activo; se crea vía el asistente de la pantalla de Asignación.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con la cuenta administradora inicial, el 100% de los módulos (13) y el 100% de
  los botones de agregar/editar son visibles y funcionales tras iniciar sesión.
- **SC-002**: Para cada uno de los 5 roles, la visibilidad de acciones y módulos coincide al
  100% con la matriz oficial (auditable módulo por módulo); cero acciones visibles rechazadas
  por el servidor y cero acciones permitidas ocultas.
- **SC-003**: En el 100% de los formularios de alta/edición, guardar con campos obligatorios
  vacíos produce mensajes visibles por campo; un usuario puede identificar qué le falta sin
  ayuda externa en menos de 5 segundos.
- **SC-004**: El repositorio versionado contiene cero credenciales con valor real
  (verificable por inspección).
- **SC-005**: Una asignación completa (identificar fornitura → elegir elemento → confirmar)
  se realiza sin asistencia y sin confusión de secciones; en la revisión visual no existe
  ningún elemento encimado en la pantalla en anchos de 360px a 1280px.
- **SC-006**: La pantalla de inicio de sesión muestra la identidad institucional completa y
  es usable en teléfonos desde 360px de ancho sin pérdida de funciones.
- **SC-007**: Cero regresiones: las suites de pruebas existentes del servidor y de la
  aplicación siguen en verde.

## Assumptions

- La matriz de permisos del servidor (ADR 0013) es correcta y **no se modifica**; la interfaz
  se alinea a ella. Almacenes y Tipos permanecen exclusivos del Administrador.
- La identidad institucional aplicable es la del ADR 0020 ("SIGEFOR — Sistema Integral de
  Gestión de Fornituras") con la paleta institucional ya existente en la aplicación.
- No existe un emblema oficial entregado; se usa un escudo vectorial provisional acorde a la
  paleta, diseñado para sustituirse por el oficial sin rediseñar la pantalla.
- La mayoría de los usuarios acceden desde teléfonos móviles; el diseño del login y de
  Asignación prioriza pantallas pequeñas sin sacrificar la versión de escritorio.
- La corrección automática de la cuenta administradora inicial aplica únicamente a la cuenta
  configurada para siembra, no a ninguna otra cuenta.
- La rotación efectiva de las credenciales expuestas es una operación del responsable del
  entorno y queda fuera del alcance (solo se documenta).
- La capacidad de "capturar" una baja o incidencia y la de "autorizarla" siguen el mapeo de
  la matriz oficial tal como el servidor la aplica hoy; la interfaz muestra cada acción solo
  a los roles que el servidor acepta para esa acción concreta.

## Out of Scope

- Cambios a la matriz de permisos del servidor o al ADR 0013 (Almacenes y Tipos siguen
  siendo exclusivos del Administrador).
- Rotación efectiva de las credenciales expuestas (la ejecuta el responsable del entorno).
- Autenticación multifactor (permanece condicionada al ADR 0014).
- El backend Java obsoleto y la landing pública.
