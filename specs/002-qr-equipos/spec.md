# Feature Specification: Generación e impresión de QR por lotes

**Feature Branch**: `002-qr-equipos`

**Created**: 2026-06-29

**Updated**: 2026-06-30 (alineado a la **implementación real** del módulo `qrcodes`)

**Status**: **Implementado (parcial)** — generación, lotes y export PDF/ZIP existen; ver estado abajo

**Input**: User description: "Generación de QR para grabado/impresión de fornituras"

> **ESTADO DE IMPLEMENTACIÓN (2026-06-30).** El módulo `fornituras-api/.../modules/qrcodes/` ya
> está implementado y es la base sobre la que se trabaja. Esta spec se **ajusta a lo que existe**:
> el QR es un **código corto opaco `FOR-XXXXX`** (sin UUID ni firma HMAC), generado **por lotes**
> con parámetros de impresión y exportable a **PDF/ZIP**. La decisión vigente es el
> [ADR 0005](../../docs/04-decisiones/0005-formato-qr-implementado.md) (reemplaza al 0002).
> **Divergencia de seguridad conocida:** no hay firma criptográfica (ver §Seguridad y ADR 0005).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generar un lote de QR (Priority: P1) — **IMPLEMENTADO**

Un usuario autorizado genera un **lote** de N códigos QR únicos indicando la descripción, la
cantidad y los parámetros de impresión (tamaño en cm, padding, posición de la etiqueta, bordes).
El sistema crea N códigos opacos `FOR-XXXXX` únicos, listos para imprimir/grabar y pegar sobre
las fornituras físicas.

**Why this priority**: Es el habilitador del flujo físico: primero se imprimen las etiquetas y
luego se asocian a las fornituras al darlas de alta/escanear.

**Independent Test**: Generar un lote de 10 con tamaño 3 cm → se crean 10 códigos `FOR-XXXXX`
únicos asociados al lote; ningún código contiene PII; dos lotes no comparten códigos.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado, **When** genera un lote con cantidad y parámetros válidos,
   **Then** el sistema crea ese número de códigos únicos `FOR-XXXXX` ligados al lote.
2. **Given** una cantidad fuera de rango (≤ 0 o > límite `app.qr.maxBatchSize`/10000), **When**
   se intenta generar, **Then** el sistema lo rechaza con un mensaje claro.
3. **Given** el contenido de cualquier código generado, **When** se inspecciona, **Then** no
   incluye número de serie, nombre ni dato alguno del elemento o de la fornitura (Principio II —
   opacidad).

---

### User Story 2 - Exportar el lote para grabado/impresión (Priority: P1) — **IMPLEMENTADO**

Un usuario descarga el lote como **PDF** imprimible o como **ZIP** con un PNG por código, usando
los ajustes guardados del lote o ajustes personalizados (tamaño, padding, etiqueta, bordes) sin
alterar los códigos.

**Why this priority**: Cierra el ciclo físico (llevar el QR a la fornitura). Depende de US1.

**Independent Test**: Descargar el PDF/ZIP de un lote → el archivo contiene un QR por código,
escaneable, con la disposición elegida; re-exportar con otro tamaño no cambia los códigos.

**Acceptance Scenarios**:

1. **Given** un lote generado, **When** el usuario descarga el PDF, **Then** obtiene un documento
   imprimible con los códigos del lote y su disposición.
2. **Given** un lote generado, **When** descarga el ZIP, **Then** obtiene un PNG por código.
3. **Given** un lote, **When** exporta con ajustes personalizados (`ReprintQrForm`), **Then** el
   archivo usa esos ajustes pero **los códigos permanecen iguales**.

---

### User Story 3 - Consultar lotes y sus códigos (Priority: P2) — **IMPLEMENTADO**

Un usuario lista los lotes (más recientes primero), ve el detalle de un lote y enumera los
códigos que contiene.

**Why this priority**: Permite reimprimir, auditar y localizar lotes ya generados.

**Acceptance Scenarios**:

1. **Given** lotes existentes, **When** el usuario los lista, **Then** los ve ordenados por fecha
   de creación (más nuevos primero).
2. **Given** un lote, **When** consulta sus códigos, **Then** obtiene la lista de `FOR-XXXXX` del
   lote.

---

### User Story 4 - Verificar autenticidad del QR (Priority: P3) — **NO IMPLEMENTADO / abierto**

*Objetivo deseable, hoy ausente.* Probar criptográficamente que un código lo emitió el sistema
(firma) para rechazar códigos fabricados. La implementación actual **no** firma los códigos; un
código solo "existe o no" en BD. Ver §Seguridad y [ADR 0005](../../docs/04-decisiones/0005-formato-qr-implementado.md)
para la decisión abierta (aceptar el riesgo + mitigaciones, o añadir firma).

**Why this priority**: Mejora la confianza del QR, pero el daño está acotado por la resolución
server-side autenticada; se trata como deuda de seguridad, no bloqueante.

### Edge Cases

- Colisión de códigos: el generador reintenta y verifica unicidad contra BD y contra el lote en
  curso; si no logra suficientes únicos, falla con mensaje claro.
- Densidad/tamaño físico del QR para grabado sobre tela/placa: el lote captura `qrSizeCm` y
  `paddingCm`; el tamaño mínimo legible se valida con prueba real (SC-006).
