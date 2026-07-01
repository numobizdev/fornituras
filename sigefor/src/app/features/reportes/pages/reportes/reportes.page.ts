import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal, WritableSignal } from '@angular/core';
import {
  IonBadge,
  IonButton,
  IonButtons,
  IonChip,
  IonContent,
  IonHeader,
  IonIcon,
  IonInput,
  IonItem,
  IonLabel,
  IonList,
  IonMenuButton,
  IonNote,
  IonText,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  alertCircleOutline,
  archiveOutline,
  checkmarkCircleOutline,
  chevronDownOutline,
  chevronUpOutline,
  closeCircleOutline,
  constructOutline,
  cubeOutline,
  documentTextOutline,
  downloadOutline,
  filterOutline,
  lockClosedOutline,
  peopleOutline,
  personOutline,
} from 'ionicons/icons';
import { firstValueFrom } from 'rxjs';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import {
  ActiveAssignmentFilter,
  ActiveAssignmentRow,
  ReportTotals,
} from '../../data/report.model';
import { ReportsService } from '../../data/reports.service';

interface TotalCard {
  key: keyof ReportTotals;
  label: string;
  icon: string;
  colorVar: string;
}

interface FilterField {
  key: keyof ActiveAssignmentFilter;
  label: string;
}

@Component({
  selector: 'app-reportes',
  templateUrl: './reportes.page.html',
  styleUrls: ['./reportes.page.scss'],
  imports: [
    DatePipe,
    IonHeader,
    IonToolbar,
    IonButtons,
    IonMenuButton,
    IonTitle,
    IonContent,
    IonList,
    IonItem,
    IonLabel,
    IonInput,
    IonButton,
    IonIcon,
    IonBadge,
    IonChip,
    IonNote,
    IonText,
  ],
})
export class ReportesPage {
  private readonly service = inject(ReportsService);
  private readonly toastController = inject(ToastController);

  private static readonly PAGE_SIZE = 20;

  readonly totals = signal<ReportTotals | null>(null);
  readonly rows = signal<ActiveAssignmentRow[]>([]);
  readonly totalsLoading = signal(false);
  readonly isLoading = signal(false);
  readonly exporting = signal(false);
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly piiMasked = signal(false);
  readonly filtersOpen = signal(false);

  readonly filterValues: Record<keyof ActiveAssignmentFilter, WritableSignal<string>> = {
    qr: signal(''),
    nombre: signal(''),
    placa: signal(''),
    municipio: signal(''),
    curp: signal(''),
    rfc: signal(''),
  };

  readonly activeFilterCount = computed(
    () => this.filterFields.filter((f) => this.filterValues[f.key]().trim() !== '').length,
  );

  // Placeholders para el skeleton de carga (índices ficticios).
  readonly skeletonSlots = [0, 1, 2, 3, 4, 5];

  readonly totalCards: TotalCard[] = [
    { key: 'totalFornituras', label: 'Total', icon: 'cube-outline', colorVar: '--status-total' },
    { key: 'disponibles', label: 'Disponibles', icon: 'checkmark-circle-outline', colorVar: '--status-disponible' },
    { key: 'asignadas', label: 'Asignadas', icon: 'person-outline', colorVar: '--status-asignado' },
    { key: 'enMantenimiento', label: 'En mantenimiento', icon: 'construct-outline', colorVar: '--status-mantenimiento' },
    { key: 'conIncidencia', label: 'Con incidencia', icon: 'alert-circle-outline', colorVar: '--status-proximo-vencer' },
    { key: 'baja', label: 'Baja', icon: 'archive-outline', colorVar: '--status-inactivo' },
    { key: 'totalElementos', label: 'Elementos', icon: 'people-outline', colorVar: '--status-asignado' },
  ];

  readonly filterFields: FilterField[] = [
    { key: 'qr', label: 'Código QR' },
    { key: 'nombre', label: 'Nombre' },
    { key: 'placa', label: 'Placa' },
    { key: 'municipio', label: 'Municipio' },
    { key: 'curp', label: 'CURP' },
    { key: 'rfc', label: 'RFC' },
  ];

  constructor() {
    addIcons({
      cubeOutline,
      checkmarkCircleOutline,
      personOutline,
      constructOutline,
      alertCircleOutline,
      archiveOutline,
      peopleOutline,
      downloadOutline,
      lockClosedOutline,
      filterOutline,
      closeCircleOutline,
      documentTextOutline,
      chevronDownOutline,
      chevronUpOutline,
    });
  }

  // Recarga en cada entrada para reflejar los últimos movimientos de inventario.
  ionViewWillEnter(): void {
    void this.loadTotals();
    void this.load();
  }

  totalValue(card: TotalCard): number {
    return this.totals()?.[card.key] ?? 0;
  }

  toggleFilters(): void {
    this.filtersOpen.update((open) => !open);
  }

  setFilter(key: keyof ActiveAssignmentFilter, value: string | null | undefined): void {
    this.filterValues[key].set(value ?? '');
    this.applyFilters();
  }

  clearFilters(): void {
    for (const field of this.filterFields) {
      this.filterValues[field.key].set('');
    }
    this.applyFilters();
  }

  private currentFilter(): ActiveAssignmentFilter {
    const filter: ActiveAssignmentFilter = {};
    for (const field of this.filterFields) {
      const value = this.filterValues[field.key]().trim();
      if (value) {
        filter[field.key] = value;
      }
    }
    return filter;
  }

  private async loadTotals(): Promise<void> {
    this.totalsLoading.set(true);
    try {
      this.totals.set(await firstValueFrom(this.service.getTotals()));
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    } finally {
      this.totalsLoading.set(false);
    }
  }

  async load(): Promise<void> {
    this.isLoading.set(true);
    try {
      const result = await firstValueFrom(
        this.service.getActiveAssignments(this.currentFilter(), this.page(), ReportesPage.PAGE_SIZE),
      );
      this.rows.set(result.content);
      this.totalElements.set(result.totalElements);
      this.totalPages.set(result.totalPages);
      this.piiMasked.set(result.content.some((r) => r.piiMasked));
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

  async exportExcel(): Promise<void> {
    this.exporting.set(true);
    try {
      const blob = await firstValueFrom(this.service.exportActiveAssignments(this.currentFilter()));
      this.triggerDownload(blob, 'asignaciones-activas.xlsx');
      await this.showToast('Exportación generada.', 'success');
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    } finally {
      this.exporting.set(false);
    }
  }

  private triggerDownload(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    URL.revokeObjectURL(url);
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
