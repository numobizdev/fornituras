# AGENTS.md — Fuente única de verdad para agentes de IA

> Este archivo es el **contrato común** para cualquier asistente de IA que trabaje en este
> repositorio (Claude Code, Cursor, Copilot, etc.). Está escrito siguiendo la convención
> [agents.md](https://agents.md). Las configuraciones específicas de cada herramienta
> (`CLAUDE.md`, `.cursor/rules/`, `.github/copilot-instructions.md`) **no duplican**
> contenido: solo apuntan aquí.
>
> **Regla de oro:** si cambias una norma del proyecto, edítala aquí, no en los archivos
> específicos de cada IA.

---

## 1. Qué es este proyecto

Sistema para **administrar blindajes** (chalecos antibalas y equipo de seguridad) de una
corporación policial en México. Funciones núcleo:

1. **Inventario** completo de equipos (chalecos antibala y equipamiento de seguridad).
2. **Asignación**: saber qué elemento policial tiene asignado cada equipo.
3. **QR único por equipo** (grabado/impreso), ligado a su número de serie único.
4. **Lectura de QR** desde cámara (Capacitor) y desde **escáner manual** de códigos.

La visión original del cliente está en [`Planeacion.md`](./Planeacion.md).
La documentación viva está en [`docs/`](./docs/).

## 2. Naturaleza del dato: ALTA SENSIBILIDAD

Este sistema almacena **datos personales de elementos policiales** (PII de personal de
seguridad pública). En México una fuga de este tipo puede **poner vidas en riesgo**.

> **La seguridad NO es opcional ni una fase posterior. Es un requisito de primer nivel en
> cada decisión de diseño, código y dependencia.**

Toda contribución de IA debe leer y respetar [`docs/02-seguridad.md`](./docs/02-seguridad.md)
**antes** de proponer cambios que toquen datos, autenticación, QR o almacenamiento.

## 3. Stack tecnológico (decidido)

| Capa        | Tecnología                                                        |
|-------------|-------------------------------------------------------------------|
| Backend     | **Java + Spring Boot** (API REST)                                 |
| Base de datos | **Microsoft SQL Server 2022**                                   |
| Frontend    | **Ionic 8 + Angular**                                             |
| Móvil/QR    | **Capacitor** (cámara) + soporte para **escáner manual** de QR    |
| Repo        | **Monorepo**: `fornituras-api/` (backend) y `sigefor/` (frontend) |

No cambiar el stack sin registrar una decisión en `docs/04-decisiones/`.

## 4. Estructura del repositorio

```
.
├── AGENTS.md                 # ESTE archivo — fuente de verdad para IAs
├── CLAUDE.md                 # Apunta a AGENTS.md + notas de Claude Code
├── README.md                 # Onboarding para humanos
├── .cursor/rules/            # Reglas de Cursor (apuntan a AGENTS.md)
├── .github/                  # Instrucciones de Copilot + CI a futuro
├── docs/                     # Documentación viva (arquitectura, seguridad, datos)
│   └── 04-decisiones/        # ADRs (registro de decisiones de arquitectura)
├── specs/                    # Especificaciones de features (spec-driven, .specify/)
├── fornituras-api/           # API Spring Boot — IMPLEMENTADA (auth, usuarios, QR)
└── sigefor/                  # App Ionic 8 + Angular — auth implementada (login/JWT)
```

> **Nota:** el frontend vive en `sigefor/` (no `frontend/`). Si existe un directorio
> `frontend/` vacío, es obsoleto y puede eliminarse.

## 5. Principios de trabajo para agentes de IA

1. **Seguridad primero.** Ante cualquier duda entre "fácil" y "seguro", elige seguro y
   explica el porqué.
2. **Nunca pongas datos personales en el QR.** El QR contiene únicamente un identificador
   **opaco y firmado** (UUID + firma). Los datos del elemento solo se resuelven en el
   servidor tras autenticación y autorización. Ver `docs/02-seguridad.md`.
3. **Nunca commitees secretos** (cadenas de conexión, llaves, contraseñas, certificados).
   Usa variables de entorno / gestor de secretos. Revisa `.gitignore` antes de añadir.
4. **No introduzcas dependencias** sin justificar necesidad, licencia y mantenimiento.
5. **Documenta las decisiones importantes** como un ADR en `docs/04-decisiones/`.
6. **Idioma:** documentación y comentarios en **español**; identificadores de código en
   **inglés** (convención estándar de Java/Angular).
7. **El backend (`fornituras-api/`) ya está implementado.** Respeta su arquitectura por
   módulos (`modules/<feature>/{controller,service,repository,entity,dto}`) y no rompas lo
   existente. El **frontend `sigefor/`** ya existe con la **autenticación implementada**
   (login por email/JWT, guards, interceptor); se extiende, no se reescribe.

## 6. Convenciones de código (cuando empiece la implementación)

- **Backend:** Java idiomático, paquetes por feature, capas claras (controller / service /
  repository). Validación de entrada en el borde. Spring Security para authn/authz.
- **Frontend:** Angular con componentes standalone, servicios para acceso a API, nada de
  lógica de negocio sensible en el cliente.
- **Base de datos:** migraciones versionadas (p. ej. Flyway/Liquibase) — nunca cambios
  manuales sin migración.
- **Commits:** mensajes claros en español, presente imperativo ("Agrega...", "Corrige...").

## 7. Comandos

**Backend** (desde `fornituras-api/`, Maven wrapper incluido):

```powershell
.\mvnw.cmd spring-boot:run     # Levanta la API
.\mvnw.cmd test                # Ejecuta los tests
.\mvnw.cmd clean package       # Compila y empaqueta
```

**Frontend** (desde `sigefor/`):

```powershell
npm install                    # Instala dependencias
npm start                      # Levanta la app (ng serve / ionic serve)
npm test                       # Ejecuta los tests
```

## 8. Qué NO hacer

- No exponer endpoints sin autenticación/autorización.
- No loguear PII ni secretos.
- No almacenar contraseñas en texto plano (usar hashing fuerte: Argon2/bcrypt).
- No desactivar validaciones de TLS/certificados "para que funcione".
- No mover ni borrar `Planeacion.md` ni los ADRs sin acuerdo explícito.
