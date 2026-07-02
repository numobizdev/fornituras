# Feature Specification: UI de lotes QR en SIGEFOR (SUPER_ADMIN)

**Feature Branch**: `021-ui-lotes-qr`

**Created**: 2026-07-01

**Status**: Draft

**Input**: UI Ionic para generación y consulta de lotes QR, rol SUPER_ADMIN exclusivo. Paridad
funcional con las pantallas Thymeleaf del backend Java obsoleto (`/qr/**`).

> **Nota.** La generación de códigos y export PDF/ZIP ya está implementada en el backend .NET
> (spec [002-qr-equipos](../002-qr-equipos/spec.md)). Esta spec añade la **capa de presentación
> en Ionic** y restringe el acceso al nuevo rol **`SUPER_ADMIN`**.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generar un lote de QR (Priority: P1)

Un usuario con rol **SUPER_ADMIN** completa un formulario con la descripción del lote, la
cantidad de códigos y los parámetros de impresión (tamaño, padding, posición de etiqueta,
bordes). Al confirmar, el sistema crea el lote de códigos opacos `FOR-XXXXX` y ofrece la
descarga en PDF o ZIP según el formato elegido.

**Why this priority**: Habilita el flujo físico de impresión/grabado de etiquetas; es el punto
de entrada del módulo.

**Independent Test**: Iniciar sesión como SUPER_ADMIN, generar un lote de 10 códigos con tamaño
3 cm y descargar PDF → se crea el lote y el archivo contiene 10 QR escaneables sin PII.

**Acceptance Scenarios**:

1. **Given** un SUPER_ADMIN autenticado, **When** envía el formulario con datos válidos,
   **Then** el sistema crea el lote y muestra la pantalla de éxito con opciones de descarga.
2. **Given** una cantidad fuera de rango (≤ 0 o > límite configurado), **When** intenta generar,
   **Then** el sistema rechaza con un mensaje claro en español.
3. **Given** un usuario que no es SUPER_ADMIN, **When** intenta acceder al formulario o a la API,
   **Then** el acceso se deniega (UI oculta + respuesta 403 en API).

---

### User Story 2 - Consultar lotes generados (Priority: P1)

Un SUPER_ADMIN ve la lista de lotes ya generados, ordenados del más reciente al más antiguo, con
ID, descripción, fecha, cantidad, rango de códigos y parámetros de impresión. Puede abrir el
detalle de cualquier lote.

**Why this priority**: Permite localizar lotes previos para reimpresión y auditoría operativa.

**Independent Test**: Con al menos un lote existente, la lista muestra sus datos y el enlace al
detalle funciona; sin lotes, se muestra un estado vacío claro.

**Acceptance Scenarios**:

1. **Given** lotes existentes, **When** el SUPER_ADMIN abre la lista, **Then** los ve ordenados
   por fecha de creación descendente.
2. **Given** ningún lote, **When** abre la lista, **Then** ve un mensaje indicando que aún no hay
   lotes y un acceso para generar el primero.

---

### User Story 3 - Ver detalle y reimprimir (Priority: P1)

Un SUPER_ADMIN consulta el detalle de un lote (metadata y configuración original) y puede
exportar PDF o ZIP con la configuración original o con ajustes personalizados de impresión sin
alterar los códigos del lote.

**Why this priority**: Cierra el ciclo de reimpresión cuando cambian las necesidades físicas de
corte o tamaño.

**Independent Test**: Descargar PDF con config original y re-exportar con otro tamaño → mismos
códigos, distinta disposición visual.

**Acceptance Scenarios**:

1. **Given** un lote existente, **When** descarga PDF con config. original, **Then** obtiene un
   documento imprimible con todos los códigos del lote.
2. **Given** un lote, **When** reimprime con ajustes personalizados, **Then** el archivo usa los
   nuevos parámetros pero los códigos permanecen iguales.

---

### User Story 4 - Pantalla de éxito post-generación (Priority: P2)

