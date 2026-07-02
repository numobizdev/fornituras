# Contrato de API: módulo `media`

Feature: `017-gestion-de-fotos` · Base: `/sigefor/api/v1` · Todos los endpoints requieren
**autenticación** (Bearer JWT). Respuestas envueltas en el `ApiResponse<T>` estándar del
proyecto (`{ success, message, data }`), salvo el binario de descarga.

## POST `/media`

Sube una imagen: valida, sanea (EXIF stripping), cifra y almacena. Devuelve la referencia.

- **Content-Type**: `multipart/form-data`
- **Autorización**: usuario autenticado. Si `context = officer`, además rol autorizado para PII
  (matriz ADR 0013) y **gating** de ADR 0003 (si la captura de foto de elemento está
  restringida, responde `403`).
- **Partes**:
  | Parte     | Tipo   | Requerido | Notas |
  |-----------|--------|-----------|-------|
  | `image`   | file   | sí        | `JPEG`/`PNG`/`WEBP`; ≤ límite de peso/dimensiones. |
  | `context` | text   | sí        | `equipment` \| `equipment_type` \| `officer` → fija `is_pii`. |

- **200 OK**:
  ```json
  { "success": true, "message": "Foto subida", "data": { "id": "<uuid>", "url": "/api/v1/media/<uuid>", "contentType": "image/jpeg" } }
  ```
- **Errores**:
  | Código | Caso |
  |--------|------|
  | `400`  | No es imagen / tipo no permitido / SVG / magic bytes no coinciden / excede dimensiones. |
  | `401`  | Sin autenticación. |
  | `403`  | Sin rol para PII, o captura de foto de elemento restringida (gating ADR 0003). |
  | `413`  | Excede el tamaño máximo de subida. |
  | `422`  | Imagen corrupta / no re-codificable. |

## GET `/media/{id}`

Descarga/streamea la imagen descifrada, si el solicitante está autorizado.

- **Autorización**: usuario autenticado. Si el asset es `is_pii = true`, exige rol autorizado;
  en caso contrario **403** (enmascaramiento por defecto). Cada acceso a asset PII se **audita**.
- **200 OK**: cuerpo binario con `Content-Type` del asset (`image/jpeg|png|webp`); cabeceras de
  cache privadas/no-store para PII.
- **Errores**: `401` sin sesión; `403` sin rol para PII; `404` id inexistente.

## DELETE `/media/{id}`  *(o vía el guardado de la entidad)*

Elimina la foto y purga el objeto conforme a retención/ARCO.

- **Autorización**: rol con permiso de edición sobre la entidad dueña; para PII, además rol
  autorizado. Evento auditado.
- **204 No Content** al eliminar; `404` si no existe.

## Integración con las entidades

Los endpoints existentes **no cambian su forma**, solo el significado de `fotoUrl`:

- `POST /officers`, `POST/PUT /equipment` y el flujo de catálogo de tipos aceptan en `fotoUrl`
  la **referencia interna** devuelta por `POST /media` (`"/api/v1/media/<uuid>"`), en vez de una
  URL externa.
- En lectura, `fotoUrl` puede contener una referencia interna o (transición) una URL previa;
  el frontend resuelve ambas (FR-013).

## Notas de seguridad (contrato)

- Sin acceso anónimo a `/media/**` (contrasta con la brecha conocida de `/qr/**`, fuera de
  alcance de esta feature).
- IDs opacos (UUID) no enumerables; la autorización se evalúa **siempre**, no solo por poseer el
  id.
- Nunca se devuelven metadatos EXIF (se eliminaron al subir).
