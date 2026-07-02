import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { IonIcon, IonSpinner } from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { imageOutline, lockClosedOutline } from 'ionicons/icons';
import { MediaService } from '../media.service';

type LoadState = 'idle' | 'loading' | 'loaded' | 'masked' | 'error';

/**
 * Muestra una foto protegida por el backend (017). Como `<img src>` no envía `Authorization`, descarga
 * la imagen vía `HttpClient` (con token, como `blob`) y crea un `objectURL`; lo **revoca** al cambiar
 * la referencia o destruir el componente para no filtrar memoria.
 *
 * <p>Resuelve tanto la referencia interna (`/api/v1/media/<uuid>`) como una URL externa previa
 * (transición, FR-013). Si el servidor responde 403 (foto de elemento sin autorización), muestra un
 * estado **enmascarado** en vez de la imagen (enmascaramiento por defecto de PII).
 */
@Component({
  selector: 'app-secure-image',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonIcon, IonSpinner],
  template: `
    @if (state() === 'loading') {
      <div class="secure-image secure-image--placeholder">
        <ion-spinner name="crescent"></ion-spinner>
      </div>
    } @else if (state() === 'loaded' && displayUrl()) {
      <img class="secure-image" [src]="displayUrl()" [alt]="alt()" />
    } @else if (state() === 'masked') {
      <div class="secure-image secure-image--masked" role="img" [attr.aria-label]="alt()">
        <ion-icon name="lock-closed-outline"></ion-icon>
      </div>
    } @else if (state() === 'error') {
      <div class="secure-image secure-image--placeholder secure-image--error" role="img" [attr.aria-label]="alt()">
        <ion-icon name="image-outline"></ion-icon>
      </div>
    }
  `,
  styles: [
    `
      .secure-image {
        display: block;
        width: 100%;
        height: 100%;
        object-fit: cover;
        border-radius: 8px;
      }
      .secure-image--placeholder,
      .secure-image--masked {
        display: flex;
        align-items: center;
        justify-content: center;
        min-height: 96px;
        background: var(--ion-color-light, #f4f5f8);
        color: var(--ion-color-medium, #92949c);
      }
      .secure-image--masked ion-icon,
      .secure-image--error ion-icon {
        font-size: 2rem;
      }
    `,
  ],
})
export class SecureImageComponent {
  private readonly media = inject(MediaService);
  private readonly destroyRef = inject(DestroyRef);

  /** Referencia guardada en `fotoUrl`: interna (`/api/v1/media/<uuid>`) o URL externa previa. */
  readonly reference = input<string | null>(null);
  readonly alt = input<string>('Foto');

  protected readonly state = signal<LoadState>('idle');
  private readonly objectUrl = signal<string | null>(null);
  private readonly externalUrl = signal<string | null>(null);

  protected readonly displayUrl = computed(() => this.objectUrl() ?? this.externalUrl());

  constructor() {
    addIcons({ lockClosedOutline, imageOutline });
    effect((onCleanup) => {
      // Solo `reference` es dependencia del effect. El resto va en `untracked` porque toca
      // `objectUrl` (leer/escribir): si esa señal fuese dependencia, escribirla al descargar
      // re-dispararía el effect en bucle (nueva descarga sin fin).
      const reference = this.reference();
      untracked(() => {
        this.revokeObjectUrl();
        this.externalUrl.set(null);

        if (!reference) {
          this.state.set('idle');
          return;
        }

        const internalId = this.media.resolveInternalId(reference);
        if (!internalId) {
          // URL externa previa (transición): se carga directamente en el <img>.
          this.externalUrl.set(reference);
          this.state.set('loaded');
          return;
        }

        this.state.set('loading');
        const subscription = this.media.download(internalId).subscribe({
          next: (blob) => {
            this.revokeObjectUrl();
            this.objectUrl.set(URL.createObjectURL(blob));
            this.state.set('loaded');
          },
          error: (error: { status?: number }) => {
            this.state.set(error?.status === 403 ? 'masked' : 'error');
          },
        });
        onCleanup(() => subscription.unsubscribe());
      });
    });

    this.destroyRef.onDestroy(() => this.revokeObjectUrl());
  }

  private revokeObjectUrl(): void {
    const current = this.objectUrl();
    if (current) {
      URL.revokeObjectURL(current);
      this.objectUrl.set(null);
    }
  }
}
