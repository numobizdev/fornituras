# Quickstart: Escaneo QR Capacitor (019)

## Prerrequisitos

- API .NET en `8080`, frontend en `sigefor/` con `npm install`
- Fornitura de prueba con QR `FOR-XXXXX` en estado `DISPONIBLE`

## Escenario 1 — Web (webcam)

1. `ionic serve` desde `sigefor/`
2. Ir a `/asignacion` → Paso 1 → botón cámara
3. Elegir webcam si hay varias → escanear QR
4. Verificar: código, descripción, **almacén**, badge «Disponible»

## Escenario 2 — Fornitura no disponible

1. Escanear fornitura ya `ASIGNADA`
2. Verificar: badge «No disponible», paso 2 bloqueado

## Escenario 3 — Fallback manual

1. Denegar permiso de cámara o usar navegador sin cámara
2. Teclear código + «Buscar» → mismo resultado que escaneo

## Escenario 4 — Android (cuando exista plataforma)

```powershell
npx cap add android
# minSdkVersion 26 en android/variables.gradle
npx cap sync
```

Abrir en dispositivo → escaneo nativo con `@capacitor/barcode-scanner`.
