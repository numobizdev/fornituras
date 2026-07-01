# 0013. Expansión del enum de roles (RBAC) del sistema

- **Estado:** **Aceptado**
- **Fecha:** 2026-07-01
- **Feature:** [013-usuarios](../../specs/013-usuarios/) (tareas T003 → T020, gated)

## Contexto

Hoy el sistema tiene **dos roles** (`Role` en el backend): `ADMIN` (control total, gestión de
usuarios) y `CAPTURISTA` (captura/operación). La autorización se aplica con seguridad de método
(`@PreAuthorize`) por endpoint. La visión de producto (`Paleta de colores.MD` §Seguridad) y
[`docs/02-seguridad.md`](../02-seguridad.md) §4 piden **cinco roles** con mínimo privilegio para
separar responsabilidades (quién autoriza, quién administra inventario, quién solo audita/consulta).

Ampliar el enum `Role` **cambia el modelo de autorización de todo el sistema** (afecta a cada feature
que hoy autoriza con `ADMIN`/`CAPTURISTA`). Por eso la spec 013 lo dejó **gated por este ADR**
(Principio VI) antes de tocar código: primero se acuerda el mapa rol→permisos, luego se implementa en
una sola tarea (T020) y se propaga.

## Decisión

Se amplía el enum `Role` a **cinco roles** y se fija la siguiente matriz de autorización (mínimo
privilegio, **rechazo por defecto**):

| Rol | Enum | Alcance (qué puede hacer) |
|-----|------|---------------------------|
| Administrador | `ADMIN` *(existe)* | Control total: usuarios, roles, configuración y todas las operaciones. |
| Supervisor | `SUPERVISOR` | Consulta completa; autoriza/gestiona altas-bajas, asignaciones y traslados; **no** administra usuarios. |
| Almacén | `ALMACEN` | Administra inventario (alta/edición de fornituras) y traslados; sin gestión de usuarios ni PII completa. |
| Auditor | `AUDITOR` | **Solo lectura**: consultas, reportes y bitácora de auditoría; sin escritura. |
| Capturista | `CAPTURISTA` *(existe)* | Operación limitada (captura/consulta/asignación); sin PII completa ni administración. |

Reglas de propagación:

1. **Escritura de inventario** (`equipment`): `ADMIN`, `ALMACEN`, `CAPTURISTA`.
2. **Asignaciones / traslados / bajas / incidencias** (autorización): `ADMIN`, `SUPERVISOR`; captura por `CAPTURISTA`/`ALMACEN` según flujo.
3. **Reportes**: cualquier rol autenticado; **PII completa** solo `ADMIN`/`SUPERVISOR`/`AUDITOR` (los demás la ven enmascarada, ver [ADR 0003](0003-pii-elementos.md)/[0006](0006-cifrado-pii-nivel-aplicacion.md)).
4. **Bitácora de auditoría** (012): `ADMIN` y `AUDITOR`.
5. **Gestión de usuarios** (013): solo `ADMIN`.

Implementación (T020): añadir los valores al enum, centralizar la matriz en constantes de expresiones
`@PreAuthorize` (p. ej. `WRITE_INVENTORY_ROLES`) reutilizadas por cada controlador, y exponer un
`hasAuthority`/`hasRole` consistente al frontend (que ya filtra menús por rol). **No hay migración de
datos** obligatoria: los usuarios actuales conservan `ADMIN`/`CAPTURISTA`; los nuevos roles se asignan
desde la pantalla de usuarios.

## Alternativas consideradas

1. **Mantener dos roles** (status quo): simple, pero no cumple el mínimo privilegio pedido (un
   capturista y un supervisor no deberían tener el mismo alcance; un auditor no debería poder escribir).
2. **Cinco roles como enum** *(propuesta)*: alineado con producto y seguridad; el enum es explícito,
   fácil de razonar y de auditar; el coste es propagar la matriz a todas las features una vez.
3. **Modelo de permisos granulares** (rol → conjunto de permisos en BD, RBAC dinámico): máxima
   flexibilidad, pero **sobre-ingeniería** para el tamaño actual; añade tablas, cacheo y una pantalla
   de administración de permisos que hoy nadie pidió. Se puede evolucionar a esto más adelante si hace
   falta, partiendo de la matriz de esta decisión.

## Consecuencias

- **Positivas:** separación real de responsabilidades (mínimo privilegio); autorización explícita y
  auditable; el frontend ya soporta ocultar por rol, así que la UI se adapta con datos.
- **Negativas / coste:** hay que revisar **cada** `@PreAuthorize` del sistema y sus tests al ampliar el
  enum (tarea concentrada en T020); riesgo de regresión si algún endpoint queda con la expresión
  antigua. Se mitiga centralizando las expresiones en constantes y con tests de autorización por rol.
- **Seguridad:** rechazo por defecto se mantiene; ampliar roles no relaja ningún control existente.
- **Implementación:** la matriz de la sección Decisión queda fijada; se materializa en la tarea **T020**
  de la spec 013 (ampliar el enum y propagar las expresiones `@PreAuthorize`).
