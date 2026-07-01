import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import {
  IonBadge,
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
  IonSpinner,
  IonText,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { downloadOutline, lockClosedOutline } from 'ionicons/icons';
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
  colorVar: string;
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
    IonListHeader,
    IonItem,
    IonLabel,
    IonInput,
    IonButton,
    IonIcon,
    IonBadge,
    IonNote,
    IonSpinner,
    IonText,
  ],
})
export class ReportesPage implements OnInit {
  private readonly service = inject(ReportsService);
  private readonly toastController = inject(ToastController);

  private static readonly PAGE_SIZE = 20;

  readonly totals = signal<ReportTotals | null>(null);
  readonly rows = signal<ActiveAssignmentRow[]>([]);
  readonly totalsLoading = signal(false);
  readonly isLoading = signal(false);
  readonly exporting = signal(false);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly piiMasked = signal(false);

  readonly qr = signal('');
  readonly nombre = signal('');
  readonly placa = signal('');
  readonly municipio = signal('');
  readonly curp = signal('');
  readonly rfc = signal('');

  readonly totalCards: TotalCard[] = [
    { key: 'totalFornituras', label: 'Total', colorVar: '--status-total' },
    { key: 'disponibles', label: 'Disponibles', colorVar: '--status-disponible' },
    { key: 'asignadas', label: 'Asignadas', colorVar: '--status-asignado' },
    { key: 'enMantenimiento', label: 'En mantenimiento', colorVar: '--status-mantenimiento' },
    { key: 'conIncidencia', label: 'Con incidencia', colorVar: '--status-proximo-vencer' },
    { key: 'baja', label: 'Baja', colorVar: '--status-inactivo' },
    { key: 'totalElementos', label: 'Elementos', colorVar: '--status-asignado' },
  ];

  constructor() {
    addIcons({ downloadOutline, lockClosedOutline });
  }

  ngOnInit(): void {
    void this.loadTotals();
    void this.load();
  }

  totalValue(card: TotalCard): number {
    return this.totals()?.[card.key] ?? 0;
  }

  private currentFilter(): ActiveAssignmentFilter {
    return {
      qr: this.qr() || undefined,
      nombre: this.nombre() || undefined,
      placa: this.placa() || undefined,
      municipio: this.municipio() || undefined,
      curp: this.curp() || undefined,
      rfc: this.rfc() || undefined,
    };
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
