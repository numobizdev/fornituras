# Planeación del proyecto — Sistema de Gestión de Blindajes

> Documento de planeación de alto nivel. Define **objetivo, alcance, requisitos, actores y
> roadmap**. El detalle técnico vive en [`docs/`](./docs/) y **no se duplica aquí** (solo se
> referencia). La fuente de verdad para agentes de IA es [`AGENTS.md`](./AGENTS.md).

---

## 1. Objetivo

Construir un sistema para **administrar los blindajes** (chalecos antibalas y equipo de
seguridad) de una corporación policial en México: saber **qué equipo existe**, **quién lo
tiene asignado** y **trazarlo mediante un QR único** por equipo, todo bajo un estándar de
**seguridad de la información de primer nivel** por tratarse de datos personales de personal
policial.

## 2. Problema y contexto

Hoy el control del equipo de blindaje es manual o disperso: no hay un inventario confiable,
ni trazabilidad de a quién se asignó cada pieza, ni forma rápida de identificar un equipo en
campo. Además, la información de los elementos policiales es **altamente sensible**: una fuga
puede poner vidas en riesgo. El sistema debe resolver el control operativo **sin** crear un
nuevo riesgo de exposición de datos.

## 3. Alcance

### Dentro del alcance (MVP)
- Inventario de equipos de blindaje (alta, baja, edición, consulta).
- Registro de elementos policiales (PII minimizada).
- Asignación equipo ↔ elemento, con historial.
- Generación de **QR opaco y firmado** por equipo, para grabar/imprimir.
- Lectura de QR por **cámara** (Capacitor) y por **escáner manual** (HID).
- Autenticación, autorización por roles y auditoría de accesos sensibles.

### Fuera del alcance (por ahora)
- App pública o acceso para los propios elementos.
- Integración con sistemas externos de la corporación (RH, nómina).
- Reportería avanzada / BI.
- Operación offline del escaneo (se evaluará; ver preguntas abiertas).

## 4. Actores y roles

Roles del sistema (detalle de permisos en [`docs/02-seguridad.md`](./docs/02-seguridad.md)):

- **ADMIN** — gestión total, incluida la administración de usuarios.
- **SUPERVISOR** — alta/baja de equipos y gestión de asignaciones.
- **OPERADOR** — consulta y escaneo; acceso a PII restringido al mínimo necesario.

> Nota: **elemento policial** (`officer`) es el dueño del equipo, **no** un usuario que
> opera la app. El usuario del sistema (`user`) es distinto. Ver
> [`docs/03-modelo-datos.md`](./docs/03-modelo-datos.md).

## 5. Requisitos funcionales

| ID    | Requisito                                                                 |
|-------|---------------------------------------------------------------------------|
| RF-01 | Registrar un equipo con número de serie único y sus atributos.            |
| RF-02 | Listar, buscar y editar equipos; cambiar su estado (disponible/baja/etc.).|
| RF-03 | Registrar elementos policiales con PII mínima necesaria.                   |
| RF-04 | Asignar un equipo a un elemento y registrar la devolución.                |
| RF-05 | Consultar el historial de asignaciones de un equipo y de un elemento.     |
| RF-06 | Generar un QR **opaco + firmado** por equipo, apto para grabar/imprimir.  |
| RF-07 | Resolver un QR escaneado a la ficha del equipo y su asignación vigente.    |
| RF-08 | Leer QR desde cámara y desde escáner manual con el mismo flujo.           |
| RF-09 | Autenticar usuarios y autorizar por rol (RBAC).                           |
| RF-10 | Auditar accesos y cambios sobre datos sensibles.                          |

## 6. Requisitos no funcionales

La seguridad es un **requisito de primer nivel**, no una fase posterior. El detalle está en
[`docs/02-seguridad.md`](./docs/02-seguridad.md); en resumen:

- **Seguridad/Privacidad:** QR sin datos personales (solo identificador opaco + firma);
  cifrado en reposo (TDE + Always Encrypted) y en tránsito (TLS 1.3); RBAC y mínimo
  privilegio; hashing fuerte de contraseñas; auditoría; cero secretos en el repositorio.
- **Cumplimiento legal:** LFPDPPP / LGPDPPSO según el régimen del responsable (por confirmar).
- **Mantenibilidad:** monorepo, capas claras, migraciones versionadas, ADRs para decisiones.
- **Usabilidad:** flujo de escaneo rápido en campo (cámara y lector manual equivalentes).
- **Disponibilidad/Respaldo:** respaldos cifrados con plan de recuperación (por definir).

## 7. Stack tecnológico

Decidido y congelado salvo ADR (ver [`AGENTS.md` §3](./AGENTS.md)):

| Capa          | Tecnología                                   |
|---------------|----------------------------------------------|
| Backend       | Java + Spring Boot (API REST)                |
| Base de datos | Microsoft SQL Server 2022                    |
| Frontend      | Ionic 8 + Angular                            |
| Móvil/QR      | Capacitor (cámara) + escáner manual (HID)    |

