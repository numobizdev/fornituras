import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { MediaUploadResponse } from './media.model';
import { MediaService } from './media.service';

describe('MediaService', () => {
  let service: MediaService;
  let httpMock: HttpTestingController;
  const base = `${environment.apiUrl}/media`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [MediaService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MediaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('uploads as multipart with the image and context and unwraps the reference', () => {
    const data: MediaUploadResponse = {
      id: 'a1b2c3',
      url: '/api/v1/media/a1b2c3',
      contentType: 'image/jpeg',
    };
    const blob = new Blob(['x'], { type: 'image/jpeg' });

    let result: MediaUploadResponse | undefined;
    service.upload(blob, 'equipment', 'photo.jpg').subscribe((r) => (result = r));

    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    const form = req.request.body as FormData;
    expect(form.get('context')).toBe('equipment');
    expect(form.get('image')).toBeTruthy();
    req.flush({ success: true, message: 'ok', data } satisfies ApiResponse<MediaUploadResponse>);

    expect(result).toEqual(data);
  });

  it('downloads the image as a blob', () => {
    const blob = new Blob(['img'], { type: 'image/png' });
    let result: Blob | undefined;
    service.download('id-1').subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${base}/id-1`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(blob);

    expect(result).toBe(blob);
  });

  it('deletes a media asset', () => {
    service.delete('id-9').subscribe();
    const req = httpMock.expectOne(`${base}/id-9`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  describe('resolveInternalId', () => {
    it('extracts the id from an internal reference', () => {
      expect(service.resolveInternalId('/api/v1/media/123e4567-e89b-12d3-a456-426614174000')).toBe(
        '123e4567-e89b-12d3-a456-426614174000',
      );
    });

    it('accepts a bare UUID reference', () => {
      expect(service.resolveInternalId('123e4567-e89b-12d3-a456-426614174000')).toBe(
        '123e4567-e89b-12d3-a456-426614174000',
      );
    });

    it('returns null for a previous external URL (transition, FR-013)', () => {
      expect(service.resolveInternalId('https://cdn.example.com/foto.jpg')).toBeNull();
    });

    it('returns null for empty references', () => {
      expect(service.resolveInternalId(null)).toBeNull();
      expect(service.resolveInternalId('')).toBeNull();
    });
  });
});
