# Research — 021 RBAC en UI, validación y rediseño

Todas las incógnitas del Technical Context quedaron resueltas por investigación directa del
código y del historial (no hubo NEEDS CLARIFICATION pendientes tras `/speckit-clarify`).

## R1. Causa raíz de "botones/módulos desaparecidos"

- **Decisión**: tratar el problema como (a) dato del admin sin rol garantizado + (b)
  desalineación de la UI con la matriz de 5 roles; NO como regresión de merge.
- **Rationale**: `git log -S` demuestra que los gates (`isAdmin`, `canWrite`) existen desde la
  primera implementación de cada página; el commit `1de688f` (T020, ADR 0013) amplió
  `UserRole` de 2 a 5 sin actualizar ningún check de página ni el menú.
  `DataSeeder.SeedAdminAsync` hace skip si el email existe, sin asegurar rol/enabled
  (`DataSeeder.cs:44-48`), y la columna `users.role` tiene default `CAPTURISTA`
  (`ApplicationDbContext.cs:76-77`). La cadena login→rol→UI se verificó completa y correcta
  para un ADMIN real (`AuthDtos.cs`, `AuthService.cs:193-198`, `auth.service.ts`,
  `token-storage.service.ts`, `provideAppInitializer` en `main.ts:29`).
- **Alternativas consideradas**: revertir merges recientes (descartado: los merges 016/019/020
  solo añadieron o renombraron entradas); consultar la BD remota para confirmar el rol
  (bloqueado por permisos — el seeder *ensure* corrige el dato en el arranque de todos modos).

## R2. Fuente de verdad de permisos para la UI

- **Decisión**: constante tipada `role-policy.ts` espejo 1:1 de `RolePolicy.cs` (mismos
  nombres de capacidad), consumida por menú y páginas vía `AuthService.hasAnyRole`.
- **Rationale**: el backend ya centralizó la matriz (ADR 0013) en `RolePolicy.cs`; duplicar la
  *forma* (no la autoridad — el servidor sigue mandando) da trazabilidad revisable a simple
  vista y elimina los combos ad-hoc dispersos. Mapeo por endpoint verificado:
  `EquipmentController`/`QrController`→WriteInventory; `TransfersController`→WriteTransfers;
  `AssignmentsController`/`IncidentsController`→WriteOperations;
  `DecommissionsController.Create`→**AuthorizeDecommission** (el POST de bajas es
  ADMIN+SUPERVISOR: el botón actual mostrado a CAPTURISTA produce 403);
  `OfficersController`→WriteOfficers; `WarehousesController`/`CatalogController`→ManageConfig;
  `LandingController`→ManageLanding; `UsersController`→ManageUsers; `AuditController`→ReadAudit.
- **Alternativas consideradas**: endpoint que exponga la matriz desde el API (sobre-ingeniería
  para 8 constantes estáticas; añade latencia/fallo al arranque); permisos en el JWT como
  claims (cambia contrato de token, innecesario).

## R3. Reactividad de los flags de visibilidad

- **Decisión**: flags como `computed(() => auth.hasAnyRole(...))` sobre la señal
  `currentUser`, en lugar de campos evaluados una vez en la construcción.
- **Rationale**: `almacenes.page.ts:76` y similares evalúan `hasRole` al construir; Ionic
  cachea páginas (`IonicRouteStrategy`), así que un cambio de sesión no se refleja. El menú
  (`app.component.ts:82`) ya usa `computed` — se homologa el patrón existente.
- **Alternativas consideradas**: directiva estructural `*appHasRole` (más aparatosa para
  el caso: los templates ya usan `@if` con flags; una directiva duplicaría el mecanismo).

## R4. Componente de errores de campo

- **Decisión**: componente standalone `app-field-errors` (input: `AbstractControl`, opcional
  `patternMessage`), mensajes es-MX centralizados; estilo global `.field-error` derivado del
  patrón ya probado `.auth-page__field-error`.
- **Rationale**: no existe pieza reutilizable hoy; las 3 pantallas auth ya validaron el patrón
  visual y de UX (mostrar al `touched`). Un componente (vs. pipe o directiva) permite
  plantilla y aria-live consistentes. Los formularios ya son Reactive Forms con validators
  correctos — solo falta la presentación.
- **Alternativas consideradas**: librería externa de validación (viola el principio de no
  añadir dependencias sin necesidad); mensajes por formulario (duplicación que esta feature
  precisamente elimina).

## R5. Login split-screen sin romper forgot/reset

- **Decisión**: el login obtiene su propio bloque de estilos/estructura (`.login-split`),
  manteniendo `auth-page.scss` compartido intacto para forgot/reset; emblema = SVG
  provisional `assets/img/escudo-sigefor.svg` (clarificación de sesión 2026-07-01).
- **Rationale**: forgot/reset consumen las clases `auth-page__*`; extender sin tocar evita
  regresiones (SC del US6). El proyecto no tiene assets de logo (solo favicon), por eso el
  escudo provisional reemplazable. Paleta y fuente ya existen (`variables.scss`, `fonts.scss`).
- **Alternativas consideradas**: rediseñar las tres pantallas auth a split-screen (alcance
  extra sin pedido del usuario; se mantiene coherencia visual con la misma paleta).

## R6. Asignación: causa del "encimado" y estructura objetivo

- **Decisión**: reestructurar a `ion-card` por paso + lista separada de vigentes; conservar
  intactos `app-qr-scan`, banner `.scan-result-banner`, spinner y selección de cámara (PR #5).
- **Rationale**: el solape no viene de `position/z-index` sino de `ion-searchbar` y un `div`
  de acciones como hijos directos de una `ion-list` única (padding propio del searchbar +
  ruptura del ritmo vertical de Ionic). Tarjetas numeradas comunican el flujo de 2 pasos.
- **Alternativas consideradas**: wizard multipágina o `ion-modal` por paso (rompe el flujo
  rápido con escáner HID, que dispara `codeCaptured` sin interacción); arreglar solo el SCSS
  (no resuelve la confusión de secciones reportada).

## R7. Remediación de secretos versionados

- **Decisión**: `git rm --cached fornituras-api-dotnet/src/Fornituras.Api/appsettings.Development.json`
  + `.gitignore` + `appsettings.Development.json.example` con placeholders; documentar
  nombres de claves y recomendación de rotación. No se reescribe el historial.
- **Rationale**: principio III de la constitución (un secreto commiteado se considera
  comprometido → rotación recomendada, responsabilidad del dueño del entorno). Reescribir
  historial compartido (filter-repo) es disruptivo para un repo con múltiples frentes activos
  y no elimina copias ya clonadas; la rotación es la mitigación real.
- **Alternativas consideradas**: `dotnet user-secrets` como único mecanismo (válido, se
  documenta como opción; el archivo local ignorado es el camino de menor fricción y funciona
  igual en CI/despliegue vía variables de entorno).

## R8. Infraestructura de pruebas

- **Decisión**: backend xUnit en `tests/Fornituras.Api.Tests` (existente); frontend
  Karma/Jasmine vía `ng test` con specs junto al código (patrón existente, p. ej.
  `qr-scan.component.spec.ts`). TDD: tests de `role-policy`/`hasAnyRole`, de visibilidad por
  rol (menú y páginas con `AuthService` doble), de `field-errors` y del seeder *ensure*.
- **Rationale**: se reutilizan los arneses existentes; no se introduce framework nuevo.
