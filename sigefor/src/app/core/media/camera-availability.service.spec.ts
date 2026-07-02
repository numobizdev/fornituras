import { CameraAvailabilityService } from './camera-availability.service';

describe('CameraAvailabilityService', () => {
  let service: CameraAvailabilityService;

  beforeEach(() => {
    service = new CameraAvailabilityService();
    Object.defineProperty(navigator, 'mediaDevices', {
      configurable: true,
      value: { getUserMedia: jasmine.createSpy('getUserMedia') },
    });
  });

  it('returns unavailable when getUserMedia is missing', async () => {
    Object.defineProperty(navigator, 'mediaDevices', { configurable: true, value: undefined });
    await expectAsync(service.checkAvailability()).toBeResolvedTo('unavailable');
  });

  it('returns a hint when camera is unavailable', () => {
    expect(service.messageFor('unavailable')).toContain('Elegir archivo');
  });

  it('returns a hint when permission is denied', () => {
    expect(service.messageFor('denied')).toContain('Permiso de cámara denegado');
  });
});
