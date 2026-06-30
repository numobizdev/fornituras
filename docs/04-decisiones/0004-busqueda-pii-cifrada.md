# 0004. Búsqueda sobre columnas de PII cifradas

- **Estado:** Propuesto
- **Fecha:** 2026-06-30

## Contexto

El padrón de elementos (spec [`003-elementos-padron`](../../specs/003-elementos-padron/spec.md))
debe permitir **buscar por nombre, CURP, RFC y placa** (FR-001) y, a la vez, mantener la **PII
cifrada en reposo** con **Always Encrypted** (Constitución, Principio I;
[`docs/02-seguridad.md`](../02-seguridad.md) §3).

Hay una tensión técnica real: **Always Encrypted clásico** solo soporta **igualdad** con cifrado
**determinista**; **no** permite `LIKE`/búsqueda parcial ni rangos. Una búsqueda "por nombre
parcial" sobre una columna cifrada de forma estándar es imposible sin descifrar.

## Decisión

*(Pendiente de confirmar la disponibilidad de secure enclaves en el SQL Server 2022 del cliente.
Recomendación técnica abajo.)*

**Recomendación: estrategia mixta.**

1. **Always Encrypted con secure enclaves** (VBS enclaves de SQL Server 2022) para las columnas
   `nombre`, `apellido_paterno`, `apellido_materno` con cifrado **randomizado**. Habilita
   **consultas confidenciales** —incluido `LIKE`— sin exponer el texto en claro al motor.
2. **Blind index (HMAC determinista)** para `curp` y `rfc`: columnas `curp_idx`/`rfc_idx` con
   `HMAC(clave, normalize(valor))` que permiten **igualdad exacta** sin depender de enclaves. La
   clave del índice vive en el **gestor de secretos** (nunca en el repo — Principio III).
3. **`placa`** se trata como identificador operativo (no PII de máxima sensibilidad): se guarda
   **en claro**, **única** y normalizada, permitiendo búsqueda directa.
4. **Normalización** (trim, mayúsculas, sin espacios) de `placa`, `curp` y `rfc` antes de
   indexar/comparar, para evitar duplicados "aparentemente distintos".

## Alternativas consideradas

- **Solo determinista, sin enclaves:** únicamente igualdad exacta; rompe la búsqueda parcial por
  nombre. Descartada como solución única (sirve solo de fallback, ver Consecuencias).
- **Descifrar en la aplicación y filtrar en memoria:** inviable a escala (decenas de miles de
  registros) y amplía la exposición de PII en el proceso. Descartada.
- **No cifrar `nombre` para poder buscar:** viola el Principio I. Descartada.
- **Búsqueda externa (motor de búsqueda) sobre PII:** multiplicaría la superficie de PII fuera
  de la BD cifrada. Descartada.

## Consecuencias

- (+) La PII permanece cifrada en reposo y frente al motor, sin renunciar a la búsqueda.
- (+) El blind index resuelve CURP/RFC exactos sin requerir enclaves.
- (−) Los **secure enclaves** exigen configuración (attestation/VBS) y verificar soporte en el
  entorno del cliente. **Si no estuvieran disponibles**, el fallback es: blind index para
  igualdad + búsqueda por `placa`, **degradando** la búsqueda parcial de nombre (a confirmar con
  el cliente como limitación aceptable o no).
- (−) Introduce columnas de índice (`*_idx`) y dependencia del gestor de secretos para su clave.

## Decisiones relacionadas / pendientes

- Alcance de PII capturada (CURP/RFC/foto): [ADR 0003](./0003-pii-elementos.md) (Propuesto).
- Gestor de secretos que custodia CMK/CEK y la clave del blind index (ADR pendiente, compartido
  con QR/JWT).
- Confirmar disponibilidad de secure enclaves en el SQL Server 2022 del cliente.
