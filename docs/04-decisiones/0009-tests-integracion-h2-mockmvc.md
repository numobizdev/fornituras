# 0009. Tests de contrato/integración con perfil H2 en memoria y MockMvc (sin Docker)

- **Estado:** **Aceptado**
- **Fecha:** 2026-06-30
- **Feature:** [001-inventario-equipos](../../specs/001-inventario-equipos/)

## Contexto

El plan de 001 preveía pruebas de **contrato**, **integración**, **autorización** y **concurrencia
de unicidad** apoyadas en **Testcontainers (MSSQL)**, para ejercitar el esquema real de SQL Server.
En la práctica:

- El entorno de desarrollo (Windows) **no tiene Docker disponible**, por lo que Testcontainers no
  puede levantar un contenedor de SQL Server ni ejecutarse localmente.
- El proyecto **ya trae** la infraestructura para pruebas de rodaja/integración sin Docker: el
  `spring-boot-starter-webmvc-test` (MockMvc), `spring-boot-starter-security-test`, la dependencia
  `h2` y un perfil de test (`src/test/resources/application-test.yml`) que arranca la aplicación
  sobre **H2 en modo `MSSQLServer`**, con Flyway deshabilitado y `ddl-auto: create-drop`.
- Introducir Testcontainers implicaría **una dependencia nueva** (Principio VI) que, además, no se
  podría ejecutar ni verificar en esta máquina.

## Decisión

1. Las pruebas de contrato/integración/autorización de 001 se implementan con
   **`@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`** sobre el perfil H2
   ya existente, **sin añadir Testcontainers ni Docker**.
2. La **autorización** se ejercita con `spring-security-test` (`@WithMockUser(roles = …)`) contra
   las reglas reales `@PreAuthorize`/`@EnableMethodSecurity` (rechazo por defecto, escritura solo
   ADMIN/CAPTURISTA).
3. La **unicidad bajo concurrencia** (SC-002) se prueba invocando el servicio real desde varios
   hilos contra la restricción `UNIQUE(codigo_normalizado)` que Hibernate genera del modelo; se
   verifica el invariante *cero duplicados* (exactamente una fila persiste).
4. Testcontainers/MSSQL queda como **extensión futura para CI** (donde sí hay Docker), para validar
   dialecto y semántica de carrera reales; no es requisito de esta feature.

## Consecuencias

- **Positivas:** cobertura ejecutable **aquí y ahora** (contrato HTTP, validación, mapeo de 400/404/
  409, paginación/filtros, autorización por rol, atomicidad del lote, bloqueo por asignación
  vigente, unicidad concurrente) **sin dependencias nuevas** (Principio VI). Las pruebas comparten
  un único contexto Spring cacheado (arranque ~6 s).
- **Límites de fidelidad:** H2 en modo `MSSQLServer` **no es** SQL Server. No cubre peculiaridades
  del dialecto, tipos específicos, índices filtrados ni la semántica exacta de bloqueo/carrera del
  motor real. El esquema de test lo genera JPA (`ddl-auto`), no las migraciones Flyway: **la validez
  de las migraciones se sigue verificando aparte** (arranque real contra SQL Server, quickstart).
- **Deuda / seguimiento:** cuando exista CI con Docker, añadir un perfil Testcontainers (MSSQL) que
  reejecute integración, migración y concurrencia contra el motor real. Ver notas de
  `specs/001-inventario-equipos/tasks.md`.
