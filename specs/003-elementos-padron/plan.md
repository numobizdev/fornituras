# Implementation Plan: Padrón de elementos policiales

**Branch**: `dev` (feature **003-elementos-padron**) | **Date**: 2026-06-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/003-elementos-padron/spec.md`

## Summary

Construir el **padrón de elementos policiales**: alta, consulta paginada con búsqueda por texto
(nombre, CURP, RFC, placa) y filtros (municipio, sexo), ficha con foto, y **enmascaramiento de
PII por rol**. Es PII de **alta sensibilidad**, así que el eje del plan es proteger esos datos
(cifrado en reposo, RBAC, auditoría de acceso) sin romper la búsqueda.

El reto técnico central: **buscar sobre columnas cifradas**. SQL Server 2022 con **Always
Encrypted** clásico solo permite igualdad con cifrado determinista (no `LIKE`/parcial). Se
resuelve con **Always Encrypted + secure enclaves** (búsqueda confidencial incl. `LIKE`) y/o
**blind index (HMAC)** para igualdad exacta de CURP/RFC. Esta es una decisión que se eleva a ADR
(ver research). El alcance exacto de qué PII se captura sigue abierto en
[ADR 0003](../../docs/04-decisiones/0003-pii-elementos.md) (Propuesto).

Enfoque: módulo backend `officers` en Spring Boot (controller/service/repository/entity/dto +
mapper), migración Flyway con columnas cifradas, y feature `elementos` en el frontend `sigefor/`
(página de listado + alta) sobre la auth ya existente (guards/interceptor).

## Technical Context

**Language/Version**: Java 25 (backend, igual que `fornituras-api/`); TypeScript + Angular/Ionic 8
(frontend `sigefor/`).

**Primary Dependencies**: Spring Boot (Web MVC, Security, Validation, Data JPA), Flyway
(`flyway-sqlserver`), `mssql-jdbc` (con soporte Always Encrypted / `columnEncryptionSetting`).
Hashing/criptografía vía Spring Security. Frontend: servicios HTTP de Angular + componentes
standalone Ionic. Almacenamiento de foto: blob/objeto cifrado (definir destino — ver research).

**Storage**: SQL Server 2022. Tabla `officers` con **TDE** + **Always Encrypted** en columnas
PII (nombre, apellidos, CURP, RFC, tipo de sangre). Catálogos `sexo`, `tipo_sangre`, `municipio`
en claro. Foto fuera de la fila (referencia `foto_url` a storage cifrado).

**Testing**: JUnit 5 + Spring Boot Test; Testcontainers (MSSQL) para integración y pruebas de
migración Flyway; pruebas de contrato de los endpoints; pruebas de autorización (rol → PII
enmascarada). Frontend: pruebas de servicio con `HttpTestingController`.

**Target Platform**: API REST en contenedor Linux; cliente Ionic (web + móvil vía Capacitor).

**Project Type**: Web — monorepo: backend `fornituras-api/`, frontend **`sigefor/`** (no
`frontend/`; ese directorio está vacío y queda obsoleto).

**Performance Goals**: primera página del listado < 2 s con decenas de miles de elementos
(SC-001); búsqueda y filtros paginados del lado servidor.

**Constraints**: ninguna PII en URLs, logs, parámetros cacheables ni en QR (Principio II); todo
acceso a la ficha completa **auditado** (Principio V); PII cifrada en reposo (Principio I);
enmascaramiento por defecto salvo rol autorizado (Principio IV).

**Scale/Scope**: decenas de miles de elementos; 2 pantallas (listado + alta/edición) + ficha;
catálogos de apoyo.

## Constitution Check

*GATE: debe pasar antes de Phase 0 y re-verificarse tras Phase 1.*

| Principio | Cómo lo cumple este plan | Estado |
|-----------|--------------------------|--------|
| I. Seguridad/privacidad primero | TDE + Always Encrypted en PII; foto en storage cifrado; minimización (CURP/RFC sujetos a ADR 0003) | ✅ |
| II. QR sin PII | El padrón no escribe PII en ningún QR; las fichas no se exponen vía identificadores opacos | ✅ (N/A directo) |
| III. Cero secretos | Claves de cifrado de columna (CMK/CEK) y credenciales en gestor de secretos / env; nunca en repo | ✅ |
| IV. Mínimo privilegio | RBAC: CURP/RFC/foto solo a rol autorizado; enmascaramiento por defecto; rechazo por defecto | ✅ |
| V. Auditoría sin fugas | Auditar acceso a ficha completa, alta/edición; log referencia por id, sin PII | ✅ |
| VI. ADR / stack congelado | Stack sin cambios; decisiones nuevas (búsqueda cifrada, alcance PII) → ADR | ⚠️ ver research |

**Resultado del gate**: PASA con dos decisiones a registrar como ADR (alcance de PII = 0003 ya
Propuesto; estrategia de **búsqueda sobre PII cifrada** = ADR nuevo recomendado). No hay
violación que justificar en Complexity Tracking; son decisiones de diseño pendientes de fijar.

## Project Structure

### Documentation (this feature)

```text
specs/003-elementos-padron/
├── plan.md              # Este archivo
├── research.md          # Phase 0: cifrado searchable, PII, foto
├── data-model.md        # Phase 1: officers + catálogos
├── quickstart.md        # Phase 1: cómo correr y validar
├── contracts/
│   └── officers-api.md  # Phase 1: contrato REST del padrón
└── tasks.md             # Phase 2: lo genera /speckit-tasks (no este comando)
```

### Source Code (repository root)

```text
fornituras-api/
└── src/
    ├── main/java/com/numobiz/solutions/fornituras/modules/officers/
    │   ├── controller/     # OfficerController (listado paginado, alta, ficha)
    │   ├── service/        # OfficerService (búsqueda, enmascaramiento por rol, auditoría)
    │   ├── repository/     # OfficerRepository (+ specs de filtro/paginación)
    │   ├── entity/         # Officer, catálogos (Sexo, TipoSangre, Municipio)
    │   ├── dto/            # OfficerCreateRequest, OfficerSummary, OfficerDetail (enmascarado)
    │   └── mapper/         # mapeo entity ↔ dto con reglas de enmascaramiento
    ├── main/resources/db/migration/   # V__create_officers_and_catalogs.sql (Flyway)
    └── test/java/.../modules/officers/  # unit + contract + integración (Testcontainers)

