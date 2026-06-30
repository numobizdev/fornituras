# Instrucciones para GitHub Copilot

La **fuente única de verdad** de este proyecto es [`AGENTS.md`](../AGENTS.md) en la raíz.
Estas instrucciones solo la resumen; en caso de conflicto, manda `AGENTS.md`.

- Sistema de gestión de blindajes (chalecos antibala) para policía. **Datos de alta
  sensibilidad** (PII de elementos policiales en México).
- **Seguridad primero.** Antes de tocar datos, autenticación o QR, lee `docs/02-seguridad.md`.
- El **QR nunca contiene datos personales**: solo un identificador opaco y firmado.
- **Nunca incluyas secretos** en el código.
- Stack: Spring Boot · SQL Server 2022 · Ionic 8 + Angular + Capacitor.
- Documentación/comentarios en español; identificadores de código en inglés.
