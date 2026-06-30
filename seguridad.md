# Inventario de Medidas de Seguridad — TemplateSecurity

Documento generado a partir del análisis del código del backend (NestJS) bajo `ServicesManager/nestjs/`. Cubre el flujo completo: desde la autenticación inicial (login) hasta la protección de recursos por permisos, criptografía, auditoría, infraestructura y mitigaciones de OWASP.

---

## 1. Bootstrap y Configuración Global (`src/main.ts`)

| Medida | Detalle |
|---|---|
| `ValidationPipe` global | `whitelist: true` + `forbidNonWhitelisted: true` + `transform: true`: rechaza propiedades no declaradas en DTOs (anti-mass-assignment) y obliga tipos. |
| CORS controlado | `app.enableCors({ origin: env.CORS_ORIGINS.split(','), credentials: true })`. Lista blanca explícita por variable de entorno. |
| Cookies firmadas/parseadas | `cookie-parser` registrado para soportar cookies `httpOnly` de sesión. |
| `app.enableShutdownHooks()` | Cierre limpio de conexiones (BD, schedulers) ante señales del sistema. |
| Documentación OpenAPI con esquemas de seguridad | `addApiKey('x-api-key')`, `addBearerAuth()` y parámetro global `X-XSRF-TOKEN` documentado. |
| Métricas de request | Middleware que registra latencia, status y ruta para detección de anomalías. |
| Validación de secretos en arranque | `JWT_SECRET` y `ENCRYPTION_PASSWORD` deben tener ≥ 32 caracteres o el servicio falla al inicializarse (`enviroment.service.ts`). |

---

## 2. Capas de Middleware Globales (`app.module.ts`)

Orden de ejecución sobre cada request:

1. **`ApiKeyMiddleware`** — Exige header `x-api-key` válido y firmado (RSA). Excluye sólo rutas de salud y webhooks (Stripe / MercadoPago) que verifican firma propia del proveedor.
2. **`CsrfProtectionMiddleware`** — Double-submit cookie sobre métodos no seguros (POST/PUT/PATCH/DELETE) cuando hay cookies de sesión.
3. **`RsaDecryptMiddleware`** — Sólo en `auth/SignIn`: descifra el sobre híbrido RSA-OAEP + AES-256-GCM antes de que el body llegue al controlador.
4. **`IsAuthorizationMiddleware`** — Hook reservado para `auth/SignIn` (deprecado el auto-login por origen; conservado por trazabilidad).

---

## 3. Autenticación de Cliente — API Key (RSA firmada)

Ubicación: `app/modules/api-access-keys/`, `auth/strategies/api-key.strategy.ts`, `auth/middleware/api-key.middleware.ts`.

- Cada cliente recibe una **ApiAccessKey** con par RSA-4096 generado en `generateKeyPairSync` con clave privada cifrada **AES-256-CBC** y passphrase derivada (no persistida).
- El "key" almacenado es una **firma SHA-256** del `keyId`, verificable contra la clave pública (`verifySigned`).
- Validación: `passport-headerapikey` → `ApiKeyStrategy.validate()` → `ApiAccessKeysService.verifySigned()`.
- Soporta **rotación** (`rotatePlatform`/`rotateForTenant`) y **revocación** (`isActive=false`), con expiración temporal opcional (`expiresIn`).
- Scoping: claves de **plataforma** (`tenantId=null`) y por **tenant**.

---

## 4. Cifrado de Credenciales en Tránsito (esquema híbrido)

Aplicado al endpoint `POST /auth/SignIn` mediante `RsaDecryptMiddleware` + `SecurityKeyService.Decrypt()`.

| Capa | Algoritmo |
|---|---|
| Clave de sesión | **AES-256-GCM** con IV de 12 bytes y auth tag (AEAD: confidencialidad + integridad) |
| Sobre de la clave AES | **RSA-OAEP (SHA-256)** con la clave pública del **SecurityKey** asignado al `OriginId` |
| Formato esperado | `{ OriginId, RsaData (key cifrada), RsaIv, RsaCt, RsaTag }` |
| Validaciones de payload | `OriginId` debe ser UUID v4; `RsaData` ≤ 1024 chars base64; bloqueo si falta `RsaData` en producción |
| Buffers sensibles | Se hacen `fill(0)` tras descifrado |