sigefor/
└── src/app/features/elementos/
    ├── pages/elementos/          # listado: filtros + tabla paginada (ya andamiado)
    ├── pages/elemento-form/      # alta/edición de elemento + sección de foto
    └── data/officers.service.ts  # acceso a API (usa auth.interceptor existente)
```

**Structure Decision**: Web app en monorepo con módulo por feature. Backend en
`modules/officers/` siguiendo el patrón existente (`auth`, `users`, `qrcodes`). Frontend en
`sigefor/src/app/features/elementos/` (página de listado ya andamiada; se añade el formulario de
alta). Toda la lógica sensible (búsqueda sobre cifrado, enmascaramiento, auditoría) vive en el
**backend**; el frontend no decide visibilidad de PII por su cuenta.

## Phase 0 — Research

Ver [research.md](./research.md). Decisiones clave:
- **Búsqueda sobre PII cifrada** → Always Encrypted **con secure enclaves** (SQL Server 2022)
  para `LIKE`/parcial confidencial; **blind index (HMAC)** para igualdad exacta de CURP/RFC.
  Recomendado registrar como **ADR nuevo**.
- **Foto del elemento** → fuera de la fila, en storage cifrado con acceso autorizado; la fila
  solo guarda `foto_url`/referencia. No va en QR ni en URLs cacheables.
- **Alcance de PII** (qué columnas se capturan) → sigue en **ADR 0003 (Propuesto)**; CURP/RFC/
  foto permanecen `[PENDIENTE]` hasta confirmación legal.

Incógnitas que no bloquean el plan pero sí la implementación final:
- Gestor de secretos / custodia de CMK-CEK (ADR pendiente compartido con QR/JWT).
- Destino concreto del storage de fotos (Azure Blob con CMK, FS cifrado, etc.).
- Régimen legal (LFPDPPP vs LGPDPPSO) que condiciona retención y ARCO.

## Phase 1 — Design & Contracts

- **Data model**: [data-model.md](./data-model.md) — tabla `officers` (PII cifrada),
  catálogos `sexo`/`tipo_sangre`/`municipio`, columna `placa` única, `blind_index` para
  CURP/RFC; relación con `assignment` (feature 004).
- **Contracts**: [contracts/officers-api.md](./contracts/officers-api.md) —
  `GET /officers` (listado paginado + búsqueda/filtros), `POST /officers` (alta),
  `GET /officers/{id}` (ficha, enmascarada por rol y **auditada**), `PUT /officers/{id}`
  (edición). Todos con authn (JWT existente) + authz por rol.
- **Quickstart**: [quickstart.md](./quickstart.md) — variables de entorno (conexión Always
  Encrypted, claves), cómo cargar catálogos, cómo validar enmascaramiento por rol y auditoría.

Re-check Constitution tras diseño: el contrato nunca devuelve CURP/RFC/foto a roles no
autorizados; `GET /officers/{id}` emite registro de auditoría; las respuestas de error no
filtran PII. **Gate sigue en PASA** (con los dos ADR señalados como acción).

## Complexity Tracking

> Sin violaciones de la constitución que justificar. Las dos decisiones abiertas (búsqueda sobre
> cifrado y alcance de PII) se resuelven por ADR, no introducen complejidad estructural extra.
