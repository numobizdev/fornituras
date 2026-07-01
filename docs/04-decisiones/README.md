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
- [0002](./0002-formato-del-qr.md) — Formato del QR: UUID + firma HMAC. **Reemplazado por 0005.**
- [0003](./0003-pii-elementos.md) — Alcance de PII del elemento (CURP/RFC/foto). **Propuesto.**
- [0004](./0004-busqueda-pii-cifrada.md) — Búsqueda sobre PII cifrada (enclaves + blind index). **Propuesto.**
- [0005](./0005-formato-qr-implementado.md) — Formato QR implementado: código corto opaco + lotes. **Aceptado** (reemplaza 0002; firma = punto abierto).
- [0006](./0006-cifrado-pii-nivel-aplicacion.md) — Cifrado de PII a nivel de aplicación.
- [0007](./0007-catalogos-genericos.md) — Catálogos genéricos (`catalog → catalog_item`) y municipio/estado como texto libre. **Aceptado.**
- [0011](./0011-libreria-export-excel.md) — Exportación de reportes a Excel con Apache POI (SXSSF, streaming). **Aceptado.**
- [0012](./0012-inmutabilidad-y-retencion-auditoria.md) — Inmutabilidad y retención de la bitácora de auditoría (append-only + triggers). **Aceptado.**
- [0013](./0013-expansion-de-roles.md) — Expansión del enum de roles (RBAC) a cinco roles con matriz de permisos. **Aceptado.**
- [0014](./0014-estrategia-mfa.md) — Estrategia de MFA para roles administrativos (TOTP con secreto cifrado). **Propuesto.**
- [0015](./0015-landing-configurable.md) — Landing configurable (dos caras), endpoint público no-PII con rate limiting, anti-XSS por texto plano + `driver.js` para el tour. **Aceptado.**
- [0016](./0016-almacenamiento-de-fotos.md) — Almacenamiento seguro de fotos: filesystem local cifrado (AES-256-GCM) tras `FileStoragePort`, metadatos en BD, EXIF stripping, servicio autenticado/auditado. **Aceptado** (resuelve el pendiente de foto de 0003/0006).

## Decisiones pendientes de registrar

- Herramienta de migraciones de BD (Flyway vs Liquibase).
- Mecanismo de tokens JWT (HS256 vs RS256).
- Gestor de secretos (también custodia la llave HMAC del QR — ver 0002).
- Régimen legal de protección de datos aplicable (condiciona 0003).
- Inmutabilidad y retención de la bitácora de auditoría / ISO 27001 (ver spec 012).
