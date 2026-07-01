# 0016. Almacenamiento seguro de fotos (media assets)

- **Estado:** Aceptado
- **Fecha:** 2026-07-01
- **Relacionado:** [0003 Alcance de PII](./0003-pii-elementos.md) (Propuesto),
  [0006 Cifrado de PII a nivel de aplicación](./0006-cifrado-pii-nivel-aplicacion.md)
  (Aceptado, interino), spec [`017-gestion-de-fotos`](../../specs/017-gestion-de-fotos/spec.md)

## Contexto

Los formularios de **elemento**, **tipo de prenda** y **equipo** guardan hoy la foto como una
**URL de texto** (`foto_url NVARCHAR(500)`). Es poco usable (obliga a publicar la imagen en un
hosting externo) e inseguro (permite apuntar a cualquier URL arbitraria, sin control sobre PII).
La spec 017 pide capturar la foto con la **cámara** o **subir un archivo**, con un gestor de
archivos propio detrás.

La **foto del elemento** es PII de personal policial en México (Constitución, Principio I;
[`docs/02-seguridad.md`](../02-seguridad.md)): exige cifrado en reposo, acceso autorizado por
rol, enmascaramiento por defecto y auditoría. La solución objetivo de PII es Always Encrypted +
enclaves ([ADR 0004](./0004-busqueda-pii-cifrada.md)), **no disponible** en el entorno actual;
por eso [ADR 0006](./0006-cifrado-pii-nivel-aplicacion.md) cifra la PII a nivel de aplicación con
**AES-256-GCM** y dejó explícitamente la **foto fuera de alcance hasta resolver su storage
cifrado**. Este ADR resuelve ese pendiente.

Restricciones a considerar: no introducir infraestructura del cliente aún no confirmada (igual
que ADR 0006), no meter dependencias sin justificar (Principio VI), y no versionar secretos
(Principio III).

## Decisión

Crear un **módulo `media`** que reciba, valide, cifre, almacene y sirva imágenes, tras un
**puerto de almacenamiento** para poder cambiar el backend sin tocar el dominio (Ports &
Adapters).

1. **Backend de almacenamiento: filesystem local cifrado.** Los bytes se guardan en un
   directorio del servidor **fuera del repo**, cifrados con **AES-256-GCM** a nivel de
   aplicación (misma familia que [ADR 0006](./0006-cifrado-pii-nivel-aplicacion.md); se
   **reutiliza** el servicio de cifrado existente, no se crea cripto nueva). Formato por objeto:
   `IV ‖ ciphertext ‖ tag`. Ningún archivo queda en claro en disco.
2. **Puerto `FileStoragePort` + adaptador `LocalEncryptedFileStorage`.** El dominio depende del
   puerto; migrar más adelante a MinIO/Azure Blob es cambiar de adaptador, sin tocar controllers
   ni servicios. Se descartan por ahora object storage en nube (soberanía de datos de PII
   policial sin dictamen legal) y BLOB en SQL Server (peso/rendimiento y acopla la imagen al
   backup transaccional de la BD).
3. **Metadatos en BD, no la imagen.** Una tabla `media_asset` (migración Flyway nueva) guarda:
   identificador opaco (UUID), `content_type`, `size_bytes`, huella `sha256`, referencia opaca al
   objeto en storage (`storage_key`), el `iv`/nonce del cifrado, indicador `is_pii`, quién subió
   y cuándo. **La tabla no contiene PII.**
4. **Entrada validada en el borde.** Whitelist de tipos (`JPEG`, `PNG`, `WEBP`), verificación por
   **magic bytes** (no confiar en la extensión ni en el `Content-Type` declarado), **rechazo de
   `SVG`** (contenido activo/XSS), y límites configurables de **peso** y **dimensiones**.
5. **Sanitizado de la imagen.** Antes de cifrar y guardar, la imagen se **re-codifica** para
   **eliminar metadatos EXIF** (crítico: EXIF puede llevar GPS → fuga de ubicación) y normalizar
   el formato. Se prioriza `ImageIO` de la plataforma; si hace falta una librería para
   miniaturas/robustez (p. ej. Thumbnailator), se justifica licencia/mantenimiento en el plan.
6. **Servicio autenticado, autorizado y auditado.** Se sirve por endpoint bajo sesión válida.
   Para `is_pii = true` (foto de elemento): **RBAC con enmascaramiento por defecto** (solo roles
   autorizados la ven) y **auditoría** de subida, visualización y exportación (módulo de auditoría
   existente), sin escribir PII en logs (Principio V). No hay acceso anónimo ni URL adivinable
   (Principio IV).
7. **Referencia interna, no URL externa.** El campo `foto_url` de cada entidad pasa a guardar una
   **referencia interna opaca** al `media_asset` (no una URL de internet). Las URLs externas
   previas se toleran en lectura durante la transición (spec 017, FR-013).
8. **Ciclo de vida.** Las subidas no asociadas a ninguna ficha (huérfanas) se limpian; la foto se
   puede eliminar y se aplica retención/baja (derechos **ARCO** para la foto de elemento).
9. **Gating de PII.** La **captura de foto de elemento** permanece **deshabilitada/restringida**
   hasta que [ADR 0003](./0003-pii-elementos.md) confirme base legal y finalidad. La foto de
   **equipo** y **tipo** (no PII) se habilita ya.

## Actualización de ADRs relacionados

- **ADR 0006 §Consecuencias:** la foto **deja de estar fuera de alcance**; su storage cifrado
  queda resuelto por este ADR (filesystem local + AES-256-GCM tras `FileStoragePort`).
- **ADR 0003 §Decisión (1):** "foto en storage cifrado con acceso autorizado" se materializa aquí;
  el punto (4) —captura de foto **[PENDIENTE]** hasta base legal— **sigue vigente** y gobierna la
  habilitación en producción (este ADR provee el mecanismo, no la base legal).

## Consecuencias

- (+) Se cumple el cifrado en reposo de la foto **sin** depender de Always Encrypted/enclaves ni
  de infra del cliente; reutiliza el cifrado ya aprobado en ADR 0006.
- (+) El puerto desacopla el backend de almacenamiento: cambiar a MinIO/Azure es un adaptador
  nuevo, sin tocar el dominio (estilo LEGO / DIP).
- (+) EXIF stripping + validación por contenido + rechazo de SVG reducen fuga de ubicación y XSS.
- (+) RBAC + enmascaramiento + auditoría limitan el radio de daño de la foto PII.
- (−) El cifrado lo hace la aplicación: la clave vive en el proceso (gestor de secretos pendiente,
  igual que ADR 0006); aceptable como interino, no como final.
- (−) El filesystem local **no** está replicado/geo-distribuido; requiere incluir el directorio de
  media en la política de **backup cifrado** y de retención.
- (−) Re-codificar la imagen añade coste de CPU en la subida y puede degradar levemente la calidad
  (aceptable frente al beneficio de eliminar metadatos).

## Reversión / camino a la solución final

Si se confirma object storage (MinIO on-prem o Azure Blob con dictamen de soberanía), se añade un
adaptador `FileStoragePort` nuevo y se migran los objetos; metadatos, RBAC, enmascaramiento,
auditoría y contrato de la API permanecen igual. Cuando exista gestor de secretos, la clave de
cifrado se mueve allí sin cambiar el formato de los objetos.
