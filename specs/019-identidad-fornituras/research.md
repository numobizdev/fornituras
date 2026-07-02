# Research — 019 Identidad del sistema

## R1. Mecanismo para actualizar el título en instalaciones existentes

- **Decision**: migración EF Core de solo datos (`migrationBuilder.Sql`) con
  `UPDATE landing_section SET titulo = 'Sistema Integral de Gestión de Fornituras',
  updated_at = SYSUTCDATETIME() WHERE scope = 'PUBLIC' AND type = 'HERO' AND
  titulo = 'Sistema de Gestión de Blindajes'`.
- **Rationale**: la constitución exige migraciones versionadas (nunca cambios manuales). El
  filtro por el valor exacto sembrado anterior hace la operación **idempotente** (segunda
  ejecución afecta 0 filas) y **respeta ediciones del administrador** (cualquier otro texto no
  coincide y no se toca). Es el mismo patrón que usó la migración Flyway
  `V24__fix_landing_public_hero_title.sql` del backend Java obsoleto, adaptado a EF Core.
- **Alternatives considered**:
  - *Actualizar en el `DataSeeder` al arrancar*: rechazado — el seeder solo corre sobre BD sin
    secciones PUBLIC (`AnyAsync` retorna temprano) y un "upsert" al arranque pisaría ediciones
    del administrador o exigiría lógica frágil de detección.
  - *Cambio manual en BD*: prohibido por la constitución (migraciones versionadas).
  - *Marcar las filas sembradas con una columna `is_seeded`*: sobre-diseño para un caso único;
    exigiría cambio de esquema sin beneficio adicional frente al filtro por valor exacto.

## R2. Dirección del rebrand vs. decisión histórica

- **Decision**: registrar **ADR 0019 — Identidad del sistema: Sistema Integral de Gestión de
  Fornituras**, que revierte explícitamente la justificación del commit `6b99f21`
  (`fix/landing-hero-title`), el cual había fijado "Sistema de Gestión de Blindajes" como
  nombre canónico alineado a README/Planeación/constitución.
- **Rationale**: sin ADR, el historial quedaría contradictorio (un fix fusionado dice que
  "Blindajes" es lo correcto). El ADR deja constancia de que el cliente decidió como identidad
  oficial la expansión del acrónimo SIGEFOR. La constitución se enmienda solo en su título
  nominal (PATCH 1.0.0 → 1.0.1 según su propio versionado semántico).
- **Alternatives considered**: no documentar (rechazado — viola el principio VI y repetiría la
  confusión que originó esta feature).

## R3. Alcance de los textos "blindajes"

- **Decision**: solo se cambian las apariciones del **nombre propio** ("Sistema de Gestión de
  Blindajes") y la marca "Gobierno de México". El vocabulario funcional del dominio
  ("administrar blindajes", "equipo de blindaje") se conserva en AGENTS.md, specs históricas y
  textos descriptivos; `Planeacion.md` queda intacta (regla del proyecto).
- **Rationale**: el sistema sigue gestionando blindajes; cambiar el vocabulario del dominio
  alteraría documentos históricos y specs cerradas sin valor para el usuario.
- **Alternatives considered**: reemplazo global de "blindajes" → "fornituras" (rechazado —
  reescribiría historia y specs fusionadas, y "fornituras" como dominio ya tiene su propio uso).

## R4. Etiqueta y descubribilidad del módulo de landing

- **Decision**: renombrar la entrada de menú de `'Contenido de bienvenida'` a
  `'Configurar landing'` en `app-navigation.ts`, conservando ícono (`megaphone`), restricción
  `roles: ['ADMIN']` y la ruta `/landing-admin` con `adminGuard`.
- **Rationale**: el módulo existía y funcionaba (spec 016); el problema reportado fue de
  reconocimiento — la etiqueta no mencionaba la landing. Elegida por el usuario en la sesión
  de planeación.
- **Alternatives considered**: mover el módulo a una sección "Administración" del menú
  (rechazado por ahora — no existe tal agrupación; sería otra feature).
