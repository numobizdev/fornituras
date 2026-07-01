/**
 * Detección de lector HID (código de barras/QR que emula teclado) por heurística de entrada:
 * un lector "teclea" muy rápido y termina con un carácter terminador (Enter/Tab), mientras que
 * una persona teclea más lento. Sin librerías (Principio VI).
 *
 * El detector es agnóstico del framework: recibe eventos de teclado y notifica cuando reconoce
 * una ráfaga completa como proveniente de un lector.
 */

export interface HidDetectorOptions {
  /** Umbral máximo (ms) entre pulsaciones para considerarlas parte de una ráfaga de lector. */
  readonly maxKeystrokeGapMs: number;
  /** Longitud mínima de la ráfaga para aceptarla como lectura (evita falsos positivos). */
  readonly minLength: number;
}

export const DEFAULT_HID_OPTIONS: HidDetectorOptions = {
  maxKeystrokeGapMs: 35,
  minLength: 3,
};

const TERMINATOR_KEYS = new Set(['Enter', 'Tab']);

/**
 * Acumula caracteres de una ráfaga rápida y, al llegar el terminador, decide si el patrón
 * corresponde a un lector HID. Es intencionalmente pequeño y testeable con un reloj inyectable.
 */
export class HidDetector {
  private buffer = '';
  private lastTimestamp = 0;

  constructor(
    private readonly options: HidDetectorOptions = DEFAULT_HID_OPTIONS,
    private readonly now: () => number = () => Date.now(),
  ) {}

  /**
   * Procesa una pulsación. Devuelve el código si la pulsación cierra una ráfaga reconocida como
   * lector HID; en cualquier otro caso devuelve `null` (y sigue acumulando o se reinicia).
   */
  push(key: string): string | null {
    const timestamp = this.now();
    const gap = timestamp - this.lastTimestamp;
    this.lastTimestamp = timestamp;

    if (TERMINATOR_KEYS.has(key)) {
      const candidate = this.buffer;
      this.reset();
      return this.isReaderBurst(candidate) ? candidate : null;
    }

    // Solo consideramos caracteres imprimibles (un solo carácter).
    if (key.length !== 1) {
      this.reset();
      return null;
    }

    // Un hueco grande rompe la ráfaga: se asume tecleo humano y se reinicia con esta tecla.
    if (this.buffer.length > 0 && gap > this.options.maxKeystrokeGapMs) {
      this.buffer = key;
      return null;
    }

    this.buffer += key;
    return null;
  }

  reset(): void {
    this.buffer = '';
    this.lastTimestamp = 0;
  }

  private isReaderBurst(candidate: string): boolean {
    return candidate.length >= this.options.minLength;
  }
}