Arquitectura en [`docs/01-arquitectura.md`](./docs/01-arquitectura.md).

## 8. Roadmap por fases

> Las fases son incrementales y cada una deja algo funcional. La seguridad transversal
> (authn/authz, cifrado, auditoría) **no es una fase final**: se incorpora desde la Fase 1.

### Fase 0 — Cimientos (en curso)
- [x] Gobernanza de IA (`AGENTS.md`, `CLAUDE.md`, reglas de Cursor/Copilot).
- [x] Documentación viva: arquitectura, seguridad, modelo de datos, ADR inicial.
- [x] Esqueleto de backend en `fornituras-api/` (Spring Boot: auth, usuarios, QR).
- [ ] Esqueleto de `frontend/` (Ionic + Angular).
- [ ] Decisiones pendientes como ADR: migraciones, formato de token/firma, gestor de secretos.
- **Entregable:** repos compilando “hello world” con CI mínima y configuración de secretos.

### Fase 1 — Inventario + seguridad base
- Modelo y migraciones de `equipment`.
- CRUD de equipos (API + UI).
- Autenticación (JWT) y autorización por rol; usuario `ADMIN` inicial.
- Auditoría básica y cifrado en reposo configurado.
- **Entregable:** se puede dar de alta y consultar equipos de forma autenticada y auditada.
- **Cubre:** RF-01, RF-02, RF-09 (parcial), RF-10 (parcial).

### Fase 2 — Elementos y asignaciones
- Modelo de `officer` (PII con Always Encrypted) y `assignment`.
- Registro de elementos y flujo de asignación/devolución con historial.
- **Entregable:** se sabe **quién tiene cada equipo** y su histórico.
- **Cubre:** RF-03, RF-04, RF-05.

### Fase 3 — QR (generación y resolución)
- Generación de QR opaco + firma (estrategia según ADR).
- Endpoint de resolución `QR → equipo/asignación` con verificación de firma + authz.
- Exportable para grabado/impresión.
- **Entregable:** cada equipo tiene su QR único e imprimible; el servidor lo resuelve seguro.
- **Cubre:** RF-06, RF-07.

### Fase 4 — Escaneo en campo
- Lectura por cámara (Capacitor) y por escáner manual (HID) con flujo unificado.
- Vista de ficha del equipo al escanear, respetando el rol del usuario.
- **Entregable:** escanear un equipo (cámara o lector) muestra su información permitida.
- **Cubre:** RF-08.

### Fase 5 — Endurecimiento y operación
- Rate limiting, cabeceras de seguridad, MFA para roles administrativos.
- Rotación de llaves, respaldo cifrado y plan de recuperación.
- Revisión de seguridad y checklist de [`docs/02-seguridad.md` §8](./docs/02-seguridad.md).
- **Entregable:** sistema listo para piloto controlado.

## 9. Supuestos

- La corporación provee equipos lectores QR/código de barras tipo teclado (HID).
- Los QR pueden grabarse o imprimirse sobre los equipos físicos.
- Existe un responsable legal del tratamiento de datos del lado del cliente.
- El despliegue inicial es interno/controlado (no acceso público).

## 10. Preguntas abiertas (a resolver vía ADR)

Registrar cada decisión en [`docs/04-decisiones/`](./docs/04-decisiones/):

- Régimen legal aplicable (entidad pública vs. particular) y requisitos ARCO.
- Herramienta de migraciones: Flyway vs. Liquibase.
- Mecanismo de token JWT: HS256 vs. RS256.
- Estrategia de QR: UUID opaco + lookup vs. token autocontenido firmado.
- Gestor de secretos: variables de entorno vs. Azure Key Vault vs. HashiCorp Vault.
- ¿Se requiere escaneo/operación offline?
- Catálogos (tipo de equipo, adscripción, nivel de protección) y política de retención.

## 11. Criterios de éxito (MVP)

- Inventario confiable: todo equipo registrado con serie única y QR.
- Trazabilidad: para cualquier equipo se conoce su asignación actual e histórica.
- Escaneo funcional por cámara y lector manual.
- Cero exposición de PII a usuarios no autorizados; QR inútil fuera del sistema.
- Auditoría verificable de accesos a datos sensibles.

---

### Referencias
- [`AGENTS.md`](./AGENTS.md) — contrato para agentes de IA (fuente de verdad).
- [`docs/01-arquitectura.md`](./docs/01-arquitectura.md) — arquitectura y flujo del QR.
- [`docs/02-seguridad.md`](./docs/02-seguridad.md) — seguridad (lectura obligatoria).
- [`docs/03-modelo-datos.md`](./docs/03-modelo-datos.md) — modelo de datos (borrador).
- [`docs/04-decisiones/`](./docs/04-decisiones/) — ADRs.
