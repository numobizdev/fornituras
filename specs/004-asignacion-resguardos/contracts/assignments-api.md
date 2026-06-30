# Phase 1 — Contrato REST: Asignación y resguardos

Endpoints del módulo `assignments`. Base sugerida: **`/api/v1/assignments`**. Requieren
`Authorization: Bearer <jwt>` y autorización por rol. Respuestas en `ApiResponse<T>`. Ninguna
respuesta expone PII del elemento a roles no autorizados (enmascaramiento heredado de 003).

## Resolver fornitura por código (paso 1) — vive en 001, se consume aquí

`GET /api/v1/equipment/by-codigo/{codigo}` → **200** `ApiResponse<EquipmentSummary>`
(descripción + `status`/disponibilidad) · **404** si el código no liga a una fornitura.
La validación de formato `^FOR-[0-9A-Z]{5}$` puede hacerse en cliente (spec 014); la resolución es
**server-side**.

## Listar asignaciones vigentes

`GET /api/v1/assignments?page=&size=` → **200** `ApiResponse<Page<AssignmentSummary>>`

```jsonc
// AssignmentSummary
{
  "id": 55,
  "fornitura": { "id": 1001, "codigo": "FOR-9A3KQ", "descripcion": "Chaleco IIIA talla M" },
  "elemento":  { "id": 123, "nombreCompleto": "GARCÍA LÓPEZ, JUAN", "placa": "PM-1042" }, // enmascarado por rol
  "fechaAsignacion": "2026-06-30T17:00:00Z"
}
```

## Asignar (paso 2 → confirmar)

`POST /api/v1/assignments` → **201** `ApiResponse<AssignmentSummary>`

```jsonc
{ "equipmentId": 1001, "officerId": 123, "firmaUrl": null, "observaciones": "Entrega inicial" }
```

- **201** crea la asignación, pone la fornitura en `asignada`, audita `ASSIGN`, opcionalmente
  genera el resguardo.
- **409** si la fornitura ya no está disponible (incluye carrera concurrente — índice único
  filtrado).
- **422/400** si la fornitura no es asignable (estado) o el elemento no existe/está de baja.

## Devolver (recepción)

`POST /api/v1/assignments/{id}/return` → **200** `ApiResponse<AssignmentSummary>`

- Cierra la asignación vigente (`fecha_devolucion`, `recibido_por`), vuelve la fornitura a
  `disponible`, audita `RETURN`. **409** si la asignación ya no está vigente.

## Reasignar

`POST /api/v1/assignments/reassign` → **201** `ApiResponse<AssignmentSummary>`

```jsonc
{ "equipmentId": 1001, "newOfficerId": 456, "observaciones": "Rotación" }
```

- En una transacción: cierra la vigente del equipo y abre una nueva para `newOfficerId`. Conserva
  historial. Audita `REASSIGN`.

## Resguardo (PDF)

`GET /api/v1/assignments/{id}/resguardo` → **200** `application/pdf` (generado al vuelo).
Acceso por rol; la generación/descarga se audita.

## Autorización — resumen

| Endpoint | ADMIN | CAPTURISTA | Notas |
|----------|:-----:|:----------:|-------|
| GET /assignments | ✅ | ✅ (PII enmascarada) | listado vigentes |
| POST /assignments | ✅ | ✅ | asignar |
| return / reassign | ✅ | ✅ | historial + auditoría |
| GET resguardo | ✅ | ✅ | descarga auditada |

> La expansión de roles (spec 013) ajustará esta matriz vía ADR antes de ampliar el enum `Role`.
