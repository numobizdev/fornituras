# Phase 0 — Research: Padrón de elementos

Decisiones técnicas y alternativas para implementar el padrón cumpliendo el cifrado de PII y la
búsqueda. Formato: Decisión / Justificación / Alternativas.

## 1. Búsqueda sobre columnas de PII cifradas (decisión central)

**Contexto.** La spec exige búsqueda por texto sobre **nombre, CURP, RFC y placa** (FR-001) y la
Constitución exige **cifrado en reposo de la PII** (Always Encrypted). Always Encrypted clásico
solo permite **igualdad** con cifrado **determinista**; **no** soporta `LIKE`/parcial ni rangos.
Hay tensión entre "buscar por nombre parcial" y "nombre cifrado".

**Decisión (recomendada, a registrar como ADR nuevo `0004-busqueda-pii-cifrada`):**

- **Always Encrypted con secure enclaves** (VBS enclaves, soportado en SQL Server 2022) para
  habilitar **consultas confidenciales** —incluido `LIKE`— sobre columnas con cifrado
  **randomizado**, sin exponer el texto en claro al motor. Esto cubre la búsqueda parcial por
  **nombre/apellidos**.
- **Blind index (HMAC determinista)** para CURP y RFC: se guarda una columna `*_idx` con
  `HMAC(clave, normalizado(valor))` que permite **igualdad exacta** sin desbloquear enclaves; la
  clave del índice vive en el gestor de secretos. Cubre "buscar por CURP/RFC exactos".
- **`placa`**: identificador operativo, **no** se trata como PII de máxima sensibilidad (no es
  dato personal en el mismo grado); puede ir en claro y **único**, permitiendo búsqueda directa.

**Justificación.** Mantiene la PII cifrada en reposo y en tránsito hacia el motor, sin renunciar
a la búsqueda. Los enclaves resuelven el `LIKE` de nombre; el blind index evita depender de
enclaves para los identificadores de igualdad exacta.

**Alternativas consideradas.**
- *Determinista sin enclaves:* solo igualdad exacta; rompe la búsqueda parcial por nombre.
  Descartada como solución única.
- *Descifrar en la app y filtrar en memoria:* inviable a escala (decenas de miles); además
  amplía la exposición de PII en el proceso. Descartada.
- *No cifrar nombre para poder buscar:* viola Principio I. Descartada.

> **Pendiente de validación:** disponibilidad de **secure enclaves** en el SQL Server 2022 del
> cliente (requiere attestation/VBS). Si no estuvieran disponibles, el fallback es blind index
> para igualdad + búsqueda por `placa`, degradando la búsqueda parcial de nombre (decisión del
> ADR).

## 2. Foto del elemento (PII biométrica indirecta)

**Decisión.** Almacenar la foto **fuera de la fila**, en storage de objetos/blob **cifrado** con
acceso autorizado; la fila `officers` guarda solo una referencia (`foto_url`/clave de objeto).
La entrega de la foto pasa por el backend con verificación de rol y queda **auditada**.

**Justificación.** Evita inflar la tabla, permite cifrado y control de acceso independientes, y
mantiene la foto fuera de URLs públicas/cacheables (Principio II/IV).

**Alternativas.** Guardar la foto como `VARBINARY` en la fila (más simple, pero complica backups,
streaming y control de acceso fino) — posible para volumen bajo, pero no preferido. Destino
concreto del storage (Azure Blob con CMK vs FS cifrado) queda como incógnita → ADR de
infraestructura.

## 3. Alcance de PII capturada

**Decisión.** Se rige por **ADR 0003 (Propuesto)**: `curp`, `rfc` y `foto` permanecen
`[PENDIENTE]` (captura restringida/deshabilitada) hasta confirmar finalidad y base legal. El
esquema los contempla cifrados desde el inicio para no migrar después.

**Justificación.** Minimización (Principio I) sin cerrar la puerta: el modelo está listo, la
captura se habilita cuando exista base legal.

## 4. Paginación y filtros

**Decisión.** Paginación **del lado servidor** (page/size) con `Pageable` de Spring Data;
filtros por catálogos (`municipio`, `sexo`) que son columnas **en claro** (FK), por lo que se
indexan y filtran normalmente. La búsqueda de texto combina blind index (CURP/RFC), `placa` y
`LIKE` confidencial (nombre vía enclave).

**Justificación.** Cumple SC-001 (< 2 s primera página) sin traer todo al cliente
(`Requerimientos.MD`: paginación obligatoria).

**Alternativas.** Paginación en cliente (descartada: no escala y expone más datos).

## 5. Enmascaramiento por rol

**Decisión.** El **backend** decide qué campos viaja en el DTO según el rol del JWT: `ADMIN` ve
ficha completa; `CAPTURISTA` ve lo operativo con CURP/RFC enmascarados (salvo que la expansión
de roles/ADR diga otra cosa). El frontend **nunca** recibe PII que no deba mostrar.

**Justificación.** Principio IV (mínimo privilegio) y "sin lógica de negocio sensible en el
cliente" (Constitución §Flujo). Evita fugas por inspección del tráfico.

## 6. Auditoría de acceso

**Decisión.** `GET /officers/{id}` (ficha completa) y toda alta/edición generan un registro en
`audit_log` (acción `VIEW_OFFICER`, `CREATE_OFFICER`, `UPDATE_OFFICER`) con actor, entidad_id,
timestamp e IP, **sin** PII en el log. Reutiliza el mecanismo de auditoría de la feature 012.

**Justificación.** Principio V; SC-002 (100% de accesos a ficha auditados).

## Incógnitas restantes (no bloquean el plan)

- Disponibilidad de secure enclaves en el SQL Server del cliente (condiciona §1).
- Gestor de secretos para CMK/CEK y clave del blind index (ADR compartido con QR/JWT).
- Destino del storage de fotos.
- Régimen legal aplicable (retención/ARCO).
