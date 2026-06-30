# Seguridad

> **Clasificación del dato: ALTA SENSIBILIDAD.** El sistema almacena datos personales de
> elementos policiales en México. Una fuga puede poner vidas en riesgo. La seguridad es un
> requisito de primer nivel, no una fase posterior.

Este documento es de lectura **obligatoria** antes de tocar datos, autenticación, QR o
almacenamiento. Aplica a cualquier persona o IA que contribuya.

## 1. Marco legal (México)

- **LFPDPPP** — Ley Federal de Protección de Datos Personales en Posesión de los
  Particulares (o la **LGPDPPSO** si el responsable es un sujeto obligado / entidad
  pública). Aplica principios de: licitud, consentimiento, minimización, calidad,
  responsabilidad y seguridad.
- Implicaciones prácticas: **minimizar** los datos recolectados, justificar su finalidad,
  permitir derechos ARCO, y mantener **medidas de seguridad** documentadas.

> Validar con el área legal del cliente qué régimen aplica exactamente (entidad pública vs
> particular). Registrar la conclusión como ADR.

## 2. Principio rector del QR (crítico)

**El QR NUNCA contiene datos personales ni información explotable.**

- El QR lleva solo un **identificador opaco** (UUID v4) y una **firma** (HMAC) que prueba
  que lo emitió el sistema.
- Quien escanee el QR sin estar **autenticado y autorizado** no obtiene absolutamente nada.
- La relación `QR → equipo → elemento asignado` se resuelve **solo en el servidor**, tras
  verificar firma + sesión + rol.
- Beneficio: si alguien fotografía un chaleco, el QR por sí solo es inútil.

Opciones a decidir en ADR:
- **A) UUID opaco + lookup en BD** (recomendado): el QR es un identificador sin significado;
  todo el dato vive en el servidor.
- **B) Token autocontenido firmado**: el QR lleva datos mínimos firmados; útil offline pero
  expone más superficie. Requiere rotación de llaves.

## 3. Cifrado

### En reposo
- **TDE** en SQL Server 2022: cifra archivos de datos y backups de toda la base.
- **Always Encrypted** para columnas con PII (nombre, identificadores del elemento,
  contacto). El dato se descifra solo en el cliente autorizado del driver, no en el motor.
- Backups **cifrados** y con custodia controlada.

### En tránsito
- **TLS 1.3** (mínimo 1.2) en todo: cliente↔API y API↔BD.
- **HTTPS obligatorio**; sin endpoints en texto plano. HSTS habilitado.
- Validación estricta de certificados. Prohibido deshabilitarla "para pruebas".

## 4. Autenticación y autorización

- **Autenticación:** OAuth2 / **JWT** vía Spring Security.
  - Access token de **vida corta** (p. ej. 15 min) + **refresh token** rotatorio.
  - Considerar **MFA** para roles administrativos.
- **Autorización:** **RBAC** por roles. Propuesta inicial:
  - `ADMIN` — gestión total y de usuarios.
  - `SUPERVISOR` — alta/baja de equipos y asignaciones.
  - `OPERADOR` — consulta y escaneo, sin ver PII completa salvo necesidad.
- **Principio de mínimo privilegio**: cada rol ve y hace solo lo necesario.
- **Contraseñas:** hashing fuerte con **Argon2id** (o bcrypt con coste alto). Nunca en
  texto plano ni reversible.

## 5. Auditoría y trazabilidad

- Registrar **quién** accedió a **qué** dato sensible y **cuándo** (logs de auditoría
  inmutables o de difícil alteración).
- **No loguear PII ni secretos** en logs de aplicación. Enmascarar/“redactar”.
- Trazabilidad de cambios en asignaciones de equipo (historial).

## 6. Gestión de secretos

- **Cero secretos en el repositorio.** Cadenas de conexión, llaves HMAC/JWT, certificados y
  contraseñas van en variables de entorno o en un **gestor de secretos** (Azure Key Vault,
  HashiCorp Vault, etc.).
- Mantener un `.env.example` solo con **nombres** de variables, sin valores.
- Rotación periódica de llaves (JWT, HMAC del QR).

## 7. Endurecimiento de la API

- Validación de entrada en el borde; rechazar por defecto.
- **Rate limiting** y protección contra fuerza bruta en login.
- Cabeceras de seguridad (CSP, HSTS, X-Content-Type-Options, etc.).
- Manejo de errores que **no filtre** detalles internos.
- Dependencias auditadas (escaneo de vulnerabilidades en CI a futuro).

## 8. Checklist para cada cambio que toque seguridad

- [ ] ¿El QR sigue sin exponer datos personales?
- [ ] ¿Hay autenticación + autorización en el endpoint?
- [ ] ¿Se cifran/peotegen los datos sensibles en reposo y tránsito?
- [ ] ¿Se evitó loguear PII/secretos?
- [ ] ¿Ningún secreto quedó en el código o en git?
- [ ] ¿Se aplicó mínimo privilegio?
- [ ] ¿Se documentó la decisión relevante como ADR?

## 9. Pendientes / por investigar

- Confirmar régimen legal aplicable (entidad pública vs particular) y requisitos ARCO.
- Definir algoritmo de firma del QR y rotación de llaves.
- Definir gestor de secretos concreto.
- Estrategia de respaldo cifrado y plan de recuperación.
