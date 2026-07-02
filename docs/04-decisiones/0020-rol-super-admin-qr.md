# ADR 0020 — Rol SUPER_ADMIN para gestión de lotes QR

**Estado:** Aceptado  
**Fecha:** 2026-07-01  
**Spec:** [021-ui-lotes-qr](../../specs/021-ui-lotes-qr/spec.md)

## Contexto

Tras migrar el backend a .NET se eliminó la UI web anónima `/qr/**` del backend Java (mejora M-1,
spec 018). Los endpoints REST `/api/v1/qr/**` quedaron protegidos con `WriteInventory`
(`ADMIN`, `ALMACEN`, `CAPTURISTA`), mezclando generación masiva de etiquetas con operación de
inventario.

Se requiere una UI en Ionic con acceso **restringido** a un rol dedicado para personal que solo
imprime/graba etiquetas QR, sin acceso al inventario, padrón ni administración.

## Decisión

1. Introducir el rol **`SUPER_ADMIN`** en el enum `Role` (backend y frontend).
2. **`SUPER_ADMIN` es QR-only:** único rol autorizado en `/api/v1/qr/**` y en el módulo Ionic
   `/qr-lotes/**`. No hereda permisos de `ADMIN` ni de inventario.
3. **`WriteInventory` deja de incluir QR:** los endpoints de lotes QR usan
   `RolePolicy.ManageQrLotes = "SUPER_ADMIN"`.
4. **Asignación:** solo un usuario con rol `ADMIN` puede crear o cambiar usuarios a
   `SUPER_ADMIN` (módulo Usuarios, spec 013).
5. **UX:** tras login, `SUPER_ADMIN` aterriza en `/qr-lotes` y no puede navegar al resto del
   shell operativo.

## Consecuencias

### Positivas

- Separación clara entre operación de almacén y generación masiva de etiquetas.
- Cierra la brecha histórica de UI QR sin autenticación.
- Principio de mínimo privilegio reforzado.

### Negativas / trade-offs

- Los roles `ADMIN`/`ALMACEN`/`CAPTURISTA` **pierden** acceso directo a generación de lotes QR
  (comportamiento intencional).
- En producción debe existir al menos un usuario `SUPER_ADMIN` creado por un `ADMIN`.

## Alternativas consideradas

| Alternativa | Motivo de rechazo |
|-------------|-------------------|
| Mantener QR en `WriteInventory` | Demasiado amplio; almacén no debe generar lotes masivos por defecto |
| `ADMIN` + QR | Mezcla administración del sistema con impresión de etiquetas |
| Reintroducir UI Thymeleaf en .NET | Duplica stack; Ionic ya es el frontend oficial |

## Referencias

- [ADR 0005](./0005-formato-qr-implementado.md) — formato `FOR-XXXXX`
- [ADR 0013](./0013-rbac-roles.md) — matriz RBAC (si existe; extender con SUPER_ADMIN)
- Spec [002-qr-equipos](../../specs/002-qr-equipos/spec.md) — backend QR
- Spec [018-auditoria-migracion-dotnet](../../specs/018-auditoria-migracion-dotnet/findings.md) — M-1
