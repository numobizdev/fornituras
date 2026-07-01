# 0006. Cifrado de PII a nivel de aplicación (interino)

- **Estado:** Aceptado (interino)
- **Fecha:** 2026-06-30
- **Relacionado:** [0003 Alcance de PII](./0003-pii-elementos.md) (Propuesto),
  [0004 Búsqueda sobre PII cifrada](./0004-busqueda-pii-cifrada.md) (Propuesto)

## Contexto

El padrón de elementos ([`003-elementos-padron`](../../specs/003-elementos-padron/spec.md))
almacena PII de personal policial y la Constitución (Principio I) y
[`docs/02-seguridad.md`](../02-seguridad.md) exigen **cifrado en reposo**. La solución objetivo
([ADR 0004](./0004-busqueda-pii-cifrada.md)) es **SQL Server Always Encrypted + secure enclaves +
blind index**, pero esa infraestructura (CMK/CEK, gestor de secretos, attestation/VBS de enclaves)
**no está disponible** en el entorno actual y depende de decisiones de infraestructura y del área
legal del cliente que siguen abiertas.

No construir nada dejaría 003 bloqueada; construir la tabla con PII **en claro** violaría el
Principio I. Se necesita un mecanismo de cifrado en reposo que **no dependa de infra del cliente**.

## Decisión

Cifrar la PII **en la capa de aplicación** como medida **interina**, hasta que Always Encrypted
esté disponible:

1. **AES-256-GCM** sobre las columnas de PII en claro (`nombre`, `apellido_paterno`,
   `apellido_materno`, `curp`, `rfc`) mediante un `AttributeConverter` de JPA
   (`EncryptedStringConverter`). El valor persistido es `Base64(IV ‖ ciphertext ‖ tag)`; el motor
   nunca ve el texto claro de estas columnas.
2. **Blind index (HMAC-SHA256)** para `curp`/`rfc`: columnas `curp_idx`/`rfc_idx` que permiten
   **igualdad exacta** sin descifrar (igual que [ADR 0004](./0004-busqueda-pii-cifrada.md) §2).
3. **`placa`** es identificador operativo (no PII de máxima sensibilidad, ADR 0004 §3): se guarda
   **en claro**, única y normalizada, permitiendo búsqueda parcial (`LIKE`).
4. **Claves desde el entorno**, nunca en el repo (Principio III): `PII_ENCRYPTION_KEY`
   (clave AES en Base64, 32 bytes) y `OFFICER_BLIND_INDEX_KEY`. Documentadas por **nombre** en
   `.env.example`.
5. **Búsqueda por nombre parcial diferida**: al estar `nombre`/apellidos cifrados de forma no
   determinista, no se soporta `LIKE` sobre ellos en este interino (haría falta enclaves o
   descifrar en memoria, inviable a escala). La búsqueda cubre `placa` (parcial) y `curp`/`rfc`
   (exacta por blind index).

## Consecuencias

- (+) PII cifrada en reposo **sin** depender de Always Encrypted/enclaves ni del gestor de
  secretos del cliente; el padrón puede desarrollarse y probarse ya.
- (+) El blind index resuelve CURP/RFC exactos; la `placa` mantiene búsqueda parcial.
- (+) Migrar a Always Encrypted después no cambia el contrato de la API ni el modelo de dominio
  (solo la estrategia de persistencia/búsqueda).
- (−) El cifrado lo hace la aplicación: la clave vive en su proceso (gestor de secretos pendiente);
  un volcado de memoria del proceso podría exponer claves. Aceptable como interino, no como final.
- (−) Sin búsqueda parcial por nombre hasta migrar a enclaves (limitación aceptada por el
  responsable el 2026-06-30).
- (−) El alcance de qué PII se captura sigue sujeto a **ADR 0003**; CURP/RFC permanecen opcionales
  y la **foto** queda fuera de alcance hasta resolver su storage cifrado.
  **Actualización (2026-07-01):** el storage cifrado de la foto queda resuelto por
  [ADR 0016](./0016-almacenamiento-de-fotos.md) (filesystem local + AES-256-GCM tras
  `FileStoragePort`); su captura sigue condicionada a la base legal de ADR 0003.

## Reversión / camino a la solución final

Cuando se confirmen Always Encrypted + enclaves + gestor de secretos: sustituir
`EncryptedStringConverter` por columnas Always Encrypted, mover la clave del blind index al gestor
de secretos y reactivar la búsqueda confidencial por nombre. El resto del módulo (DTOs, RBAC,
enmascaramiento, auditoría, contrato REST) permanece igual.
