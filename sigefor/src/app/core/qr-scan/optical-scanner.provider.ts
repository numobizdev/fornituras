import { inject } from '@angular/core';
import { CapacitorBarcodeScannerService } from './capacitor-barcode-scanner';
import { OpticalScanner, WebBarcodeDetectorScanner } from './optical-scanner';

/** Preferencia Capacitor (019); fallback web BarcodeDetector (ADR 0008). */
export function provideOpticalScanner() {
  return {
    provide: OpticalScanner,
    useFactory: () => {
      const capacitor = inject(CapacitorBarcodeScannerService);
      if (capacitor.isSupported()) {
        return capacitor;
      }
      return inject(WebBarcodeDetectorScanner);
    },
  };
}