Tras crear un lote, el SUPER_ADMIN ve un resumen (ID, descripción, cantidad, rango, fecha,
configuración) y puede descargar PDF/ZIP, ir al detalle o generar otro lote. Si eligió un
formato al crear, se inicia la descarga automática de ese formato.

**Why this priority**: Mejora la UX del flujo de alta; depende de US1.

**Acceptance Scenarios**:

1. **Given** un lote recién creado, **When** llega a la pantalla de éxito, **Then** ve el resumen
   completo y botones de descarga PDF y ZIP.
2. **Given** que eligió ZIP al generar, **When** llega a éxito, **Then** se descarga
   automáticamente el ZIP (con indicador de progreso si tarda).

### Edge Cases

- Lotes grandes: la UI muestra un indicador persistente y deshabilita doble envío mientras
  genera o descarga.
- Pérdida de sesión durante generación: redirige a login; no se asume éxito parcial en cliente.
- SUPER_ADMIN intenta navegar manualmente a otras pantallas (inventario, usuarios): redirige al
  módulo QR.
- Error de red en descarga: mensaje claro y posibilidad de reintentar.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST exponer el módulo de lotes QR **únicamente** al rol `SUPER_ADMIN`
  (menú, rutas de la app y endpoints de la API).
- **FR-002**: El formulario de generación MUST capturar: descripción, cantidad, tamaño QR (cm),
  padding (cm), posición de etiqueta (`NONE`/`TOP`/`BOTTOM`), mostrar bordes y formato de
  descarga inicial (PDF o ZIP).
- **FR-003**: El sistema MUST validar rangos de entrada (cantidad, tamaños) y mostrar errores en
  español.
- **FR-004**: El SUPER_ADMIN MUST poder listar lotes, ver detalle, listar códigos del lote y
  descargar/reimprimir PDF y ZIP.
- **FR-005**: Tras login, un SUPER_ADMIN MUST aterrizar en el módulo QR; otros roles no ven ni
  acceden al módulo.
- **FR-006**: Un ADMIN MUST poder asignar el rol `SUPER_ADMIN` al crear o editar usuarios (spec
  013); un usuario no puede auto-asignarse el rol.
- **FR-007**: La generación de lotes MUST quedar auditada (evento `GENERATE_QR_BATCH`, spec 012).
- **FR-008**: El contenido de los QR MUST NOT incluir PII (Principio II; heredado de spec 002).

### Key Entities

- **Lote de QR**: lote con parámetros de impresión y rango de consecutivos (spec 002).
- **Código QR**: valor opaco `FOR-XXXXX` asociado a un lote.
- **Usuario SUPER_ADMIN**: rol dedicado, acceso exclusivo al módulo QR, sin permisos operativos
  del resto del sistema.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un SUPER_ADMIN completa generación + descarga de un lote de prueba en menos de 2
  minutos en condiciones normales de red.
- **SC-002**: Un usuario ADMIN (u otro rol) no ve el ítem de menú ni obtiene datos de lotes QR
  (403).
- **SC-003**: Re-exportar con otros ajustes no altera los códigos del lote.
- **SC-004**: El 100% de los códigos exportados son escaneables y libres de PII.

## Assumptions

- La API de lotes QR en .NET (`/api/v1/qr/**`) permanece como backend; no se reintroduce UI
  server-side en Java.
- El límite máximo por lote lo define la configuración del servidor (`maxBatchSize`, hoy 10 000).
- Valores por defecto del formulario: cantidad 10, tamaño 3 cm, padding 0.5 cm, etiqueta abajo,
  bordes sí (paridad con Java).
- Dependencias: specs 002 (QR backend), 013 (usuarios/roles), 012 (auditoría).

## Out of Scope

- Cambiar formato `FOR-XXXXX` o añadir firma HMAC (ADR 0005).
- Escaneo de QR (specs 014/019).
- Permisos de inventario para roles distintos de SUPER_ADMIN sobre lotes QR.
