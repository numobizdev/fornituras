<!--
SYNC IMPACT REPORT
Versión: 1.0.0 → 1.0.1
Tipo de cambio: PATCH (corrección del título nominal del sistema a "Sistema Integral de
Gestión de Fornituras (SIGEFOR)" conforme al ADR 0019; los principios no cambian)

--- Historial 1.0.0 ---
Versión: (plantilla) → 1.0.0
Tipo de cambio: MAJOR (ratificación inicial; primera constitución concreta)
Principios definidos:
  I.   Seguridad y privacidad primero (NO NEGOCIABLE)
  II.  El QR nunca expone datos personales
  III. Cero secretos en el repositorio
  IV.  Mínimo privilegio y autorización en cada acceso
  V.   Trazabilidad y auditoría sin fugas
  VI.  Decisiones documentadas (ADR) y stack congelado
Secciones añadidas: Requisitos de seguridad y cumplimiento; Flujo de desarrollo y puertas de calidad; Governance
Plantillas revisadas:
  ✅ .specify/templates/plan-template.md — usar "Constitution Check" contra estos principios
  ✅ .specify/templates/spec-template.md — secciones de seguridad/privacidad aplican
  ✅ .specify/templates/tasks-template.md — incluir tareas de authz/auditoría/cifrado
Fuentes de verdad: AGENTS.md, docs/02-seguridad.md, docs/01-arquitectura.md
TODOs diferidos: ninguno
-->

# Constitución — Sistema Integral de Gestión de Fornituras (SIGEFOR)

> Esta constitución concreta y hace exigibles los principios de [`AGENTS.md`](../../AGENTS.md)
> y [`docs/02-seguridad.md`](../../docs/02-seguridad.md) dentro del flujo Spec-Driven
> Development. Si hay conflicto entre un atajo y un principio, **gana el principio**.

## Core Principles

### I. Seguridad y privacidad primero (NO NEGOCIABLE)

El sistema almacena datos personales de elementos policiales en México; una fuga puede poner
vidas en riesgo. La seguridad es un **requisito de primer nivel en cada decisión**, no una
fase posterior. Toda spec, plan o tarea que toque datos, autenticación, QR o almacenamiento
DEBE citar y cumplir `docs/02-seguridad.md`. Ante la disyuntiva entre "fácil" y "seguro", se
elige seguro y se justifica el porqué. **Rationale:** el costo de una fuga es irreversible y
humano, no solo técnico.

### II. El QR nunca expone datos personales

El QR DEBE contener únicamente un **identificador opaco (UUID) + una firma (HMAC)**. Está
PROHIBIDO incluir nombre, número de serie sensible, adscripción o cualquier dato explotable.
La relación `QR → equipo → elemento` se resuelve **solo en el servidor**, tras verificar
firma + sesión + rol. Quien escanee sin estar autenticado y autorizado no obtiene nada.
**Rationale:** un chaleco fotografiado en la calle no debe filtrar a quién pertenece.

### III. Cero secretos en el repositorio

Cadenas de conexión, llaves HMAC/JWT, certificados y contraseñas NUNCA se versionan. Van en
variables de entorno o gestor de secretos. El repo solo contiene `.env.example` con los
**nombres** de variables, sin valores. Las llaves se rotan periódicamente. **Rationale:** un
secreto commiteado se considera comprometido para siempre.

### IV. Mínimo privilegio y autorización en cada acceso

Ningún endpoint se expone sin autenticación + autorización (RBAC). Cada rol (`ADMIN`,
`SUPERVISOR`, `OPERADOR`) ve y hace **solo lo necesario**. La validación de entrada ocurre en
el borde y se rechaza por defecto. Las contraseñas se almacenan con hashing fuerte
(Argon2id/bcrypt), nunca en texto plano ni reversible. **Rationale:** limitar el radio de
daño ante una credencial comprometida.

### V. Trazabilidad y auditoría sin fugas

Todo acceso o cambio sobre datos sensibles DEBE registrarse (quién, qué, cuándo) en auditoría
de difícil alteración. Está PROHIBIDO loguear PII o secretos en logs de aplicación: se
enmascaran o se referencian por id. Los cambios de asignación conservan historial.
**Rationale:** sin trazabilidad no hay responsabilidad; con PII en logs, la auditoría se
vuelve otra superficie de fuga.

### VI. Decisiones documentadas (ADR) y stack congelado

El stack (Java + Spring Boot, SQL Server 2022, Ionic 8 + Angular, Capacitor) está decidido y
**no se cambia sin un ADR** en `docs/04-decisiones/`. Toda decisión arquitectónica relevante
(migraciones, formato de token/firma, gestor de secretos) se registra como ADR antes de
implementarla. No se introducen dependencias sin justificar necesidad, licencia y
mantenimiento. **Rationale:** las decisiones implícitas se olvidan y se contradicen; las
escritas se revisan.

## Requisitos de seguridad y cumplimiento

Estos requisitos son transversales a toda feature (ver detalle en `docs/02-seguridad.md`):

- **Cifrado en tránsito:** TLS 1.3 (mínimo 1.2) en cliente↔API y API↔BD; HTTPS obligatorio;
  HSTS; validación estricta de certificados (prohibido deshabilitarla "para pruebas").
- **Cifrado en reposo:** TDE en SQL Server 2022; **Always Encrypted** para columnas con PII.
- **Autenticación:** JWT vía Spring Security; access token de vida corta + refresh rotatorio;
  MFA para roles administrativos.
- **Endurecimiento de API:** rate limiting, protección contra fuerza bruta, cabeceras de
  seguridad, manejo de errores que no filtre detalles internos.
- **Minimización de datos:** se recolecta solo la PII estrictamente necesaria para la
  finalidad; se respetan derechos ARCO.
- **Marco legal:** LFPDPPP / LGPDPPSO según el régimen del responsable (a confirmar vía ADR).

## Flujo de desarrollo y puertas de calidad

- **Spec-Driven Development:** cada feature pasa por `spec → plan → tasks → implement`. La
  spec describe el **QUÉ/POR QUÉ** (sin tecnología); el plan, el **CÓMO**.
- **Constitution Check:** todo plan DEBE verificar explícitamente el cumplimiento de los
  principios I–VI antes de implementar. Una violación requiere justificación registrada o se
  rediseña.
- **Migraciones versionadas:** ningún cambio de esquema manual; se usa la herramienta de
  migración acordada por ADR (Flyway/Liquibase).
- **Idioma:** documentación y comentarios en español; identificadores de código en inglés.
- **Capas claras:** backend `controller → service → repository`; frontend con servicios para
  API y sin lógica de negocio sensible en el cliente.
- **Checklist de seguridad** (`docs/02-seguridad.md` §8) obligatorio en cada cambio que toque
  datos, auth, QR o almacenamiento.

## Governance

- Esta constitución **prevalece** sobre conveniencias de implementación. `AGENTS.md` sigue
  siendo la fuente de verdad de gobernanza; esta constitución la operacionaliza para SDD y no
  la contradice.
- **Enmiendas:** se proponen por escrito (ADR o PR), se justifican y se versionan aquí.
- **Versionado semántico de la constitución:** MAJOR = remoción/redefinición incompatible de
  principios; MINOR = nuevo principio o sección; PATCH = aclaraciones y correcciones.
- **Cumplimiento:** toda revisión de spec/plan/PR verifica el apego a los principios. La
  complejidad o cualquier excepción debe justificarse explícitamente.
- **Guía operativa en runtime:** `AGENTS.md` y los documentos de `docs/`.

**Version**: 1.0.1 | **Ratified**: 2026-06-29 | **Last Amended**: 2026-07-01
