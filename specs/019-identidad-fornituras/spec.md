# Feature Specification: Identidad del sistema — Sistema Integral de Gestión de Fornituras

**Feature Branch**: `019-identidad-fornituras`

**Created**: 2026-07-01

**Status**: Draft

**Input**: User description: "Identidad del sistema: rebrand a 'Sistema Integral de Gestión de Fornituras' (expansión del acrónimo SIGEFOR). Alcance: (1) la landing pública debe presentar el sistema como 'Sistema Integral de Gestión de Fornituras' en el hero por defecto (valor sembrado y datos existentes, sin pisar ediciones deliberadas del administrador), en el fallback del frontend y en el footer; (2) quitar 'Gobierno de México' del título del navegador — pasa a 'SIGEFOR | Sistema Integral de Gestión de Fornituras'; (3) renombrar la entrada de menú del módulo de administración de la landing de 'Contenido de bienvenida' a 'Configurar landing' (solo ADMIN); (4) actualizar documentación canónica y registrar un ADR que revierte la decisión previa que fijó 'Sistema de Gestión de Blindajes' como canónico; Planeacion.md se conserva intacta. Cambia el nombre/marca, no el dominio."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Visitante ve la nueva identidad en la landing pública (Priority: P1)

Un visitante (sin sesión) abre la aplicación web y la portada lo recibe presentando el
sistema como **"Sistema Integral de Gestión de Fornituras"**: en el título principal (hero),
en el pie de página y en la pestaña del navegador. Ya no aparece la marca
"Gobierno de México" ni el nombre anterior "Sistema de Gestión de Blindajes".

**Why this priority**: es el objetivo central del rebrand — la cara pública es lo primero que
ve cualquier persona y hoy muestra la identidad equivocada.

**Independent Test**: abrir la URL pública sin iniciar sesión y verificar los tres puntos
(hero, footer, pestaña del navegador). Entrega valor por sí sola aunque no se haga nada más.

**Acceptance Scenarios**:

1. **Given** un despliegue con los datos sembrados por defecto, **When** un visitante abre la
   landing pública, **Then** el título del hero dice "Sistema Integral de Gestión de Fornituras".
2. **Given** la landing pública, **When** el visitante mira el pie de página, **Then** dice
   "SIGEFOR · Sistema Integral de Gestión de Fornituras".
3. **Given** cualquier página de la aplicación, **When** el visitante mira la pestaña del
   navegador, **Then** el título es "SIGEFOR | Sistema Integral de Gestión de Fornituras" y no
   contiene "Gobierno de México".
4. **Given** que el servicio de contenido no responde, **When** se carga la landing, **Then**
   el texto de respaldo (fallback) también muestra la nueva identidad.

---

### User Story 2 - Instalaciones existentes reciben el nuevo título sin perder personalizaciones (Priority: P2)

Una instalación en operación cuyo hero público todavía tiene el valor sembrado antiguo
("Sistema de Gestión de Blindajes") recibe el nuevo título automáticamente al actualizar.
Si el administrador ya había personalizado deliberadamente ese título con otro texto,
su edición se respeta y no se sobrescribe.

**Why this priority**: sin esto, el rebrand solo aplicaría a bases de datos nuevas y el
problema reportado ("sigo viendo el título viejo") persistiría en los entornos reales.

**Independent Test**: sobre una base de datos con el valor antiguo sembrado, aplicar la
actualización y verificar el cambio; sobre una base con título personalizado, verificar que
no cambia.

**Acceptance Scenarios**:

1. **Given** una base de datos cuyo hero público dice exactamente "Sistema de Gestión de
   Blindajes", **When** se aplica la actualización, **Then** el hero pasa a decir
   "Sistema Integral de Gestión de Fornituras".
2. **Given** una base de datos cuyo hero público fue editado por el administrador (cualquier
   otro texto), **When** se aplica la actualización, **Then** el título se conserva intacto.
3. **Given** una base de datos vacía (instalación nueva), **When** se siembra el contenido
   inicial, **Then** el hero público nace con "Sistema Integral de Gestión de Fornituras".

---

### User Story 3 - El ADMIN encuentra fácilmente el módulo de configuración de la landing (Priority: P3)

