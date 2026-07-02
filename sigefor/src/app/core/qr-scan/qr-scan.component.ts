import {
  Component,
  OnDestroy,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { IonButton, IonIcon, IonInput, IonNote, IonSpinner } from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { cameraOutline } from 'ionicons/icons';
import { HidDetector } from './hid-detector';
import { CameraSelectionService } from './camera-selection.service';
import { OpticalScanner } from './optical-scanner';
import { QrScanService } from './qr-scan.service';
import { QrCapture, QrCaptureError } from './qr-scan.types';

/**
 * Componente único de captura de QR (feature 014), reutilizado por 001/004/007/009.
 *
 * Un campo de texto cubre **lector HID** (detección por ráfaga) y **tecleo manual**; el botón de
 * cámara abre el **escaneo óptico** (Capacitor barcode-scanner). Emite el código capturado por
 * `codeCaptured` y errores de cámara por `captureError`. **No** resuelve datos: la relación código
 * → fornitura/elemento la hace el servidor en el consumidor (Principios II/IV).
 */
@Component({
  selector: 'app-qr-scan',
  templateUrl: './qr-scan.component.html',
  styleUrls: ['./qr-scan.component.scss'],
  imports: [IonInput, IonButton, IonIcon, IonNote, IonSpinner],
})
export class QrScanComponent implements OnDestroy {
  private readonly service = inject(QrScanService);
  private readonly scanner = inject(OpticalScanner);
  private readonly cameraSelection = inject(CameraSelectionService);

  /** Texto del placeholder del campo (lector + manual). */
  readonly placeholder = input('Escanee con el lector o teclee el código');
  /** Etiqueta del botón de confirmación (p. ej. "Buscar" o "Agregar"). */
  readonly actionLabel = input('Buscar');
  /** Si es `true`, valida el formato `FOR-XXXXX` antes de emitir (ADR 0005). */
  readonly validateFormat = input(false);
  /** Limpia el campo tras emitir (útil en captura repetida, p. ej. lote). */
  readonly clearOnCapture = input(false);

  /** Código capturado y entregado al consumidor (opaco). */
  readonly codeCaptured = output<string>();
  /** Detalle de captura (código + origen) para consumidores que quieran distinguir el medio. */
  readonly captured = output<QrCapture>();
  /** Error de captura (cámara denegada/no disponible). El flujo sigue por lector/manual. */
  readonly captureError = output<QrCaptureError>();

  readonly value = signal('');
  readonly isScanning = signal(false);
  readonly formatError = signal(false);
  readonly cameraSupported = this.scanner.isSupported();

  readonly canSubmit = computed(() => this.value().trim().length > 0);

  private readonly hidDetector = new HidDetector();
  private abortController?: AbortController;

  constructor() {
    addIcons({ cameraOutline });
  }

  ngOnDestroy(): void {
    this.stopCamera();
  }

  /** Entrada de teclado: alimenta la heurística de lector HID y emite si reconoce una ráfaga. */
  onKeyDown(event: KeyboardEvent): void {
    const detected = this.hidDetector.push(event.key);
    if (detected !== null) {
      event.preventDefault();
      this.emit(detected, 'hid');
    } else if (event.key === 'Enter') {
      event.preventDefault();
      this.submitManual();
    }
  }

  onInput(value: string): void {
    this.value.set(value);
    this.formatError.set(false);
  }

  /** Confirmación manual (botón o Enter cuando no se reconoció como lector). */
  submitManual(): void {
    this.emit(this.value(), 'manual');
  }

  async startCamera(): Promise<void> {
    if (!this.cameraSupported || this.isScanning()) {
      return;
    }

    let deviceId: string | null | undefined;
    try {
      deviceId = await this.cameraSelection.resolveDeviceIdForScan();
    } catch (error) {
      if ((error as QrCaptureError).reason) {
        this.captureError.emit(error as QrCaptureError);
      }
      return;
    }
    if (deviceId === null) {
      return;
    }

    this.isScanning.set(true);
    this.abortController = new AbortController();
    let capturedRaw: string | null = null;
    try {
      capturedRaw = await this.scanner.scan(this.abortController.signal, {
        deviceId: deviceId ?? undefined,
      });
    } catch (error) {
      if ((error as QrCaptureError).reason !== 'scan-failed') {
        this.captureError.emit(error as QrCaptureError);
      }
    } finally {
      this.stopCamera();
    }
    if (capturedRaw !== null) {
      this.emit(capturedRaw, 'camera');
    }
  }

  /** Restablece el campo (p. ej. al limpiar el formulario del consumidor). */
  reset(): void {
    this.value.set('');
    this.formatError.set(false);
  }

  stopCamera(): void {
    this.abortController?.abort();
    this.abortController = undefined;
    this.isScanning.set(false);
  }

  private emit(raw: string, source: QrCapture['source']): void {
    const capture = this.service.toCapture(raw, source);
    if (capture === null) {
      return;
    }
    if (this.validateFormat() && !this.service.isValidFormat(capture.code)) {
      this.formatError.set(true);
      return;
    }
    this.formatError.set(false);
    if (!this.clearOnCapture()) {
      this.value.set(capture.code);
    }
    this.codeCaptured.emit(capture.code);
    this.captured.emit(capture);
    if (this.clearOnCapture()) {
      this.value.set('');
    }
  }
}
