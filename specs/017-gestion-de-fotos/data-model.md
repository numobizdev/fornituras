# Data Model: Captura y almacenamiento seguro de fotos

Feature: `017-gestion-de-fotos` · Fecha: 2026-07-01

## Entidad nueva: `MediaAsset`

Representa una imagen almacenada de forma segura. La imagen **no** vive en esta tabla (vive
cifrada en el filesystem); aquí solo hay **metadatos**. La tabla **no contiene PII**.

Tabla `media_asset` (migración **EF Core `AddMediaAsset`**; implementada en `fornituras-api-dotnet/`):

| Columna         | Tipo (SQL Server)      | Notas |
|-----------------|------------------------|-------|
| `id`            | `UNIQUEIDENTIFIER`     | PK, identificador opaco (GUID generado por la app). Es lo que se referencia desde `foto_url`. |
| `storage_key`   | `NVARCHAR(200)`        | Referencia opaca al objeto en storage (ruta relativa `yyyy/MM/<guid>.enc`). Único. |
| `content_type`  | `NVARCHAR(60)`         | Tipo final tras re-codificar (`image/jpeg`, `image/png`, `image/webp`). |
| `size_bytes`    | `BIGINT`               | Tamaño del objeto saneado (antes de cifrar). |
| `is_pii`        | `BIT`                  | `1` si es foto de elemento (activa RBAC + auditoría reforzada). |
| `context`       | `NVARCHAR(20)`         | `EQUIPMENT` \| `EQUIPMENT_TYPE` \| `OFFICER` (enum como cadena). |
| `created_at`    | `DATETIME2`            | Alta. |
| `updated_at`    | `DATETIME2`            | Última actualización. |

> **Nota (implementación .NET vs diseño original):** el `iv`/nonce **no** es una columna: viaja
> **dentro** del objeto cifrado en disco (`IV ‖ ciphertext ‖ tag`). No se persisten `sha256` ni
> `uploaded_by`: la trazabilidad (quién subió/vio una foto PII) la lleva el módulo de **auditoría**
> por evento (`UPLOAD/VIEW/DELETE_OFFICER_PHOTO`), no una FK en esta tabla.

**Reglas de validación (previas a insertar):**
- `content_type` ∈ {`image/jpeg`, `image/png`, `image/webp`} — verificado por magic bytes/firma.
- `size_bytes` ≤ límite configurado (objetivo 5 MB); dimensiones ≤ límite configurado.
- Imagen **re-codificada** (sin EXIF/IPTC/XMP) antes de cifrar y guardar.
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
- El autor de la subida **no** se guarda en `media_asset`; queda en el evento de auditoría.
- La auditoría (módulo `audit` existente) registra eventos sobre `MediaAsset` con `is_pii = 1`
  (subida, visualización, exportación), sin duplicar aquí su esquema.

## Consideraciones de seguridad (resumen, ver spec §Requirements)

- Nada de PII en `media_asset`; la sensibilidad se marca con `is_pii` para gobernar acceso.
- El objeto en disco nunca está en claro; la clave vive en el entorno, no en BD ni repo.
- El `id` es opaco (UUID) → no enumerable/adivinable; el acceso siempre pasa por autorización.
