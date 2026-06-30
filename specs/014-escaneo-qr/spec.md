# Feature Specification: Escaneo y captura de QR (cámara, lector, manual)

**Feature Branch**: `014-escaneo-qr`

**Created**: 2026-06-30

**Status**: Draft

**Input**: `Requerimientos.MD` §1, §3, §5 (captura de QR por lector / cámara / manual con
detección automática). Es el componente de captura reutilizado por Asignación (**004**),
Traslados (**007**), Alta de fornituras (**001**) y Bajas (**009**).

> **Reutilización (estilo LEGO).** Esta feature define **un solo componente/servicio de captura
> de QR** que el resto de pantallas consume, en lugar de duplicar la lógica de escaneo en cada
> una. La verificación de firma del QR vive en el servidor (**002**); aquí solo se obtiene el
> código y se entrega.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Capturar un QR con detección automática de origen (Priority: P1)

En cualquier pantalla que lo requiera, el usuario captura un código QR y el sistema **detecta
automáticamente** el origen: lector HID (emula teclado), cámara (móvil o, si es viable, webcam de
PC) o **tecleo manual**. Un campo único con placeholder ("Escanee con el lector o teclee el
código") más un botón "Buscar/Agregar" cubre lector y manual; un botón de cámara abre el escaneo
óptico.

**Why this priority**: Es el modo de entrada físico del sistema; sin captura fiable no funcionan
asignación, traslados ni alta.

**Independent Test**: Capturar el mismo código por los tres medios (lector, cámara, manual) y
verificar que los tres entregan exactamente el mismo valor al consumidor.

**Acceptance Scenarios**:

1. **Given** un lector HID conectado, **When** el usuario escanea, **Then** el código aparece en
   el campo y se entrega al consumidor sin pasos extra (detección por patrón de entrada rápida +
   terminador).
2. **Given** un dispositivo con cámara, **When** el usuario activa el escaneo óptico, **Then** la
   cámara reconoce el QR y entrega el código.
3. **Given** ningún lector ni cámara, **When** el usuario teclea el código y pulsa el botón,
   **Then** el código se entrega igual que por los otros medios.
4. **Given** un código capturado (`FOR-XXXXX`), **When** se entrega al consumidor, **Then** este
   resuelve `código → fornitura` **solo en el servidor**, tras verificar sesión + rol y la
   existencia del código (Principio II — opacidad; los códigos **no** llevan firma, ver ADR 0005).

### Edge Cases

- En PC sin cámara: el flujo degrada a lector/manual sin romperse (cámara de PC es "best effort").
- Permisos de cámara denegados (Capacitor): mensaje claro y fallback a manual/lector.
- Lectura óptica de QR dañado/grabado sobre tela: reintentos y mensaje; el tamaño/densidad
  mínimo se valida físicamente (ver **002**).
- Código con formato no reconocido o firma inválida: el servidor lo rechaza sin revelar
  información (**002** FR-004/005).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST proveer un **componente/servicio único** de captura de QR,
  reutilizable por **001**, **004**, **007** y **009**.
- **FR-002**: El componente MUST aceptar entrada por **lector HID**, **cámara** (Capacitor en
  móvil; webcam en PC como best-effort) y **tecleo manual**, detectando el origen automáticamente
  cuando sea posible.
- **FR-003**: El componente MUST entregar al consumidor el **mismo valor** independientemente del
  medio de captura.
- **FR-004**: El componente MUST NOT resolver datos por sí mismo en el cliente: la relación
  `código → fornitura/elemento` se resuelve en el servidor tras verificar sesión + autorización y
  la existencia del código (Principios II, IV). Los códigos `FOR-XXXXX` **no** llevan firma
  (ADR 0005); validar el **formato** localmente (`^FOR-[0-9A-Z]{5}$`) es opcional antes de enviar.
- **FR-005**: Ante denegación de permisos de cámara o ausencia de hardware, el componente MUST
  degradar a lector/manual con un mensaje claro, sin bloquear el flujo.
- **FR-006**: El componente MUST NOT registrar PII; el código QR es un identificador opaco.

### Key Entities

- No introduce entidades; produce un **valor de código QR** opaco `FOR-XXXXX` (formato definido en
  **002** / [ADR 0005](../../docs/04-decisiones/0005-formato-qr-implementado.md); sin firma).

## Success Criteria *(mandatory)*

- **SC-001**: El mismo QR capturado por lector, cámara y manual entrega un valor idéntico el 100%
  de las veces.
- **SC-002**: En un dispositivo sin cámara, el flujo de captura funciona por lector/manual sin
  errores.
- **SC-003**: Cero resolución de datos en el cliente sin verificación del servidor.

## Assumptions

- La cámara móvil usa Capacitor (stack congelado, Principio VI). La webcam de PC es opcional y de
  mejor esfuerzo.
- La detección de lector HID se basa en velocidad de entrada y carácter terminador.

## Dependencies

- Constitución (Principios II, IV, VI); `docs/02-seguridad.md` §2.
- Features consumidoras: **001**, **004**, **007**, **009**; verificación: **002-qr-equipos**.
- ADR formato QR: [`docs/04-decisiones/0005-formato-qr-implementado.md`](../../docs/04-decisiones/0005-formato-qr-implementado.md) (reemplaza al 0002).
