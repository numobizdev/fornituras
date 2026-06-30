# Phase 1 — Data model: Padrón de elementos

Refina, para esta feature, las entidades de [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
La fuente de verdad del esquema sigue siendo ese documento + las migraciones Flyway; aquí se
detallan columnas, cifrado, índices y validaciones.

## `officers` — elemento policial (**PII de alta sensibilidad**)

| Columna           | Tipo            | Cifrado            | Notas / validación                         |
|-------------------|-----------------|--------------------|--------------------------------------------|
| id                | (PK BaseEntity)  | —                  | Identificador interno opaco.               |
| placa             | NVARCHAR único   | en claro           | Identificador operativo; **único**; normalizado (trim/upper). |
| nombre            | NVARCHAR         | Always Encrypted (rand. + enclave) | Requerido. Búsqueda `LIKE` vía enclave. |
| apellido_paterno  | NVARCHAR         | Always Encrypted (rand. + enclave) | Requerido.                          |
| apellido_materno  | NVARCHAR (null)  | Always Encrypted (rand. + enclave) | Opcional.                           |
| curp              | NVARCHAR (null)  | Always Encrypted (determinista)    | **[PENDIENTE ADR 0003]** formato CURP; igualdad exacta. |
| curp_idx          | VARBINARY (null) | — (es un HMAC)     | Blind index `HMAC(k, normalize(curp))` para búsqueda exacta. |
| rfc               | NVARCHAR (null)  | Always Encrypted (determinista)    | **[PENDIENTE ADR 0003]** formato RFC.       |
| rfc_idx           | VARBINARY (null) | — (es un HMAC)     | Blind index de RFC.                         |
| sexo_id           | FK → sexo        | en claro           | Catálogo; filtrable.                        |
| tipo_sangre_id    | FK → tipo_sangre | en claro (FK)      | Catálogo. *(El valor textual es PII sensible, pero el FK no revela nada por sí solo.)* |
| municipio_id      | FK → municipio   | en claro           | Catálogo; filtrable.                        |
| foto_url          | NVARCHAR (null)  | referencia         | Apunta a storage cifrado; **[PENDIENTE ADR 0003]**. |
| adscripcion       | NVARCHAR (null)  | en claro           | Unidad/área (no PII fuerte).                |
| status            | enum             | en claro           | activo / baja.                              |
| (auditoría base)  | created/updated  | en claro           | De `BaseEntity` (fecha alta/edición).       |

**Índices.** `UNIQUE(placa)`; índice en `curp_idx`, `rfc_idx`; índices en `municipio_id`,
`sexo_id`, `status` para filtros. El `LIKE` sobre `nombre`/apellidos usa enclave (no índice
b-tree clásico).

**Normalización.** `placa`, `curp`, `rfc` se normalizan (trim, mayúsculas, sin espacios) antes de
comparar/indexar, para evitar duplicados "aparentemente distintos".

## Catálogos (en claro, controlados)

- **`sexo`**: `id`, `nombre`. Semilla: Masculino, Femenino (+ los que defina el cliente).
- **`tipo_sangre`**: `id`, `nombre`. Semilla: O+, O−, A+, A−, B+, B−, AB+, AB−.
- **`municipio`**: `id`, `nombre`. Semilla: catálogo de municipios del estado del cliente.

## Relaciones

- `officers` N—1 `sexo`, `tipo_sangre`, `municipio`.
- `officers` 1—N `assignment` (feature **004**; un elemento puede tener histórico de resguardos).
- Acceso a la ficha completa → `audit_log` (feature **012**), no es FK pero sí evento obligatorio.

## Reglas de validación (en el borde — Principio IV)

- `placa` requerida y única (normalizada).
- `nombre`, `apellido_paterno` requeridos.
- `curp` (si se captura): 18 caracteres, formato CURP válido.
- `rfc` (si se captura): 12–13 caracteres, formato RFC válido.
- `sexo_id`, `municipio_id` deben existir en catálogo; `tipo_sangre_id` opcional pero validado.
- Foto: tipo/imagen y tamaño máximo acotados; se rechaza lo demás.

## Transiciones de estado

`activo` → `baja` (el elemento deja el cuerpo). La política de **retención/anonimización** del
elemento dado de baja (vs. conservar por histórico de asignaciones) queda sujeta a ADR 0003 /
régimen legal.

## Notas de cifrado (resumen, ver research §1)

- **TDE** a nivel base (cifra datos/backups).
- **Always Encrypted** randomizado + **secure enclaves** para `nombre`/apellidos (permite `LIKE`).
- **Always Encrypted** determinista para `curp`/`rfc` (igualdad) + **blind index** para búsqueda.
- CMK/CEK y clave del blind index en **gestor de secretos** (nunca en repo — Principio III).