**Rotación con grace period**: `SecurityKeyService.rotate()` permite mantener la llave previa activa hasta N días para que clientes en vuelo no fallen.

---

## 5. Flujo de Login (`auth.controller.ts` + `auth.service.ts`)

| Paso | Mecanismo |
|---|---|
| **Rate-limit credencial** | `@Throttle(AUTH_CREDENTIAL_ROUTE_THROTTLE)` — 10 req/min por defecto sobre `/SignIn`, `/SignUp`, `/AcceptSystemInvite` |
| **Validación de credenciales** | `LocalStrategy` → `AuthService.validateUser()` → comparación contra hash con **Argon2id** (`memoryCost=2^16`, `timeCost=4`, `parallelism=2`) |
| **Mensajes genéricos** | `AUTH_ERRORS.INVALID_CREDENTIALS = 'Usuario o contraseña incorrectos'` (no revela si existe el usuario) |
| **Verificaciones post-credencial** | `isActive=true` y `hasAccess=true` antes de emitir tokens |
| **Generación JWT** | Algoritmo `HS256`, secreto único (`JWT_SECRET` ≥ 32 chars), payload mínimo (`jti` UUIDv4 + `userId` + `tenantId`), TTL `AUTH_ACCESS_EXPIRES_IN` (default 15m) |
| **Refresh token** | JWT separado, TTL `1d` (sesión) o `30d` (remember-me persistente) |
| **Persistencia de sesión en BD** | `auth_access_tokens` con relación padre/hijo (`parentId`) entre refresh y access; permite revocación granular |
| **Auditoría de login** | Fila en `auth_session_audit` con catalogo `LOGIN`/`REFRESH`/`SESSION_REVOKE`, IP, user-agent y serialización de `OriginRequestDataDto` |
| **Transacciones atómicas** | Toda emisión de tokens se hace dentro de `Transaction.process()` con rollback ante fallo |

---

## 6. Cookies de Sesión

| Cookie | `httpOnly` | `secure` | `sameSite` | Notas |
|---|---|---|---|---|
| `access_token` | ✅ | en producción | `lax` | TTL = JWT TTL; sin `maxAge` cuando no hay "remember me" → cookie de sesión |
| `refresh_token` | ✅ | en producción | `lax` | `maxAge` derivado de `AUTH_REFRESH_PERSISTENT_EXPIRES_IN` cuando hay remember-me |
| `XSRF-TOKEN` | ❌ (legible por JS) | en producción | `lax` | Token aleatorio `randomBytes(32).toString('hex')` para double-submit |

- `logout` borra las 3 cookies (incluye el path legacy `/auth`).
- Las cookies son la fuente preferida del JWT; el header `Authorization: Bearer` es fallback para clientes nativos.

---

## 7. Protección CSRF (`csrf-protection.middleware.ts`)

- **Estrategia**: double-submit cookie. Si la petición trae cookies `access_token`/`refresh_token`, el cliente debe enviar el header `X-XSRF-TOKEN` (o `X-CSRF-TOKEN`) con valor idéntico a la cookie `XSRF-TOKEN`.
- **Aplicabilidad**: sólo métodos no seguros (POST/PUT/PATCH/DELETE).
- **Exclusiones**: webhooks de PSP (firma propia), `auth/signin`, `auth/signup`, `auth/acceptsysteminvite` (no hay cookie aún), rutas de salud.
- **Skip inteligente**: si no hay cookie de sesión (cliente Bearer-only), se omite (no aplica el riesgo CSRF).
- **Validación robusta**: ambos tokens deben existir, tener ≥ 16 chars y ser idénticos.
- Toggle por entorno: `CSRF_PROTECTION_ENABLED`.

---

## 8. Rate Limiting (`@nestjs/throttler`)

Configurado en `app.module.ts` y `settings/throttle-constants.ts`:

| Throttler | Default | Aplicado a |
|---|---|---|
| `auth` | 10 req / 60s | `/auth/SignIn`, `/auth/SignUp`, `/auth/AcceptSystemInvite` |
| `auth` (session) | 120 req / 60s | `/auth/refresh`, `/auth/me`, `/auth/sessions*` |
| `tenantStorageUpload` | 20 req / 60s | `POST /tenant/storage/objects` |
| `auth` (notification) | 40 req / 60s | Endpoints de despacho de notificaciones |

