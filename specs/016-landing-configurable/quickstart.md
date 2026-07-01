# Quickstart — Validación de Landing configurable + tour

Guía para validar el feature end-to-end una vez implementado. No contiene código de
implementación; ver `contracts/landing-api.md` y `data-model.md` para los detalles.

## Prerrequisitos

- Backend levantado (`fornituras-api/`): `.\mvnw.cmd spring-boot:run` con SQL Server y la
  migración `V19` aplicada (seed cargado).
- Frontend (`sigefor/`): `npm install` (incluye `driver.js`) y `npm start`.
- Un usuario **ADMIN** y un usuario **CAPTURISTA** de prueba.

## Escenario 1 — Home configurable (P1)

1. Inicia sesión como CAPTURISTA.
2. Verifica que `/inicio` muestra el hero, el aviso y los accesos rápidos sembrados, en orden.
3. Pulsa un acceso rápido y confirma que navega a la función correspondiente.
   - **Esperado**: contenido visible < 2 s; secciones en el orden configurado (SC-006).

## Escenario 2 — Edición por ADMIN (P1)

1. Inicia sesión como ADMIN; abre el editor de landing (ítem de menú visible solo a ADMIN).
2. Crea un aviso `HOME/ANNOUNCEMENT`, guárdalo.
3. Con otra sesión CAPTURISTA (o recargando), confirma que el aviso aparece en `/inicio`.
4. Edita el aviso, reordénalo y desactívalo; verifica cada efecto en el home.
5. Con CAPTURISTA, intenta acceder a la ruta del editor → **denegado** (SC-004).
6. En un campo de texto introduce `<script>alert(1)</script>` y guarda; ábrelo como usuario →
   **se muestra literal, no se ejecuta** (SC-003).

## Escenario 3 — Landing pública (P2)

1. Sin sesión, abre la app → se muestra la landing pública con el contenido `PUBLIC` activo.
2. Pulsa "Acceder" → llega al login.
3. Con sesión iniciada, navega a la ruta pública → **redirige a `/inicio`**.
4. Inspecciona la respuesta de `GET /api/v1/landing/public`: **no contiene PII** (SC-002).
5. Repite muchas peticiones rápidas al endpoint público → eventualmente **429** (rate limit).

## Escenario 4 — Tour guiado (P3)

1. Con un usuario que entra por primera vez a `/inicio`, el tour se inicia **solo** y resalta
   hero, accesos rápidos y menú.
2. Cierra/completa el tour, recarga `/inicio` → **no** se inicia solo (flag en Preferences).
3. Pulsa "Ver tutorial" → el tour se reinicia (SC-005).

## Verificación automatizada

- Backend: `.\mvnw.cmd test`
  - `/public` sin auth → 200, solo `PUBLIC` activas, sin PII; 429 al exceder rate limit.
  - `/home` sin auth → 401; con auth → 200.
  - CRUD `/sections` → 403 sin ADMIN; 201/200 con ADMIN; 400 en validación (URL peligrosa,
    `QUICK_LINKS` sin items, longitudes excedidas).
- Frontend: `npm test`
  - `landing.service` mapea `ApiResponse`.
  - `tour.service` arranca/limpia y respeta el flag de primera visita.

## Checklist de seguridad (obligatorio)

- [ ] Respuesta pública sin PII (SC-002).
- [ ] Contenido con marcado/script se muestra escapado (SC-003).
- [ ] Edición restringida a ADMIN (SC-004).
- [ ] Rate limiting activo en el endpoint público.
- [ ] Escrituras auditadas (quién/qué/cuándo) sin PII en logs.
- [ ] ADR `docs/04-decisiones/00XX-landing-configurable.md` creado.
