import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  forwardRef,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { IonButton, IonIcon, IonNote, IonSpinner } from '@ionic/angular/standalone';
import { Camera, CameraResultType, CameraSource } from '@capacitor/camera';
import { addIcons } from 'ionicons';
import { cameraOutline, cloudUploadOutline, imagesOutline, trashOutline } from 'ionicons/icons';
import { firstValueFrom } from 'rxjs';
import { MediaContext } from '../media.model';
import { MediaService } from '../media.service';
import { SecureImageComponent } from '../secure-image/secure-image.component';

/**
 * Selector de foto reutilizable (017): permite **tomar foto** con la cámara (`@capacitor/camera`) o
 * **elegir un archivo**, con **vista previa** y quitar/reemplazar. Si no hay cámara o se deniega el
 * permiso, no bloquea: se puede subir un archivo (fallback web, FR-004).
 *
 * <p>Es un `ControlValueAccessor`: se enlaza con `formControlName`/`ngModel` y su valor es la
 * **referencia interna** (`fotoUrl`) que devuelve el backend tras subir. Sube en cuanto se elige la
 * imagen (la limpieza de huérfanas si no se guarda la ficha corresponde a FR-016). Cuando el control
 * está deshabilitado (gating de PII o rol), se oculta la captura y solo se muestra la foto existente.
 */
@Component({
  selector: 'app-photo-picker',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonButton, IonIcon, IonNote, IonSpinner, SecureImageComponent],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PhotoPickerComponent),
      multi: true,
    },
  ],
  templateUrl: './photo-picker.component.html',
  styleUrls: ['./photo-picker.component.scss'],
})
export class PhotoPickerComponent implements ControlValueAccessor {
  private readonly media = inject(MediaService);
  private readonly destroyRef = inject(DestroyRef);

  /** Contexto de la foto: fija `is_pii` y las reglas de RBAC/gating del servidor. */
  readonly context = input.required<MediaContext>();
  /** Motivo por el que la captura está deshabilitada (p. ej. gating de PII), a mostrar al usuario. */
  readonly disabledReason = input<string | null>(null);

  private readonly fileInput = viewChild<ElementRef<HTMLInputElement>>('fileInput');

  protected readonly value = signal<string | null>(null);
  protected readonly localPreview = signal<string | null>(null);
  protected readonly uploading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly disabled = signal(false);

  private onChange: (value: string | null) => void = () => {};
  private onTouched: () => void = () => {};

  constructor() {
    addIcons({ cameraOutline, imagesOutline, cloudUploadOutline, trashOutline });
    this.destroyRef.onDestroy(() => this.revokeLocalPreview());
  }

  writeValue(value: string | null): void {
    this.value.set(value ?? null);
    this.revokeLocalPreview();
  }

  registerOnChange(fn: (value: string | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }

  protected hasPhoto(): boolean {
    return this.localPreview() !== null || this.value() !== null;
  }

  protected async takePhoto(): Promise<void> {
    if (this.disabled() || this.uploading()) {
      return;
    }
    try {
      const photo = await Camera.getPhoto({
        quality: 80,
        resultType: CameraResultType.Uri,
        source: CameraSource.Camera,
      });
      if (photo.webPath) {
        const blob = await fetch(photo.webPath).then((response) => response.blob());
        await this.uploadBlob(blob, `photo.${photo.format ?? 'jpg'}`);
      }
    } catch {
      // Sin cámara o permiso denegado: no bloquea, el usuario puede elegir un archivo (FR-004).
      this.error.set('No se pudo usar la cámara. Puedes elegir un archivo.');
    }
  }

  protected chooseFile(): void {
    if (this.disabled() || this.uploading()) {
      return;
    }
    this.fileInput()?.nativeElement.click();
  }

  protected async onFileSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (file) {
      await this.uploadBlob(file, file.name);
    }
  }

  protected remove(): void {
    this.revokeLocalPreview();
    this.value.set(null);
    this.error.set(null);
    this.onChange(null);
    this.onTouched();
  }

  private async uploadBlob(blob: Blob, fileName: string): Promise<void> {
    this.error.set(null);
    this.uploading.set(true);
    this.setLocalPreview(blob);
    try {
      const result = await firstValueFrom(this.media.upload(blob, this.context(), fileName));
      this.value.set(result.url);
      this.onChange(result.url);
      this.onTouched();
    } catch (error) {
      this.revokeLocalPreview();
      this.error.set(this.messageFor(error));
    } finally {
      this.uploading.set(false);
    }
  }

  private setLocalPreview(blob: Blob): void {
    this.revokeLocalPreview();
    this.localPreview.set(URL.createObjectURL(blob));
  }

  private revokeLocalPreview(): void {
    const current = this.localPreview();
    if (current) {
      URL.revokeObjectURL(current);
      this.localPreview.set(null);
    }
  }

  private messageFor(error: unknown): string {
    const status = (error as { status?: number })?.status;
    if (status === 403) {
      return 'No tienes autorización para subir esta foto.';
    }
    if (status === 413) {
      return 'La imagen supera el tamaño máximo permitido.';
    }
    if (status === 400 || status === 422) {
      return 'El archivo no es una imagen válida (JPEG/PNG/WEBP).';
    }
    return 'No se pudo subir la foto. Inténtalo de nuevo.';
  }
}
