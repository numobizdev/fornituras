import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonInput,
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
import { addOutline } from 'ionicons/icons';
import { AuthService } from '../../../../core/services/auth.service';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { EquipmentTypesService } from '../../../tipos/data/equipment-types.service';
import { EquipmentTypeSummary } from '../../../tipos/data/equipment-type.model';
import { DecommissionsService } from '../../data/decommissions.service';
import { DecommissionReasonItem, DecommissionSummary } from '../../data/decommission.model';

@Component({
  selector: 'app-bajas',
  templateUrl: './bajas.page.html',
  styleUrls: ['./bajas.page.scss'],
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
    IonInput,
    IonSelect,
    IonSelectOption,
    IonButton,
    IonIcon,
    IonSpinner,
  ],
})
export class BajasPage implements OnInit {
  private readonly service = inject(DecommissionsService);
  private readonly typesService = inject(EquipmentTypesService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);

  private static readonly PAGE_SIZE = 20;

  readonly decommissions = signal<DecommissionSummary[]>([]);
  readonly reasons = signal<DecommissionReasonItem[]>([]);
  readonly types = signal<EquipmentTypeSummary[]>([]);
  readonly isLoading = signal(false);
  readonly page = signal(0);
  readonly totalPages = signal(0);

  readonly filterFechaDesde = signal<string>('');
  readonly filterFechaHasta = signal<string>('');
  readonly filterTipo = signal<number | ''>('');
  readonly filterMotivo = signal<number | ''>('');

  readonly canWrite = this.auth.hasRole('ADMIN');

  constructor() {
    addIcons({ addOutline });
  }

  ngOnInit(): void {
    void this.loadCatalogs();
    void this.load();
  }

  private async loadCatalogs(): Promise<void> {
    try {
      this.reasons.set(await firstValueFrom(this.service.reasons()));
    } catch {
      // El filtro de motivo es opcional; si falla la carga, la lista sigue operativa.
    }
    try {
      const page = await firstValueFrom(this.typesService.list({ active: true, size: 100 }));
      this.types.set(page.content);
    } catch {
      // El filtro de tipo es opcional.
    }
  }

  async load(): Promise<void> {
    this.isLoading.set(true);
    try {
      const result = await firstValueFrom(
        this.service.list({
          page: this.page(),
          size: BajasPage.PAGE_SIZE,
          fechaDesde: this.filterFechaDesde() || undefined,
          fechaHasta: this.filterFechaHasta() || undefined,
          tipoId: this.filterTipo() || undefined,
          motivoId: this.filterMotivo() || undefined,
        }),
      );
      this.decommissions.set(result.content);
      this.totalPages.set(result.totalPages);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    } finally {
      this.isLoading.set(false);
    }
  }

  applyFilters(): void {
    this.page.set(0);
    void this.load();
  }

  newBaja(): void {
    void this.router.navigate(['/bajas/nueva']);
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
