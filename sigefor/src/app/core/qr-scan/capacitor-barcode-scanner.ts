import { Injectable } from '@angular/core';
import { Capacitor } from '@capacitor/core';
import {
  CapacitorBarcodeScanner,
  CapacitorBarcodeScannerTypeHint,
} from '@capacitor/barcode-scanner';
import { OpticalScanner } from './optical-scanner';
import { OpticalScanOptions, QrCaptureError } from './qr-scan.types';
import { scanQrOnWeb } from './web-html5-qrcode-scanner';

/**
 * Escaneo óptico con `@capacitor/barcode-scanner` (ADR 0019): modal nativo en Android/iOS.
 * En web usa Html5Qrcode con `deviceId` elegido por el usuario (varias webcams).
 */
@Injectable({ providedIn: 'root' })
export class CapacitorBarcodeScannerService extends OpticalScanner {
  isSupported(): boolean {
    if (typeof navigator === 'undefined') {
      return false;
    }
    if (Capacitor.isPluginAvailable('CapacitorBarcodeScanner')) {
      return navigator.mediaDevices?.getUserMedia !== undefined;
    }
    return false;
  }

  async scan(signal: AbortSignal, options?: OpticalScanOptions): Promise<string> {
    if (signal.aborted) {
      throw this.error('scan-failed', 'Escaneo cancelado.');
    }
    if (!this.isSupported()) {
      throw this.error('unsupported', 'El escaneo por cámara no está disponible en este dispositivo.');
    }

    if (Capacitor.getPlatform() === 'web') {
      const deviceId = options?.deviceId?.trim();
      if (!deviceId) {
        throw this.error('scan-failed', 'Escaneo cancelado.');
      }
      try {
        return await scanQrOnWeb(deviceId, signal);
      } catch (cause) {
        if (isQrCaptureError(cause)) {
          throw cause;
        }
        throw this.error('scan-failed', 'No se pudo completar el escaneo. Use el lector o teclee el código.');
      }
    }

    const abortPromise = new Promise<never>((_, reject) => {
      signal.addEventListener(
        'abort',
        () => reject(this.error('scan-failed', 'Escaneo cancelado.')),
        { once: true },
      );
    });

    try {
      const result = await Promise.race([
        CapacitorBarcodeScanner.scanBarcode({
          hint: CapacitorBarcodeScannerTypeHint.QR_CODE,
          scanInstructions: 'Apunte la cámara al código QR de la fornitura',
        }),
        abortPromise,
      ]);
      const code = result.ScanResult?.trim();
      if (!code) {
        throw this.error('scan-failed', 'No se detectó un código QR.');
      }
      return code;
    } catch (cause) {
      if (isQrCaptureError(cause)) {
        throw cause;
      }
      throw this.error('scan-failed', 'No se pudo completar el escaneo. Use el lector o teclee el código.');
    }
  }

  private error(reason: QrCaptureError['reason'], message: string): QrCaptureError {
    return { reason, message };
  }
}

function isQrCaptureError(value: unknown): value is QrCaptureError {
  return (
    typeof value === 'object' &&
    value !== null &&
    'reason' in value &&
    'message' in value
  );
}
