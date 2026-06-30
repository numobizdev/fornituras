# CLAUDE.md — Notas para Claude Code

La **fuente de verdad** de este proyecto es [`AGENTS.md`](./AGENTS.md). Léela completa
antes de trabajar. Este archivo solo añade lo específico de Claude Code.

@AGENTS.md

---

## Específico de Claude Code

- **Plataforma:** desarrollo en Windows (PowerShell). Usa rutas y sintaxis de Windows
  cuando ejecutes comandos de shell.
- **Fase actual:** el backend está implementado en `fornituras-api/` (Spring Boot: auth,
  usuarios, QR). El **frontend vive en `sigefor/`** (Ionic 8 + Angular) y ya tiene la
  **autenticación** (login por email/JWT, guards, interceptor, recuperación por código). Para
  builds/tests del backend usa el Maven wrapper desde `fornituras-api/` (`.\mvnw.cmd ...`); para
  el frontend, `npm` desde `sigefor/`. El directorio `frontend/` (vacío) es obsoleto.
- **Antes de tocar seguridad, QR, autenticación o datos personales**, lee
  [`docs/02-seguridad.md`](./docs/02-seguridad.md). Es información de alta sensibilidad
  (personal policial en México).
- **Secretos:** nunca los escribas en archivos versionados. Si necesitas valores locales,
  usa un `.env` (ya ignorado por git) y documenta solo el *nombre* de la variable.
- **Decisiones de arquitectura:** regístralas como ADR en `docs/04-decisiones/`.
