import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal, WritableSignal } from '@angular/core';
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
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  chevronDownOutline,
  chevronUpOutline,
  closeCircleOutline,
  filterOutline,
  shieldCheckmarkOutline,
} from 'ionicons/icons';
import { firstValueFrom } from 'rxjs';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { AuditFilter, AuditLogSummary } from '../../data/audit.model';
import { AuditService } from '../../data/audit.service';

interface FilterField {
  key: keyof AuditFilter;
  label: string;
  type: 'text' | 'datetime-local';
}

@Component({
  selector: 'app-auditoria',
  templateUrl: './auditoria.page.html',
  styleUrls: ['./auditoria.page.scss'],
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
  ],
})
export class AuditoriaPage implements OnInit {
  private readonly service = inject(AuditService);
  private readonly toastController = inject(ToastController);

  private static readonly PAGE_SIZE = 20;

  readonly rows = signal<AuditLogSummary[]>([]);
  readonly isLoading = signal(false);
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly filtersOpen = signal(false);

  readonly skeletonSlots = [0, 1, 2, 3, 4, 5, 6, 7];

  readonly filterFields: FilterField[] = [
    { key: 'actor', label: 'Usuario (actor)', type: 'text' },
    { key: 'accion', label: 'Acción', type: 'text' },
    { key: 'entidad', label: 'Entidad', type: 'text' },
    { key: 'desde', label: 'Desde', type: 'datetime-local' },
    { key: 'hasta', label: 'Hasta', type: 'datetime-local' },
  ];

  readonly filterValues: Record<keyof AuditFilter, WritableSignal<string>> = {
    actor: signal(''),
    accion: signal(''),
    entidad: signal(''),
    desde: signal(''),
    hasta: signal(''),
  };

  readonly activeFilterCount = computed(
    () => this.filterFields.filter((f) => this.filterValues[f.key]().trim() !== '').length,
  );

  constructor() {
    addIcons({ filterOutline, closeCircleOutline, chevronDownOutline, chevronUpOutline, shieldCheckmarkOutline });
  }

  ngOnInit(): void {
    void this.load();
  }

  toggleFilters(): void {
    this.filtersOpen.update((open) => !open);
  }

  setFilter(key: keyof AuditFilter, value: string | null | undefined): void {
    this.filterValues[key].set(value ?? '');
    this.applyFilters();
  }

  clearFilters(): void {
    for (const field of this.filterFields) {
      this.filterValues[field.key].set('');
    }
    this.applyFilters();
  }

  private currentFilter(): AuditFilter {
    const filter: AuditFilter = {};
    for (const field of this.filterFields) {
      const value = this.filterValues[field.key]().trim();
      if (value) {
        filter[field.key] = value;
      }
    }
    return filter;
  }

  async load(): Promise<void> {
    this.isLoading.set(true);
    try {
      const result = await firstValueFrom(
        this.service.query(this.currentFilter(), this.page(), AuditoriaPage.PAGE_SIZE),
      );
      this.rows.set(result.content);
      this.totalElements.set(result.totalElements);
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
    const toast = await this.toastController.create({ message, duration: 3500, color, position: 'top' });
    await toast.present();
  }
}
