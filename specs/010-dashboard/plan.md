# Implementation Plan: Tablero de control (Dashboard)

**Branch**: `dev` (feature **010-dashboard**) | **Date**: 2026-06-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/010-dashboard/spec.md`

## Summary

Construir el **tablero de control** (`/inicio`, ya andamiado en `sigefor/`): al entrar, el usuario
autenticado ve los indicadores clave del inventario (total, disponibles, asignadas, próximas a vencer,
caducadas, en mantenimiento) con su **color semántico** institucional. No introduce entidades: **consume
agregados** de Fornitura (**001**), Asignación (**004**) e Incidencia (**008**). El eje técnico es
**rendimiento**: calcular con **consultas agregadas del lado servidor** (no traer el inventario al
cliente) y respetar rol sin exponer PII.

Enfoque: endpoint de agregados en un módulo `dashboard` (o un controlador de métricas que reutiliza los
repositorios existentes), y la página `inicio` del frontend que lo pinta con la paleta.

## Technical Context

**Language/Version**: Java 25 (backend `fornituras-api/`); TypeScript + Angular/Ionic 8 (`sigefor/`).

**Primary Dependencies**: Spring Boot (Web MVC, Security, Data JPA), `mssql-jdbc`. Sin dependencias
nuevas; reutiliza repositorios de 001/004/008. Frontend: servicios HTTP + componentes Ionic con color
semántico (`docs/05-ui-ux.md`).

**Storage**: SQL Server 2022. **Sin tablas nuevas**: consultas `COUNT`/`GROUP BY` sobre `equipment`
(estado, `fecha_vencimiento`) y, si aplica, `assignment`/`incident`. Considerar índices ya creados por
001 (status, `fecha_vencimiento`).

**Testing**: JUnit 5 + Spring Boot Test; Testcontainers (MSSQL) para verificar que los contadores
coinciden con los listados filtrados equivalentes; pruebas de autorización (sin PII); rendimiento básico.
Frontend: pruebas de servicio + render de colores.

**Target Platform**: API REST en contenedor Linux; cliente Ionic.

**Project Type**: Web — monorepo `fornituras-api/` + `sigefor/`.

**Performance Goals**: tablero carga < 2 s con decenas de miles de fornituras (SC-001) → agregados
eficientes, una sola llamada.

**Constraints**: contadores con **consultas agregadas** server-side (FR-003); coinciden exactamente con
los listados filtrados (SC-002); criterio de vigencia idéntico a 001/008 (FR-005); requiere
autenticación y respeta rol; **cero PII** (FR-004, SC-003).

**Scale/Scope**: 1 pantalla (`/inicio`) + 1 endpoint de métricas; sin entidades nuevas.

## Constitution Check

*GATE: debe pasar antes de Phase 0 y re-verificarse tras Phase 1.*

| Principio | Cómo lo cumple este plan | Estado |
|-----------|--------------------------|--------|
| I. Seguridad/privacidad primero | Solo agregados numéricos; cero PII en el tablero | ✅ |
| II. QR sin PII | N/A | ✅ (N/A) |
| III. Cero secretos | Sin secretos | ✅ |
| IV. Mínimo privilegio | Autenticado; muestra solo indicadores que el rol puede ver; sin PII | ✅ |
| V. Auditoría sin fugas | Consulta de agregados; sin volcar PII (auditar si el rol lo exige) | ✅ |
| VI. ADR / stack congelado | Sin cambios de stack ni dependencias nuevas | ✅ |

**Resultado del gate**: PASA. Feature de lectura/agregación; depende de 001/004/008 = orden de
implementación. Sin decisiones abiertas.

## Project Structure

### Documentation (this feature)

```text
specs/010-dashboard/
├── plan.md              # Este archivo
└── tasks.md             # Phase 2: lo genera /speckit-tasks
```

> Sin entidades nuevas; el contrato del endpoint de métricas se describe inline.

### Source Code (repository root)

```text
fornituras-api/
└── src/
    ├── main/java/com/numobiz/solutions/fornituras/modules/dashboard/
    │   ├── controller/     # DashboardController (GET /dashboard/summary)
    │   ├── service/        # DashboardService (agregados; criterio de vigencia compartido)
    │   └── dto/            # DashboardSummary (contadores)
    └── test/java/.../modules/dashboard/
# Reutiliza EquipmentRepository (001) y, si aplica, repos de 004/008 vía consultas agregadas.

sigefor/
└── src/app/features/inicio/         # ya andamiada (/inicio)
    ├── pages/inicio/                # tarjetas de indicadores con color semántico
    └── data/dashboard.service.ts
```

**Structure Decision**: módulo `dashboard/` ligero (controller + service + dto), **sin entidades**. Reúsa
los repositorios existentes con consultas agregadas (`COUNT`/`GROUP BY`) y el **mismo criterio de
vigencia** que 001/008 (`VigenciaCriteria`, una sola fuente de verdad). El frontend extiende la página
`inicio` ya andamiada.

## Phase 0 — Research

Decisiones inline:
- **Agregación**: un único `GET /dashboard/summary` que devuelve todos los contadores en una respuesta,
  resueltos con consultas agregadas (no N llamadas, no traer registros).
- **Vigencia**: reutilizar el criterio de 001/008 (≤ 90 días / vencida) para que el tablero **coincida**
  con incidencias/listados (SC-002).
- **Rol**: filtrar qué indicadores se devuelven según el rol; nunca incluir PII.
- **Eficiencia**: apoyarse en índices de 001 (status, `fecha_vencimiento`); cachear breve si hiciera
  falta (decisión posterior, no MVP).

## Phase 1 — Design & Contracts

- **Contract** (inline): `GET /dashboard/summary` → `{ total, disponibles, asignadas, proximasAVencer,
  caducadas, enMantenimiento }` (y los que el rol permita). Authn requerido; sin PII.
- **Quickstart** (inline): con datos sembrados, los contadores coinciden con los listados filtrados
  equivalentes (001/008); inventario vacío → ceros sin error; cambio de datos → recarga refleja.

Re-check Constitution tras diseño: solo números; criterio de vigencia compartido; sin PII. **Gate sigue
en PASA.**

## Complexity Tracking

> Sin violaciones. Feature de agregación sin entidades; reutiliza repos y el criterio de vigencia
> existente. Sin complejidad estructural.