`ThrottlerGuard` se aplica a nivel de controlador en `AuthController`.

---

## 9. Verificación de JWT (`jwt.strategy.ts` + `jwt-refresh.strategy.ts`)

Cada request autenticada pasa por:

1. Extracción del JWT: cookie `access_token` primero, luego `Authorization: Bearer`.
2. `passport-jwt` verifica firma con `JWT_SECRET` y algoritmo `HS256` (lista blanca explícita: `algorithms: ['HS256']` en `verifyToken`).
3. `ignoreExpiration: false` — expiración estricta.
4. **El payload NO contiene datos del usuario**; sólo `jti` + `userId` + `tenantId`. La identidad se vuelve a leer de la BD usando el token como índice.
5. `findSessionByToken()` valida que el token siga registrado.
6. Estado del usuario: `isActive` + `hasAccess` deben ser true.
7. `validAuthorization()` confirma `isActive && !isDeleted && !isRevoked && expiresAt > now` en `auth_access_tokens`.

---

## 10. Gestión de Sesiones (`/auth/sessions`)

- `GET /auth/sessions` lista refresh-sessions activas del usuario (no expone JWT, marca `current=true` para la sesión actual).
- `DELETE /auth/sessions/:id` revoca una sesión específica (logout remoto por dispositivo).
- Admin de tenant puede listar/revocar sesiones de otros miembros con `ForbiddenException` si intenta atacar a sí mismo.
- Cada revocación se persiste en `auth_session_audit` (evento `SESSION_REVOKE`) con `wasCurrentSession`, `administeredByUserId`, `administeredForTenantId`.
- **Cron** `@Cron('0 * * * *')` ejecuta `purgeExpiredTokens()` cada hora: borra tokens expirados/revocados/inactivos y cascada de access tokens hijos.

---

## 11. Autorización Multinivel (Guards encadenados)

Orden típico: `JwtAuthGuard` → `TenantContextGuard` → `PermissionsGuard` (o `PoliciesGuard`) → `SubscriptionGuard` / `TenantModuleGuard`.

### 11.1 `TenantContextGuard` (`auth/guards/tenant-context.guard.ts`)

- Resuelve el tenant efectivo a partir de: `params.tenantId` → header `x-tenant-id` → claim del JWT.
- Si la API key está atada a un tenant, valida que coincida con el solicitado (cross-tenant leakage protection).
- Construye `request.principal` con `userId`, `tenantId`, `userTenantId`, `roleIds`, `roleCodes`, `permissionCodes`.
- Modo `MULTI_TENANT_ENABLED=false` exige tenant único.

### 11.2 `PermissionsGuard` (`auth/guards/permissions.guard.ts`)

- Lee `@RequirePermissions('CODE')` por reflexión.
- Verifica `hasAllPermissions(principal, required)` contra los `permissionCodes` resueltos vía rol → role-permission → permission.
- Filtra asignaciones inactivas/eliminadas a todos los niveles (`assignment.isActive`, `role.isActive`, `rolePermission.isActive`, `permission.isActive`).

### 11.3 `PoliciesGuard` (`core/authorization/policies/policies.guard.ts`)

- Permite policies declarativas por recurso (`@CheckPolicies({resource, action})`).
- Resuelve la policy registrada y delega a `policy.can(action, subject)` con razones explícitas.

### 11.4 `SubscriptionGuard` y `TenantModuleGuard`

- `SubscriptionGuard`: bloquea endpoints cuyas features no estén incluidas en el plan activo del tenant.
- `TenantModuleGuard`: deshabilita módulos opcionales por tenant (catálogo de módulos).

---

## 12. Modelo de Permisos / RBAC

- Cadena: **User → UserTenant → UserTenantRole → Role → RolePermission → Permission**.
- Soporta jerarquía de roles (`role.parentId`) con `AuthorizationService.isDescendant()` para validar escalado.
- Permisos identificados por `code` (string estable) usados por `@RequirePermissions(...)`.
- Roles asignables por catálogo (`SystemRoleCode`) y permisos derivados de seeds.

---

## 13. Criptografía (`utils/encryption.ts`)

