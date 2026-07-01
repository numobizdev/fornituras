/**
 * Contrato del componente/servicio de captura de QR (feature 014).
 *
 * El componente entrega únicamente el **código opaco** capturado (`FOR-XXXXX`); nunca resuelve
 * `código → fornitura/elemento` en el cliente. Esa resolución es server-side en cada consumidor
 * (Principios II y IV; ADR 0005).
 */

/** Formato opcional de los códigos del sistema (ADR 0005). Sin firma; validar formato es opcional. */
export const QR_CODE_PATTERN = /^FOR-[0-9A-Z]{5}$/;

/** Origen desde el que se capturó un código. */
export type QrCaptureSource = 'hid' | 'camera' | 'manual';

/** Valor entregado al consumidor tras una captura correcta. */
export interface QrCapture {
  /** Código normalizado (trim + mayúsculas) listo para enviar al servidor. */
  readonly code: string;
  /** Medio por el que se capturó, con fines de UX (no altera el valor entregado). */
  readonly source: QrCaptureSource;
}

/** Motivos por los que la captura óptica no está disponible o falla. */
export type QrCaptureErrorReason =
  | 'permission-denied'
  | 'no-camera'
  | 'unsupported'
  | 'scan-failed';

/** Error de captura (p. ej. cámara denegada). El flujo degrada a lector/manual sin romperse. */
export interface QrCaptureError {
  readonly reason: QrCaptureErrorReason;
  /** Mensaje legible para el usuario, sin filtrar detalles internos ni PII. */
  readonly message: string;
}
