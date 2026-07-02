# Feature Specification: Auditoría de cobertura de la migración a .NET (specs 001–017)

**Feature Branch**: `018-auditoria-migracion-dotnet`

**Created**: 2026-07-01

**Status**: Draft

**Input**: User description: "haz un spec dedicado a revisar que todas las specs (000/001 hasta 017)
estén aplicadas en `fornituras-api-dotnet/` y que no se haya perdido nada en la migración a .NET"

## Contexto

El backend se migró de **Java (Spring Boot)** a **ASP.NET Core** (ADR 0016, spec
`017-migracion-api-dotnet`). La migración se hizo como una reimplementación amplia; existe el
**riesgo de que algún comportamiento, endpoint, validación, control de seguridad, columna de datos
o regla de negocio definido en las specs previas se haya quedado fuera** o cambiado de forma no
intencional. El backend Java (`fornituras-api/`) queda como **referencia histórica** para comparar.

Esta feature **no añade producto nuevo**: es una **auditoría de paridad** que verifica que las
specs **001–017** están efectivamente aplicadas en `fornituras-api-dotnet/` y documenta cualquier
brecha para su remediación. El resultado es una **matriz de cobertura** y una lista priorizada de
huecos, no un cambio funcional (las remediaciones que surjan se harán en la rama de la spec que
corresponda, según la regla de una rama por spec — AGENTS.md §5.8).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Matriz de cobertura spec → .NET (Priority: P1)

Como responsable técnico, quiero una **matriz** que, para cada spec 001–017, indique si sus
elementos verificables (endpoints, entidades/columnas, reglas de negocio, validaciones, RBAC,
migraciones) están **presentes en `fornituras-api-dotnet/`**, para saber con evidencia qué está
cubierto y qué no.

**Why this priority**: Sin el inventario de cobertura no se puede afirmar que "no se perdió nada";
es el entregable base del que dependen las demás historias.

**Independent Test**: Tomar una spec cualquiera (p. ej. 004-asignacion-resguardos), listar sus
endpoints/entidades/reglas y confirmar que cada uno tiene su contraparte en el código .NET
(controller/service/entidad/migración) o marcarlo como brecha. Entrega valor por sí sola.

**Acceptance Scenarios**:

1. **Given** la lista de specs 001–017, **When** se completa la auditoría, **Then** existe una
   fila por spec con estado `Cubierta` / `Parcial` / `Ausente` y evidencia (archivo/endpoint) para
   cada elemento verificable.
2. **Given** una spec marcada `Cubierta`, **When** se revisa su evidencia, **Then** cada requisito
   funcional citable de esa spec tiene una referencia concreta en `fornituras-api-dotnet/`.

### User Story 2 - Detección de pérdidas frente al backend Java (Priority: P1)

Como responsable técnico, quiero **comparar** el backend Java (referencia) con el .NET para
detectar **endpoints, validaciones, columnas de BD, reglas o mensajes que existían y ya no**, para
no perder comportamiento en la migración.

**Why this priority**: Es el corazón del pedido ("que no se haya perdido nada"); una spec puede
figurar "cubierta" a grandes rasgos y aun así haber perdido un endpoint secundario o una validación.

**Independent Test**: Enumerar los controllers/endpoints del backend Java y verificar que cada uno
tiene equivalente en .NET (o justificar su ausencia); igual para columnas de las migraciones y para
validaciones de entrada. Cada diferencia se registra como brecha con severidad.

**Acceptance Scenarios**:

1. **Given** el conjunto de endpoints del backend Java, **When** se contrasta con los del .NET,
   **Then** toda diferencia queda listada como brecha (con severidad y spec asociada) o como
   **cambio intencional** documentado.
2. **Given** el esquema de datos del backend Java (entidades/columnas/índices), **When** se compara
   con las migraciones EF Core, **Then** se listan columnas/índices/constraints faltantes o
   divergentes.
3. **Given** una regla de negocio con test en Java (p. ej. unicidad de placa, cálculo de vencimiento),
   **When** se busca su equivalente en .NET, **Then** se confirma que la regla persiste o se marca
   como brecha.

### User Story 3 - Los controles de seguridad transversales sobrevivieron (Priority: P1)

Como responsable de seguridad, quiero verificar que los **controles transversales** definidos en
las specs y en `docs/02-seguridad.md` siguen vigentes en .NET: autenticación/authz en todos los
endpoints, cifrado de PII, QR sin PII, sin acceso anónimo indebido, rate limiting donde aplique,
auditoría append-only, y sin PII en logs.

**Why this priority**: Una pérdida aquí es de máxima gravedad (PII de personal policial); la
migración no debe relajar la postura de seguridad.

**Independent Test**: Recorrer el checklist de `docs/02-seguridad.md` §8 sobre el backend .NET y
confirmar cada control o registrar la brecha.

**Acceptance Scenarios**:

1. **Given** cada endpoint del .NET, **When** se revisa su autorización, **Then** ninguno queda
   expuesto sin autenticación salvo los explícitamente públicos ya decididos (p. ej. login, landing
   pública), y esas excepciones coinciden con las del backend Java.
