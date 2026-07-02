# Checklist de cobertura: specs 001–017 en `fornituras-api-dotnet/`

Matriz de auditoría (US1/US2/US3 de la spec 018). Marcar `[X]` cuando el elemento está verificado
como **presente** en el backend .NET con evidencia; si falta o diverge, dejar `[ ]` y anotar la
**brecha** (severidad + qué falta). Referencia de comparación: backend Java en `fornituras-api/`.

Leyenda de estado por spec: **C** = Cubierta · **P** = Parcial · **A** = Ausente · **FE** = Fuera
de alcance del backend (frontend).

---

## Transversal — Seguridad (`docs/02-seguridad.md` §8) · US3

- [ ] Ningún endpoint queda sin `[Authorize]` salvo los públicos decididos (login, recuperación, landing pública, health)
- [ ] Las excepciones públicas del .NET coinciden con las del backend Java (sin nuevas superficies anónimas)
- [ ] PII de `Officer` cifrada AES-256-GCM (`PiiCipher`) + blind index (`BlindIndexer`) para búsqueda
- [ ] Enmascaramiento de PII por rol (`RolePolicy.CanViewFullPii`) preservado
- [ ] QR no contiene PII (identificador opaco) — sin regresión
- [ ] Auditoría append-only e inmutable (12) preservada; sin PII en logs
- [ ] Rate limiting donde aplique (login, landing pública) — verificar equivalente de Bucket4j
- [ ] Brecha conocida `/qr/**` sin auth: confirmar si persiste/cambió en .NET y documentar

## Transversal — Endpoints y datos (US2)

- [ ] Inventario de endpoints Java vs .NET: cada endpoint Java tiene equivalente o es decisión documentada
- [ ] Esquema de datos: cada tabla/columna/índice/constraint/enum de Java existe en migraciones EF Core
- [ ] Envoltura `ApiResponse<T>` y manejo de errores (400/401/403/404/409/413/422/429) equivalentes
- [ ] Semilla de datos (`DataSeeder`) equivalente al `data.sql`/seed de Java

---

## Por spec

### 001-inventario-equipos → `EquipmentController` / `EquipmentService` / `Equipment`
- [ ] Alta/edición/consulta/baja lógica de equipos · [ ] `foto_url`, estados (`EquipmentStatus`) · [ ] validaciones (código único normalizado) · **Estado: __**

### 002-qr-equipos → `QrController` / `QrService` / `LoteQr`
- [ ] Generación de código QR opaco + lotes · [ ] formato QR (ADR 0005) · [ ] descarga PDF/ZIP · **Estado: __**

### 003-elementos-padron → `OfficersController` / `OfficerService` / `Officer`
- [ ] CRUD elementos · [ ] PII cifrada (nombre/curp/rfc) + blind index · [ ] enmascaramiento por rol · [ ] unicidad placa/CURP/RFC · **Estado: __**

### 004-asignacion-resguardos → `AssignmentsController` / `AssignmentService` / `Assignment`
- [ ] Asignar/devolver equipo a elemento · [ ] índice de asignación vigente único · [ ] historial · **Estado: __**

### 005-almacenes → `WarehousesController` / `WarehouseService` / `Warehouse`
- [ ] CRUD almacenes · [ ] responsable/ubicación/capacidad · [ ] tipo por catálogo (ADR 0007) · **Estado: __**

### 006-tipos-fornitura → `CatalogController` / `Equipment` (tipo) / catálogo de tipos
- [ ] Tipos de prenda como catálogo · [ ] tallas · [ ] `foto_url` de tipo · **Estado: __**

### 007-traslados → `TransfersController` / `TransferService` / `Transfer`+`TransferItem`
- [ ] Crear/enviar/recibir traslado entre almacenes · [ ] estados (`TransferStatus`) · [ ] ítems · **Estado: __**

### 008-incidencias → `IncidentsController` / `IncidentService` / `Incident`
- [ ] Reportar/seguir incidencia sobre equipo · [ ] tipos/estados · **Estado: __**

### 009-bajas → `DecommissionsController` / `DecommissionService` / `Decommission`+`DecommissionReason`
- [ ] Dar de baja equipo con motivo · [ ] autorización de baja (ADR/roles) · **Estado: __**

### 010-dashboard → `DashboardController` / `DashboardService`
- [ ] KPIs/conteos · [ ] agregados sin fugas de PII · **Estado: __**

### 011-reportes → `ReportsController` / `ReportService`
- [ ] Exportación (Excel) · [ ] verificar equivalente .NET de Apache POI (ADR 0011) · **Estado: __**

### 012-auditoria → `AuditController` / `AuditLogService` / `AuditWriter` / `AuditLog`
- [ ] Bitácora append-only (ADR 0012) · [ ] cadena de hash/`prev_hash` · [ ] consulta por rol AUDITOR · **Estado: __**

### 013-usuarios → `UsersController` / `AuthController` / `RolePolicy` / `User`
- [ ] CRUD usuarios · [ ] login JWT + recuperación por código · [ ] RBAC 5 roles (ADR 0013) · [ ] MFA gated (ADR 0014) · **Estado: __**

### 014-escaneo-qr → (frontend `sigefor`) + resolución en `QrController`
- [ ] Endpoint de resolución de QR autenticado · [ ] escaneo cámara/manual es **FE** (frontend) · **Estado: __**

### 015-catalogos-sexo-sangre → `CatalogController` / `Catalog`+`CatalogItem`
- [ ] Catálogos SEXO y TIPO_SANGRE sembrados · [ ] uso en alta de elemento · **Estado: __**

### 016-landing-configurable → `LandingController` / `LandingService` / `LandingSection`
- [ ] Landing pública (no-PII) + panel · [ ] rate limiting público · [ ] anti-XSS por texto plano (ADR 0015) · **Estado: __**

### 017-gestion-de-fotos → `MediaController` / `MediaService` / `MediaAsset` ✅ (recién portada)
- [ ] `/media` autenticado · [ ] cifrado en reposo + EXIF stripping (ImageSharp) · [ ] RBAC/gating PII · **Estado: __**

### 017-migracion-api-dotnet → (la migración misma)
- [ ] Contrato Ionic (`contracts/ionic-api-contract.md`) cumplido punto por punto · [ ] path base `/sigefor` · [ ] todo lo que prometió la spec de migración está presente · **Estado: __**

---

## Salidas de la auditoría

- [ ] Tabla resumen con estado por spec (C/P/A/FE)
- [ ] Lista de **brechas** priorizadas (severidad + spec + remediación propuesta)
- [ ] Lista de **decisiones de migración** (diferencias intencionales) con justificación/ADR
- [ ] Confirmación de que no se modificó código de producción durante la auditoría (SC-005)
