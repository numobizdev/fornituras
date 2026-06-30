# Registro de Decisiones de Arquitectura (ADR)

Aquí se documentan las **decisiones importantes** del proyecto: por qué se eligió algo, qué
alternativas se consideraron y qué consecuencias tiene. Sirve para que cualquier persona o
IA que llegue después entienda el porqué, no solo el qué.

## Cómo agregar un ADR

1. Copia el formato de abajo a un archivo nuevo: `NNNN-titulo-corto.md`
   (numeración incremental: `0001-...`, `0002-...`).
2. Llénalo y enlázalo en la lista de abajo.
3. Un ADR no se borra: si una decisión cambia, se crea uno nuevo que **reemplaza** al
   anterior y se marca el viejo como "Reemplazado por NNNN".

## Formato

```markdown
# NNNN. Título de la decisión

- **Estado:** Propuesto | Aceptado | Reemplazado por NNNN
- **Fecha:** AAAA-MM-DD

## Contexto
Qué problema o pregunta estamos resolviendo.

## Decisión
Qué decidimos hacer.

## Alternativas consideradas
Qué otras opciones evaluamos y por qué se descartaron.

## Consecuencias
Qué implica esta decisión (positivo y negativo).
```

## Decisiones registradas

- [0001](./0001-monorepo-y-gobernanza-ia.md) — Monorepo y gobernanza agnóstica de IA.
- [0002](./0002-formato-del-qr.md) — Formato del QR: identificador opaco UUID + firma HMAC.
- [0003](./0003-pii-elementos.md) — Alcance de PII del elemento (CURP/RFC/foto). **Propuesto.**
- [0004](./0004-busqueda-pii-cifrada.md) — Búsqueda sobre PII cifrada (enclaves + blind index). **Propuesto.**

## Decisiones pendientes de registrar

- Herramienta de migraciones de BD (Flyway vs Liquibase).
- Mecanismo de tokens JWT (HS256 vs RS256).
- Gestor de secretos (también custodia la llave HMAC del QR — ver 0002).
- Régimen legal de protección de datos aplicable (condiciona 0003).
- Inmutabilidad y retención de la bitácora de auditoría / ISO 27001 (ver spec 012).
