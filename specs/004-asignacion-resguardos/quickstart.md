# Phase 1 — Quickstart: Asignación y resguardos

Guía de validación end-to-end. Referencia [contracts/assignments-api.md](./contracts/assignments-api.md)
y [data-model.md](./data-model.md). No incluye código de implementación.

## Prerrequisitos

- **Mínimo de 001** implementado: entidad `equipment` con `codigo_qr` y
  `GET /equipment/by-codigo/{codigo}` devolviendo descripción + `status`.
- **Mínimo de 003** implementado: `GET /officers?q=...` con enmascaramiento por rol.
- Módulo `qrcodes` (existente) con al menos un `codigo_qr` `FOR-XXXXX` ligado a una fornitura
  `disponible`.
- Migración Flyway de `assignment` aplicada (incluido el índice único filtrado de vigencia).
- Sesión iniciada (JWT) con un usuario `ADMIN` y uno `CAPTURISTA`.

## Arrancar

```powershell
# Backend (desde fornituras-api/)
.\mvnw.cmd spring-boot:run
# Frontend (desde sigefor/)
npm install; npm start
```

## Escenarios de validación

1. **Asignar (flujo 2 pasos)** (US2 / SC-001)
   - Paso 1: `GET /equipment/by-codigo/FOR-9A3KQ` → fornitura `disponible` con descripción.
   - Paso 2: `GET /officers?q=PM-1042` → elemento con foto/nombre (según rol).
   - `POST /assignments {equipmentId, officerId}` → **201**; la fornitura pasa a `asignada` y
     aparece en `GET /assignments` (vigentes).

2. **Bloqueo de no disponibles** (US2 / SC-002)
   - Repetir `POST /assignments` sobre la misma fornitura → **409** "ya no disponible".
   - Intentar asignar una fornitura en `mantenimiento`/`baja`/`en_traslado` → **422**.

3. **Concurrencia** (SC-004)
   - Lanzar dos `POST /assignments` simultáneos sobre la misma fornitura disponible → exactamente
     **uno** crea (201), el otro **409** (índice único filtrado).

4. **Devolución y reasignación** (US3 / SC-003)
   - `POST /assignments/{id}/return` → fornitura vuelve a `disponible`; la asignación queda en el
     historial con `fecha_devolucion`.
   - `POST /assignments/reassign {equipmentId, newOfficerId}` → cierra la vigente y abre otra;
     el historial conserva ambas.

5. **Resguardo**
   - `GET /assignments/{id}/resguardo` → PDF descargable; la descarga se audita.

6. **Auditoría y PII** (FR-008/009 / Principios IV, V)
   - Verificar registros `ASSIGN`/`RETURN`/`REASSIGN` en `audit_log` sin PII.
   - Como `CAPTURISTA`, el listado y la búsqueda de elemento muestran PII enmascarada.

## Criterios de aceptación cubiertos

SC-001 (asignar < 1 min), SC-002 (no disponibles bloqueadas), SC-003 (historial + auditoría),
SC-004 (cero colisiones de vigencia). Ver [spec.md](./spec.md) §Success Criteria.
