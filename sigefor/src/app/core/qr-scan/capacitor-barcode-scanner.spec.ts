import { Capacitor } from '@capacitor/core';
import { CapacitorBarcodeScanner, CapacitorBarcodeScannerTypeHint } from '@capacitor/barcode-scanner';
import { CapacitorBarcodeScannerService } from './capacitor-barcode-scanner';

describe('CapacitorBarcodeScannerService', () => {
  let service: CapacitorBarcodeScannerService;

  beforeEach(() => {
    service = new CapacitorBarcodeScannerService();
    spyOn(Capacitor, 'isPluginAvailable').and.returnValue(true);
    Object.defineProperty(navigator, 'mediaDevices', {
      configurable: true,
      value: { getUserMedia: jasmine.createSpy('getUserMedia') },
    });
  });

  it('uses modal scan (no embedded video)', () => {
    expect(service.usesEmbeddedVideo()).toBeFalse();
  });

  it('returns the scanned QR code from CapacitorBarcodeScanner', async () => {
    spyOn(CapacitorBarcodeScanner, 'scanBarcode').and.resolveTo({
      ScanResult: 'FOR-ABC12',
      format: CapacitorBarcodeScannerTypeHint.QR_CODE,
    });

    const code = await service.scan(document.createElement('video'), new AbortController().signal);

    expect(code).toBe('FOR-ABC12');
    expect(CapacitorBarcodeScanner.scanBarcode).toHaveBeenCalledWith(
      jasmine.objectContaining({
        hint: CapacitorBarcodeScannerTypeHint.QR_CODE,
        web: jasmine.objectContaining({ showCameraSelection: true }),
      }),
    );
  });

  it('is not supported when getUserMedia is unavailable', () => {
    Object.defineProperty(navigator, 'mediaDevices', { configurable: true, value: undefined });
    expect(service.isSupported()).toBeFalse();
  });
});
