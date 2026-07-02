import { CapacitorBarcodeScannerService } from './capacitor-barcode-scanner';
import { OpticalScanner } from './optical-scanner';

/** Escaneo óptico vía `@capacitor/barcode-scanner` (ADR 0019). */
export function provideOpticalScanner() {
  return {
    provide: OpticalScanner,
    useExisting: CapacitorBarcodeScannerService,
  };
}