Un usuario con rol ADMIN abre el menú lateral y reconoce de inmediato la opción para
configurar la portada: la entrada se llama **"Configurar landing"** (antes "Contenido de
bienvenida", nombre que no se asociaba con la landing). La restricción de acceso no cambia:
solo ADMIN la ve y puede usarla.

**Why this priority**: mejora de descubribilidad; el módulo ya existe y funciona, solo era
difícil de reconocer.

**Independent Test**: iniciar sesión como ADMIN y localizar la entrada "Configurar landing"
en el menú; iniciar sesión con otro rol y comprobar que no aparece.

**Acceptance Scenarios**:

1. **Given** una sesión con rol ADMIN, **When** se abre el menú lateral, **Then** existe la
   entrada "Configurar landing" que lleva al editor de la landing.
2. **Given** una sesión con rol distinto de ADMIN, **When** se abre el menú lateral, **Then**
   la entrada no aparece, y navegar directo a la ruta redirige fuera (comportamiento actual).

---

### Edge Cases

- Hero público con título personalizado que casualmente contiene la palabra "Blindajes" pero
  no es exactamente el valor sembrado: NO debe tocarse (la actualización se acota al valor
  exacto sembrado).
- Base de datos donde la actualización ya corrió una vez: volver a aplicarla no debe duplicar
  ni alterar nada (idempotencia).
- Servicio de contenido caído al abrir la landing: los textos de respaldo del cliente deben
  mostrar la nueva identidad, nunca la antigua.
- Subtítulos y descripciones funcionales que hablan de "blindajes" como dominio (qué administra
  el sistema) se conservan: el rebrand aplica al **nombre propio** del sistema, no al vocabulario
  del dominio.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La landing pública MUST presentar el sistema como "Sistema Integral de Gestión
  de Fornituras" en el título del hero para instalaciones nuevas (contenido sembrado por defecto).
- **FR-002**: Las instalaciones existentes cuyo hero público conserve exactamente el valor
  sembrado anterior ("Sistema de Gestión de Blindajes") MUST recibir el nuevo título mediante
  una actualización de datos versionada e idempotente.
- **FR-003**: La actualización de datos MUST respetar (no sobrescribir) títulos del hero que el
  administrador haya editado deliberadamente (cualquier valor distinto del sembrado exacto).
- **FR-004**: El texto de respaldo del cliente (cuando el contenido dinámico no carga) MUST
  mostrar "Sistema Integral de Gestión de Fornituras" como título y un subtítulo coherente con
  la identidad de fornituras.
- **FR-005**: El pie de página de la landing pública MUST decir
  "SIGEFOR · Sistema Integral de Gestión de Fornituras".
- **FR-006**: El título del documento (pestaña del navegador) MUST ser
  "SIGEFOR | Sistema Integral de Gestión de Fornituras" y MUST NOT contener "Gobierno de México".
- **FR-007**: La entrada del menú lateral hacia el editor de la landing MUST llamarse
  "Configurar landing" y MUST seguir visible únicamente para el rol ADMIN, con la misma
  protección de ruta actual.
- **FR-008**: La documentación canónica del proyecto (README, constitución, instrucciones de
  Copilot) MUST reflejar el nombre "Sistema Integral de Gestión de Fornituras (SIGEFOR)".
- **FR-009**: La decisión de identidad MUST registrarse como ADR, revirtiendo explícitamente la
  decisión previa que fijó "Sistema de Gestión de Blindajes" como nombre canónico.
- **FR-010**: `Planeacion.md` MUST conservarse intacta (visión original del cliente). Las
  descripciones funcionales del dominio ("administrar blindajes") MUST conservarse en specs y
  documentos históricos.

### Key Entities

- **Sección de landing (hero público)**: contenido editable de la portada; su título es el
  nombre visible del sistema. Distingue entre valor sembrado por defecto y valor editado por
  el administrador.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de los puntos visibles de identidad de la cara pública (hero, footer,
  pestaña del navegador) muestran "Sistema Integral de Gestión de Fornituras" tras la
  actualización, en una instalación con datos sembrados por defecto.
- **SC-002**: Cero menciones de "Gobierno de México" visibles para el usuario en la aplicación.
- **SC-003**: Una instalación con el hero personalizado por el administrador conserva el 100 %
  de sus textos tras aplicar la actualización.
- **SC-004**: Un usuario ADMIN localiza el módulo de configuración de la landing desde el menú
  en el primer intento (la etiqueta nombra explícitamente la landing).
- **SC-005**: Los usuarios con rol distinto de ADMIN siguen sin ver ni poder acceder al módulo
  (misma cobertura de control de acceso que antes del cambio).

## Assumptions

- "SIGEFOR" se mantiene como acrónimo visible (menú, login, footer); lo que cambia es su
  expansión oficial: "Sistema Integral de Gestión de Fornituras".
- La actualización de datos se acota al par exacto (sección pública tipo hero, título igual al
  valor sembrado anterior) para diferenciar "valor por defecto" de "edición deliberada"; no se
  requiere marca adicional de auditoría para distinguirlos.
- El dominio del sistema no cambia: sigue administrando blindajes (chalecos y equipo de
  seguridad); solo cambia el nombre propio/marca del producto.
- La entrada de menú conserva su ícono actual; el cambio es solo de etiqueta.
- No hay materiales impresos ni externos (manuales, QR grabados) que dependan del nombre
  anterior y queden fuera del alcance de este cambio.
