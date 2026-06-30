# 0001. Monorepo y gobernanza agnóstica de IA

- **Estado:** Aceptado
- **Fecha:** 2026-06-29

## Contexto

El proyecto será desarrollado principalmente con asistentes de IA (Claude Code, y
potencialmente Cursor u otros en el futuro). Se necesita una estructura que: (1) permita que
distintas IAs trabajen con las mismas reglas, (2) no duplique configuración, y (3) organice
backend y frontend de forma coordinada.

## Decisión

1. **Monorepo** con `backend/` (Spring Boot) y `frontend/` (Ionic + Angular) en un solo
   repositorio, con documentación raíz compartida.
2. **`AGENTS.md` como fuente única de verdad** para asistentes de IA (convención
   [agents.md](https://agents.md)). Las configuraciones específicas de cada herramienta
   (`CLAUDE.md`, `.cursor/rules/`, `.github/copilot-instructions.md`) **apuntan** a ese
   archivo en lugar de duplicar contenido.

## Alternativas consideradas

- **Repos separados** para backend y frontend: más aislamiento, pero duplica documentación y
  reglas de IA y complica la coordinación en fase temprana. Descartado por ahora.
- **Reglas solo en `CLAUDE.md`**: ataría el proyecto a una sola herramienta; contradice el
  requisito de ser agnóstico de IA.

## Consecuencias

- (+) Una sola fuente de verdad: cambiar una norma se hace en un único lugar.
- (+) Fácil incorporar otra IA en el futuro: solo lee `AGENTS.md`.
- (+) Documentación y decisiones centralizadas.
- (−) En un monorepo hay que cuidar que las herramientas de build de backend y frontend no
  se mezclen (se maneja con `.gitignore` y rutas separadas).
