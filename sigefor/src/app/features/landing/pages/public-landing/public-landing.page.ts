import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonButton, IonContent, IonIcon, IonSpinner } from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  lockClosedOutline,
  logInOutline,
  qrCodeOutline,
  shieldCheckmarkOutline,
} from 'ionicons/icons';
import { firstValueFrom } from 'rxjs';
import { LandingSectionsComponent } from '../../components/landing-sections/landing-sections.component';
import { LandingSectionPublic } from '../../data/landing.model';
import { LandingService } from '../../data/landing.service';

interface InstitutionalFeature {
  icon: string;
  title: string;
  description: string;
}

/**
 * Landing pública pre-login (US3): primera pantalla institucional para visitantes sin sesión. Presenta
 * un hero de marca (GOBMX), una franja de valores institucionales y el contenido PUBLIC configurable,
 * con un botón "Acceder" al login. No monta el shell autenticado (FR-015/016). La respuesta no tiene PII;
 * todo el contenido se muestra por interpolación (sin `innerHTML`) para evitar XSS (ADR 0015).
 */
@Component({
  selector: 'app-public-landing',
  templateUrl: './public-landing.page.html',
  styleUrls: ['./public-landing.page.scss'],
  imports: [RouterLink, IonContent, IonButton, IonIcon, IonSpinner, LandingSectionsComponent],
})
export class PublicLandingPage implements OnInit {
  private readonly service = inject(LandingService);

  readonly sections = signal<LandingSectionPublic[]>([]);
  readonly isLoading = signal(false);
  readonly hasError = signal(false);

  /** El primer HERO configurable alimenta el encabezado; el resto se renderiza como contenido. */
  readonly heroSection = computed(() => this.sections().find((s) => s.type === 'HERO') ?? null);
  readonly restSections = computed(() => {
    const hero = this.heroSection();
    return this.sections().filter((s) => s !== hero);
  });

  readonly year = new Date().getFullYear();

  readonly features: InstitutionalFeature[] = [
    {
      icon: 'shield-checkmark-outline',
      title: 'Inventario centralizado',
      description: 'Control total del equipo de blindaje, su ubicación y su estado operativo.',
    },
    {
      icon: 'qr-code-outline',
      title: 'Trazabilidad por QR',
      description: 'Cada equipo se identifica por un código único, opaco y verificable.',
    },
    {
      icon: 'lock-closed-outline',
      title: 'Datos protegidos',
      description: 'Información sensible cifrada y con acceso autorizado y auditado.',
    },
  ];

  constructor() {
    addIcons({ shieldCheckmarkOutline, qrCodeOutline, lockClosedOutline, logInOutline });
  }

  ngOnInit(): void {
    void this.load();
  }

  async load(): Promise<void> {
    this.isLoading.set(true);
    this.hasError.set(false);
    try {
      this.sections.set(await firstValueFrom(this.service.getPublic()));
    } catch {
      this.hasError.set(true);
    } finally {
      this.isLoading.set(false);
    }
  }

  heroTitle(): string {
    return this.heroSection()?.titulo || 'Sistema de Gestión de Blindajes';
  }

  heroSubtitle(): string {
    return this.heroSection()?.subtitulo || 'Control institucional de blindajes y dotación de seguridad';
  }
}