| Algoritmo | Uso |
|---|---|
| **Argon2id** (`memoryCost=2^16, timeCost=4, parallelism=2`) | Hash de contraseñas (reemplaza bcrypt) |
| **AES-256-GCM** (AEAD) | Cifrado simétrico autenticado de payloads y AES envelope |
| **AES-256-CTR** | Cifrado de secretos en BD con prefijo `enc:` (passphrase derivada con scrypt) |
| **RSA-OAEP (SHA-256)** | Cifrado asimétrico para sobre de clave AES |
| **RSA-PSS / firma SHA-256/512** | Firma de `keyId` para verificación de ApiAccessKey |
| **scrypt** | Derivación de claves desde `ENCRYPTION_PASSWORD` + salt (passphrase nunca se persiste) |
| **PKCS#8 + AES-256-CBC** | Almacenamiento de la clave privada RSA cifrada con passphrase derivada |
| `randomBytes(32)` | Tokens CSRF y claves AES de sesión |
| `uuid v4` | `jti` y `originId` |
| `buffer.fill(0)` | Limpieza explícita de buffers sensibles tras uso |

Validaciones de fuerza:
- `ENCRYPTION_PASSWORD` ≥ 12 chars (≥ 32 chars exigido por `EnviromentService`).
- `JWT_SECRET` ≥ 32 chars o el servicio rompe en arranque.
- Tamaño de clave AES validado: `key.length === 32`.

---

## 14. Saneamiento y Validación de Entrada

- `class-validator` + `class-transformer` aplicados globalmente por `ValidationPipe`.
- DTOs con `excludeExtraneousValues: true` evitan filtración de columnas internas (`@SerializeOptions` + `ClassSerializerInterceptor`).
- `ParseUUIDPipe` para parámetros UUID (`/auth/sessions/:id`).
- `forbidNonWhitelisted: true` rechaza payloads con propiedades fuera del DTO.
- Validaciones manuales adicionales en `RsaDecryptMiddleware`: longitudes máximas, formato UUID, tipos.

---

## 15. Aislamiento Multi-Tenant

- **Tenant-scoping de API Keys**: claves de plataforma (`tenantId=null`) vs claves de tenant; `TenantContextGuard` rechaza desajuste.
- **Resolución estricta de tenant**: `resolveRequestedTenantId()` exige membresía verificada vía `UserTenant.isActive && !isDeleted`.
- **Modo single-tenant**: cuando `MULTI_TENANT_ENABLED=false`, sólo se permite el `APP_BUSINESS_TENANT_NAME`.
- **Tests de cross-tenant isolation**: `wisp-cross-tenant-isolation.spec.ts` valida que recursos de un tenant no sean accesibles desde otro.
- **Filtros automáticos por `tenantId`** en repositorios de negocio.

---

## 16. Auditoría y Trazabilidad

| Sistema | Tabla | Eventos |
|---|---|---|
| Auditoría de sesiones | `auth_session_audit` | LOGIN, REFRESH, SESSION_REVOKE (con IP, user-agent, geo si aplica) |
| Auditoría de dominio | `domain_audit_events` (módulo `domain-audit`) | Cambios en entidades de negocio críticas; purga retentiva configurable (`DOMAIN_AUDIT_RETENTION_DAYS`) |
| Errores aplicativos | `application_error_events` | Sólo errores ≥ 500 / no-HTTP; inserción no bloqueante (`setImmediate`); purga configurable (`ERROR_LOG_RETENTION_DAYS`) |

Captura de origen via `OriginRequestDataDto.create(req)`: IP, user-agent, referer, etc.

---

## 17. Manejo Seguro de Errores (`http-exception.filter.ts`)

- **Filter global** registrado con `APP_FILTER`.
- Respuestas 5xx → mensaje genérico `'An unexpected error occurred...'` (no se filtra stack ni mensaje original al cliente).
- Mensajes de credenciales genéricos en `AUTH_ERRORS` para no revelar si un usuario existe.
- Stack y mensaje completo se loguean server-side y se persisten asíncronamente en `application_error_events`.
- Limites configurables de longitud (`ERROR_LOG_MAX_MESSAGE_CHARS=500`, `ERROR_LOG_MAX_STACK_CHARS=8192`).

---

## 18. Verificación de Webhooks (PSPs)

