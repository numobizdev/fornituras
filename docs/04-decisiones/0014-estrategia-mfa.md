# 0014. Estrategia de MFA para roles administrativos

- **Estado:** **Propuesto**
- **Fecha:** 2026-07-01
- **Feature:** [013-usuarios](../../specs/013-usuarios/) (tareas T004 → T022/T023, gated)

## Contexto

[`docs/02-seguridad.md`](../02-seguridad.md) §4 y la spec 013 (FR-004, SC-003) exigen **segundo factor
de autenticación (MFA)** para los roles administrativos: al tratarse de PII de personal policial, una
credencial comprometida no debe bastar para acceder. Hoy el login es **email + contraseña** (hash
bcrypt) con JWT y ya hay **bloqueo anti-fuerza-bruta** por intentos (feature 013, migración V22). Falta
el segundo factor.

Introducir MFA toca autenticación y, según la variante, **añade una dependencia** y **almacena un
secreto** (Principio VI y "cero secretos en claro"). Por eso está gated por este ADR.

## Decisión

*(Propuesta a validar.)* Adoptar **TOTP (RFC 6238)** con app de autenticación (Google Authenticator,
Authy, etc.) como segundo factor **obligatorio para `ADMIN`** (y para los roles administrativos que
resulten de [ADR 0013](0013-expansion-de-roles.md), p. ej. `SUPERVISOR`/`AUDITOR`), con estas reglas:

1. **Alta del factor:** al activar MFA se genera un secreto TOTP y se muestra como QR/clave para
   registrar en la app; se confirma con un código válido antes de habilitarlo.
2. **Login en dos pasos:** contraseña correcta → si el usuario tiene MFA activo, se pide el código TOTP
   antes de emitir el JWT. El bloqueo por intentos (V22) aplica también al segundo paso.
3. **Almacenamiento del secreto cifrado:** el secreto TOTP se guarda **cifrado a nivel de aplicación**
   reutilizando el cifrado de PII existente ([ADR 0006](0006-cifrado-pii-nivel-aplicacion.md),
   `EncryptedStringConverter`/`PiiCipher`); **nunca** en claro ni en logs ni en respuestas.
4. **Recuperación:** **códigos de respaldo** de un solo uso (hash almacenado) entregados al activar MFA;
   el reseteo del factor por un administrador queda **auditado** (012). Sin esto, perder el dispositivo
   deja al usuario fuera.
5. **Dependencia (Principio VI / regla 4):** usar una librería TOTP madura y de licencia permisiva
   (p. ej. `dev.samstevens.totp` o `com.eatthepath:java-otp`, ambas Apache-2.0), registrada aquí con
   necesidad (control de acceso reforzado), licencia y mantenimiento.

## Alternativas consideradas

1. **TOTP con app autenticadora** *(propuesta)*: estándar (RFC 6238), **offline**, no depende del
   correo ni de SMS, secreto por usuario. Coste: una dependencia ligera y gestionar el enrolamiento y
   los códigos de respaldo.
2. **OTP por correo** (reusa `EmailService` y la infraestructura de códigos ya existente): **cero
   dependencias nuevas** y rápido de implementar, pero **más débil** (si el correo se compromete, el
   segundo factor también) y **depende de la disponibilidad del correo** en cada login. Válido como
   **medida interina** si se necesita MFA antes de integrar TOTP.
3. **WebAuthn / passkeys**: el factor más fuerte (phishing-resistant), pero **desproporcionado** hoy
   para el cliente Ionic/Capacitor y el tamaño del proyecto; mayor complejidad de integración y soporte
   de dispositivos. Queda como evolución futura.
4. **SMS OTP**: descartado — coste por mensaje, dependencia de un proveedor y debilidades conocidas
   (SIM swapping); no aporta sobre TOTP.

## Consecuencias

- **Positivas:** una credencial robada no basta para entrar a cuentas administrativas (cumple FR-004 /
  SC-003); TOTP es estándar, offline y sin coste recurrente; el secreto se protege con el cifrado de
  PII ya existente (no se añade un nuevo mecanismo de custodia).
- **Negativas / coste:** una dependencia nueva (ligera); flujo de login en dos pasos en backend y
  frontend (`core/auth`); UX de enrolamiento y **códigos de respaldo** que hay que soportar y
  documentar; el reseteo del factor debe ser una operación administrativa auditada.
- **Alcance/migración:** MFA arranca **opcional y luego obligatorio** para roles administrativos; los
  usuarios existentes deben enrolarse en su siguiente acceso. Requiere **nueva migración** (columnas
  `mfa_enabled`, `mfa_secret` cifrado y almacén de códigos de respaldo).
- **Pendiente de decisión:** (a) TOTP definitivo vs OTP-correo interino; (b) obligatoriedad inmediata o
  gradual; (c) librería TOTP concreta.
