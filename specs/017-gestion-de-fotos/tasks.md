---
description: "Task list — 017 Captura y almacenamiento seguro de fotos"
---

# Tasks: Captura y almacenamiento seguro de fotos

**Input**: Design documents from `/specs/017-gestion-de-fotos/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/media-api.md

**Tests**: SÍ se incluyen tareas de test — el proyecto exige pruebas de authz/cifrado/auditoría
(constitución §Flujo; ADR 0009 tests H2/MockMvc). Son obligatorias, no opcionales aquí.

**Organization**: por historia de usuario (US1, US2, US3) para entrega incremental.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede correr en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: US1/US2/US3
- Rutas relativas a la raíz del monorepo (`fornituras-api/`, `sigefor/`)

> **Actualización (migración a .NET):** las rutas Java de abajo son **históricas**. La feature se
> implementó en el backend **ASP.NET Core** (`fornituras-api-dotnet/`, ver [ADR 0016] y plan.md):
> el módulo `media` (T005–T018, T024–T030) vive en `Controllers/`, `Services/`, `Data/`, `Dto/`,
> `Configuration/`; migración EF `AddMediaAsset` (no Flyway V25); saneo con SixLabors.ImageSharp;
> tests xUnit (`dotnet test`). El frontend (T019–T023, T031–T032) sí está en `sigefor/` como se
> describe. La verificación de que nada se perdió en la migración se cubre en la spec
> `018-auditoria-migracion-dotnet`.

Paths base (histórico — backend Java, sustituido por `fornituras-api-dotnet/`):
`api = fornituras-api/src/main/java/com/numobiz/solutions/fornituras`
`res = fornituras-api/src/main/resources`
`itest = fornituras-api/src/test/java/com/numobiz/solutions/fornituras`
`fe = sigefor/src/app`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Preparar dependencias y configuración del módulo de media.

- [X] T001 Crear la rama `017-gestion-de-fotos` desde `dev` (una rama por spec; conservar tras merge).
- [X] T002 [P] Añadir `@capacitor/camera` a `sigefor/package.json` y correr `npm install`; registrar permisos de cámara en config de Capacitor.
- [X] T003 [P] Añadir `MediaProperties` en `api/config/MediaProperties.java` (`app.media.storage-path`, `max-size`, `max-width`, `max-height`, tipos permitidos) y sus claves en `res/application.yml` leyendo de entorno; documentar los **nombres** en `.env.example`.
- [X] T004 [P] Configurar límites de multipart en `res/application.yml` (`spring.servlet.multipart.max-file-size` / `max-request-size`) acordes al límite de peso.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infraestructura del módulo `media` que todas las historias necesitan.

**⚠️ CRITICAL**: Ninguna historia puede completarse hasta terminar esta fase.

- [ ] T005 Crear migración `res/db/migration/V25__create_media_asset.sql` con la tabla `media_asset` (ver data-model.md).
- [ ] T006 [P] Crear entidad `api/modules/media/entity/MediaAsset.java` y `api/modules/media/repository/MediaAssetRepository.java`.
- [ ] T007 Verificar el **nombre real** del servicio de cifrado AES-256-GCM existente (introducido en ADR 0006) y, si solo cifra `String`, extraer/añadir un método para cifrar/descifrar **bytes**; documentar dónde vive.
- [ ] T008 Definir el puerto `api/modules/media/service/FileStoragePort.java` (store/load/delete de bytes por `storage_key`).
- [ ] T009 [US-] Implementar el adaptador `api/modules/media/service/LocalEncryptedFileStorage.java`: escribe/lee objetos cifrados (`IV ‖ ciphertext ‖ tag`) bajo `app.media.storage-path`, fuera del repo (reutiliza T007).
- [ ] T010 [P] Implementar `api/modules/media/service/ImageSanitizer.java`: valida magic bytes (JPEG/PNG/WEBP), rechaza SVG/otros, valida dimensiones, **re-codifica con ImageIO** eliminando EXIF; devuelve bytes saneados + content-type.
- [ ] T011 Implementar `api/modules/media/service/MediaService.java` orquestando validación → saneo (T010) → cifrado+persistencia (T009) → alta en `media_asset`; y `load(id)` con descifrado. Marca `is_pii` según `context`.
- [ ] T012 Configurar autorización de `/media/**` en la config de Spring Security (autenticado siempre; sin acceso anónimo) y asegurar que NO queda en `permitAll`.

**Checkpoint**: Base de media lista — pueden empezar las historias.

---

## Phase 3: User Story 1 - Foto de equipo/tipo (Priority: P1) 🎯 MVP

**Goal**: Capturar/subir foto de **equipo** y **tipo** (no PII), con vista previa, y verla.

**Independent Test**: Alta de equipo subiendo/tomando una foto → se guarda → se ve en la ficha;
el objeto en disco está cifrado y sin EXIF; fila en `media_asset` con `is_pii = 0`.

### Tests for User Story 1 ⚠️

- [ ] T013 [P] [US1] Test de integración `itest/modules/media/MediaUploadIT.java` (H2/MockMvc): subir JPEG válido → 200 + fila `media_asset`; verificar que el objeto guardado NO es la imagen en claro y NO tiene EXIF.
- [ ] T014 [P] [US1] Test `itest/modules/media/MediaValidationIT.java`: rechazo (400) de SVG, de archivo no-imagen renombrado y de imagen sobre-dimensionada/oversize (413).
- [ ] T015 [P] [US1] Test `itest/modules/media/MediaDownloadIT.java`: `GET /media/{id}` de asset no-PII autenticado → 200 con content-type correcto; sin sesión → 401.

### Implementation for User Story 1

- [ ] T016 [US1] Implementar `api/modules/media/controller/MediaController.java`: `POST /media` (multipart `image` + `context`) y `GET /media/{id}`; DTOs en `api/modules/media/dto/`.
- [ ] T017 [US1] Integrar en `api/modules/equipment/**`: al guardar equipo, `foto_url` acepta la referencia interna del media; lectura tolera URL previa (FR-013).
- [ ] T018 [US1] Integrar en el flujo de catálogo de tipos (`api/modules/catalog/**`) para la foto de tipo de prenda.
- [ ] T019 [P] [US1] Frontend: `fe/core/media/media.service.ts` con `upload(file) → {id,url}` (multipart, usa interceptor de auth).
- [ ] T020 [P] [US1] Frontend: `fe/core/media/photo-picker/photo-picker.component.ts` (standalone, reutilizable): botones Tomar foto (`@capacitor/camera`) / Elegir archivo, **vista previa**, quitar/reemplazar; fallback web si no hay cámara (FR-004). Usar la skill `ui-ux-pro-max`.
- [ ] T021 [P] [US1] Frontend: `fe/core/media/secure-image/secure-image.component.ts`: descarga blob autenticada → `objectURL`, revoca al destruir.
- [ ] T022 [US1] Reemplazar el input "URL de foto" por `<app-photo-picker>` en `fe/features/fornituras/pages/fornitura-form/` y en `fe/features/tipos/pages/tipo-form/`; al guardar, subir primero y setear `fotoUrl` con la referencia. Mostrar la foto con `<app-secure-image>`.
- [ ] T023 [P] [US1] Tests frontend (`npm test`) de `MediaService`, `PhotoPickerComponent` y `SecureImageComponent`.

**Checkpoint**: US1 funcional e independiente (fotos de equipo y tipo).

---

## Phase 4: User Story 2 - Foto de elemento (PII) con RBAC + auditoría (Priority: P1)

**Goal**: Misma captura para **elemento**, pero con `is_pii`, RBAC + enmascaramiento y auditoría;
habilitación condicionada a base legal (ADR 0003).

**Independent Test**: rol autorizado sube/ve foto de elemento; rol no autorizado la ve
enmascarada; accesos auditados; sin PII en logs.

### Tests for User Story 2 ⚠️

- [ ] T024 [P] [US2] Test `itest/modules/media/MediaPiiAuthzIT.java`: `GET /media/{id}` de asset `is_pii=1` → 200 con rol autorizado; 403 con rol no autorizado; 401 sin sesión.
- [ ] T025 [P] [US2] Test `itest/modules/media/MediaAuditIT.java`: subida y visualización de foto de elemento generan registro de auditoría; verificar que NO se loguea PII.
- [ ] T026 [P] [US2] Test `itest/modules/media/MediaGatingIT.java`: con la captura de foto de elemento restringida (flag/gating ADR 0003) → `POST /media` con `context=officer` responde 403.

### Implementation for User Story 2

- [ ] T027 [US2] Autorización PII en `MediaController`/`MediaService`: para `is_pii=1`, exigir rol autorizado (matriz RBAC ADR 0013); enmascaramiento por defecto.
- [ ] T028 [US2] Registrar en el módulo `audit` los eventos de subida/visualización/exportación de assets PII (quién, qué elemento, cuándo), sin PII en logs (Principio V).
- [ ] T029 [US2] Añadir el **gate** configurable de captura de foto de elemento (deshabilitada/restringida hasta base legal ADR 0003 confirmada) — FR-015.
- [ ] T030 [US2] Integrar en `api/modules/officers/**`: `foto_url` del elemento guarda referencia interna con `is_pii=1`.
- [ ] T031 [US2] Frontend: `<app-photo-picker>` en `fe/features/elementos/pages/elemento-form/`; ocultar/deshabilitar el control si el gate está activo o el rol no autoriza; mostrar foto con `<app-secure-image>` (enmascarada si no autorizado).

**Checkpoint**: US1 y US2 funcionan de forma independiente.

---

## Phase 5: User Story 3 - Reemplazo del campo URL y transición (Priority: P2)

**Goal**: Ningún formulario pide URL de texto; las fichas con URL previa no rompen.

**Independent Test**: abrir los 3 formularios (sin campo URL) y una ficha con URL antigua (sin
error).

- [ ] T032 [P] [US3] Confirmar que los 3 form pages ya no exponen input de URL (limpieza de plantillas/validadores `fotoUrl` de texto en `elemento-form`, `tipo-form`, `fornitura-form`).
- [ ] T033 [US3] `SecureImageComponent`/servicio: resolver tanto referencia interna (`/media/{id}`) como URL externa previa en lectura (FR-013), sin romper la vista.
- [ ] T034 [P] [US3] Limpieza de huérfanas (FR-016): purgar media `sin asociar` (borrado en el flujo de guardado o tarea de limpieza) + borrado de foto respetando retención/ARCO (FR-014).

**Checkpoint**: transición completa.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T035 [P] Ejecutar el **checklist de seguridad** `docs/02-seguridad.md` §8 sobre el cambio y registrar el resultado.
- [ ] T036 [P] Documentar en `.env.example` los nombres de variables de media (ruta y, si aplica, clave dedicada) sin valores.
- [ ] T037 Añadir el directorio de media a la política de **backup cifrado** / retención (nota operativa, ver ADR 0017).
- [ ] T038 Ejecutar la validación de `quickstart.md` de punta a punta (escenarios 1–6).
- [ ] T039 [P] Correr `.\mvnw.cmd test` (backend) y `npm test` (frontend); dejar verde.

---

## Dependencies & Execution Order

- **Setup (Ph1)** → **Foundational (Ph2)** bloquea todo lo demás.
- **US1 (Ph3)** y **US2 (Ph4)** dependen de Ph2; US2 reutiliza el controller/servicio de US1
  (T016) → conviene US1 antes que US2, aunque son testables por separado.
- **US3 (Ph5)** depende de que US1/US2 hayan movido los formularios al picker.
- **Polish (Ph6)** al final.

### Within each story
- Tests primero (deben fallar) → modelos → servicios → endpoints → frontend → integración.

### Parallel Opportunities
- Setup: T002/T003/T004 en paralelo.
- Foundational: T006 y T010 en paralelo; T009/T011 tras T007/T008.
- US1: los tests T013–T015 en paralelo; frontend T019/T020/T021 en paralelo.
- US2: tests T024–T026 en paralelo.

---

## Implementation Strategy

### MVP First (US1)
1. Ph1 Setup → 2. Ph2 Foundational (crítico) → 3. Ph3 US1 → **validar** fotos de equipo/tipo → demo.

### Incremental Delivery
US1 (equipo/tipo, no PII) → US2 (elemento PII, gated) → US3 (transición). Cada historia agrega
valor sin romper la anterior.

---

## Notes

- **Bloqueante legal**: US2 no se habilita en producción hasta confirmar base legal/finalidad
  (ADR 0003). El pipeline se construye igual, tras el gate T029.
- Cada tarea que toca datos/authz/almacenamiento debe respetar la constitución (I, IV, V) y el
  checklist de `docs/02-seguridad.md`.
- Commit por tarea o grupo lógico; mensajes en español, presente imperativo.
