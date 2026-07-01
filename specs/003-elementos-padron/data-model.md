# Phase 1 — Data model: Padrón de elementos

Refina, para esta feature, las entidades de [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
La fuente de verdad del esquema sigue siendo ese documento + las migraciones Flyway; aquí se
detallan columnas, cifrado, índices y validaciones.

> **Actualizado (ADR 0006 + ADR 0007):** la PII se cifra **a nivel de aplicación** (AES-GCM vía
> `EncryptedStringConverter`) + **blind index HMAC** para igualdad de CURP/RFC —no SQL Always
> Encrypted—; y `municipio`/`estado` son **texto libre** (ya no FK a `municipio`). Migraciones:
> `V12__create_officers_and_catalogs.sql` (crea `officers`, `sexo`, `tipo_sangre`) y
> `V15__generic_catalog.sql` (retira `municipio_id` y añade `municipio`/`estado` texto libre).

## `officers` — elemento policial (**PII de alta sensibilidad**)

| Columna           | Tipo            | Cifrado            | Notas / validación                         |
|-------------------|-----------------|--------------------|--------------------------------------------|
| id                | (PK BaseEntity)  | —                  | Identificador interno opaco.               |
| placa             | NVARCHAR único   | en claro           | Identificador operativo; **único**; normalizado (trim/upper). |
| nombre            | NVARCHAR         | App-level AES-GCM  | Requerido. No buscable por texto (cifrado no determinista). |
| apellido_paterno  | NVARCHAR         | App-level AES-GCM  | Requerido.                          |
| apellido_materno  | NVARCHAR (null)  | App-level AES-GCM  | Opcional.                           |
| curp              | NVARCHAR (null)  | App-level AES-GCM  | **[PENDIENTE ADR 0003]** formato CURP; igualdad vía blind index. |
| curp_idx          | VARBINARY (null) | — (es un HMAC)     | Blind index `HMAC(k, normalize(curp))` para búsqueda exacta. |
| rfc               | NVARCHAR (null)  | App-level AES-GCM  | **[PENDIENTE ADR 0003]** formato RFC.       |
| rfc_idx           | VARBINARY (null) | — (es un HMAC)     | Blind index de RFC.                         |
| sexo_id           | FK → sexo        | en claro           | Catálogo (tabla plana); filtrable.          |
| tipo_sangre_id    | FK → tipo_sangre | en claro (FK)      | Catálogo (tabla plana). *(El valor textual es PII sensible, pero el FK no revela nada por sí solo.)* |
| municipio         | NVARCHAR(120) (null) | en claro       | **Texto libre** (ADR 0007). Filtrable por `LIKE`. Antes `municipio_id` FK. |
| estado            | NVARCHAR(120) (null) | en claro       | **Texto libre** (ADR 0007). Añadido en `V15`. |
| foto_url          | NVARCHAR (null)  | referencia         | Apunta a storage cifrado; **[PENDIENTE ADR 0003]**. |
| adscripcion       | NVARCHAR (null)  | en claro           | Unidad/área (no PII fuerte).                |
| status            | enum             | en claro           | activo / baja.                              |
| (auditoría base)  | created/updated  | en claro           | De `BaseEntity` (fecha alta/edición).       |

**Índices.** `UNIQUE(placa)`; índice en `curp_idx`, `rfc_idx`; índices en `sexo_id`, `status` para
filtros. El filtro por `municipio` es un `LIKE` sobre texto en claro. `nombre`/apellidos van cifrados
y **no** se buscan por texto (ver Notas de cifrado).

**Normalización.** `placa`, `curp`, `rfc` se normalizan (trim, mayúsculas, sin espacios) antes de
comparar/indexar, para evitar duplicados "aparentemente distintos".

## Catálogos (en claro, controlados)

- **`sexo`** (tabla plana): `id`, `nombre`. Semilla: MASCULINO, FEMENINO.
- **`tipo_sangre`** (tabla plana): `id`, `etiqueta`. Semilla: O+, O−, A+, A−, B+, B−, AB+, AB−.
- **Municipio/estado**: **ya no son catálogo** (texto libre en `officers`, ADR 0007).

> Migrar `sexo`/`tipo_sangre` a la estructura genérica `catalog → catalog_item` (spec **006**) es un
> **candidato pendiente** (ADR 0007); hoy siguen como tablas planas con sus propias entidades
> (`Sexo`, `TipoSangre`) y repositorios, expuestas por `OfficerCatalogController`.

## Relaciones

- `officers` N—1 `sexo`, `tipo_sangre` (tablas planas); `municipio`/`estado` son texto libre.
- `officers` 1—N `assignment` (feature **004**; un elemento puede tener histórico de resguardos).
- Acceso a la ficha completa → `audit_log` (feature **012**), no es FK pero sí evento obligatorio.

## Reglas de validación (en el borde — Principio IV)

- `placa` requerida y única (normalizada).
- `nombre`, `apellido_paterno` requeridos.
- `curp` (si se captura): 18 caracteres, formato CURP válido.
- `rfc` (si se captura): 12–13 caracteres, formato RFC válido.
- `sexo_id` debe existir en catálogo; `tipo_sangre_id` opcional pero validado; `municipio`/`estado`
  texto libre opcional (límite de longitud).
- Foto: tipo/imagen y tamaño máximo acotados; se rechaza lo demás.

## Transiciones de estado

`activo` → `baja` (el elemento deja el cuerpo). La política de **retención/anonimización** del
elemento dado de baja (vs. conservar por histórico de asignaciones) queda sujeta a ADR 0003 /
régimen legal.

## Notas de cifrado (resumen — ADR 0006, interino)

- **Cifrado a nivel de aplicación** (AES-GCM vía `EncryptedStringConverter`, `common/crypto/PiiCipher`)
  para `nombre`/apellidos/`curp`/`rfc`; la `placa` va en claro (única, normalizada).
- **Blind index HMAC** (`common/crypto/BlindIndexer`) para igualdad exacta de `curp`/`rfc`
  (`curp_idx`/`rfc_idx`).
- Claves (`PII_ENCRYPTION_KEY`, `OFFICER_BLIND_INDEX_KEY`) en **entorno / gestor de secretos** (nunca
  en repo — Principio III; ADR 0004).
- **Búsqueda por nombre parcial no disponible** (cifrado no determinista). La migración futura a SQL
  **Always Encrypted + enclaves** (para `LIKE` confidencial) queda registrada como reversión posible
  en ADR 0006; **TDE** a nivel base es complementario.
