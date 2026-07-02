import { Html5Qrcode, Html5QrcodeSupportedFormats } from 'html5-qrcode';
import { QrCaptureError } from './qr-scan.types';

const SCANNER_ELEMENT_ID = 'cap-os-barcode-scanner-container-scanner';
const DIALOG_ID = 'cap-os-barcode-scanner-container-dialog';
const SCAN_INSTRUCTIONS = 'Apunte la cámara al código QR de la fornitura';

declare global {
  interface Window {
    OSBarcodeWebScanner?: Html5Qrcode | null;
  }
}

/**
 * Escaneo web con Html5Qrcode y `deviceId` explícito (el plugin Capacitor no lo expone).
 * Reutiliza el mismo DOM y estilos globales del modal de escaneo.
 */
export async function scanQrOnWeb(deviceId: string, signal: AbortSignal): Promise<string> {
  if (signal.aborted) {
    throw scanError('scan-failed', 'Escaneo cancelado.');
  }

  ensureScannerDom();
  showScannerDialog();

  return new Promise<string>((resolve, reject) => {
    let finished = false;

    const finish = async (action: 'resolve' | 'reject', value: string | QrCaptureError): Promise<void> => {
      if (finished) {
        return;
      }
      finished = true;
      signal.removeEventListener('abort', onAbort);
      await stopAndHideScanner();
      if (action === 'resolve') {
        resolve(value as string);
      } else {
        reject(value as QrCaptureError);
      }
    };

    const onAbort = (): void => {
      void finish('reject', scanError('scan-failed', 'Escaneo cancelado.'));
    };
    signal.addEventListener('abort', onAbort, { once: true });

    const closeButton = document.getElementById('cap-os-barcode-scanner-close-button');
    if (closeButton) {
      closeButton.onclick = () => {
        void finish('reject', scanError('scan-failed', 'Escaneo cancelado.'));
      };
    }

    const scannerHost = document.getElementById(SCANNER_ELEMENT_ID);
    if (!scannerHost) {
      void finish('reject', scanError('scan-failed', 'No se pudo abrir el escáner.'));
      return;
    }

    const scanner = new Html5Qrcode(SCANNER_ELEMENT_ID, {
      formatsToSupport: [Html5QrcodeSupportedFormats.QR_CODE],
      verbose: false,
    });
    window.OSBarcodeWebScanner = scanner;

    const config = {
      fps: 10,
      qrbox: Math.max(scannerHost.getBoundingClientRect().width * (9 / 16) - 10, 200),
      aspectRatio: 16 / 9,
    };

    void scanner
      .start(
        deviceId,
        config,
        (decodedText) => {
          const code = decodedText.trim();
          if (!code) {
            void finish('reject', scanError('scan-failed', 'No se detectó un código QR.'));
            return;
          }
          void finish('resolve', code);
        },
        (errorMessage) => {
          const benign = [
            'NotFoundException',
            'No barcode or QR code detected',
            'No MultiFormat Readers were able to detect the code',
          ];
          if (!benign.some((fragment) => errorMessage.includes(fragment))) {
            void finish('reject', scanError('scan-failed', 'No se pudo completar el escaneo.'));
          }
        },
      )
      .catch(() => {
        void finish(
          'reject',
          scanError('scan-failed', 'No se pudo abrir la cámara seleccionada. Elija otra cámara.'),
        );
      });
  });
}

function ensureScannerDom(): void {
  if (document.getElementById('cap-os-barcode-scanner-container')) {
    const instructions = document.getElementById('cap-os-barcode-scanner-instructions');
    if (instructions) {
      instructions.textContent = SCAN_INSTRUCTIONS;
    }
    return;
  }

  const root = document.body.appendChild(document.createElement('div'));
  root.id = 'cap-os-barcode-scanner-container';

  const dialog = document.createElement('div');
  dialog.id = DIALOG_ID;
  dialog.className = 'scanner-dialog';

  const inner = document.createElement('div');
  inner.className = 'scanner-dialog-inner';

  const close = document.createElement('span');
  close.id = 'cap-os-barcode-scanner-close-button';
  close.className = 'close-button';
  close.innerHTML = '&times;';
  inner.appendChild(close);

  const instructions = document.createElement('p');
  instructions.id = 'cap-os-barcode-scanner-instructions';
  instructions.className = 'scanner-instructions';
  instructions.textContent = SCAN_INSTRUCTIONS;
  inner.appendChild(instructions);

  const scannerHost = document.createElement('div');
  scannerHost.className = 'scanner-container-full-width';
  scannerHost.id = SCANNER_ELEMENT_ID;
  inner.appendChild(scannerHost);

  dialog.appendChild(inner);
  root.appendChild(dialog);
}

function showScannerDialog(): void {
  const dialog = document.getElementById(DIALOG_ID);
  if (dialog) {
    dialog.style.display = 'block';
  }
}

async function stopAndHideScanner(): Promise<void> {
  const scanner = window.OSBarcodeWebScanner;
  if (scanner) {
    try {
      await scanner.stop();
    } catch {
      // El escáner puede estar ya detenido al cerrar.
    }
    window.OSBarcodeWebScanner = null;
  }
  const dialog = document.getElementById(DIALOG_ID);
  if (dialog) {
    dialog.style.display = 'none';
  }
}

function scanError(reason: QrCaptureError['reason'], message: string): QrCaptureError {
  return { reason, message };
}