- Reimpresión: se re-exporta el mismo lote (mismos códigos) con ajustes nuevos sin regenerar.
- Código escaneado que no existe en BD: se resuelve como "no encontrado" sin filtrar detalles
  (la resolución vive en specs 001/004/014).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST generar, por lote, N **códigos opacos únicos** con formato
  `FOR-XXXXX` (prefijo/sufijo configurables) sin significado ni datos derivables. *(Implementado.)*
- **FR-002**: El sistema MUST garantizar unicidad de cada código (verificación contra BD y contra
  el lote en curso) usando aleatoriedad segura (`SecureRandom`). *(Implementado.)*
- **FR-003**: El contenido del QR MUST NOT incluir número de serie, nombre, adscripción ni ningún
  dato personal o explotable (Principio II — **opacidad**). *(Implementado.)*
- **FR-004**: El sistema MUST permitir **exportar** el lote a **PDF** y a **ZIP (PNG)**, con los
  ajustes del lote o personalizados, sin alterar los códigos. *(Implementado.)*
- **FR-005**: El sistema MUST capturar parámetros de impresión por lote: `cantidad`,
  `descripcion`, `qrSizeCm`, `paddingCm`, `labelPosition` (`NONE`/`TOP`/`BOTTOM`), `mostrarBordes`,
  con validación de rangos. *(Implementado.)*
- **FR-006**: El sistema MUST permitir **listar** lotes y **enumerar** los códigos de un lote.
  *(Implementado.)*
- **FR-007**: Los endpoints de QR MUST requerir autenticación (JWT) y, idealmente, autorización
  por rol. *(Autenticación implementada; autorización por rol fino → pendiente con la expansión
  de roles, spec 013.)*
- **FR-008**: La resolución `código → fornitura/asignación` MUST ocurrir solo en el servidor tras
  authn + authz (la asociación y el escaneo se especifican en **001**/**004**/**014**).
- **FR-009** *(abierto)*: El sistema SHOULD poder **verificar la autenticidad** de un código
  (firma) — **no implementado**; decisión en [ADR 0005](../../docs/04-decisiones/0005-formato-qr-implementado.md).
- **FR-010** *(recomendado)*: La generación y exportación de lotes SHOULD quedar **auditadas**
  (actor, lote, cantidad, cuándo) — verificar/añadir según feature **012**.

### Key Entities *(include if feature involves data)*

- **Lote de QR** (`lote_qr`): `cantidad`, `descripcion`, `qrSizeCm`, `paddingCm`, `labelPosition`,
  `mostrarBordes`, y la colección de códigos. Parámetros de impresión física.
- **Código QR** (`codigo_qr`): `codigo` (`FOR-XXXXX`, único) + FK al lote. Es el valor opaco que
  viaja en el QR. **No** está ligado a una fornitura al generarse; el enlace ocurre en el alta de
  fornitura (**001**). Ver [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).

## Seguridad (divergencia conocida)

La implementación **preserva la opacidad** (sin PII en el código) pero **omite la firma HMAC**
que pedía el Principio II. Sin firma no hay prueba criptográfica de emisión; el riesgo está
acotado por la **resolución server-side autenticada**, la unicidad y el espacio de códigos, pero
existe riesgo de **enumeración** por un usuario autenticado. Mitigaciones recomendadas: rate
limiting y auditoría de resoluciones. La decisión (aceptar el riesgo y enmendar el Principio II, o
añadir firma) está abierta en [ADR 0005](../../docs/04-decisiones/0005-formato-qr-implementado.md).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los códigos generados son únicos (cero colisiones).
- **SC-002**: El 100% del contenido de los códigos está libre de PII (verificable inspeccionando
  el contenido crudo).
- **SC-003**: La exportación PDF/ZIP de un lote produce un QR escaneable por código.
- **SC-004**: Re-exportar un lote con otros ajustes **no** cambia los códigos.
- **SC-005**: Un usuario autorizado genera y exporta un lote en menos de 1 minuto.
- **SC-006**: Un QR grabado/impreso con el tamaño definido se lee por cámara y por lector manual
  en el primer intento en condiciones normales.

## Assumptions

- El formato y los parámetros se rigen por el ADR 0005 y por `app.qr.*` (prefijo, longitud de
  sufijo, tamaño máximo de lote, reintentos).
- La lectura/escaneo del código (cámara Capacitor, lector HID, manual) y la pantalla de ficha al
  escanear se especifican en **014-escaneo-qr**.
- La asociación `código → fornitura` se realiza al dar de alta/asignar la fornitura (**001**/**004**).
- Existe una UI web (Thymeleaf, `QrWebController`) además de la API REST; el frontend `sigefor/`
  puede consumir la API REST para integrarlo en la app.

## Dependencies

- Constitución (Principio II — opacidad cumplida; firma divergente, ver ADR 0005);
  [`docs/02-seguridad.md`](../../docs/02-seguridad.md) §2.
- ADR vigente: [`0005-formato-qr-implementado.md`](../../docs/04-decisiones/0005-formato-qr-implementado.md)
  (reemplaza al 0002).
- Features: **001-inventario-equipos** (asociación), **014-escaneo-qr** (lectura),
  **012-auditoria** (auditoría de generación/export).
- Contrato REST: [contracts/qr-api.md](./contracts/qr-api.md).
