# Data Model — 021 RBAC en UI, validación y rediseño

Sin cambios de esquema de base de datos ni migraciones. El modelo relevante es de
**configuración/UI** más una corrección de datos vía seeder.

## Entidades y reglas

### Rol (`UserRole` — existente, sin cambios)

`'ADMIN' | 'SUPERVISOR' | 'ALMACEN' | 'AUDITOR' | 'CAPTURISTA'`
(frontend `core/models/auth.model.ts`; backend enum `Role`, persistido como cadena en
`users.role`, default de columna `CAPTURISTA`).

- **Etiqueta visible (NUEVO)**: mapa rol→etiqueta es-MX para el menú:
  ADMIN→"Administrador", SUPERVISOR→"Supervisor", ALMACEN→"Almacén", AUDITOR→"Auditor",
  CAPTURISTA→"Capturista". Rol no reconocido → etiqueta vacía y CERO capacidades (rechazo
  por defecto, FR-007).

### Matriz de permisos de UI (NUEVO — constante, espejo de `RolePolicy.cs`)

| Capacidad | Roles | Consumidores UI |
|---|---|---|
| WRITE_INVENTORY | ADMIN, ALMACEN, CAPTURISTA | Fornituras (FAB alta/lote, editar), form fornitura |
| WRITE_TRANSFERS | ADMIN, SUPERVISOR, ALMACEN, CAPTURISTA | Traslados (botón registrar) |
| WRITE_OPERATIONS | ADMIN, SUPERVISOR, CAPTURISTA | Asignación (asistente), Incidencias (registrar/resolver) |
| AUTHORIZE_DECOMMISSION | ADMIN, SUPERVISOR | Bajas (botón registrar baja) |
| WRITE_OFFICERS | ADMIN, SUPERVISOR, CAPTURISTA | Elementos (FAB alta, editar) |
| MANAGE_CONFIG | ADMIN | Almacenes y Tipos (FAB/editar/toggle) |
| MANAGE_LANDING | ADMIN | Configurar landing (menú + página) |
| MANAGE_USERS | ADMIN | Usuarios (menú + página) |
| READ_AUDIT | ADMIN, AUDITOR | Menú "Bitácora de Auditoría" |

Invariante: esta tabla DEBE ser idéntica a `Security/RolePolicy.cs`; cualquier cambio futuro
en el backend exige actualizar el espejo (nota en ambos archivos).

### Ítem de navegación (`NavItem` — existente, regla nueva)

`roles?: UserRole[]` — pasa de literales a derivarse de la matriz:
Bitácora de Auditoría → READ_AUDIT; Usuarios → MANAGE_USERS; Configurar landing →
MANAGE_LANDING; resto sin `roles` (visibles a todo autenticado). Total menú ADMIN: 13 ítems.

### Cuenta administradora inicial (dato — corrección por seeder)

Estado requerido tras cada arranque con `Seed.Admin.Enabled=true` y email configurado:
`role = ADMIN` y `enabled = true` para la fila cuyo `email` = `Seed.Admin.Email`.

Transiciones del *ensure*:
- No existe → se crea (comportamiento actual).
- Existe con `role != ADMIN` → `role = ADMIN` (+ log de corrección).
- Existe con `enabled = false` → `enabled = true` (+ log de corrección).
- Existe correcta → sin escritura (no-op).
- Seed deshabilitado / email vacío → no-op.

### Reglas de validación de formularios (alineadas al backend)

| Formulario | Obligatorios | Formatos |
|---|---|---|
| Fornitura | codigoQr, equipmentTypeId, warehouseId | codigoQr max 60 |
| Fornitura lote | equipmentTypeId, warehouseId | cantidad ≥ 1 |
| Elemento | nombre, apellidoPaterno, placa, sexoId | CURP `^[A-Za-z0-9]{18}$` (opcional), RFC `^[A-Za-z0-9]{12,13}$` (opcional) |
| Usuario | name, email, role | email válido |
| Almacén | codigo, nombre, tipoItemId | emailContacto email (opcional) |
| Traslado | origenId, destinoId | observaciones max 500 |
| Baja | motivoId | observaciones max 500 |
| Incidencia (migra a Reactive Forms) | código de fornitura resuelto, tipo, descripción | descripción max 500 |
| Asignación | fornitura disponible + elemento seleccionado | (asistente, no form clásico) |

Mensajes es-MX por tipo de error (centralizados en el componente `app-field-errors`):
`required` → "Este campo es obligatorio."; `email` → "Ingrese un correo válido.";
`maxlength` → "Máximo {n} caracteres."; `minlength` → "Mínimo {n} caracteres.";
`pattern` → mensaje específico por campo (CURP: "La CURP debe tener 18 caracteres
alfanuméricos."; RFC: "El RFC debe tener 12 o 13 caracteres alfanuméricos.").
