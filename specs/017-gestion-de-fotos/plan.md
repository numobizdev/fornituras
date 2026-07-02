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
tras un `FileStoragePort`).

## Technical Context

**Language/Version**: Backend Java 17 + Spring Boot (Maven wrapper); Frontend TypeScript,
Ionic 8 + Angular (standalone components).

**Primary Dependencies**:
- Backend: Spring Web (multipart), Spring Security (authz existente), Flyway (migraciones),
  reutiliza el servicio de cifrado AES-256-GCM de [ADR 0006](../../docs/04-decisiones/0006-cifrado-pii-nivel-aplicacion.md),
  `javax.imageio`/`ImageIO` para re-codificar y eliminar EXIF (evaluar Thumbnailator solo si
  hace falta; justificar por Principio VI).
- Frontend: `@capacitor/camera` (nuevo) para cámara/galería nativa; `HttpClient` + interceptor
  de auth existente; Angular Reactive Forms.

**Storage**: Filesystem local del servidor, **fuera del repo**, objetos cifrados AES-256-GCM;
metadatos en **SQL Server** (tabla `media_asset`, migración Flyway nueva). Ver ADR 0017.

**Testing**: Backend `@SpringBootTest` + MockMvc sobre H2 (patrón
[ADR 0009](../../docs/04-decisiones/0009-tests-integracion-h2-mockmvc.md)); Frontend Jasmine/Karma
(`npm test`).

**Target Platform**: API Spring Boot (Windows/servidor); app Ionic (web + móvil vía Capacitor).

**Project Type**: Web application (monorepo: `fornituras-api/` backend + `sigefor/` frontend).

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
| **VI. ADR y stack congelado** | Decisión de almacenamiento en **ADR 0017**; dependencia nueva `@capacitor/camera` justificada; sin cambio de stack. ✅ |

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
fornituras-api/src/main/java/com/numobiz/solutions/fornituras/
├── modules/media/                     # NUEVO módulo
│   ├── controller/MediaController.java
│   ├── service/MediaService.java              # orquesta validación + saneo + cifrado + persistencia
│   ├── service/ImageSanitizer.java            # re-codifica y elimina EXIF
│   ├── service/FileStoragePort.java           # puerto (interfaz)
│   ├── service/LocalEncryptedFileStorage.java # adaptador filesystem cifrado (AES-256-GCM)
│   ├── repository/MediaAssetRepository.java
│   ├── entity/MediaAsset.java
│   └── dto/{MediaUploadResponse.java, ...}
├── config/MediaProperties.java        # app.media.* (ruta, límites, tipos permitidos)
└── modules/{officers,equipment,catalog}/  # integración: foto_url → referencia interna

fornituras-api/src/main/resources/
├── db/migration/V25__create_media_asset.sql
└── application.yml                    # app.media.storage-path, límites (por env)

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

**Structure Decision**: Web application (Opción 2). Backend por módulos (patrón existente
`controller/service/repository/entity/dto`); el módulo `media` es **genérico y reutilizable**
(Ports & Adapters) para no acoplarlo a ninguna entidad. Frontend con componentes standalone
reutilizables en `core/media/`.

## Complexity Tracking

> Sin violaciones de la constitución. No aplica.
