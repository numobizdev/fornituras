# Quickstart de validación — 021 RBAC en UI, validación y rediseño

## Prerrequisitos

- SQL Server accesible con la cadena configurada en el `appsettings.Development.json` LOCAL
  (no versionado; ver `appsettings.Development.json.example`).
- Node 20+ y .NET 10 SDK.

## Arranque

```powershell
# Backend (puerto 8080, path /sigefor)
dotnet run --project fornituras-api-dotnet/src/Fornituras.Api

# Frontend (desde sigefor/)
npm install
npm start
```

## Suites automatizadas

```powershell
dotnet test fornituras-api-dotnet          # incluye tests del seeder "ensure admin"
cd sigefor; npm test                        # role-policy, visibilidad, field-errors, forms
```

Resultado esperado: todo verde (SC-007).

## Escenarios manuales

1. **Admin recuperado (US1/SC-001)**: arrancar la API (el seeder corrige rol/enabled del
   admin configurado; ver log "Seed admin corrected" si aplicó). Iniciar sesión con la cuenta
   admin sembrada → el menú lista **13 módulos** y su rol "Administrador" bajo el nombre;
   Almacenes y Catálogo de Tipos muestran FAB de agregar funcional.
2. **Visibilidad por rol (US2/SC-002)**: crear (desde Usuarios) un usuario por rol e iniciar
   sesión con cada uno; comparar contra la "Matriz de verificación por rol" de
   [`contracts/ui-permissions.md`](./contracts/ui-permissions.md). Puntos clave: ALMACEN ve
   agregar en Fornituras y Traslados pero no en Almacenes; AUDITOR ve Bitácora y ningún botón
   de escritura; CAPTURISTA ya NO ve el botón de Bajas (el servidor se lo rechazaba).
3. **Validación visible (US4/SC-003)**: en cada formulario (fornitura, lote, elemento,
   almacén, tipo, usuario, traslado, baja, incidencia) pulsar guardar vacío → mensaje en
   español bajo cada campo obligatorio; en Elemento, capturar CURP de 5 caracteres → mensaje
   de formato.
4. **Login (US6/SC-006)**: abrir `/login` a ancho ≥900px → dos paneles con escudo y "SIGEFOR
   — Sistema Integral de Gestión de Fornituras"; reducir a ~390px → cabecera compacta +
   formulario completo. Probar submit vacío (errores por campo) y credenciales malas (toast).
   Verificar que `/forgot-password` y `/reset-password` se ven y funcionan igual que antes.
5. **Asignación (US5/SC-005)**: con ADMIN o CAPTURISTA, escanear/teclear un código de
   fornitura disponible → tarjeta Paso 1 muestra el equipo y banner; buscar elemento por
   placa → seleccionar en tarjeta Paso 2; Asignar → aparece en "Asignaciones vigentes"
   (sección separada). Revisar en 360px y 1280px que nada se encima. Con AUDITOR → solo la
   lista de vigentes.
6. **Secretos (US3/SC-004)**: `git ls-files | Select-String appsettings.Development` → sin
   resultados; existe `appsettings.Development.json.example` sin valores reales;
   `git grep -i "F0rn1tur4s"` sobre el árbol versionado → sin resultados. La API local sigue
   arrancando con el archivo local en disco.
