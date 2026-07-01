# Phase 1 — Quickstart: Padrón de elementos

Guía de validación end-to-end. No incluye código de implementación; referencia el contrato
([contracts/officers-api.md](./contracts/officers-api.md)) y el modelo
([data-model.md](./data-model.md)).

## Prerrequisitos

- SQL Server 2022 accesible. La PII se cifra **a nivel de aplicación** (AES-GCM + blind index HMAC,
  ADR 0006); no requiere Always Encrypted ni enclaves.
- Backend `fornituras-api/` con las migraciones Flyway de `officers` + catálogos (`V12`, `V15`) aplicadas.
- Frontend `sigefor/` con la feature `elementos` (listado ya andamiado; formulario de alta nuevo).
- Sesión iniciada (login por email/JWT ya existente) con un usuario `ADMIN` y uno `CAPTURISTA`
  para probar enmascaramiento.

## Variables de entorno (solo nombres — valores en `.env`, nunca en repo)

```
DB_HOST= / DB_PORT= / DB_NAME= / DB_USER= / DB_PASSWORD=
PII_ENCRYPTION_KEY=           # clave AES-GCM del cifrado de PII a nivel app (ADR 0006)
OFFICER_BLIND_INDEX_KEY=      # clave HMAC del blind index de CURP/RFC
PHOTO_STORAGE_TARGET=         # destino del storage cifrado de fotos (gated por ADR 0003)
```

## Arrancar

```powershell
# Backend (desde fornituras-api/)
.\mvnw.cmd spring-boot:run

# Frontend (desde sigefor/)
npm install
npm start
```

## Escenarios de validación

1. **Alta + unicidad de placa** (US2 / SC: cero duplicados)
   - `POST /officers` con placa nueva → **201**.
   - Repetir misma placa (o CURP/RFC normalizados) → **409**.

2. **Búsqueda y paginación** (US1 / SC-001)
   - Cargar varios elementos; `GET /officers?q=PM-1042` → devuelve el correcto.
   - `GET /officers?municipio=Centro&page=0&size=20` → solo ese municipio (texto libre, `LIKE`), paginado.
   - Búsqueda exacta de CURP/RFC (`q=<curp>`) → coincidencia vía blind index. La búsqueda por nombre
     parcial **no está disponible** (cifrado no determinista, ADR 0006).

3. **Enmascaramiento por rol** (FR-005 / SC-004)
   - `GET /officers/{id}` como **CAPTURISTA** → CURP/RFC enmascarados o ausentes.
   - El mismo endpoint como **ADMIN** → ficha completa (si la captura de CURP/RFC está habilitada
     por ADR 0003).
   - Inspeccionar el tráfico: la PII no autorizada **no** viaja al cliente.

4. **Auditoría de acceso** (FR-006 / SC-002)
   - Tras `GET /officers/{id}`, verificar un registro `VIEW_OFFICER` en `audit_log` con actor,
     id, timestamp e IP, **sin** PII en el log.
   - Tras alta/edición, verificar `CREATE_OFFICER` / `UPDATE_OFFICER`.

5. **Cifrado en reposo** (FR-007 / SC-003)
   - Consultar la tabla `officers` directamente en BD → las columnas PII (`nombre`, apellidos,
     `curp`, `rfc`) se ven **cifradas** a nivel app (no texto claro); `curp_idx`/`rfc_idx` son HMAC.

6. **Sin PII en lugares prohibidos** (FR-008 / SC-005)
   - Revisar logs y URLs: ni CURP/RFC/nombre ni la foto aparecen en logs, query strings
     cacheables ni en ningún QR.

## Criterios de aceptación cubiertos

SC-001 (búsqueda paginada < 2 s), SC-002 (100% accesos a ficha auditados), SC-003 (PII cifrada),
SC-004 (sin PII a roles no autorizados), SC-005 (cero PII en logs/URLs/QR). Ver
[spec.md](./spec.md) §Success Criteria.
