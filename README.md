# Sistema de Gestión de Blindajes

Sistema para administrar el inventario, asignación y trazabilidad (vía QR) de **chalecos
antibalas y equipo de seguridad** de una corporación policial.

> ⚠️ **Información de alta sensibilidad.** Este sistema maneja datos personales de elementos
> policiales. La seguridad es un requisito de primer nivel. Lee
> [`docs/02-seguridad.md`](./docs/02-seguridad.md) antes de contribuir.

## Funcionalidad

- Inventario de equipos de blindaje.
- Asignación de equipo a elemento policial.
- QR único por equipo (grabado/impreso) ligado a número de serie.
- Lectura de QR por cámara y por escáner manual.

## Stack

- **Backend:** Java + Spring Boot (API REST)
- **Base de datos:** Microsoft SQL Server 2022
- **Frontend:** Ionic 8 + Angular + Capacitor

## Estructura

```
fornituras-api/   API Spring Boot (implementada: auth, usuarios, QR)
frontend/         App Ionic + Angular (en construcción)
docs/             Documentación: arquitectura, seguridad, modelo de datos, decisiones
specs/            Especificaciones de features (spec-driven con .specify/)
```

## Trabajo con asistentes de IA

Este repo está preparado para ser usado por distintos asistentes de IA. El contrato común
está en **[`AGENTS.md`](./AGENTS.md)** (fuente de verdad). Las configuraciones de cada
herramienta apuntan a ese archivo:

- Claude Code → [`CLAUDE.md`](./CLAUDE.md)
- Cursor → [`.cursor/rules/`](./.cursor/rules/)
- GitHub Copilot → [`.github/copilot-instructions.md`](./.github/copilot-instructions.md)

## Estado

🚧 **En desarrollo.** El backend (`fornituras-api/`) ya está implementado con autenticación,
gestión de usuarios y generación de QR. El frontend (Ionic + Angular) está pendiente.
