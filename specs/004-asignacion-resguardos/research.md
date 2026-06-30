# Phase 0 — Research: Asignación de fornituras y resguardos

Decisiones técnicas. Formato: Decisión / Justificación / Alternativas.

## 1. Dependencias 001 (Fornitura) y 003 (Elemento) — orden de implementación

**Contexto.** Los módulos backend `equipment` (001) y `officers` (003) **no existen aún**; solo
están `auth`, `users`, `qrcodes`. La asignación los necesita.

**Decisión.** Definir el **mínimo viable** que 004 requiere de cada uno:
- De **001**: entidad `equipment` con su `codigo_qr` (FK al `codigo_qr.codigo` existente), `status`
  y un endpoint **`GET /equipment/by-codigo/{codigo}`** que devuelva la fornitura y su
  disponibilidad. Sin esto, el paso 1 del wizard no funciona.
- De **003**: búsqueda de elemento (`GET /officers?q=...`) con enmascaramiento por rol. Sin esto,
  el paso 2 no funciona.

**Justificación.** 004 es el núcleo, pero es una capa **sobre** inventario y padrón. Implementar
001 y 003 (al menos su mínimo) **antes** evita stubs frágiles.

**Alternativas.** Mockear equipment/officer en 004 (descartado: genera retrabajo y acoplamiento
falso).

## 2. Una sola asignación vigente por fornitura (concurrencia)

**Contexto.** SC-004 / FR (spec): una fornitura nunca debe quedar con dos asignaciones vigentes,
incluso ante dos solicitudes concurrentes.

**Decisión.** **Índice único filtrado** en SQL Server:
`CREATE UNIQUE INDEX ux_assignment_vigente ON assignment(equipment_id) WHERE fecha_devolucion IS NULL;`
Complementado con una **transacción** que valida disponibilidad y crea la asignación; la base
rechaza la segunda inserción concurrente (violación de unicidad → 409 "ya no disponible").

**Justificación.** El índice filtrado da garantía a nivel de motor (no depende de timing de la
app). SQL Server soporta índices únicos filtrados.

**Alternativas.** Bloqueo pesimista (`SELECT ... FOR UPDATE`/`UPDLOCK`) — válido pero más costoso;
se puede combinar. Solo validación en la app (descartado: condición de carrera).

## 3. Generación del resguardo (PDF)

**Decisión.** Generar el **PDF del resguardo al vuelo** reutilizando la librería de PDF ya usada
por `qrcodes` (`QrPdfService`), en un `ResguardoPdfService`. Persistir solo **metadatos** del
resguardo (a qué asignación pertenece, cuándo se emitió, si lleva firma), no necesariamente el
binario.

**Justificación.** Reutilización (estilo LEGO; Principio VI sin dependencias nuevas); evita
almacenar PDFs voluminosos. Se regenera de forma determinista desde la asignación.

**Alternativas.** Plantilla Thymeleaf → PDF (también viable; ya hay Thymeleaf en el proyecto).
Persistir el PDF firmado (necesario solo si la firma debe congelarse; ver §4).

## 4. Firma electrónica (alcance)

**Decisión.** La firma electrónica es **opcional** en el MVP: si el dispositivo la captura
(p. ej. firma manuscrita en canvas o firma del usuario), se adjunta al resguardo; si no, el
resguardo se emite **sin firma** y el evento de entrega/recepción queda **auditado**.

**Justificación.** No bloquear la operación por falta de hardware/feature; la trazabilidad la da
la auditoría. El nivel legal de la firma (simple vs avanzada) se decide después (posible ADR).

**Alternativas.** Exigir firma siempre (descartado: bloquea operación en campo). Firma avanzada
con certificado (fuera de alcance MVP).

## 5. Reasignación y devolución

**Decisión.** Reasignar = **cerrar** la asignación vigente (set `fecha_devolucion`, `recibido_por`)
y **abrir** una nueva en una sola transacción, conservando el historial. Devolver = solo cerrar y
volver la fornitura a `disponible`.

**Justificación.** Mantiene el historial (Principio V) y la coherencia de estado de la fornitura.

## 6. Enmascaramiento y auditoría

**Decisión.** El listado de vigentes y la búsqueda de elemento aplican enmascaramiento por rol
(reusa 003). ASSIGN/RETURN/REASSIGN generan auditoría (actor, equipment_id, officer_id, fecha) sin
PII (reusa 012). La resolución `codigo → fornitura` no escribe el código crudo en logs si no aporta.

## Incógnitas restantes (no bloquean el plan, sí la implementación)

- Nivel legal de la firma electrónica (simple/avanzada) → posible ADR.
- Formato/plantilla exacta del resguardo (logo institucional, campos).
- Si la fornitura debe quedar `asignada` o conservar sub-estados (p. ej. en traslado bloquea).