2. **Given** la PII de elementos, **When** se inspecciona su persistencia en .NET, **Then** sigue
   cifrada (AES-256-GCM) con blind index para búsqueda, igual que en Java.
3. **Given** la brecha conocida de `/qr/**` sin auth (memoria del proyecto), **When** se audita el
   .NET, **Then** se confirma si persiste, se corrigió o cambió, y se documenta.

### Edge Cases

- **Spec no implementable en backend**: specs puramente de frontend (p. ej. 014-escaneo-qr,
  parte de 016/017) — la auditoría verifica su parte de API si la tiene y marca el resto como
  "fuera del alcance del backend".
- **Cambio intencional en la migración**: cuando el .NET difiere a propósito (p. ej. Flyway→EF Core,
  Bucket4j→otra estrategia de rate limiting), no es una pérdida: se registra como **decisión** con
  su justificación/ADR, no como brecha.
- **Elemento ambiguo**: si una spec no define un comportamiento con precisión, se marca
  `[NEEDS CLARIFICATION]` en la matriz en vez de asumir cobertura.
- **Doble 017**: existen `017-gestion-de-fotos` y `017-migracion-api-dotnet`; ambas entran en el
  alcance (la segunda es la propia migración: se audita que hizo lo que prometió).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La auditoría MUST cubrir **todas** las specs de `specs/001-*` a `specs/017-*`
  (ambos 017 incluidos), sin omitir ninguna.
- **FR-002**: Para cada spec, la auditoría MUST registrar un **estado de cobertura**
  (`Cubierta` / `Parcial` / `Ausente` / `Fuera de alcance backend`) con **evidencia** concreta
  (ruta de archivo, nombre de endpoint, columna/migración) en `fornituras-api-dotnet/`.
- **FR-003**: La auditoría MUST comparar el **conjunto de endpoints** del backend Java de referencia
  con el del .NET y listar toda diferencia.
- **FR-004**: La auditoría MUST comparar el **esquema de datos** (entidades, columnas, índices,
  constraints, enums) entre Java (referencia) y las migraciones EF Core del .NET.
- **FR-005**: La auditoría MUST verificar las **reglas de negocio y validaciones** con respaldo en
  tests del backend Java y confirmar su presencia en .NET.
- **FR-006**: La auditoría MUST verificar los **controles de seguridad transversales**
  (`docs/02-seguridad.md` §8): authn/authz por endpoint, cifrado de PII, QR sin PII, ausencia de
  acceso anónimo indebido, rate limiting, auditoría inmutable, sin PII en logs.
- **FR-007**: Cada **brecha** encontrada MUST clasificarse por **severidad**
  (`Crítica`/`Alta`/`Media`/`Baja`) y asociarse a la spec y al elemento afectado.
- **FR-008**: Las diferencias **intencionales** de la migración MUST distinguirse de las brechas y
  documentarse con su justificación (o ADR).
- **FR-009**: La auditoría MUST producir una **lista priorizada de remediación**; cada remediación
  se ejecuta en la rama de la spec correspondiente (AGENTS.md §5.8), no en esta.
- **FR-010**: La auditoría MUST ser **no destructiva**: solo lee y documenta; no modifica código de
  producción (las correcciones son trabajo posterior).

### Key Entities *(include if feature involves data)*

- **Ítem de cobertura**: una fila de la matriz — spec, elemento verificable (endpoint/entidad/
  regla/control), estado, evidencia en .NET, notas.
- **Brecha**: algo presente en las specs/Java y ausente o divergente en .NET — descripción,
  severidad, spec asociada, remediación propuesta.
- **Decisión de migración**: diferencia intencional Java→.NET — descripción y justificación/ADR.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las specs 001–017 tiene una fila de cobertura con estado y evidencia
  (0 specs sin revisar).
- **SC-002**: El 100% de los endpoints del backend Java de referencia está clasificado como
  `presente en .NET`, `ausente (brecha)` o `retirado intencionalmente (decisión)`.
- **SC-003**: Toda brecha de severidad `Crítica` o `Alta` queda documentada con remediación
  propuesta antes de cerrar la auditoría.
- **SC-004**: Los controles de seguridad de `docs/02-seguridad.md` §8 están todos verificados
  (cada uno con veredicto `preservado` / `brecha`).
- **SC-005**: La auditoría no introduce ningún cambio en el código de producción (diff de
  `fornituras-api-dotnet/src` = 0 durante la auditoría).

## Assumptions

- El backend Java en `fornituras-api/` se conserva accesible como **fuente de verdad de
  referencia** para el comportamiento previo a la migración.
- Las specs en `specs/` reflejan el alcance acordado; cuando una spec sea ambigua, se marca
  `[NEEDS CLARIFICATION]` en vez de inferir.
- El alcance es el **backend** (`fornituras-api-dotnet/`); la parte de frontend de cada spec se
  verifica solo en su superficie de API. La cobertura de `sigefor/` puede auditarse aparte.
- Las remediaciones detectadas se planifican y ejecutan como trabajo posterior, cada una en la
  rama de su spec.
