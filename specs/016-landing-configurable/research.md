# Research — Landing configurable + tour guiado

Phase 0 del plan. Resuelve las decisiones técnicas abiertas antes del diseño. Formato por
decisión: **Decisión / Justificación / Alternativas descartadas**.

## R1. Librería de tour guiado

- **Decisión**: `driver.js ^1.6.0` (MIT, ~5 kB, sin dependencias), integrada directamente y
  envuelta en un servicio Angular (`core/tour/tour.service.ts`) que gestiona creación,
  arranque y destrucción/limpieza.
- **Justificación**: es la opción moderna, ligera y mantenida (última versión publicada
  recientemente); framework-agnostic y compatible con componentes standalone de Angular 20 sin
  necesidad de wrapper. Menor superficie y peso que las alternativas.
- **Alternativas descartadas**:
  - *Shepherd.js*: más pesada y con más configuración de la necesaria.
  - *Intro.js*: licencia comercial para uso no open-source; se evita por coste/licencia.
  - *ngx-web-tour / wrappers Angular específicos*: mantenimiento irregular o abandonado; no
    aportan sobre usar `driver.js` directo.
- **Fuentes**: driverjs.com · npmjs.com/package/driver.js · comparativas de librerías de tour
  para Angular (Chameleon).

## R2. Prevención de stored XSS en contenido administrable

- **Decisión**: almacenar el contenido como **texto plano estructurado** (campos: título,
  subtítulo, cuerpo, CTA) y renderizarlo en Angular con **interpolación** (`{{ }}`), que
  auto-escapa. Prohibido `[innerHTML]` con contenido de usuario. En backend, validación de
  longitud y de formato de URL para `ctaUrl`/`imagenUrl`/enlaces (esquema permitido:
  `http/https` y rutas internas relativas).
- **Justificación**: el contenido lo captura un ADMIN pero se muestra a usuarios (y a público
  sin sesión); el escape por defecto de Angular elimina la ejecución de scripts sin depender de
  saneadores frágiles. Cumple Principio I y el checklist de seguridad.
- **Alternativas descartadas**:
  - *Permitir HTML enriquecido con saneador (p. ej. OWASP Java HTML Sanitizer / DomSanitizer)*:
    mayor superficie de ataque y complejidad; se difiere a una futura iteración si se requiere
    formato rico, y se haría con allowlist en servidor.
  - *Confiar en que "solo ADMIN edita"*: rechazado; una cuenta ADMIN comprometida no debe poder
    inyectar scripts a la cara pública.

## R3. Exposición del endpoint público sin autenticación

- **Decisión**: `GET /api/v1/landing/public` en `permitAll` (SecurityConfig), devolviendo
  **solo** campos no sensibles de secciones `PUBLIC` activas. Se aplica **rate limiting** con
  el patrón Bucket4j ya usado en `by-codigo` (QR). Manejo de errores que no filtra detalles
  internos.
- **Justificación**: la landing pública debe verse sin sesión, pero es la única superficie sin
  auth del feature; se acota a contenido institucional (cero PII) y se protege contra abuso y
  enumeración (Principios I y IV; §Endurecimiento de API de la constitución).
- **Alternativas descartadas**:
  - *Servir la landing pública desde assets estáticos del front*: pierde el requisito
    "configurable por ADMIN sin redepliegue" (SC-001).
  - *Requerir un token anónimo*: fricción innecesaria para una página de branding pública.

## R4. Modelo de datos: una tabla con `scope` vs. tablas separadas

- **Decisión**: una única entidad/tabla `landing_section` con discriminador `scope`
  (`PUBLIC` | `HOME`) y `type` de sección. Datos flexibles de accesos rápidos en `config_json`
  (NVARCHAR(MAX)).
- **Justificación**: ambas caras comparten "motor de configuración" (petición explícita del
  usuario); mismo CRUD, mismo mapper, menos duplicación (DRY, alta cohesión). El JSON evita una
  tabla hija solo para los ítems de accesos rápidos.
- **Alternativas descartadas**:
  - *Dos tablas (pública/home)*: duplica repositorio/servicio/DTOs sin ganancia.
  - *Reutilizar el catálogo genérico existente*: el catálogo modela pares código/valor para
    dropdowns, no contenido con título/cuerpo/imagen/orden; forzarlo sería un mal encaje.

## R5. Rutas del frontend e inserción de la landing pública

- **Decisión**: añadir una ruta pública (p. ej. `bienvenida`) protegida por `guestGuard`
  (redirige a `/inicio` si ya hay sesión) y hacer que el arranque para invitados aterrice en la
  landing pública, manteniendo `/login` accesible desde el botón "Acceder". El grupo
  `authGuard → /inicio` no se altera.
- **Justificación**: introduce la cara pública sin romper el flujo de auth existente
  (`authGuard`, `guestGuard`) ni la redirección post-login.
- **Alternativas descartadas**:
  - *Convertir `/` (root) directamente en la landing con lógica condicional de sesión*: más
    frágil; se prefiere separar ruta pública y usar los guards ya probados.

## R6. Persistencia del "tour ya visto"

- **Decisión**: flag booleano por usuario/dispositivo en **Capacitor Preferences**
  (clave nueva en `STORAGE_KEYS`, p. ej. `sigefor.tour.home.done`). El tour se lanza solo si el
  flag no existe; "Ver tutorial" lo relanza sin tocar el arranque automático.
- **Justificación**: reutiliza el mecanismo de almacenamiento ya usado para la sesión
  (`TokenStorageService`); no requiere backend ni nueva columna. Cambiar de dispositivo puede
  volver a mostrarlo (aceptable, ver Assumptions de la spec).
- **Alternativas descartadas**:
  - *Guardar la preferencia en backend (por usuario)*: sobre-ingeniería para un flag de UX;
    se puede migrar después si se quiere consistencia multi-dispositivo.

## R7. ADR requerido

- **Decisión**: registrar un ADR en `docs/04-decisiones/00XX-landing-configurable.md` que
  cubra: (a) alta de dependencia `driver.js` (necesidad, licencia MIT, mantenimiento),
  (b) exposición del endpoint público no-PII con rate limiting, (c) estrategia anti-XSS
  (texto plano + escape de Angular).
- **Justificación**: Principio VI (ADR para dependencias y decisiones arquitectónicas) y
  Principio I (seguridad). Es la primera tarea de implementación.
