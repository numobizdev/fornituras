# Feature Specification: Landing configurable + tour guiado

**Feature Branch**: `016-landing-configurable`

**Created**: 2026-06-30

**Status**: Draft

**Input**: User description: "Landing configurable + tour guiado. Módulo de landing
configurable con dos caras (landing pública pre-login y home post-login) editables por rol
ADMIN desde la app y persistidas en backend, más un tour guiado del home la primera vez que
entra el usuario. El endpoint público no expone PII."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Home de bienvenida configurable tras el login (Priority: P1)

Al iniciar sesión, el usuario ya no ve un placeholder vacío ("Panel de indicadores
próximamente"), sino una página de inicio con contenido de bienvenida útil: un encabezado
institucional, avisos vigentes de la corporación y accesos rápidos a las funciones que más
usa. Ese contenido lo mantiene un administrador y puede cambiar sin actualizar la app.

**Why this priority**: Es el corazón del feature y el que aporta valor a diario a todos los
usuarios autenticados. Sustituye una pantalla inútil por una funcional y comunica avisos
oficiales al personal. Puede entregarse solo (con contenido inicial sembrado) y ya es un MVP.

**Independent Test**: Con secciones de tipo HOME activas cargadas, iniciar sesión y verificar
que `/inicio` muestra encabezado, avisos y accesos rápidos ordenados; desactivar una sección
y comprobar que deja de mostrarse tras recargar.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado y secciones HOME activas, **When** entra a la página de
   inicio, **Then** ve las secciones activas en el orden configurado.
2. **Given** un aviso configurado como inactivo, **When** el usuario entra al inicio,
   **Then** ese aviso no aparece.
3. **Given** un acceso rápido configurado con destino a una función de la app, **When** el
   usuario lo pulsa, **Then** navega a esa función.

---

### User Story 2 - Edición del contenido por el administrador (Priority: P1)

Un administrador entra a un editor dentro de la app y da de alta, modifica, reordena, activa
o desactiva las secciones de bienvenida, tanto de la cara pública como del inicio interno.
Los cambios quedan guardados y se reflejan para los demás usuarios sin intervención técnica.

**Why this priority**: Sin edición, el contenido sería estático y el feature perdería su
razón de ser ("configurable"). Es requisito para que la Historia 1 y la 3 tengan datos reales
que mostrar.

**Independent Test**: Iniciar sesión como administrador, crear una sección, comprobar que
aparece en la vista correspondiente; editarla y desactivarla, y verificar el efecto. Con un
usuario no administrador, confirmar que el editor no está disponible.

**Acceptance Scenarios**:

1. **Given** un administrador en el editor, **When** crea una sección de inicio y la guarda,
   **Then** la sección queda persistida y visible en el inicio de los usuarios.
2. **Given** un usuario sin rol de administrador, **When** intenta acceder al editor,
   **Then** el acceso se deniega.
3. **Given** varias secciones existentes, **When** el administrador cambia su orden,
   **Then** se muestran en el nuevo orden.
4. **Given** un administrador que introduce texto con marcado o código en un campo, **When**
   se muestra ese contenido a los usuarios, **Then** se presenta como texto literal y nunca
   se ejecuta.

---

### User Story 3 - Página pública de bienvenida antes del login (Priority: P2)

Una persona que abre la aplicación sin haber iniciado sesión ve una página de presentación
con la identidad institucional de la corporación y un botón para acceder al inicio de sesión.
Esta página no requiere autenticación y no muestra ningún dato personal.

**Why this priority**: Aporta imagen institucional y una entrada clara, pero el sistema es
usable sin ella (se podría ir directo al login). Depende del mismo motor de contenido que la
Historia 2, por eso va después del MVP interno.

**Independent Test**: Sin sesión iniciada, abrir la app y comprobar que se muestra la página
pública con el contenido configurado y un botón "Acceder"; pulsarlo lleva al inicio de sesión.
Confirmar que la respuesta pública no contiene datos personales.

**Contexto (defecto actual a corregir)**: hoy, al abrir la app sin sesión, la aplicación monta
el shell autenticado (menú lateral de navegación) "como si el usuario estuviera logueado", y al
pulsar cualquier entrada del menú se rebota al inicio de sesión. La causa es doble: (a) la ruta
raíz apunta a un área protegida en vez de a una landing pública, y (b) la visibilidad del menú
se decide por el prefijo de la URL y no por el estado real de sesión. Esta historia debe dejar
como entrada por defecto la landing pública y ligar el shell a la sesión.

**Acceptance Scenarios**:

1. **Given** un visitante sin sesión, **When** abre la aplicación, **Then** ve la página
   pública de bienvenida con el contenido público activo.
2. **Given** la página pública, **When** el visitante pulsa "Acceder", **Then** llega al
   formulario de inicio de sesión.
3. **Given** un usuario ya autenticado, **When** navega a la página pública, **Then** se le
   redirige a su inicio.
4. **Given** un visitante sin sesión, **When** abre la aplicación, **Then** en ningún momento
   se muestra el menú de navegación interno (shell); solo ve la landing pública.

---

### User Story 4 - Tour guiado de primera vez (Priority: P3)

La primera vez que un usuario llega a su inicio, un recorrido guiado resalta paso a paso las
zonas clave (encabezado, accesos rápidos y menú principal) con explicaciones breves. El
recorrido solo aparece una vez de forma automática, pero el usuario puede relanzarlo cuando
quiera desde un botón "Ver tutorial".

**Why this priority**: Mejora la adopción y la orientación de usuarios nuevos, pero es un
complemento; el inicio funciona sin él. Se apoya en las secciones de la Historia 1.

**Independent Test**: Con un usuario que entra por primera vez, verificar que el recorrido se
inicia solo y resalta las zonas esperadas; completarlo o cerrarlo y confirmar que no vuelve a
lanzarse solo; pulsar "Ver tutorial" y comprobar que se reinicia.

**Acceptance Scenarios**:

1. **Given** un usuario que entra al inicio por primera vez, **When** carga la página,
   **Then** se inicia automáticamente el recorrido guiado.
2. **Given** un usuario que ya completó o cerró el recorrido, **When** vuelve a entrar al
   inicio, **Then** el recorrido no se inicia solo.
3. **Given** cualquier usuario en el inicio, **When** pulsa "Ver tutorial", **Then** el
   recorrido guiado se reinicia.

---

### Edge Cases

- **Sin contenido configurado**: si no hay secciones activas para una cara, la página muestra
  un estado por defecto/vacío coherente (no una pantalla rota).
- **Enlaces de acceso rápido inválidos o a funciones sin permiso**: el sistema evita
  navegaciones rotas y no revela funciones a las que el usuario no tiene acceso.
- **Arranque sin sesión (shell fantasma)**: al abrir la app sin sesión no debe montarse el
  shell autenticado ni el menú lateral; la primera pantalla es la landing pública. Pulsar una
  función protegida nunca deja ver el menú ni contenido interno; a lo sumo redirige al inicio
  de sesión.
- **Contenido malicioso en campos de texto**: cualquier marcado/script capturado por un
  administrador se muestra como texto literal, nunca se ejecuta (protección anti-XSS).
- **Abuso del endpoint público**: peticiones excesivas a la cara pública se limitan por tasa
  para evitar enumeración/abuso.
- **Imágenes que no cargan**: se degrada con elegancia (espacio reservado o se omite).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE mostrar, tras iniciar sesión, una página de inicio compuesta por
  las secciones de contenido de tipo "inicio" que estén activas, en el orden configurado.
- **FR-002**: El sistema DEBE mostrar, sin requerir autenticación, una página pública de
  bienvenida compuesta por las secciones de tipo "público" activas, con un acceso al inicio
  de sesión.
- **FR-003**: El sistema DEBE permitir a un administrador crear, editar, reordenar, activar y
  desactivar secciones de contenido para ambas caras (pública e inicio).
- **FR-004**: El sistema DEBE restringir la edición de contenido exclusivamente al rol
  administrador; los demás roles no pueden crear ni modificar secciones.
- **FR-005**: El sistema DEBE persistir el contenido configurado de forma que sobreviva a
  reinicios y se comparta entre todos los usuarios.
- **FR-006**: El sistema NO DEBE incluir datos personales del personal (PII) en el contenido
  ni en la respuesta de la cara pública; esta solo contiene información institucional/de marca.
- **FR-007**: El sistema DEBE tratar todo el contenido capturado como texto y presentarlo sin
  ejecutar marcado ni scripts, evitando la inyección de contenido malicioso.
- **FR-008**: El sistema DEBE limitar por tasa las peticiones a la cara pública para mitigar
  abuso y enumeración.
- **FR-009**: El sistema DEBE registrar en auditoría las operaciones de creación, edición y
  desactivación de contenido, identificando al administrador responsable.
- **FR-010**: El sistema DEBE ofrecer accesos rápidos configurables en el inicio que naveguen
  a funciones de la aplicación.
- **FR-011**: El sistema DEBE iniciar automáticamente un recorrido guiado del inicio la
  primera vez que un usuario accede a él, y no repetirlo automáticamente después.
- **FR-012**: El sistema DEBE permitir al usuario relanzar el recorrido guiado a demanda.
- **FR-013**: El sistema DEBE mostrar un estado vacío coherente cuando no haya secciones
  activas para una cara.
- **FR-014**: El sistema DEBE redirigir a su inicio a un usuario ya autenticado que intente
  ver la página pública de bienvenida.
- **FR-015**: Al abrir la aplicación sin una sesión válida, el sistema DEBE presentar como
  primera pantalla la página pública de bienvenida, y NUNCA el shell autenticado (menú de
  navegación interno). La ruta raíz de la aplicación resuelve a la landing pública para
  visitantes y a su inicio para usuarios autenticados.
- **FR-016**: El sistema DEBE mostrar el menú de navegación interno y el shell autenticado
  únicamente a usuarios con sesión válida; un visitante sin sesión no ve el menú ni sus
  entradas en ningún momento. La visibilidad del shell DEBE derivarse del estado de sesión, no
  de la URL actual.

### Key Entities *(include if feature involves data)*

- **Sección de bienvenida (LandingSection)**: unidad de contenido configurable. Atributos
  clave: cara a la que pertenece (pública / inicio), tipo de sección (encabezado, aviso,
  accesos rápidos, texto), título, subtítulo, cuerpo, imagen, etiqueta y destino de una acción
  (call-to-action), orden de aparición y estado (activa/inactiva). Para accesos rápidos, una
  lista de elementos con etiqueta, destino e icono.
- **Preferencia de recorrido del usuario**: marca de que un usuario ya vio el recorrido guiado
  del inicio, para no repetirlo automáticamente (ámbito por usuario/dispositivo).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un administrador puede publicar un aviso nuevo y verlo reflejado en el inicio de
  los usuarios en menos de 2 minutos, sin intervención técnica ni redepliegue.
- **SC-002**: El 100 % de las respuestas de la cara pública están libres de datos personales
  del personal.
- **SC-003**: El contenido con marcado o scripts introducido en cualquier campo se muestra
  como texto literal en el 100 % de los casos (0 ejecuciones).
- **SC-004**: Ningún usuario sin rol administrador logra crear o modificar contenido (100 % de
  intentos denegados).
- **SC-005**: El recorrido guiado se muestra automáticamente solo en la primera visita al
  inicio y puede relanzarse a demanda en el 100 % de los intentos.
- **SC-006**: Tras iniciar sesión, la página de inicio con su contenido queda visible en menos
  de 2 segundos en condiciones normales de red.
- **SC-007**: En el 100 % de los arranques sin sesión, la primera pantalla es la landing
  pública y el menú de navegación interno no se renderiza en ningún momento.

## Assumptions

- El sistema de autenticación y los roles existentes (administrador y capturista) se reutilizan
  tal cual; "administrador" es quien edita el contenido.
- El idioma del contenido y de la interfaz es español (la app es monolingüe hoy).
- La identidad visual sigue la paleta institucional ya definida en la app.
- El contenido inicial de ambas caras se siembra con valores por defecto en el despliegue para
  que las páginas nunca aparezcan vacías al arrancar.
- La preferencia de "recorrido ya visto" se guarda a nivel de usuario/dispositivo; cambiar de
  dispositivo puede volver a mostrar el recorrido, lo cual es aceptable.
- Este feature es el último del roadmap ("al final"); no bloquea features anteriores.
- El recorrido guiado se apoya en una librería de terceros mantenida y ligera para tours de
  producto (decisión técnica documentada aparte en el plan/ADR).
