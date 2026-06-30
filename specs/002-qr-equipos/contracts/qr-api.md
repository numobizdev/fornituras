# Phase 1 — Contrato REST: API de QR

> Todos los endpoints requieren **autenticación (JWT)** y **autorización por rol**
> (constitución, Principio IV). Errores con cuerpo **genérico**, sin filtrar detalles internos
> (FR-005). Ninguna respuesta incluye PII ni la llave HMAC.

Base path: `/api/v1`

## 1. Generar QR de un equipo

`POST /equipment/{equipmentId}/qr`

- **Rol:** SUPERVISOR o ADMIN.
- **Comportamiento:** si el equipo no tiene QR, emite `opaqueId` (UUID v4) + firma con la
  versión de llave activa, persiste `qr_*` y audita `QR_GENERATE`. Si ya tiene QR, es
  **idempotente**: devuelve el existente (no genera otro) — usar reissue para reemitir.
- **201 Created** (o 200 si ya existía):

```json
{
  "equipmentId": "f1a2...",
  "payload": "v1.MWZhMi4uLg.QmFzZTY0dXJsSG1hYw",
  "keyVersion": 1,
  "issuedAt": "2026-06-29T18:20:00Z"
}
```

- **404** equipo inexistente · **409** equipo en estado no emisible (p. ej. baja) ·
  **403** sin rol · **401** sin sesión.

## 2. Obtener la imagen del QR

`GET /equipment/{equipmentId}/qr?format=png|svg&ecc=Q|H`

- **Rol:** SUPERVISOR o ADMIN (export es operación administrativa).
- **200 OK:** binario de la imagen (`image/png` o `image/svg+xml`) que codifica el `payload`.
- El contenido visual codifica **solo** el payload opaco+firmado.
- **404** si el equipo no tiene QR generado.

## 3. Reemitir QR (rotación / compromiso)

`POST /equipment/{equipmentId}/qr:reissue`

- **Rol:** ADMIN.
- **Comportamiento:** genera un **nuevo** `opaqueId` con la versión de llave activa, invalida
  el anterior y audita `QR_REISSUE`. Acción explícita (FR-006).
- **200 OK:** mismo cuerpo que (1) con el nuevo payload · **404** inexistente · **403** sin rol.

## 4. Verificar un QR

`POST /qr:verify`

- **Rol:** cualquier usuario autenticado (la resolución de datos del equipo es un endpoint
  aparte que aplica authz de lectura).
- **Request:**

```json
{ "payload": "v1.MWZhMi4uLg.QmFzZTY0dXJsSG1hYw" }
```

- **200 OK** (firma válida y opaqueId conocido):

```json
{ "valid": true, "equipmentId": "f1a2..." }
```

- **200 OK** (firma inválida, payload alterado o desconocido) — **respuesta uniforme** para no
  filtrar la causa (FR-005):

```json
{ "valid": false }
```

- **400** payload con formato ilegible.

> Nota: la **resolución completa** `QR → ficha de equipo/asignación` (con datos sujetos a rol)
> pertenece a la feature de **escaneo** y reutiliza la verificación de firma de aquí.

## Reglas transversales

- **Auditoría:** generate/reissue/export registran actor + timestamp + `equipmentId` (sin PII).
- **Rate limiting** en `qr:verify` para evitar sondeo por fuerza bruta.
- **Sin secretos en logs:** nunca se registran payloads firmados ni la llave.
- **Versionado de llave:** `verify` selecciona la llave por la `version` del payload, de modo
  que QR grabados con una versión anterior siguen verificando tras rotar (SC-004).
