# Phase 1 — Quickstart: Padrón de elementos

Guía de validación end-to-end. No incluye código de implementación; referencia el contrato
([contracts/officers-api.md](./contracts/officers-api.md)) y el modelo
([data-model.md](./data-model.md)).

## Prerrequisitos

- SQL Server 2022 accesible (TDE habilitado; **secure enclaves** si se valida búsqueda parcial
  sobre PII cifrada — ver research §1). Para desarrollo, Testcontainers levanta MSSQL.
- Backend `fornituras-api/` con la migración Flyway de `officers` + catálogos aplicada.
- Frontend `sigefor/` con la feature `elementos` (listado ya andamiado; formulario de alta nuevo).
- Sesión iniciada (login por email/JWT ya existente) con un usuario `ADMIN` y uno `CAPTURISTA`
  para probar enmascaramiento.

## Variables de entorno (solo nombres — valores en `.env`, nunca en repo)

```
DB_HOST= / DB_PORT= / DB_NAME= / DB_USER= / DB_PASSWORD=
SQLSERVER_AE_ENABLED=         # columnEncryptionSetting=Enabled en la cadena de conexión
AE_CMK_PROVIDER=              # proveedor del Column Master Key (gestor de secretos)
OFFICER_BLIND_INDEX_KEY=      # clave HMAC del blind index de CURP/RFC
PHOTO_STORAGE_TARGET=         # destino del storage cifrado de fotos
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
   - `GET /officers?municipioId=7&page=0&size=20` → solo ese municipio, paginado.
   - Búsqueda parcial por apellido (`q=GARC`) → coincidencias (requiere enclave; si no hay
     enclave, validar el fallback definido en el ADR).

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
   - Consultar la tabla `officers` con una conexión **sin** Always Encrypted habilitado → las
     columnas PII se ven como **cifradas** (no texto claro).

6. **Sin PII en lugares prohibidos** (FR-008 / SC-005)
   - Revisar logs y URLs: ni CURP/RFC/nombre ni la foto aparecen en logs, query strings
     cacheables ni en ningún QR.

## Criterios de aceptación cubiertos

SC-001 (búsqueda paginada < 2 s), SC-002 (100% accesos a ficha auditados), SC-003 (PII cifrada),
SC-004 (sin PII a roles no autorizados), SC-005 (cero PII en logs/URLs/QR). Ver
[spec.md](./spec.md) §Success Criteria.
