import { QrScanService } from './qr-scan.service';

describe('QrScanService', () => {
  let service: QrScanService;

  beforeEach(() => {
    service = new QrScanService();
  });

  it('normaliza quitando espacios/guiones y pasando a mayúsculas', () => {
    expect(service.normalize(' for-1a2b3 ')).toBe('FOR1A2B3');
  });

  it('valida el formato FOR-XXXXX (ADR 0005) de forma opcional', () => {
    expect(service.isValidFormat('FOR-1A2B3')).toBeTrue();
    expect(service.isValidFormat('for-1a2b3')).toBeTrue();
    expect(service.isValidFormat('FOR-12')).toBeFalse();
    expect(service.isValidFormat('XXX-1A2B3')).toBeFalse();
  });

  // SC-001: el mismo código capturado por lector, cámara o manual entrega un valor idéntico.
  it('entrega el mismo valor independientemente del origen (SC-001)', () => {
    const fromReader = service.toCapture('for-1a2b3', 'hid');
    const fromCamera = service.toCapture('FOR-1A2B3', 'camera');
    const fromManual = service.toCapture(' FOR-1A2B3 ', 'manual');

    expect(fromReader?.code).toBe('FOR-1A2B3');
    expect(fromCamera?.code).toBe('FOR-1A2B3');
    expect(fromManual?.code).toBe('FOR-1A2B3');
    expect(fromReader?.source).toBe('hid');
    expect(fromCamera?.source).toBe('camera');
  });

  it('descarta un valor vacío tras recortar', () => {
    expect(service.toCapture('   ', 'manual')).toBeNull();
  });
});
