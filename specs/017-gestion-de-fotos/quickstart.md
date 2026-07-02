# Quickstart / Validación: Captura y almacenamiento seguro de fotos

Feature: `017-gestion-de-fotos`. Guía para validar la feature de punta a punta. Detalles de
diseño en [plan.md](./plan.md), [data-model.md](./data-model.md) y
[contracts/media-api.md](./contracts/media-api.md).

## Prerrequisitos

- Backend levantado: desde `fornituras-api-dotnet/` → `dotnet run --project src/Fornituras.Api`
  (con la migración `AddMediaAsset` aplicada y `App:Media:*` configurado; `OfficerPhotoEnabled`
  gobierna el gating de la foto de elemento).
- Frontend levantado: desde `sigefor/` → `npm install` (incluye `@capacitor/camera`) y
  `npm start`.
- Un usuario con rol autorizado y otro sin rol de PII, para probar el enmascaramiento.

## Escenario 1 — Foto de equipo (no PII) · US1

1. Iniciar sesión, ir a **alta de equipo/fornitura**.
2. En el selector de foto, elegir **Subir archivo** con un `JPEG` válido → ver **vista previa**.
3. Guardar. Reabrir la ficha → la foto se muestra (cargada por endpoint autenticado).
4. **Esperado**: imagen visible; en el servidor, el objeto en el directorio de media está
   **cifrado** (no abre como imagen) y **sin EXIF**; hay una fila en `media_asset` con
   `is_pii = 0`.

## Escenario 2 — Captura por cámara y fallback · US1 / FR-004

1. En móvil (o navegador con cámara), elegir **Tomar foto** → capturar → vista previa → guardar.
2. En un entorno sin cámara o con permiso denegado → el botón de cámara no bloquea; se puede
   **Subir archivo**.
3. **Esperado**: ambos caminos producen una foto asociada.

## Escenario 3 — Rechazos de validación · FR-005/006/007

1. Intentar subir un **SVG** → **rechazado** (`400`).
2. Intentar subir un archivo no-imagen **renombrado** a `.jpg` → **rechazado** (magic bytes).
3. Intentar subir una imagen que **excede** el límite de peso/dimensiones → **rechazado**.
4. Subir un `JPEG` con **GPS en EXIF** → aceptado, pero la imagen almacenada **no** conserva EXIF.
5. **Esperado**: mensajes claros; nada inseguro llega a almacenarse.

## Escenario 4 — Foto de elemento (PII) y RBAC · US2

1. Con **rol autorizado**: subir la foto de un elemento → guardar → verla en la ficha.
2. Con **rol NO autorizado**: abrir la misma ficha → la foto aparece **enmascarada/oculta** y no
   es descargable.
3. Pedir `/media/{id}` de un asset PII **sin sesión** → **401**; con sesión sin rol → **403**.
4. **Esperado**: en la **auditoría** constan la subida y las visualizaciones (quién, qué
   elemento, cuándo); **ningún** log de aplicación contiene PII.

## Escenario 5 — Gating legal · FR-015

1. Con la captura de foto de elemento **restringida** (base legal ADR 0003 no confirmada):
   intentar subir foto de elemento → **403/deshabilitado**, mientras que equipos/tipos siguen
   funcionando.

## Escenario 6 — Transición del campo URL · US3 / FR-013

1. Abrir los tres formularios → **ninguno** pide una URL de texto (hay selector de foto).
2. Abrir una ficha que tuviera una URL previa → la pantalla **no** rompe.

## Pruebas automatizadas (referencia)

- **Backend** (desde `fornituras-api-dotnet/`): `dotnet test`. Tests xUnit que: saneen una imagen
  y verifiquen cifrado en disco + ausencia de EXIF; rechacen SVG/no-imagen/oversize; comprueben el
  gating y el RBAC de PII (upload y `GET` de un asset PII → `403` sin rol autorizado).
- **Frontend** (desde `sigefor/`): `npm test` para `MediaService`, `PhotoPickerComponent`
  (cámara/archivo/preview) y `SecureImageComponent` (carga blob autenticada).
