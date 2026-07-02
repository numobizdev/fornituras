import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { QrScanComponent } from './qr-scan.component';
import { OpticalScanner } from './optical-scanner';
import { CameraSelectionService } from './camera-selection.service';
import { QrCaptureError } from './qr-scan.types';

/** Escáner óptico de prueba, configurable para resolver un valor o rechazar con un error. */
class FakeOpticalScanner extends OpticalScanner {
  supported = true;
  result: { code?: string; error?: QrCaptureError } = {};

  isSupported(): boolean {
    return this.supported;
  }

  scan(_signal: AbortSignal): Promise<string> {
    if (this.result.error) {
      return Promise.reject(this.result.error);
    }
    return Promise.resolve(this.result.code ?? '');
  }
}

describe('QrScanComponent', () => {
  let fixture: ComponentFixture<QrScanComponent>;
  let component: QrScanComponent;
  let scanner: FakeOpticalScanner;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    scanner = new FakeOpticalScanner();
    await TestBed.configureTestingModule({
      imports: [QrScanComponent],
      providers: [
        { provide: OpticalScanner, useValue: scanner },
        {
          provide: CameraSelectionService,
          useValue: {
            resolveDeviceIdForScan: async () => undefined,
          },
        },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(QrScanComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('emite el código normalizado al confirmar manualmente', () => {
    const emitted: string[] = [];
    component.codeCaptured.subscribe((code) => emitted.push(code));

    component.onInput(' for-1a2b3 ');
    component.submitManual();

    expect(emitted).toEqual(['FOR-1A2B3']);
  });

  // SC-001: cámara y manual entregan exactamente el mismo valor.
  it('la cámara entrega el mismo valor que el modo manual (SC-001)', async () => {
    const emitted: string[] = [];
    component.captured.subscribe((c) => emitted.push(`${c.source}:${c.code}`));
    scanner.result = { code: 'for-1a2b3' };

    component.onInput('for-1a2b3');
    component.submitManual();
    await component.startCamera();

    expect(emitted).toEqual(['manual:FOR-1A2B3', 'camera:FOR-1A2B3']);
    expect(component.isScanning()).toBeFalse();
    expect(component.value()).toBe('FOR-1A2B3');
  });

  // SC-002 / FR-005: permiso de cámara denegado → captureError y el modo manual sigue operativo.
  it('degrada a manual cuando se deniega la cámara (SC-002, FR-005)', async () => {
    const errors: QrCaptureError[] = [];
    const codes: string[] = [];
    component.captureError.subscribe((e) => errors.push(e));
    component.codeCaptured.subscribe((c) => codes.push(c));
    scanner.result = { error: { reason: 'permission-denied', message: 'denegado' } };

    await component.startCamera();
    expect(errors.length).toBe(1);
    expect(component.isScanning()).toBeFalse();

    // El flujo no se rompe: la captura manual sigue funcionando.
    component.onInput('FOR-9Z9Z9');
    component.submitManual();
    expect(codes).toEqual(['FOR-9Z9Z9']);
  });

  // SC-003 / FR-004: el componente no resuelve datos en el cliente (no hace peticiones HTTP).
  it('no resuelve datos en el cliente al capturar (SC-003, FR-004)', () => {
    component.onInput('FOR-1A2B3');
    component.submitManual();
    // verify() lanza si el componente hubiera disparado alguna resolución server-side.
    expect(() => httpMock.verify()).not.toThrow();
  });

  it('rechaza el formato inválido cuando validateFormat está activo', () => {
    fixture.componentRef.setInput('validateFormat', true);
    const codes: string[] = [];
    component.codeCaptured.subscribe((c) => codes.push(c));

    component.onInput('NO-VALIDO');
    component.submitManual();

    expect(codes).toEqual([]);
    expect(component.formatError()).toBeTrue();
  });
});
