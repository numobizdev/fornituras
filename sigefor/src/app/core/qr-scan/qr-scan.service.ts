import { Injectable } from '@angular/core';
import { QR_CODE_PATTERN, QrCapture, QrCaptureSource } from './qr-scan.types';

/**
 * Lógica de captura de QR independiente de la UI: normalización del valor y validación de formato
 * **opcional**. No resuelve datos (Principio II/IV): solo produce el código que el consumidor
 * enviará al servidor. Reutilizable por lector, cámara y tecleo manual → mismo valor (SC-001).
 */
@Injectable({ providedIn: 'root' })
export class QrScanService {
  /** Normaliza a la forma canónica: sin espacios ni guiones, en mayúsculas. */
  normalize(raw: string): string {
    return raw.replace(/[\s-]+/g, '').toUpperCase();
  }

  /** Validación de formato opcional (ADR 0005): se aplica sobre el valor tal cual, con guion. */
  isValidFormat(code: string): boolean {
    return QR_CODE_PATTERN.test(code.trim().toUpperCase());
  }

  /**
   * Construye la captura entregada al consumidor a partir del texto crudo de cualquier medio.
   * Devuelve `null` si el valor queda vacío tras recortar. El valor es idéntico sin importar el
   * origen (lector/cámara/manual): esa es la garantía central de la feature.
   */
  toCapture(raw: string, source: QrCaptureSource): QrCapture | null {
    const code = raw.trim().toUpperCase();
    if (code.length === 0) {
      return null;
    }
    return { code, source };
  }
}
