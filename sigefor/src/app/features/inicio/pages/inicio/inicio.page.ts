import { Component, inject, OnInit, signal } from '@angular/core';
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
  personOutline,
  refreshOutline,
  timeOutline,
} from 'ionicons/icons';
import { firstValueFrom } from 'rxjs';
import { DashboardService } from '../../data/dashboard.service';
import {
  DASHBOARD_INDICATORS,
  DashboardIndicator,
  DashboardSummary,
} from '../../data/dashboard.model';

/**
 * Tablero de control (`/inicio`): al entrar muestra los indicadores clave del inventario con su color
 * semántico institucional. Los contadores llegan agregados del servidor (una sola llamada, sin PII).
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
  ],
})
export class InicioPage implements OnInit {
  private readonly service = inject(DashboardService);

  readonly summary = signal<DashboardSummary | null>(null);
  readonly isLoading = signal(false);
  readonly hasError = signal(false);

  readonly indicators = DASHBOARD_INDICATORS;
  // Placeholders para el skeleton de carga (uno por indicador).
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
    });
  }

  ngOnInit(): void {
    void this.load();
  }

  async load(): Promise<void> {
    this.isLoading.set(true);
    this.hasError.set(false);
    try {
      this.summary.set(await firstValueFrom(this.service.getSummary()));
    } catch {
      this.hasError.set(true);
    } finally {
      this.isLoading.set(false);
    }
  }

  async refresh(event: RefresherCustomEvent): Promise<void> {
    await this.load();
    await event.target.complete();
  }

  value(indicator: DashboardIndicator): number {
    return this.summary()?.[indicator.key] ?? 0;
  }
}
