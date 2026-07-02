import { OpticalScanOptions } from './qr-scan.types';

/**
 * Puerto de escaneo óptico (estilo LEGO). El componente de captura depende de esta abstracción,
 * no de una implementación concreta (p. ej. `@capacitor/barcode-scanner`). Ver ADR 0008 / 0019.
 */
export abstract class OpticalScanner {
  /** ¿Hay soporte de escaneo óptico en esta plataforma? Si no, el flujo degrada a lector/manual. */
  abstract isSupported(): boolean;

  /**
   * Abre el escáner y resuelve con el **texto crudo** del primer código detectado.
   * Se cancela con `signal`. Rechaza con {@link QrCaptureError} ante permiso denegado / ausencia de
   * cámara / falta de soporte.
   */
  abstract scan(signal: AbortSignal, options?: OpticalScanOptions): Promise<string>;
}
