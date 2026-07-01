import { Injectable } from '@angular/core';
import { QrCaptureError } from './qr-scan.types';

/**
 * Puerto de escaneo óptico (estilo LEGO). El componente de captura depende de esta abstracción,
 * no de una implementación concreta, de modo que la cámara web (best-effort, sin dependencias) se
 * puede sustituir por un plugin nativo (p. ej. ML Kit de Capacitor) sin tocar a los consumidores.
 * Ver ADR 0008.
 */
export abstract class OpticalScanner {
  /** ¿Hay soporte de escaneo óptico en esta plataforma? Si no, el flujo degrada a lector/manual. */
  abstract isSupported(): boolean;

  /**
   * Abre la cámara sobre el `video` dado y resuelve con el **texto crudo** del primer código
   * detectado. Se cancela con `signal` (p. ej. al cerrar el visor). Rechaza con {@link QrCaptureError}
   * ante permiso denegado / ausencia de cámara / falta de soporte.
   */
  abstract scan(video: HTMLVideoElement, signal: AbortSignal): Promise<string>;
}

/** Tipado mínimo de la API web `BarcodeDetector` (aún no incluida en las libs de TS). */
interface BarcodeDetectorLike {
  detect(source: CanvasImageSource): Promise<Array<{ rawValue: string }>>;
}
interface BarcodeDetectorCtor {
  new (options?: { formats?: string[] }): BarcodeDetectorLike;
  getSupportedFormats?(): Promise<string[]>;
}

/** Intervalo (ms) entre intentos de detección sobre el fotograma actual. */
const DETECT_INTERVAL_MS = 250;

/**
 * Implementación best-effort con `getUserMedia` + `BarcodeDetector` (Chrome/Edge y Android WebView).
 * Cero dependencias nuevas (Principio VI). En navegadores sin soporte, {@link isSupported} es `false`
 * y el componente ofrece solo lector/manual.
 */
@Injectable({ providedIn: 'root' })
export class WebBarcodeDetectorScanner extends OpticalScanner {
  private get detectorCtor(): BarcodeDetectorCtor | undefined {
    return (globalThis as unknown as { BarcodeDetector?: BarcodeDetectorCtor }).BarcodeDetector;
  }

  isSupported(): boolean {
    return (
      this.detectorCtor !== undefined &&
      typeof navigator !== 'undefined' &&
      navigator.mediaDevices?.getUserMedia !== undefined
    );
  }

  async scan(video: HTMLVideoElement, signal: AbortSignal): Promise<string> {
    const Detector = this.detectorCtor;
    if (Detector === undefined) {
      throw this.error('unsupported', 'El escaneo por cámara no está disponible en este dispositivo.');
    }

    let stream: MediaStream;
    try {
      stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' },
      });
    } catch (cause) {
      throw this.fromGetUserMediaError(cause);
    }

    video.srcObject = stream;
    await video.play().catch(() => undefined);
    const detector = new Detector({ formats: ['qr_code'] });

    try {
      return await this.detectLoop(detector, video, signal);
    } finally {
      stream.getTracks().forEach((track) => track.stop());
      video.srcObject = null;
    }
  }

  private detectLoop(
    detector: BarcodeDetectorLike,
    video: HTMLVideoElement,
    signal: AbortSignal,
  ): Promise<string> {
    return new Promise<string>((resolve, reject) => {
      if (signal.aborted) {
        reject(this.error('scan-failed', 'Escaneo cancelado.'));
        return;
      }
      let timer: ReturnType<typeof setTimeout> | undefined;

      const onAbort = (): void => {
        clearTimeout(timer);
        reject(this.error('scan-failed', 'Escaneo cancelado.'));
      };
      signal.addEventListener('abort', onAbort, { once: true });

      const tick = async (): Promise<void> => {
        try {
          const codes = await detector.detect(video);
          const hit = codes.find((c) => c.rawValue.length > 0);
          if (hit) {
            signal.removeEventListener('abort', onAbort);
            resolve(hit.rawValue);
            return;
          }
        } catch {
          // Un fotograma no legible no es fatal: se reintenta en el siguiente tick.
        }
        timer = setTimeout(() => void tick(), DETECT_INTERVAL_MS);
      };

      void tick();
    });
  }

  private fromGetUserMediaError(cause: unknown): QrCaptureError {
    const name = cause instanceof DOMException ? cause.name : '';
    if (name === 'NotAllowedError' || name === 'SecurityError') {
      return this.error('permission-denied', 'Permiso de cámara denegado. Use el lector o teclee el código.');
    }
    if (name === 'NotFoundError' || name === 'OverconstrainedError') {
      return this.error('no-camera', 'No se encontró una cámara. Use el lector o teclee el código.');
    }
    return this.error('scan-failed', 'No se pudo abrir la cámara. Use el lector o teclee el código.');
  }

  private error(reason: QrCaptureError['reason'], message: string): QrCaptureError {
    return { reason, message };
  }
}
