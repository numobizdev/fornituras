# Arquitectura

## Visión general

Sistema cliente-servidor de 3 capas:

```
┌──────────────────────────┐        HTTPS / TLS 1.3        ┌──────────────────────────┐
│  Frontend (Ionic 8 +     │  ───────────────────────────▶ │  Backend (Spring Boot)   │
│  Angular + Capacitor)    │   JWT en cada petición        │  API REST + Spring       │
│                          │ ◀───────────────────────────  │  Security                │
│  - Cámara (lee QR)       │                               │                          │
│  - Escáner manual (HID)  │                               └───────────┬──────────────┘
└──────────────────────────┘                                           │ cifrado/conexión segura
                                                                       ▼
                                                          ┌──────────────────────────┐
                                                          │  SQL Server 2022          │
                                                          │  - TDE (cifrado en reposo)│
                                                          │  - Always Encrypted (PII) │
                                                          └──────────────────────────┘
```

## Componentes

### Frontend — Ionic 8 + Angular + Capacitor
- App para gestionar inventario, asignaciones y consultar equipos por QR.
- **Lectura de QR por cámara** vía plugin de Capacitor.
- **Lectura por escáner manual**: la mayoría de escáneres QR/código de barras actúan como
  teclado (HID) y "escriben" el contenido. La UI debe tener un campo/handler que capture
  esa entrada y la procese igual que la lectura por cámara.
- El cliente **no** guarda lógica de negocio sensible ni datos personales en caché
  persistente.

### Backend — Spring Boot
- API REST. Capas: `controller` → `service` → `repository`.
- **Spring Security** para autenticación (JWT) y autorización (RBAC por roles).
- Genera y **valida la firma** de los QR.
- Resuelve `idOpaco del QR → datos del equipo/asignación` solo tras authn + authz.
- Registra **auditoría** de accesos a datos sensibles.

### Base de datos — SQL Server 2022
- **TDE** (Transparent Data Encryption) para cifrado en reposo de toda la base.
- **Always Encrypted** para columnas con PII de elementos (el dato viaja cifrado incluso
  para el motor de BD).
- Migraciones versionadas (Flyway o Liquibase — decidir en ADR).

## Flujo del QR (resumen)

1. Al registrar un equipo, el backend genera un **UUID opaco** y una **firma HMAC**.
2. El QR impreso/grabado contiene `UUID + firma` (o un token compacto firmado). **Nunca**
   contiene nombre, serie sensible ni datos del elemento.
3. Al escanear (cámara o lector manual), el frontend manda el contenido al backend.
4. El backend **verifica la firma**, valida la sesión del usuario y su rol, y solo entonces
   devuelve la información permitida.

Detalle de seguridad en [`02-seguridad.md`](./02-seguridad.md).

## Decisiones pendientes (registrar como ADR)

- Herramienta de migraciones (Flyway vs Liquibase).
- Mecanismo exacto de tokens (JWT firma simétrica HS256 vs asimétrica RS256).
- Estrategia de QR: UUID + lookup vs token autocontenido firmado.
- Gestor de secretos (variables de entorno, Azure Key Vault, HashiCorp Vault).
