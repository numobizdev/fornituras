# Data Model — 019 Identidad del sistema

## Sin cambios de esquema

Esta feature **no crea ni altera tablas, columnas ni índices**. Opera sobre la entidad
existente de la spec 016:

## Entidad afectada: `landing_section`

(Definida en `fornituras-api-dotnet/src/Fornituras.Api/Data/Entities/LandingSection.cs`;
esquema en la migración `20260701235121_InitialCreate`.)

| Columna      | Tipo           | Relevancia para esta feature                      |
|--------------|----------------|---------------------------------------------------|
| `id`         | bigint PK      | —                                                 |
| `scope`      | nvarchar(10)   | Filtro: `'PUBLIC'`                                |
| `type`       | nvarchar(20)   | Filtro: `'HERO'`                                  |
| `titulo`     | nvarchar(160)  | Valor a actualizar (nombre visible del sistema)   |
| `updated_at` | datetime2      | Se actualiza a `SYSUTCDATETIME()` en la migración |

## Regla de actualización de datos (FR-002, FR-003)

```sql
UPDATE landing_section
SET titulo = 'Sistema Integral de Gestión de Fornituras',
    updated_at = SYSUTCDATETIME()
WHERE scope = 'PUBLIC'
  AND type = 'HERO'
  AND titulo = 'Sistema de Gestión de Blindajes';
```

Propiedades:

- **Idempotente**: tras la primera ejecución ya no hay filas que coincidan; re-ejecutar afecta 0 filas.
- **Conservadora**: un título editado por el administrador (cualquier valor ≠ al sembrado
  exacto) no coincide con el `WHERE` y se conserva íntegro (FR-003, SC-003).
- El nuevo valor (41 caracteres) cabe holgadamente en `nvarchar(160)`.

## Valores sembrados (instalaciones nuevas — FR-001)

`DataSeeder.SeedLandingAsync` pasa a sembrar el HERO `PUBLIC` con
`Titulo = "Sistema Integral de Gestión de Fornituras"`. El resto de secciones sembradas
(HOME hero, aviso, accesos rápidos) no cambian.
