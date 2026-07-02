# Contrato REST — QR (módulo `qrcodes`, IMPLEMENTADO)

> **Actualizado 2026-06-30** para reflejar el `QrController` real (ADR 0005). Sustituye el
> contrato anterior basado en UUID+HMAC (`POST /equipment/{id}/qr`, `qr:verify`), que **no** se
> implementó. Los códigos son `FOR-XXXXX` opacos, **sin firma**.

Base: **`/api/v1/qr`**. Todos requieren `Authorization: Bearer <jwt>`. Datos en `ApiResponse<T>`;
las descargas devuelven binarios con `Content-Disposition: attachment`.

## Generar lote

`POST /api/v1/qr/lotes` → **201** `ApiResponse<LoteQrResponseDTO>`

Body `GenerateQrForm`:

```jsonc
{
  "descripcion": "Códigos prendas Chiapas",  // requerido, ≤ 255
  "cantidad": 100,                            // requerido, 1..10000 (y ≤ app.qr.maxBatchSize)
  "qrSizeCm": 3.0,                            // requerido, 1.0..15.0
  "paddingCm": 0.5,                           // requerido, 0.0..5.0
  "labelPosition": "BOTTOM",                  // NONE | TOP | BOTTOM
  "mostrarBordes": true                        // requerido
}
```

Efecto: crea el lote y `cantidad` códigos únicos `FOR-XXXXX`.

## Listar lotes

`GET /api/v1/qr/lotes` → **200** `ApiResponse<List<LoteQrResponseDTO>>` (más recientes primero).

## Detalle de lote

`GET /api/v1/qr/lotes/{id}` → **200** `ApiResponse<LoteQrResponseDTO>`.

## Códigos de un lote

`GET /api/v1/qr/lotes/{id}/codigos` → **200** `ApiResponse<List<CodigoQrResponseDTO>>`
(cada `CodigoQrResponseDTO` lleva el `codigo` y el id del lote).

## Descargas con los ajustes del lote

- `GET /api/v1/qr/lotes/{id}/pdf` → **200** `application/pdf` (un QR por código).
- `GET /api/v1/qr/lotes/{id}/zip` → **200** `application/zip` (un PNG por código).

## Exportación con ajustes personalizados (no cambia los códigos)

- `POST /api/v1/qr/lotes/{id}/export/pdf` → **200** `application/pdf`
- `POST /api/v1/qr/lotes/{id}/export/zip` → **200** `application/zip`

Body `ReprintQrForm` (mismos rangos que `GenerateQrForm`, sin `descripcion`/`cantidad`):

```jsonc
{ "qrSizeCm": 4.0, "paddingCm": 0.5, "labelPosition": "TOP", "mostrarBordes": false }
```

## Notas

- **Autenticación:** JWT obligatorio (`Bearer`). **Autorización:** solo rol **`SUPER_ADMIN`**
  (`RolePolicy.ManageQrLotes`, ADR 0020). Los roles operativos (`ADMIN`, `ALMACEN`, `CAPTURISTA`)
  ya no tienen acceso a estos endpoints.
- **UI:** módulo Ionic `/qr-lotes/**` (spec 021); la UI Thymeleaf Java (`/qr/**`) está obsoleta.
- **Sin verificación de firma:** no existe `qr:verify`; los códigos no se firman (ADR 0005,
  divergencia de seguridad conocida respecto al Principio II).
- **Auditoría:** generación registrada como `GENERATE_QR_BATCH` (feature 012).
- **Resolución `código → fornitura`:** NO vive aquí; ocurre al dar de alta/asignar la fornitura
  (specs 001/004) y al escanear (014), siempre server-side tras authn+authz.
