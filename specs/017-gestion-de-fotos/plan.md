# Implementation Plan: Captura y almacenamiento seguro de fotos

**Branch**: `017-gestion-de-fotos` | **Date**: 2026-07-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/017-gestion-de-fotos/spec.md`

## Summary

Reemplazar el campo "URL de foto" (texto) de los formularios de **elemento**, **tipo de prenda**
y **equipo** por una **captura de foto** (cámara nativa o subida de archivo, con vista previa),
respaldada por un **módulo `media`** en el backend que valida, sanea (EXIF stripping),
**cifra en reposo** (AES-256-GCM) y **sirve** las imágenes solo a usuarios autenticados y
autorizados, con **auditoría**. La foto de **elemento** es PII y recibe RBAC + enmascaramiento;
su habilitación queda condicionada a la base legal de ADR 0003. Decisión de almacenamiento:
[ADR 0017](../../docs/04-decisiones/0017-almacenamiento-de-fotos.md) (filesystem local cifrado
tras un `IFileStorage`).

> **Actualización (migración a .NET):** este plan se escribió para el backend Java (Spring Boot).
> El backend se migró a **ASP.NET Core** ([ADR 0016](../../docs/04-decisiones/0016-backend-aspnetcore.md));
> la feature se **implementó en `fornituras-api-dotnet/`**. Las secciones siguientes reflejan el
> stack .NET real. La equivalencia Spring→.NET no cambia el diseño (Ports & Adapters, RBAC, cifrado).

## Technical Context

**Language/Version**: Backend **C# / .NET 10 (ASP.NET Core Web API)**; Frontend TypeScript,
Ionic 8 + Angular (standalone components).

**Primary Dependencies**:
- Backend: ASP.NET Core MVC (multipart `IFormFile`), autenticación JWT Bearer + RBAC existente,
  **EF Core** (migraciones), reutiliza el cifrado AES-256-GCM de `PiiCipher`
  ([ADR 0006](../../docs/04-decisiones/0006-cifrado-pii-nivel-aplicacion.md), método de bytes
  añadido), **SixLabors.ImageSharp 2.1** (Apache-2.0) para re-codificar y eliminar EXIF/IPTC/XMP.
- Frontend: `@capacitor/camera` (nuevo) para cámara/galería nativa; `HttpClient` + interceptor
  de auth existente; Angular Reactive Forms.

**Storage**: Filesystem local del servidor, **fuera del repo**, objetos cifrados AES-256-GCM
(`IV ‖ ciphertext ‖ tag`); metadatos en **SQL Server** (tabla `media_asset`, migración EF Core
`AddMediaAsset`). Ver ADR 0017.

**Testing**: Backend **xUnit** (`dotnet test`) con EF Core InMemory + directorio temporal para el
storage; Frontend Jasmine/Karma (`npm test`).

**Target Platform**: API ASP.NET Core (Windows/servidor, path base `/sigefor`, puerto 8080);
app Ionic (web + móvil vía Capacitor).

**Project Type**: Web application (monorepo: `fornituras-api-dotnet/` backend + `sigefor/` frontend).

**Performance Goals**: Subida y previsualización de una foto percibida en < 1 s (SC-001) para
imágenes hasta el límite configurado; re-codificación server-side sin bloquear otras peticiones.

**Constraints**: Cifrado en reposo obligatorio; ningún acceso anónimo a imágenes; sin PII en
logs; validación estricta por contenido (magic bytes), rechazo de SVG; límite de peso
(objetivo 5 MB) y dimensiones configurables; EXIF eliminado.

**Scale/Scope**: 3 entidades con **una** foto cada una; volumen del orden del padrón/inventario
de una corporación (miles de fotos, no millones). Sin galería múltiple en esta versión.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cumplimiento en este plan |
|-----------|---------------------------|
| **I. Seguridad y privacidad primero** | Foto cifrada en reposo (AES-256-GCM), EXIF eliminado, validación estricta; foto de elemento tratada como PII. Cita `docs/02-seguridad.md` §8. ✅ |
| **II. QR nunca expone PII** | El QR no cambia; no referencia la foto ni datos personales (FR-012). ✅ |
| **III. Cero secretos en el repo** | Ruta de storage y clave de cifrado por variable de entorno; solo el **nombre** en `.env.example`. Storage fuera del repo. ✅ |
| **IV. Mínimo privilegio y autorización** | Endpoints de media autenticados + RBAC; `is_pii` → solo roles autorizados; validación en el borde, rechazo por defecto. ✅ |
| **V. Trazabilidad y auditoría sin fugas** | Auditoría de subida/visualización/exportación de foto de elemento; sin PII en logs (referencia por id). ✅ |
| **VI. ADR y stack congelado** | Decisión de almacenamiento en **ADR 0017**; backend en ASP.NET Core (**ADR 0016**); dependencias nuevas `@capacitor/camera` (frontend) y `SixLabors.ImageSharp` (Apache-2.0, saneo/EXIF) justificadas. ✅ |

**Resultado del gate**: PASA. Sin violaciones; no se requiere Complexity Tracking.

**Nota de gating (no es violación):** la captura de foto de **elemento** permanece
restringida hasta base legal confirmada (ADR 0003, FR-015); el mecanismo se construye igual.

## Project Structure

### Documentation (this feature)

```text
specs/017-gestion-de-fotos/
├── plan.md              # Este archivo
├── research.md          # Fase 0
├── data-model.md        # Fase 1
├── quickstart.md        # Fase 1
├── contracts/           # Fase 1 (contrato del endpoint de media)
├── checklists/
│   └── requirements.md  # Checklist de calidad de la spec
└── tasks.md             # Fase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
fornituras-api-dotnet/src/Fornituras.Api/
├── Controllers/MediaController.cs          # POST/GET/DELETE /media (autenticado)
├── Services/MediaService.cs                # orquesta validación + saneo + cifrado + persistencia + RBAC/gating
├── Services/ImageSanitizer.cs              # re-codifica y elimina EXIF (SixLabors.ImageSharp)
├── Services/FileStorage.cs                 # IFileStorage (puerto) + LocalEncryptedFileStorage (adaptador cifrado)
├── Data/Entities/MediaAsset.cs             # entidad + enum MediaContext (id GUID)
├── Data/Migrations/*_AddMediaAsset.cs      # migración EF Core (tabla media_asset)
├── Dto/MediaDtos.cs                        # MediaUploadResponse
├── Configuration/MediaOptions.cs           # App:Media (ruta, límites, gating officer)
├── Common/Crypto/PiiCipher.cs              # + EncryptBytes/DecryptBytes (reutiliza clave AES)
└── Security/RolePolicy.cs                  # + CanCaptureOfficerPhoto / CanViewOfficerPhoto

(entidades Officer/Equipment/CatalogItem ya tienen foto_url → guarda la referencia interna)

sigefor/src/app/
├── core/media/
│   ├── media.service.ts               # upload(file) → {id, url}
│   ├── photo-picker/photo-picker.component.ts   # cámara + archivo + preview (reutilizable)
│   └── secure-image/secure-image.component.ts   # carga blob autenticado → objectURL
└── features/
    ├── elementos/pages/elemento-form/           # reemplaza input fotoUrl por <app-photo-picker>
    ├── tipos/pages/tipo-form/
    └── fornituras/pages/fornitura-form/
```

**Structure Decision**: Web application (Opción 2). Backend .NET por capas (patrón existente
`Controllers/Services/Data/Dto/Security`); el módulo `media` es **genérico y reutilizable**
(Ports & Adapters vía `IFileStorage`) para no acoplarlo a ninguna entidad. Frontend con
componentes standalone reutilizables en `core/media/`.

## Complexity Tracking

> Sin violaciones de la constitución. No aplica.
