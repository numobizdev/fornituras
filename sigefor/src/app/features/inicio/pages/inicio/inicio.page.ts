import { Component, inject, OnDestroy, signal } from '@angular/core';
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonMenuButton,
  IonRefresher,
  IonRefresherContent,
  IonTitle,
  IonToolbar,
  RefresherCustomEvent,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  alertCircleOutline,
  checkmarkCircleOutline,
  cloudOfflineOutline,
  constructOutline,
  cubeOutline,
  helpCircleOutline,
  personOutline,
  refreshOutline,
  timeOutline,
} from 'ionicons/icons';
import type { DriveStep } from 'driver.js';
import { firstValueFrom } from 'rxjs';
import { LandingSectionsComponent } from '../../../landing/components/landing-sections/landing-sections.component';
import { LandingSectionPublic } from '../../../landing/data/landing.model';
import { LandingService } from '../../../landing/data/landing.service';
import { TourService } from '../../../../core/tour/tour.service';
import { DashboardService } from '../../data/dashboard.service';
import {
  DASHBOARD_INDICATORS,
  DashboardIndicator,
  DashboardSummary,
} from '../../data/dashboard.model';

/**
 * Inicio (`/inicio`): muestra el contenido de bienvenida configurable (secciones HOME activas, US1) y,
 * debajo, los indicadores del inventario. La primera visita lanza el recorrido guiado (US4), relanzable
 * con "Ver tutorial". Las secciones se renderizan con interpolación (sin `innerHTML`).
 */
@Component({
  selector: 'app-inicio',
  templateUrl: './inicio.page.html',
  styleUrls: ['./inicio.page.scss'],
  imports: [
    IonHeader,
    IonToolbar,
    IonButtons,
    IonMenuButton,
    IonTitle,
    IonContent,
    IonRefresher,
    IonRefresherContent,
    IonIcon,
    IonButton,
    LandingSectionsComponent,
  ],
})
export class InicioPage implements OnDestroy {
  private readonly service = inject(DashboardService);
  private readonly landing = inject(LandingService);
  private readonly tour = inject(TourService);

  readonly summary = signal<DashboardSummary | null>(null);
  readonly homeSections = signal<LandingSectionPublic[]>([]);
  readonly isLoading = signal(false);
  readonly hasError = signal(false);

  readonly indicators = DASHBOARD_INDICATORS;
  readonly skeletonSlots = DASHBOARD_INDICATORS.map((_, i) => i);

  constructor() {
    addIcons({
      cubeOutline,
      checkmarkCircleOutline,
      personOutline,
      timeOutline,
      alertCircleOutline,
      constructOutline,
      cloudOfflineOutline,
      refreshOutline,
      helpCircleOutline,
    });
  }

  // Recarga en cada entrada para reflejar indicadores y contenido de inicio actualizados.
  ionViewWillEnter(): void {
    void this.load();
  }

  ngOnDestroy(): void {
    this.tour.destroy();
  }

  async load(): Promise<void> {
    this.isLoading.set(true);
    this.hasError.set(false);
    try {
      const [summary, sections] = await Promise.all([
        firstValueFrom(this.service.getSummary()),
        this.loadHomeSections(),
      ]);
      this.summary.set(summary);
      this.homeSections.set(sections);
    } catch {
      this.hasError.set(true);
    } finally {
      this.isLoading.set(false);
      this.scheduleFirstVisitTour();
    }
  }

  private async loadHomeSections(): Promise<LandingSectionPublic[]> {
    try {
      return await firstValueFrom(this.landing.getHome());
    } catch {
      // El inicio sigue siendo útil con los indicadores aunque el contenido no cargue.
      return [];
    }
  }

  async refresh(event: RefresherCustomEvent): Promise<void> {
    await this.load();
    await event.target.complete();
  }

  value(indicator: DashboardIndicator): number {
    return this.summary()?.[indicator.key] ?? 0;
  }

  /** Relanza el recorrido guiado a demanda ("Ver tutorial"). */
  replayTour(): void {
    this.tour.startHomeTour(this.buildSteps());
  }

  private scheduleFirstVisitTour(): void {
    // Espera un ciclo para que el DOM de las secciones exista antes de resaltar.
    setTimeout(() => {
      void this.tour.autoStartHomeTour(this.buildSteps());
    }, 400);
  }

  /** Construye los pasos del recorrido solo con los elementos presentes en la página. */
  private buildSteps(): DriveStep[] {
    const steps: DriveStep[] = [];
    if (document.querySelector('#landing-hero')) {
      steps.push({
        element: '#landing-hero',
        popover: { title: 'Bienvenida', description: 'Aquí verás los avisos y el encabezado institucional.' },
      });
    }
    if (document.querySelector('#landing-quicklinks')) {
      steps.push({
        element: '#landing-quicklinks',
        popover: { title: 'Accesos rápidos', description: 'Entra directo a las funciones que más usas.' },
      });
    }
    if (document.querySelector('#inicio-menu-button')) {
      steps.push({
        element: '#inicio-menu-button',
        popover: { title: 'Menú', description: 'Abre el menú para navegar por todo el sistema.' },
      });
    }
    return steps;
  }
}