- **Stripe**: verifica `Stripe-Signature` con `STRIPE_WEBHOOK_SECRET`. Sin secret → ruta devuelve **503**.
- **Mercado Pago**: verifica `x-signature` con `PLATFORM_MERCADOPAGO_WEBHOOK_SECRET` y ventana de tolerancia anti-replay (`BILLING_WEBHOOK_TOLERANCE_SECONDS`, default 300s).
- Webhooks excluidos del `ApiKeyMiddleware` y CSRF (autenticación por firma del proveedor).
- Reconciliación periódica (`MERCADOPAGO_RECONCILIATION_ENABLED`) para pagos pendientes.

---

## 19. Tareas Programadas de Limpieza

| Cron | Función |
|---|---|
| `0 * * * *` (cada hora) | `purgeExpiredTokens()` borra refresh tokens inválidos y access tokens hijos |
| Programado | Purga de `domain_audit_events` con retención configurable |
| Programado | Purga de `application_error_events` con retención configurable |
| Programado | Reconciliación de pagos pendientes (MP) |
| Programado | Recuperación de notificaciones atascadas (`NOTIFICATION_STUCK_RESET_SECONDS`) |

---

## 20. Resumen — Mitigaciones por OWASP Top 10

| Riesgo OWASP | Mitigación |
|---|---|
| **A01 Broken Access Control** | RBAC por permisos + `PermissionsGuard` + `PoliciesGuard` + `TenantContextGuard` |
| **A02 Cryptographic Failures** | Argon2id, AES-256-GCM, RSA-4096+OAEP, scrypt para derivación, secretos ≥ 32 chars |
| **A03 Injection** | TypeORM parametrizado, `ValidationPipe` global, DTOs estrictos, `ParseUUIDPipe` |
| **A04 Insecure Design** | Separación cleara guards/middleware/services, principal mínima en JWT (`jti`+ids), revocación granular |
| **A05 Security Misconfiguration** | CORS por lista blanca, cookies `httpOnly`+`secure`+`sameSite`, secretos validados en arranque |
| **A06 Vulnerable Components** | Stack moderno (`NestJS`, `@nestjs/throttler`, `argon2`); migración de bcrypt → argon2 explícita |
| **A07 Identification & Auth Failures** | Throttling, JWT corto + refresh rotable, sesiones revocables, auditoría LOGIN/REVOKE |
| **A08 Software & Data Integrity** | Firma RSA de API keys, AES-GCM AEAD, verificación de firmas en webhooks |
| **A09 Security Logging** | `auth_session_audit`, `domain_audit_events`, `application_error_events` con purga retentiva |
| **A10 SSRF** | URLs de vendors/webhooks restringidas vía `.env`; clientes con `timeout` (`FTTH_NETWORK_SYNC_VENDOR_TIMEOUT_MS`) |

---

## 21. Mapa Rápido de Archivos Clave

| Capa | Archivo |
|---|---|
| Bootstrap | `ServicesManager/nestjs/src/main.ts` |
| Módulo raíz / pipeline | `src/app/app.module.ts` |
| Filtro de errores | `src/common/filters/http-exception.filter.ts` |
| Auth controller | `src/app/modules/auth/auth.controller.ts` |
| Auth service | `src/app/modules/auth/auth.service.ts` |
| Estrategias Passport | `src/app/modules/auth/strategies/{jwt,jwt-refresh,local,api-key}.strategy.ts` |
| Guards globales | `src/core/auth/guards/*.ts` |
| Guards de tenant/permisos | `src/app/modules/auth/guards/{tenant-context,permissions}.guard.ts` |
| Policies | `src/core/authorization/policies/*.ts` |
| CSRF | `src/app/modules/auth/middleware/csrf-protection.middleware.ts` |
| API Key middleware | `src/app/modules/auth/middleware/api-key.middleware.ts` |
| Descifrado RSA login | `src/app/modules/security-key/middleware/rsa-decrypt.middleware.ts` |
| Cripto core | `src/app/modules/utils/encryption.ts` |
| RSA service | `src/app/modules/rsa/rsa.service.ts` |
| Security Keys | `src/app/modules/security-key/security-key.service.ts` |
| API Access Keys | `src/app/modules/api-access-keys/api-access-keys.service.ts` |
| Auditoría sesiones | catálogo `auth-session-audit-catalog.ts` + `AuthSessionAudit` entity |
| Auditoría dominio | `src/app/modules/domain-audit/*` |
| Rate-limit config | `src/settings/throttle-constants.ts` |
| Validación env | `src/settings/enviroment.service.ts` |
