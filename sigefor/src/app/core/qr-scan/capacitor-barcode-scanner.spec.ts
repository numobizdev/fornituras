import { Capacitor } from '@capacitor/core';
import { CapacitorBarcodeScanner, CapacitorBarcodeScannerTypeHint } from '@capacitor/barcode-scanner';
import { CapacitorBarcodeScannerService } from './capacitor-barcode-scanner';

describe('CapacitorBarcodeScannerService', () => {
  let service: CapacitorBarcodeScannerService;
  let scanBarcodeSpy: jasmine.Spy;

  beforeEach(() => {
    service = new CapacitorBarcodeScannerService();
    spyOn(Capacitor, 'isPluginAvailable').and.returnValue(true);
    scanBarcodeSpy = spyOn(CapacitorBarcodeScanner, 'scanBarcode');
    Object.defineProperty(navigator, 'mediaDevices', {
      configurable: true,
      value: { getUserMedia: jasmine.createSpy('getUserMedia') },
    });
  });

  it('requires deviceId on web before opening the scanner', async () => {
    spyOn(Capacitor, 'getPlatform').and.returnValue('web');

    await expectAsync(service.scan(new AbortController().signal)).toBeRejected();
    expect(scanBarcodeSpy).not.toHaveBeenCalled();
  });

  it('uses CapacitorBarcodeScanner on native platforms', async () => {
    spyOn(Capacitor, 'getPlatform').and.returnValue('android');
    scanBarcodeSpy.and.resolveTo({
      ScanResult: 'FOR-ABC12',
      format: CapacitorBarcodeScannerTypeHint.QR_CODE,
    });

    const code = await service.scan(new AbortController().signal);

    expect(code).toBe('FOR-ABC12');
    expect(scanBarcodeSpy).toHaveBeenCalled();
  });

  it('is not supported when getUserMedia is unavailable', () => {
    Object.defineProperty(navigator, 'mediaDevices', { configurable: true, value: undefined });
    expect(service.isSupported()).toBeFalse();
  });
});
