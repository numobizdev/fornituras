# Phase 1 — Quickstart: QR opaco y firmado

> Cómo configurar, ejecutar y **validar** la feature una vez implementada. El backend aún no
> existe (fase de inicialización); este documento define el contrato de uso para cuando se
> implemente con `/speckit-tasks` → `/speckit-implement`.

## 1. Variables de entorno (nunca en el repo)

Añadir a `.env.example` **solo los nombres** (Principio III):

```
# Llaves HMAC del QR: "version:base64key" separadas por coma. NUNCA versionar el valor real.
QR_HMAC_KEYS=
# Versión de llave activa usada al emitir nuevos QR.
QR_HMAC_ACTIVE_VERSION=
```

En local, el valor real va en `.env` (ya ignorado por git). Para producción, en el gestor de
secretos (ADR pendiente).

## 2. Generar el QR de un equipo

```bash
# 1) Autenticarse y obtener el JWT (feature de auth)
# 2) Generar el QR de un equipo existente
curl -X POST https://localhost:8443/api/v1/equipment/{equipmentId}/qr \
  -H "Authorization: Bearer $TOKEN"

# 3) Descargar la imagen para grabado/impresión (ECC alto recomendado)
curl -X GET "https://localhost:8443/api/v1/equipment/{equipmentId}/qr?format=png&ecc=H" \
  -H "Authorization: Bearer $TOKEN" -o equipo-qr.png
```

## 3. Validaciones clave (mapean a Success Criteria)

- **No-fuga (SC-002):** decodificar el contenido del PNG generado y verificar que es
  exactamente `v<ver>.<b64url(uuid)>.<b64url(hmac)>` — sin número de serie, nombre ni nada del
  elemento.
- **Integridad (SC-003):** alterar un carácter del `payload` y llamar `POST /qr:verify` →
  debe responder `{"valid": false}`.
- **Unicidad (SC-001):** generar el QR de dos equipos distintos → `opaqueId` distintos.
- **Rotación (SC-004):** emitir un QR con versión 1, configurar versión activa 2, y verificar
  que el QR de versión 1 sigue dando `valid: true`.
- **Idempotencia:** llamar dos veces a `POST .../qr` sobre el mismo equipo → mismo `opaqueId`.

## 4. Pruebas automatizadas esperadas

- **Unit:** firma/verificación HMAC (incl. versión de llave); serialización de `QrPayload`.
- **Contract:** los 4 endpoints según [contracts/qr-api.md](./contracts/qr-api.md), incluida la
  respuesta uniforme de `verify` ante fallo.
- **Integración (Testcontainers MSSQL):** unicidad de `qr_opaque_id`, idempotencia y reissue.
- **Seguridad:** test que falla si algún log contiene el payload o la llave.
