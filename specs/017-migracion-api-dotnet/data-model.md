# Modelo de datos — Migración API .NET

**Feature**: 017-migracion-api-dotnet  
**Referencia**: Flyway V1–V14 (`fornituras-api/src/main/resources/db/migration/`), `docs/03-modelo-datos.md`

> Esquema implementado con **EF Core migrations** en `fornituras-api-dotnet/`. No es obligatorio
> reutilizar los scripts Flyway byte a byte; sí respetar el modelo lógico y las restricciones de
> negocio.

## Entidades

### users

| Campo | Tipo | Notas |
|-------|------|-------|
| id | bigint PK | Identity |
| name | nvarchar | |
| email | nvarchar UNIQUE | Login |
| password | nvarchar | BCrypt hash |
| role | nvarchar | `ADMIN`, `CAPTURISTA` |
| enabled | bit | |
| created_at, updated_at | datetime2 | Auditoría |

### verification_tokens / password_reset_tokens

Tokens de activación y reset (código, expiración, user_id FK).

### equipment_type / size

Catálogo de tipos de prenda y tallas (soft deactivate).

### municipio

Catálogo geográfico.

### warehouse

Almacenes (`CENTRAL`, `REGIONAL`, `MOVIL`, `TEMPORAL`).

### equipment

Inventario de fornituras: código, tipo, talla, almacén, estado, fechas de caducidad derivadas.

**Estados**: `DISPONIBLE`, `ASIGNADA`, `EN_MANTENIMIENTO`, `EN_TRASLADO`, `EXTRAVIADA`, `BAJA_DEFINITIVA`.

### sexo / tipo_sangre

Catálogos para elementos.

### officers

Padrón de elementos policiales. Campos sensibles **cifrados en aplicación** (ADR 0006); blind index
para CURP/RFC; `placa` en claro (identificador operativo).

### assignment

Asignación fornitura ↔ elemento. Una asignación vigente por equipment (índice único filtrado).
Campos: equipment_id, officer_id, fechas, observaciones, recibido_por.

### lote_qr

Lotes de códigos QR secuenciales (`consecutivo_inicial`, `consecutivo_final`, parámetros impresión).
Tabla `codigo_qr` eliminada en V14 — códigos calculados por rango.

## Relaciones principales

```text
User 1──* VerificationToken / PasswordResetToken
EquipmentType 1──* Size
EquipmentType 1──* Equipment
Size 0..1──* Equipment
Warehouse 1──* Equipment
Equipment 1──* Assignment
Officer 1──* Assignment
Municipio 1──* Officer
Sexo / TipoSangre ──* Officer
```

## Enums (Domain)

- `Role`: ADMIN, CAPTURISTA
- `EquipmentStatus`: ver arriba
- `WarehouseType`: CENTRAL, REGIONAL, MOVIL, TEMPORAL
- `ExpiryStatus`: VIGENTE, PROXIMA_A_VENCER, CADUCADA

## Restricciones de negocio críticas

1. **Assignment**: máximo una asignación vigente por `equipment_id` (409 en carrera concurrente).
2. **Equipment status**: transiciones validadas; no bajar a DISPONIBLE con asignación activa sin return.
3. **Officer PII**: cifrado AES-256-GCM; búsqueda exacta CURP/RFC vía HMAC blind index.
4. **QR**: secuencia global con lock en generación de lote; formato `FOR-` + 6 dígitos.
5. **Warehouse delete**: bloqueado si hay equipment vinculado.
