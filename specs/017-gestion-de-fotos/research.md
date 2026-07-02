# Research: Captura y almacenamiento seguro de fotos

Feature: `017-gestion-de-fotos` · Fecha: 2026-07-01

Este documento resuelve las incógnitas técnicas del plan. Las decisiones se apoyan en la
exploración del código existente y en [ADR 0017](../../docs/04-decisiones/0017-almacenamiento-de-fotos.md).

## 1. Backend de almacenamiento

- **Decisión**: Filesystem local del servidor, objetos cifrados AES-256-GCM, tras un puerto
  `FileStoragePort`. Metadatos en tabla `media_asset` (SQL Server).
- **Rationale**: On-prem, sin infra de nube ni dictamen de soberanía pendiente; reutiliza el
  cifrado ya aprobado (ADR 0006). El puerto deja abierta la migración a MinIO/Azure.
- **Alternativas**: BLOB en SQL Server (infla BD y backup, descartado); object storage en nube
  (soberanía de PII sin base legal, descartado por ahora). Ver ADR 0017.

## 2. Cifrado en reposo

- **Decisión**: Reutilizar el servicio de cifrado **AES-256-GCM** existente del módulo de PII
  (introducido en ADR 0006 como `EncryptedStringConverter` / servicio asociado). Para archivos
  se cifra el flujo de bytes completo con formato `IV ‖ ciphertext ‖ tag`.
- **Rationale**: No crear cripto nueva (Principio VI y buenas prácticas); consistencia con la PII
  ya cifrada. Clave desde entorno (`PII_ENCRYPTION_KEY` u otra variable dedicada de media, a
  decidir en implementación; documentar solo el nombre).
- **Acción de implementación**: verificar el **nombre real** de la clase/servicio de cifrado en
  `modules/officers` (o `config`/`common`) antes de reutilizarlo; extraer un método de bytes si
  hoy solo cifra `String`.
- **Alternativas**: cifrado de disco/TDE del SO (no cubre acceso lógico ni portabilidad;
  complementario, no sustituto).

## 3. Recepción de la subida (multipart)

- **Decisión**: Endpoint `POST /media` que recibe `multipart/form-data` con la parte `image`
  (`MultipartFile`) más metadatos mínimos (p. ej. `context`: equipo/tipo/elemento para marcar
  `is_pii`).
- **Rationale**: Patrón estándar de Spring MVC; el interceptor de auth del frontend ya añade el
  Bearer token. Límite de tamaño configurado en `spring.servlet.multipart.max-file-size`.
- **Alternativas**: base64 en JSON (mayor overhead y memoria; descartado).

## 4. Validación de contenido

- **Decisión**: (a) whitelist de `Content-Type` **y** verificación por **magic bytes**
  (`JPEG` FF D8 FF, `PNG` 89 50 4E 47, `WEBP` RIFF…WEBP); (b) **rechazo de SVG** y de cualquier
  tipo fuera de la whitelist; (c) límite de peso (objetivo 5 MB) y de dimensiones máximas.
- **Rationale**: No confiar en extensión ni en `Content-Type` declarado (edge cases de la spec).
  SVG puede contener scripts → XSS al servirlo.
- **Alternativas**: validar solo por extensión (inseguro, descartado).

## 5. Sanitizado y eliminación de EXIF

- **Decisión**: **Re-codificar** la imagen con `ImageIO` (leer a `BufferedImage` y volver a
  escribir) → produce una imagen **sin metadatos EXIF** y en formato normalizado. Opcional:
  generar una miniatura para vistas de lista.
- **Rationale**: EXIF puede incluir GPS → fuga de ubicación (FR-007). Re-codificar es la forma
  más simple y robusta de garantizar que no quedan metadatos.
- **Acción**: si `ImageIO` no cubre bien `WEBP` o miniaturas, evaluar **Thumbnailator** (licencia
  MIT, mantenida) y justificar la dependencia en el plan (Principio VI).
- **Alternativas**: parsear y borrar solo EXIF (frágil, deja otros metadatos; descartado).

## 6. Servicio autenticado + autorización PII

- **Decisión**: `GET /media/{id}` bajo sesión válida. Si `is_pii = true`, se exige rol autorizado
  (matriz RBAC de [ADR 0013](../../docs/04-decisiones/0013-expansion-de-roles.md)); no autorizado
  → 403/enmascarado. Cada acceso a foto de elemento se registra en **auditoría** (módulo `audit`,
  patrón append-only de [ADR 0012](../../docs/04-decisiones/0012-inmutabilidad-y-retencion-auditoria.md)).
- **Rationale**: Principios IV y V; enmascaramiento por defecto de ADR 0003.
- **Acción**: identificar los roles que pueden ver PII de foto (reutilizar la matriz de 013).

## 7. Visualización segura en el frontend

- **Decisión**: `<img src>` **no** envía el header `Authorization`. Se usa un
  `SecureImageComponent` que descarga la imagen vía `HttpClient` (con token, como `blob`) y crea
  un `objectURL` para el `<img>`; se revoca el objectURL al destruir el componente.
- **Rationale**: El endpoint de media requiere Bearer token; el patrón blob es el estándar en
  Angular para recursos protegidos. Evita exponer URLs firmadas de larga vida.
- **Alternativas**: URLs firmadas temporales (más complejidad de servidor; no necesarias con el
  patrón blob para el caso actual).

## 8. Captura por cámara (Capacitor + fallback web)

- **Decisión**: `@capacitor/camera` (`Camera.getPhoto`) para tomar foto o elegir de galería en
  móvil; en web, el plugin ofrece PWA elements o se usa `input[type=file]`/`getUserMedia` como
  fallback. El resultado se normaliza a un `Blob`/`File` que consume `MediaService.upload`.
- **Rationale**: Es el plugin oficial de Capacitor (ya se usa Capacitor en el proyecto), buena
  mantenibilidad y licencia MIT. El proyecto ya tiene precedente de cámara web para QR.
- **Acción**: añadir `@capacitor/camera` a `package.json`; gestionar permisos y el caso de
  permiso denegado (fallback a subir archivo, FR-004).
- **Alternativas**: solo `input[type=file]` (pierde la captura nativa fluida en móvil; el usuario
  pidió cámara explícitamente).

## 9. Integración con `foto_url` existente

- **Decisión**: Conservar la columna `foto_url` pero pasar a guardar una **referencia interna**
  (`/media/{id}` o el id opaco). En lectura se tolera una URL externa previa (transición,
  FR-013).
- **Rationale**: Cambio mínimo de esquema; las tres entidades ya tienen el campo.
- **Alternativas**: FK dedicada `media_asset_id` en cada entidad (más integridad referencial pero
  más migraciones); se puede adoptar después sin romper el contrato.

## 10. Ciclo de vida y huérfanas

- **Decisión**: Un `media_asset` recién subido queda "sin asociar" hasta que el alta/edición lo
  referencia; una tarea de limpieza (o borrado en el mismo flujo de guardado) evita acumular
  huérfanas (FR-016). Borrado de foto respeta retención/ARCO (FR-014).
- **Rationale**: Evita fugas por acumulación y cumple minimización.

## Riesgos y mitigaciones

- **Clave de cifrado en el proceso** (heredado de ADR 0006): aceptable como interino; gestor de
  secretos pendiente.
- **Coste de CPU** al re-codificar: aceptable por volumen; límites de tamaño acotan el peor caso.
- **Backup del directorio de media**: incluirlo en la política de backup cifrado (ADR 0017).
