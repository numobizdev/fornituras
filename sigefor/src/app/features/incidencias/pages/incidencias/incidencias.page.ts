import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  AlertController,
  IonBadge,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonListHeader,
  IonMenuButton,
  IonNote,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { addOutline, warningOutline } from 'ionicons/icons';
import { AuthService } from '../../../../core/services/auth.service';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { IncidentsService } from '../../data/incidents.service';
import {
  AlertItem,
  EXPIRY_ALERTS,
  INCIDENT_STATUSES,
  INCIDENT_TYPES,
  IncidentStatus,
  IncidentSummary,
  IncidentType,
} from '../../data/incident.model';

@Component({
  selector: 'app-incidencias',
  templateUrl: './incidencias.page.html',
  styleUrls: ['./incidencias.page.scss'],
  imports: [
    DatePipe,
    IonHeader,
    IonToolbar,
    IonButtons,
    IonMenuButton,
    IonTitle,
    IonContent,
    IonList,
    IonListHeader,
    IonItem,
    IonLabel,
    IonNote,
    IonSelect,
    IonSelectOption,
    IonBadge,
    IonButton,
    IonIcon,
    IonSpinner,
  ],
})
export class IncidenciasPage implements OnInit {
  private readonly service = inject(IncidentsService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);
  private readonly alertController = inject(AlertController);

  private static readonly PAGE_SIZE = 20;

  readonly incidents = signal<IncidentSummary[]>([]);
  readonly alerts = signal<AlertItem[]>([]);
  readonly isLoading = signal(false);
  readonly alertsLoading = signal(false);
  readonly page = signal(0);
  readonly totalPages = signal(0);

  readonly filterEstado = signal<IncidentStatus | ''>('');

  readonly statuses = INCIDENT_STATUSES;
  readonly types = INCIDENT_TYPES;
  readonly expiryAlerts = EXPIRY_ALERTS;
  readonly canWrite = this.auth.hasRole('ADMIN') || this.auth.hasRole('CAPTURISTA');

  constructor() {
    addIcons({ addOutline, warningOutline });
  }

  ngOnInit(): void {
    void this.loadAlerts();
    void this.load();
  }

  async load(): Promise<void> {
    this.isLoading.set(true);
    try {
      const result = await firstValueFrom(
        this.service.list({
          page: this.page(),
          size: IncidenciasPage.PAGE_SIZE,
          estado: this.filterEstado() || undefined,
        }),
      );
      this.incidents.set(result.content);
      this.totalPages.set(result.totalPages);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    } finally {
      this.isLoading.set(false);
    }
  }

  private async loadAlerts(): Promise<void> {
    this.alertsLoading.set(true);
    try {
      this.alerts.set(await firstValueFrom(this.service.vigenciaAlerts()));
    } catch {
      // Las alertas son complementarias; si fallan, el listado de incidencias sigue operativo.
    } finally {
      this.alertsLoading.set(false);
    }
  }

  applyFilters(): void {
    this.page.set(0);
    void this.load();
  }

  typeLabel(tipo: IncidentType): string {
    return this.types[tipo].label;
  }

  statusLabel(estado: IncidentStatus): string {
    return this.statuses[estado].label;
  }

  statusColor(estado: IncidentStatus): string {
    return this.statuses[estado].color;
  }

  alertLabel(alert: AlertItem): string {
    return this.expiryAlerts[alert.expiryStatus as 'PROXIMA_A_VENCER' | 'CADUCADA'].label;
  }

  alertColor(alert: AlertItem): string {
    return this.expiryAlerts[alert.expiryStatus as 'PROXIMA_A_VENCER' | 'CADUCADA'].color;
  }

  newIncident(): void {
    void this.router.navigate(['/incidencias/nueva']);
  }

  async openUpdate(incident: IncidentSummary): Promise<void> {
    const alert = await this.alertController.create({
      header: `Actualizar incidencia #${incident.id}`,
      subHeader: incident.equipmentCodigo ?? undefined,
      inputs: (Object.keys(this.statuses) as IncidentStatus[]).map((estado) => ({
        type: 'radio',
        label: this.statuses[estado].label,
        value: estado,
        checked: estado === incident.estado,
      })),
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        {
          text: 'Guardar',
          handler: (estado: IncidentStatus) => {
            if (estado && estado !== incident.estado) {
              void this.applyUpdate(incident.id, estado);
            }
          },
        },
      ],
    });
    await alert.present();
  }

  private async applyUpdate(id: number, estado: IncidentStatus): Promise<void> {
    try {
      await firstValueFrom(this.service.update(id, { estado }));
      await this.showToast('Incidencia actualizada.', 'success');
      await this.load();
      await this.loadAlerts();
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    }
  }

  prev(): void {
    if (this.page() > 0) {
      this.page.update((p) => p - 1);
      void this.load();
    }
  }

  next(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.page.update((p) => p + 1);
      void this.load();
    }
  }

  private async showToast(message: string, color: string): Promise<void> {
    const toast = await this.toastController.create({
      message,
      duration: 3500,
      color,
      position: 'top',
    });
    await toast.present();
  }
}
