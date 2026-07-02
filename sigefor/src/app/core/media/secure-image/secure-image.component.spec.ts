import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { MediaService } from '../media.service';
import { SecureImageComponent } from './secure-image.component';

describe('SecureImageComponent', () => {
  let fixture: ComponentFixture<SecureImageComponent>;
  let mediaSpy: jasmine.SpyObj<MediaService>;

  beforeEach(async () => {
    mediaSpy = jasmine.createSpyObj<MediaService>('MediaService', ['download', 'resolveInternalId']);

    await TestBed.configureTestingModule({
      imports: [SecureImageComponent],
      providers: [{ provide: MediaService, useValue: mediaSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(SecureImageComponent);
  });

  it('downloads an internal reference and renders an <img> with an object URL', () => {
    mediaSpy.resolveInternalId.and.returnValue('id-1');
    mediaSpy.download.and.returnValue(of(new Blob(['img'], { type: 'image/jpeg' })));

    fixture.componentRef.setInput('reference', '/api/v1/media/id-1');
    fixture.detectChanges();

    expect(mediaSpy.download).toHaveBeenCalledWith('id-1');
    const img: HTMLImageElement | null = fixture.nativeElement.querySelector('img.secure-image');
    expect(img).toBeTruthy();
    expect(img!.src).toContain('blob:');
  });

  it('uses a previous external URL directly without downloading (FR-013)', () => {
    mediaSpy.resolveInternalId.and.returnValue(null);

    fixture.componentRef.setInput('reference', 'https://cdn.example.com/foto.jpg');
    fixture.detectChanges();

    expect(mediaSpy.download).not.toHaveBeenCalled();
    const img: HTMLImageElement | null = fixture.nativeElement.querySelector('img.secure-image');
    expect(img!.src).toBe('https://cdn.example.com/foto.jpg');
  });

  it('shows a masked state when the server denies PII access (403)', () => {
    mediaSpy.resolveInternalId.and.returnValue('pii-1');
    mediaSpy.download.and.returnValue(throwError(() => ({ status: 403 })));

    fixture.componentRef.setInput('reference', '/api/v1/media/pii-1');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.secure-image--masked')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('img')).toBeNull();
  });
});
