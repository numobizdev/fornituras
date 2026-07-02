# Instrucciones para GitHub Copilot

La **fuente única de verdad** de este proyecto es [`AGENTS.md`](../AGENTS.md) en la raíz.
Estas instrucciones solo la resumen; en caso de conflicto, manda `AGENTS.md`.

- **SIGEFOR — Sistema Integral de Gestión de Fornituras** (ADR 0019): gestión de blindajes
  (chalecos antibala) para policía. **Datos de alta sensibilidad** (PII de elementos
  policiales en México).
- **Seguridad primero.** Antes de tocar datos, autenticación o QR, lee `docs/02-seguridad.md`.
- El **QR nunca contiene datos personales**: solo un identificador opaco y firmado.
- **Nunca incluyas secretos** en el código.
- Stack: Spring Boot · SQL Server 2022 · Ionic 8 + Angular + Capacitor.
- **Una rama por spec, siempre.** Cada spec/feature va en su propia rama creada desde `dev`
  (nombre = slug de la spec); nunca trabajes una spec directamente sobre `dev`/`main`. Aplica
  aunque el trabajo se lance desde Copilot. Ver `AGENTS.md` §5.8.
- Documentación/comentarios en español; identificadores de código en inglés.
