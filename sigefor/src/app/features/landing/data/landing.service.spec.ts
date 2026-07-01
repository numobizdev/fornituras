import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/api-response.model';
import { LandingSectionAdmin, LandingSectionPublic } from './landing.model';
import { LandingService } from './landing.service';

/** Verifica que el servicio desenvuelve `ApiResponse<T>` y usa las rutas correctas (T037). */
describe('LandingService', () => {
  let service: LandingService;
  let httpMock: HttpTestingController;
  const base = `${environment.apiUrl}/landing`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [LandingService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(LandingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getPublic returns the unwrapped data array', () => {
    const data: LandingSectionPublic[] = [
      {
        type: 'HERO',
        titulo: 'Hola',
        subtitulo: null,
        cuerpo: null,
        imagenUrl: null,
        ctaLabel: 'Acceder',
        ctaUrl: '/login',
        orden: 0,
        quickLinks: [],
      },
    ];

    let result: LandingSectionPublic[] | undefined;
    service.getPublic().subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${base}/public`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'ok', data } satisfies ApiResponse<LandingSectionPublic[]>);

    expect(result).toEqual(data);
  });

  it('getHome calls /landing/home', () => {
    service.getHome().subscribe();
    const req = httpMock.expectOne(`${base}/home`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'ok', data: [] });
  });

  it('listSections passes the scope as a query param', () => {
    service.listSections('PUBLIC').subscribe();
    const req = httpMock.expectOne((r) => r.url === `${base}/sections`);
    expect(req.request.params.get('scope')).toBe('PUBLIC');
    req.flush({ success: true, message: 'ok', data: [] });
  });

  it('createSection posts and unwraps the created section', () => {
    const created: LandingSectionAdmin = {
      id: 5,
      scope: 'HOME',
      type: 'ANNOUNCEMENT',
      titulo: 'Aviso',
      subtitulo: null,
      cuerpo: 'x',
      imagenUrl: null,
      ctaLabel: null,
      ctaUrl: null,
      orden: 1,
      active: true,
      quickLinks: [],
      createdAt: '2026-07-01T00:00:00',
      updatedAt: '2026-07-01T00:00:00',
    };

    let result: LandingSectionAdmin | undefined;
    service
      .createSection({ scope: 'HOME', type: 'ANNOUNCEMENT', titulo: 'Aviso', cuerpo: 'x', orden: 1 })
      .subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${base}/sections`);
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, message: 'ok', data: created });

    expect(result).toEqual(created);
  });

  it('reorder patches /sections/reorder', () => {
    service.reorder({ items: [{ id: 1, orden: 0 }] }).subscribe();
    const req = httpMock.expectOne(`${base}/sections/reorder`);
    expect(req.request.method).toBe('PATCH');
    req.flush({ success: true, message: 'ok', data: [] });
  });
});
