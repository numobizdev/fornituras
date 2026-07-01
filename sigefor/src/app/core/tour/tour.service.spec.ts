import { TestBed } from '@angular/core/testing';
import { Preferences } from '@capacitor/preferences';
import { STORAGE_KEYS } from '../constants/storage-keys';
import { TourService } from './tour.service';

/**
 * El recorrido respeta el flag de primera vez y limpia el driver al destruirse (T032). Usa la
 * implementación web real de Preferences (localStorage): el proxy de Capacitor no es "spy-able".
 */
describe('TourService', () => {
  let service: TourService;

  beforeEach(async () => {
    TestBed.configureTestingModule({ providers: [TourService] });
    service = TestBed.inject(TourService);
    await Preferences.remove({ key: STORAGE_KEYS.tourHomeDone });
  });

  afterEach(async () => {
    service.destroy();
    await Preferences.remove({ key: STORAGE_KEYS.tourHomeDone });
  });

  it('hasSeenHomeTour reflects the stored flag', async () => {
    expect(await service.hasSeenHomeTour()).toBeFalse();
    await Preferences.set({ key: STORAGE_KEYS.tourHomeDone, value: 'true' });
    expect(await service.hasSeenHomeTour()).toBeTrue();
  });

  it('autoStartHomeTour marks the tour as seen on first visit', async () => {
    await service.autoStartHomeTour([]);
    expect(await service.hasSeenHomeTour()).toBeTrue();
  });

  it('autoStartHomeTour does nothing when already seen', async () => {
    await Preferences.set({ key: STORAGE_KEYS.tourHomeDone, value: 'true' });
    const anchor = document.createElement('div');
    anchor.id = 'seen-anchor';
    document.body.appendChild(anchor);

    await service.autoStartHomeTour([
      { element: '#seen-anchor', popover: { title: 'x', description: 'y' } },
    ]);

    expect(document.querySelector('.driver-popover')).toBeNull();
    anchor.remove();
  });

  it('startHomeTour renders a popover and destroy cleans it up', () => {
    const anchor = document.createElement('div');
    anchor.id = 'tour-anchor';
    document.body.appendChild(anchor);

    service.startHomeTour([
      { element: '#tour-anchor', popover: { title: 'Paso', description: 'Descripción' } },
    ]);
    expect(document.querySelector('.driver-popover')).not.toBeNull();

    service.destroy();
    expect(document.querySelector('.driver-popover')).toBeNull();

    anchor.remove();
  });
});
