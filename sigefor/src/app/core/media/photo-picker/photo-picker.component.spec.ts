import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CameraAvailabilityService } from '../camera-availability.service';
import { MediaUploadResponse } from '../media.model';
import { MediaService } from '../media.service';
import { PhotoPickerComponent } from './photo-picker.component';

describe('PhotoPickerComponent', () => {
  let fixture: ComponentFixture<PhotoPickerComponent>;
  let component: PhotoPickerComponent;
  let mediaSpy: jasmine.SpyObj<MediaService>;
  let cameraSpy: jasmine.SpyObj<CameraAvailabilityService>;

  beforeEach(async () => {
    mediaSpy = jasmine.createSpyObj<MediaService>('MediaService', [
      'upload',
      'download',
      'resolveInternalId',
      'delete',
    ]);
    mediaSpy.resolveInternalId.and.returnValue(null);

    cameraSpy = jasmine.createSpyObj<CameraAvailabilityService>('CameraAvailabilityService', [
      'checkAvailability',
      'messageFor',
      'isHardwareAvailable',
    ]);
    cameraSpy.checkAvailability.and.resolveTo('available');
    cameraSpy.messageFor.and.returnValue(null);

    await TestBed.configureTestingModule({
      imports: [PhotoPickerComponent],
      providers: [
        { provide: MediaService, useValue: mediaSpy },
        { provide: CameraAvailabilityService, useValue: cameraSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PhotoPickerComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('context', 'equipment');
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('uploads the chosen file and emits the returned reference as its value', async () => {
    const response: MediaUploadResponse = {
      id: 'x',
      url: '/api/v1/media/x',
      contentType: 'image/jpeg',
    };
    mediaSpy.upload.and.returnValue(of(response));

    const onChange = jasmine.createSpy('onChange');
    component.registerOnChange(onChange);

    const file = new File(['data'], 'photo.jpg', { type: 'image/jpeg' });
    const event = { target: { files: [file], value: 'photo.jpg' } } as unknown as Event;
    await (component as unknown as { onFileSelected(e: Event): Promise<void> }).onFileSelected(event);

    expect(mediaSpy.upload).toHaveBeenCalledWith(file, 'equipment', 'photo.jpg');
    expect(onChange).toHaveBeenCalledWith('/api/v1/media/x');
    expect((component as unknown as { value(): string | null }).value()).toBe('/api/v1/media/x');
  });

  it('clears the value when the photo is removed', () => {
    const onChange = jasmine.createSpy('onChange');
    component.registerOnChange(onChange);
    component.writeValue('/api/v1/media/old');

    (component as unknown as { remove(): void }).remove();

    expect(onChange).toHaveBeenCalledWith(null);
    expect((component as unknown as { value(): string | null }).value()).toBeNull();
  });

  it('reflects the disabled state from the form control', () => {
    component.setDisabledState(true);
    expect((component as unknown as { disabled(): boolean }).disabled()).toBeTrue();
  });

  it('disables take photo when camera is unavailable', async () => {
    cameraSpy.checkAvailability.and.resolveTo('unavailable');
    cameraSpy.messageFor.and.returnValue('Cámara no disponible en este dispositivo. Usa «Elegir archivo».');

    await (component as unknown as { ngOnInit(): Promise<void> }).ngOnInit();
    fixture.detectChanges();

    expect((component as unknown as { cameraAvailable(): boolean }).cameraAvailable()).toBeFalse();
    expect((component as unknown as { cameraHint(): string | null }).cameraHint()).toContain('Elegir archivo');
  });
});
