# Data Model: Captura y almacenamiento seguro de fotos

Feature: `017-gestion-de-fotos` · Fecha: 2026-07-01

## Entidad nueva: `MediaAsset`

Representa una imagen almacenada de forma segura. La imagen **no** vive en esta tabla (vive
cifrada en el filesystem); aquí solo hay **metadatos**. La tabla **no contiene PII**.

Tabla `media_asset` (migración `V25__create_media_asset.sql`):

| Columna         | Tipo (SQL Server)      | Notas |
|-----------------|------------------------|-------|
| `id`            | `UNIQUEIDENTIFIER`     | PK, identificador opaco (UUID). Es lo que se referencia desde `foto_url`. |
| `content_type`  | `VARCHAR(50)`          | Tipo final tras re-codificar (`image/jpeg`, `image/png`, `image/webp`). |
| `size_bytes`    | `BIGINT`               | Tamaño del objeto **cifrado** o del original saneado (definir en impl.). |
| `sha256`        | `CHAR(64)`             | Huella de la imagen saneada (deduplicación/integridad). |
| `storage_key`   | `VARCHAR(255)`         | Referencia opaca al objeto en storage (ruta relativa dentro del dir de media). |
| `iv`            | `VARBINARY(16)` / `VARCHAR` | Nonce/IV del cifrado AES-256-GCM del objeto. |
| `is_pii`        | `BIT`                  | `1` si es foto de elemento (activa RBAC + auditoría reforzada). |
| `uploaded_by`   | `BIGINT` / `UNIQUEIDENTIFIER` | FK lógica al usuario que subió (para auditoría). |
| `created_at`    | `DATETIME2`            | Alta. |

**Reglas de validación (previas a insertar):**
- `content_type` ∈ {`image/jpeg`, `image/png`, `image/webp`} — verificado por magic bytes.
- `size_bytes` ≤ límite configurado (objetivo 5 MB); dimensiones ≤ límite configurado.
- Imagen **re-codificada** (sin EXIF) antes de calcular `sha256`, cifrar y guardar.
- `storage_key` único; el objeto en disco está cifrado (`IV ‖ ciphertext ‖ tag`).

**Ciclo de vida / estados:**
- `sin asociar` (recién subida) → `asociada` (referenciada por una entidad) → `eliminada`
  (borrado + purga del objeto, conforme a retención/ARCO). Las `sin asociar` caducan (limpieza).

## Entidades existentes afectadas (sin cambio de esquema)

Las tres ya tienen `foto_url NVARCHAR(500)`. Cambia el **significado**: pasa a guardar una
referencia interna al `MediaAsset` (p. ej. el `id` o `"/api/v1/media/{id}"`), no una URL externa.

| Entidad         | Tabla       | Campo      | `is_pii` de su foto |
|-----------------|-------------|------------|---------------------|
| **Equipment**   | `equipment` | `foto_url` | No (no PII) |
| **EquipmentType** (catálogo) | catálogo de tipos | `foto_url` | No (no PII) |
| **Officer**     | `officers`  | `foto_url` | **Sí (PII)** → `is_pii = 1` al subir |

> Alternativa futura (no en esta versión): FK explícita `media_asset_id` por entidad para
> integridad referencial. Se puede añadir después sin romper el contrato REST.

## Relaciones

- Una entidad (equipo/tipo/elemento) referencia **como máximo un** `MediaAsset` (1–0..1).
- Un `MediaAsset` es creado por **un** usuario (`uploaded_by`) — usado por auditoría.
- La auditoría (módulo `audit` existente) registra eventos sobre `MediaAsset` con `is_pii = 1`
  (subida, visualización, exportación), sin duplicar aquí su esquema.

## Consideraciones de seguridad (resumen, ver spec §Requirements)

- Nada de PII en `media_asset`; la sensibilidad se marca con `is_pii` para gobernar acceso.
- El objeto en disco nunca está en claro; la clave vive en el entorno, no en BD ni repo.
- El `id` es opaco (UUID) → no enumerable/adivinable; el acceso siempre pasa por autorización.
