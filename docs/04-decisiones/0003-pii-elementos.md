# 0003. Alcance de PII del elemento policial (CURP, RFC, foto)

- **Estado:** Propuesto
- **Fecha:** 2026-06-30

## Contexto

`Requerimientos.MD` §2 pide que el **padrón de elementos** (spec
[`003-elementos-padron`](../../specs/003-elementos-padron/spec.md)) capture **nombre y
apellidos, sexo, tipo de sangre, CURP, RFC, identificador (placa/serie), municipio y
fotografía**.

La Constitución (Principio I) y [`docs/02-seguridad.md`](../02-seguridad.md) §1 exigen
**minimizar la PII**: recolectar solo lo estrictamente necesario para la finalidad, con base
legal (LFPDPPP / LGPDPPSO según el régimen del responsable, aún por confirmar). Estos datos son
**PII de personal de seguridad pública en México**; una fuga puede poner vidas en riesgo.

Hay tensión directa entre el requerimiento (capturar todo) y el principio (minimizar). Esta
decisión la debe **confirmar el responsable / área legal del cliente**; el ADR queda
**Propuesto** hasta entonces.

## Decisión

*(Pendiente de confirmación del responsable. Recomendación técnica abajo.)*

**Recomendación: Postura A acotada.** Capturar el conjunto que tenga **finalidad declarada y
base legal** (control de dotación e identificación inequívoca del resguardatario), con estos
controles **obligatorios**:

1. **Always Encrypted** para nombre, apellidos, CURP, RFC y tipo de sangre; **foto** en storage
   cifrado con acceso autorizado (mecanismo resuelto por
   [ADR 0016](./0016-almacenamiento-de-fotos.md); su captura sigue condicionada a la base legal
   de este ADR).
2. **RBAC con enmascaramiento por defecto**: CURP/RFC/foto visibles solo a roles autorizados; el
   resto los ve enmascarados (ver spec **013-usuarios**).
3. **Auditoría** de todo acceso a la ficha completa y de toda exportación (specs **012**, **011**).
4. **Minimización efectiva**: `curp`, `rfc` y `foto` permanecen **[PENDIENTE]** (captura
   deshabilitada o restringida a un solo rol) hasta que exista finalidad documentada y base
   legal confirmada.
5. **Retención y ARCO**: política de conservación y baja/anonimización de datos documentada.

## Alternativas consideradas

- **Postura A plena (capturar todo desde el inicio):** cumple el requerimiento literal pero
  asume mayor superficie de riesgo sin haber confirmado finalidad/base legal de cada campo.
- **Postura B (minimizar y diferir CURP/RFC/foto):** recolectar solo placa, nombre, municipio y
  foto. Cumple mejor el principio de minimización pero choca con el requerimiento; puede
  insuficiente para identificación inequívoca.

## Consecuencias

- (+) Cumple el requerimiento de captura sin renunciar a los controles de seguridad.
- (+) El enmascaramiento + cifrado + auditoría limitan el radio de daño ante una fuga.
- (−) Mayor complejidad (Always Encrypted, gestión de claves, enmascaramiento por rol).
- (−) Depende de confirmar el **régimen legal** aplicable (decisión pendiente, ver
  [`README`](./README.md)).

## Decisiones relacionadas / pendientes

- Régimen legal de protección de datos aplicable (ADR pendiente).
- Columnas exactas con Always Encrypted (ver [`docs/03-modelo-datos.md`](../03-modelo-datos.md)).
