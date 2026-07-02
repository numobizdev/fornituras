# Feature Specification: Escaneo QR con Capacitor y asignación en almacén

**Feature Branch**: `019-escaneo-qr-capacitor`

**Created**: 2026-07-01

**Status**: Draft

**Input**: Integrar `@capacitor/barcode-scanner` para escaneo QR en móvil y navegador (webcam);
mejorar `/asignacion` para identificar fornituras disponibles en almacén tras escanear el QR.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Escaneo QR con Capacitor en móvil y web (Priority: P1)

Como operador, quiero escanear el QR de una fornitura con la cámara del dispositivo (Android nativo
o webcam del navegador) usando el plugin oficial de Capacitor, con fallback a lector HID y tecleo
manual cuando la cámara no esté disponible.

**Why this priority**: Sin escaneo fiable en campo (móvil) y en escritorio (webcam) el flujo de
asignación y otras operaciones que dependen de QR quedan limitados al lector físico o tecleo.

**Independent Test**: Escanear el mismo código `FOR-XXXXX` por cámara Capacitor y por tecleo manual;
ambos entregan el mismo valor al consumidor.

**Acceptance Scenarios**:

1. **Given** un dispositivo Android con cámara, **When** el usuario activa el escaneo óptico,
   **Then** el plugin abre el visor de cámara y devuelve el código QR leído.
2. **Given** `ionic serve` en un navegador con webcam, **When** el usuario activa el escaneo,
   **Then** puede elegir la cámara disponible (`showCameraSelection`) y leer el QR.
3. **Given** permiso de cámara denegado o sin hardware, **When** el usuario intenta escanear,
   **Then** recibe un mensaje claro y puede usar lector HID o tecleo manual (FR-005 de 014).
4. **Given** cualquier medio de captura, **When** se entrega el código, **Then** el componente **no**
   resuelve datos en el cliente (Principios II/IV).

---

### User Story 2 - Asignación: identificar fornitura en almacén (Priority: P1)

Como capturista en `/asignacion`, al escanear el QR de una fornitura quiero ver sus datos y el
**almacén donde está registrada**, y solo poder continuar si está **disponible en almacén**
(`DISPONIBLE`).

**Why this priority**: La asignación entrega una fornitura del almacén a un elemento; el operador
debe confirmar que escaneó la pieza correcta y que aún no está asignada.

**Independent Test**: Escanear una fornitura `DISPONIBLE` → ver almacén y asignar; escanear una ya
`ASIGNADA` → bloqueo con mensaje claro.

**Acceptance Scenarios**:

1. **Given** una fornitura con `status = DISPONIBLE`, **When** se escanea su QR en asignación,
   **Then** se muestra código, descripción, **nombre del almacén** y badge «Disponible»; el paso 2
   (elemento) se habilita.
2. **Given** una fornitura con otro estado (`ASIGNADA`, `EN_TRASLADO`, etc.), **When** se escanea,
   **Then** se muestra badge «No disponible» con mensaje explicativo y el paso 2 queda bloqueado.
3. **Given** un QR desconocido, **When** se intenta resolver, **Then** el servidor responde sin
   revelar información de enumeración (404 genérico).
4. **Given** una asignación completada, **When** se consulta la fornitura, **Then** su estado pasa
   a `ASIGNADA` (ya no está en almacén disponible).

---

### User Story 3 - Reutilización LEGO del componente (Priority: P2)

Como desarrollador, quiero que `app-qr-scan` siga siendo el único punto de captura QR para 001/004/007/009,
registrando `CapacitorBarcodeScanner` vía el puerto `OpticalScanner` sin duplicar lógica.

**Independent Test**: Traslados y bajas siguen recibiendo el código por `codeCaptured` sin cambios en su
plantilla.

---

### Edge Cases

- Navegador sin contexto seguro (HTTP): cámara web no disponible; lector/manual siguen funcionando.
- Usuario cancela el modal del escáner Capacitor: no error fatal; puede reintentar o usar manual.
- Plugin Capacitor no instalado/cargado: fallback a `WebBarcodeDetectorScanner` (ADR 0008).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Instalar y configurar `@capacitor/barcode-scanner` (Android `minSdkVersion` 26).
- **FR-002**: Implementar `CapacitorBarcodeScanner` como implementación preferida del puerto
  `OpticalScanner`.
- **FR-003**: Mantener `WebBarcodeDetectorScanner` como fallback si el plugin no está disponible.
- **FR-004**: HID + tecleo manual sin cambios (014).
- **FR-005**: En `/asignacion`, mostrar `almacenNombre` tras resolver la fornitura.
- **FR-006**: En `/asignacion`, bloquear paso 2 si `status !== DISPONIBLE`.
- **FR-007**: En web, `scanBarcode` MUST usar `web.showCameraSelection: true` para elegir webcam;
  **no** requiere `@ionic/pwa-elements` (Html5Qrcode integrado en el plugin).
- **FR-008**: Sin PII en logs del escáner; solo código opaco.

### Key Entities

- No introduce entidades. Consume `EquipmentDetail` existente (`warehouseId`, `almacenNombre`, `status`).

## Success Criteria *(mandatory)*

- **SC-001**: Escaneo Capacitor en Android y web entrega el mismo valor que tecleo manual.
- **SC-002**: Asignación muestra almacén y bloquea fornituras no disponibles.
- **SC-003**: Cero resolución de datos en el cliente sin verificación del servidor.

## Assumptions

- Backend .NET ya expone `GET /equipment/by-codigo/{codigo}` con `almacenNombre` y valida `DISPONIBLE`
  en `POST /assignments`.
- PWA Elements (`@ionic/pwa-elements`) es responsabilidad de **017-gestion-de-fotos** (plugin Camera),
  no de esta spec.

## Dependencies

- [014-escaneo-qr](../014-escaneo-qr/spec.md), [004-asignacion-resguardos](../004-asignacion-resguardos/spec.md)
- [ADR 0008](../../docs/04-decisiones/0008-escaneo-optico-qr.md) (extendido por ADR 0019)
- [ADR 0005](../../docs/04-decisiones/0005-formato-qr-implementado.md) (formato `FOR-XXXXX`)
