# 0015. Landing configurable (dos caras), endpoint público no-PII y tour guiado

- **Estado:** **Aceptado**
- **Fecha:** 2026-07-01
- **Feature:** [016-landing-configurable](../../specs/016-landing-configurable/) (tarea T001)

> **Nota de numeración:** la spec 016 preveía este ADR como `0011` y la migración como `V19`.
> Ambos números fueron ocupados por features anteriores fusionadas antes que 016
> ([0011 = librería de export Excel](0011-libreria-export-excel.md);
> `V19__create_incident.sql` de 008). Este ADR toma el siguiente libre (**0015**) y la migración de
> landing pasa a **`V23__create_landing.sql`**. El resto de la spec se implementa tal cual.

## Contexto

El feature 016 introduce un **motor de contenido configurable** con dos caras —landing pública
pre-login y home post-login— editable por rol ADMIN desde la app, más un **tour guiado** del home la
primera vez. Tres decisiones requieren registro (Principios I y VI):

1. **Alta de dependencia** para el tour (Constitución VI: toda dependencia se justifica).
2. **Exposición de un endpoint público sin autenticación** (la primera superficie sin auth del
   sistema fuera del login): riesgo de fuga de PII y de abuso/enumeración (Principio I).
3. **Contenido editable que se muestra a otros usuarios**: riesgo de **stored XSS** si un ADMIN
   (o una cuenta ADMIN comprometida) inyecta scripts.

## Decisión

1. **Dependencia `driver.js ^1.6.0`** (frontend) para el tour guiado:
   - **Necesidad:** recorrido guiado por pasos con resaltado de zonas (FR-011/FR-012); reimplementarlo
     a mano es superficie innecesaria.
   - **Licencia:** **MIT** (compatible).
   - **Mantenimiento:** activa y publicada recientemente; ~5 kB, sin dependencias transitivas,
     framework-agnostic. Se envuelve en `core/tour/tour.service.ts` (creación/arranque/limpieza) para
     aislar a la app del vendor (DIP).
   - Alternativas descartadas: Shepherd.js (más pesada), Intro.js (licencia comercial para uso no
     open-source), wrappers Angular abandonados.

2. **Endpoint público `GET /api/v1/landing/public` en `permitAll`**, acotado y protegido:
   - Devuelve **solo** la proyección `LandingSectionPublic` de secciones `PUBLIC` activas: contenido
     institucional/de marca, **cero PII** por diseño (excluye id interno, scope, banderas y
     timestamps). El modelo de datos no almacena PII en estas secciones (FR-006, SC-002).
   - **Rate limiting** reutilizando el puerto `RateLimiter` (Bucket4j, [ADR 0010](0010-rate-limiting-bucket4j.md))
     con clave por IP de origen; al agotar el cupo → **HTTP 429** con mensaje genérico (FR-008).
   - Errores sin filtrar detalles internos (`GlobalExceptionHandler`).
   - El resto de endpoints de landing (`/home`, `/sections*`) siguen autenticados; la edición es
     **solo ADMIN** (`hasRole('ADMIN')`, matriz `RolePolicy`).

3. **Anti-XSS por texto plano + escape de Angular:**
   - El contenido se captura y almacena como **texto plano estructurado** (título, subtítulo, cuerpo,
     CTA, accesos rápidos); se renderiza con **interpolación** Angular (`{{ }}`), que auto-escapa.
     **Prohibido** `[innerHTML]` con contenido de usuario (FR-007, SC-003).
   - En el backend se valida **longitud** y **esquema de URL** de `imagenUrl`/`ctaUrl`/enlaces de
     accesos rápidos: solo `http`, `https` o ruta interna que empiece por `/`; se rechazan
     `javascript:`, `data:` y demás. No se confía en "solo ADMIN edita": una cuenta ADMIN
     comprometida no debe poder inyectar scripts a la cara pública.

4. **Corrección de arranque/shell (FR-015/FR-016):** la ruta raíz resuelve a la landing pública para
   invitados (`guestGuard`) y al home para autenticados; el shell autenticado (menú lateral) se monta
   **solo con sesión válida**, derivándolo del estado de sesión y no del prefijo de la URL. Elimina el
   "menú fantasma" previo.

## Consecuencias

- **Positivas:** contenido de bienvenida configurable sin redepliegue (SC-001); una sola cara pública
  sin auth, minimizada y limitada por tasa; anti-XSS robusto sin saneadores frágiles; tour ligero y
  aislado tras un servicio; arranque de invitados coherente y sin fuga del shell.
- **Límites/deuda:** el rate limiting por defecto es **por instancia** (heredado del ADR 0010). El
  contenido es texto plano: si en el futuro se requiere formato enriquecido, se hará con allowlist de
  HTML saneada en servidor (nuevo ADR), no habilitando `innerHTML`. La preferencia de "tour visto" es
  por dispositivo (Capacitor Preferences); cambiar de dispositivo puede re-mostrarlo (aceptable).
