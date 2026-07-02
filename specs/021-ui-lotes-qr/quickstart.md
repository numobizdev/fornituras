# Quickstart: UI de lotes QR (SUPER_ADMIN)

Validación manual del feature 021 tras implementación.

## 1. Prerrequisitos

- Backend .NET en marcha: `dotnet run --project fornituras-api-dotnet/src/Fornituras.Api`
- Frontend Ionic: `npm start` desde `sigefor/`
- Usuario seed dev (solo entorno local con seed habilitado):
  - **Email:** `superadmin@fornituras.local`
  - **Contraseña:** `SuperAdmin#2026`

> Un ADMIN puede crear otros SUPER_ADMIN desde **Usuarios y Roles**.

## 2. Login como SUPER_ADMIN

1. Abrir `http://localhost:8100/login`
2. Iniciar sesión con el usuario SUPER_ADMIN
3. **Esperado:** redirección a `/qr-lotes` (lista de lotes), no a `/inicio`
4. **Esperado:** menú lateral solo muestra **Lotes QR** (+ cerrar sesión)

## 3. Generar un lote

1. Ir a **Generar lote** (`/qr-lotes/generar`)
2. Completar: descripción "Prueba quickstart", cantidad **5**, tamaño **3** cm, padding **0.5** cm,
   etiqueta abajo, bordes sí, formato **PDF**
3. Confirmar y esperar (overlay visible en lotes grandes)
4. **Esperado:** pantalla de éxito con ID, rango FOR-XXXXXX y descarga PDF automática

## 4. Listar y detalle

1. Volver a la lista (`/qr-lotes`)
2. **Esperado:** el lote recién creado aparece primero
3. Abrir detalle
4. **Esperado:** metadata + formulario de reimpresión
5. Descargar **ZIP con config. original**
6. **Esperado:** archivo ZIP con 5 PNG escaneables

## 5. Reimpresión personalizada

1. En detalle, cambiar tamaño a **4** cm y reimprimir PDF
2. **Esperado:** PDF con nuevo layout; mismos códigos FOR-XXXXXX

## 6. Control de acceso

1. Cerrar sesión; iniciar como `ADMIN` (u otro rol operativo)
2. **Esperado:** no aparece ítem **Lotes QR** en menú
3. Navegar manualmente a `/qr-lotes`
4. **Esperado:** redirección a `/inicio`
5. (Opcional) `curl` a `POST /sigefor/api/v1/qr/lotes` con JWT de ADMIN
6. **Esperado:** HTTP 403

## 7. SUPER_ADMIN aislado

1. Iniciar sesión como SUPER_ADMIN
2. Navegar manualmente a `/fornituras` o `/usuarios`
3. **Esperado:** redirección a `/qr-lotes`

## Criterios de éxito (spec)

- [ ] SC-001: generación + descarga < 2 min (lote pequeño)
- [ ] SC-002: ADMIN sin acceso UI ni API
- [ ] SC-003: re-export no cambia códigos
- [ ] SC-004: QR escaneables, sin PII en contenido
