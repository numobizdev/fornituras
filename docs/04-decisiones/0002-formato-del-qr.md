# 0002. Formato del QR: identificador opaco UUID + firma HMAC

- **Estado:** Aceptado
- **Fecha:** 2026-06-29

## Contexto

Cada equipo de blindaje llevará un QR **grabado o impreso** sobre el equipo físico (ver
[`Planeacion.md`](../../Planeacion.md) y la feature [`specs/002-qr-equipos`](../../specs/002-qr-equipos/spec.md)).
Por tratarse de un soporte físico, el formato del QR es **prácticamente irreversible**: un
cambio posterior obligaría a re-grabar el equipo. El cliente necesita empezar a generar QR
cuanto antes, así que esta decisión es **bloqueante** y debe fijarse antes del grabado en
serie.

El principio rector (constitución, Principio II; [`docs/02-seguridad.md`](../02-seguridad.md) §2)
exige que **el QR nunca contenga datos personales ni información explotable**.

## Decisión

El QR contiene un **identificador opaco + una firma**, sin ningún dato del equipo o del
elemento (**opción A: UUID opaco + lookup en servidor**):

1. **Identificador opaco:** `UUID v4` generado por el servidor, único por equipo, sin
   significado ni datos derivables.
2. **Firma:** `HMAC-SHA256` sobre el identificador, que prueba que el QR lo emitió el sistema
   y detecta alteraciones.
3. **Versionado de llave:** el payload incluye un **identificador de versión de llave** para
   permitir **rotación** sin invalidar los QR ya grabados (la verificación elige la llave por
   su versión).
4. **Codificación del payload (compacta y estable):**
   `v<version>.<base64url(uuid)>.<base64url(hmac)>` — solo ASCII seguro para QR, sin PII.
5. **Resolución:** la relación `QR → equipo → asignación` se resuelve **solo en el servidor**,
   tras verificar firma + sesión + rol. El QR por sí solo es inútil fuera del sistema.
6. **Llave HMAC:** vive en el gestor de secretos (pendiente de definir en un ADR aparte) o, en
   local, en variable de entorno; **nunca** se versiona (constitución, Principio III).

## Alternativas consideradas

- **Opción B — Token autocontenido firmado** (datos mínimos del equipo dentro del QR, p. ej.
  JWT compacto): permitiría resolver **offline** sin consultar el servidor, pero **expone más
  superficie** (más información viaja en el soporte físico), complica la rotación de llaves y
  riesga filtrar datos si el esquema cambia. Descartada por contradecir la minimización y el
  Principio II. Si en el futuro se requiere operación offline, se evaluará en un nuevo ADR.
- **UUID sin firma** (solo identificador opaco): más simple, pero un atacante podría
  **falsificar** identificadores válidos y sondear el sistema. Descartada: la firma es lo que
  hace al QR confiable.
- **Hash del número de serie como identificador:** descartada; un hash de un espacio de series
  conocido/pequeño es **adivinable** y ataría el QR a un dato sensible.

## Consecuencias

- (+) Un equipo fotografiado en la calle **no filtra** a quién pertenece: el QR es opaco.
- (+) La firma detecta QR inventados o alterados (integridad y autenticidad).
- (+) El versionado de llave permite **rotación** sin re-grabar equipos.
- (+) Formato estable y definitivo: se puede grabar en serie sin retrabajo.
- (−) La resolución **requiere conexión** al servidor (no hay modo offline). Aceptable para el
  caso de uso (consulta autenticada).
- (−) Introduce dependencia operativa del **gestor de secretos** para custodiar/rotar la llave
  HMAC (decisión pendiente en ADR aparte).

## Decisiones relacionadas / pendientes

- Gestor de secretos donde reside la llave HMAC (ADR pendiente).
- Mecanismo de tokens de sesión JWT HS256 vs RS256 (ADR pendiente, decisión independiente).
