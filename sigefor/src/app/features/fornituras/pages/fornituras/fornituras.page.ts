import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  IonBadge,
  IonButton,
  IonButtons,
  IonContent,
  IonFab,
  IonFabButton,
  IonFabList,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonMenuButton,
  IonNote,
  IonSearchbar,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { add, chevronBack, chevronForward, layersOutline } from 'ionicons/icons';
import { ROLE_POLICY } from '../../../../core/security/role-policy';
import { AuthService } from '../../../../core/services/auth.service';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { EquipmentTypesService } from '../../../tipos/data/equipment-types.service';
import { EquipmentTypeSummary } from '../../../tipos/data/equipment-type.model';
import { WarehousesService } from '../../../almacenes/data/warehouses.service';
import { WarehouseSummary } from '../../../almacenes/data/warehouse.model';
import { EquipmentService } from '../../data/equipment.service';
import {
  EQUIPMENT_STATUSES,
  EXPIRY_STATUSES,
  EquipmentStatus,
  EquipmentSummary,
  ExpiryStatus,
} from '../../data/equipment.model';

@Component({
  selector: 'app-fornituras',
  templateUrl: './fornituras.page.html',
  styleUrls: ['./fornituras.page.scss'],
  imports: [
    IonHeader,
    IonToolbar,
    IonButtons,
    IonMenuButton,
    IonTitle,
    IonContent,
    IonSearchbar,
    IonSelect,
    IonSelectOption,
    IonList,
    IonItem,
    IonLabel,
    IonNote,
    IonBadge,
    IonButton,
    IonIcon,
    IonSpinner,
    IonFab,
    IonFabButton,
    IonFabList,
  ],
})
export class ForniturasPage implements OnInit {
  private readonly service = inject(EquipmentService);
  private readonly typesService = inject(EquipmentTypesService);
  private readonly warehousesService = inject(WarehousesService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);

  private static readonly PAGE_SIZE = 20;

  readonly items = signal<EquipmentSummary[]>([]);
  readonly isLoading = signal(false);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  readonly q = signal('');
  readonly status = signal<EquipmentStatus | null>(null);
  readonly equipmentTypeId = signal<number | null>(null);
  readonly warehouseId = signal<number | null>(null);

  readonly types = signal<EquipmentTypeSummary[]>([]);
  readonly warehouses = signal<WarehouseSummary[]>([]);

  readonly statuses = EQUIPMENT_STATUSES;
  readonly canWrite = computed(() => this.auth.hasAnyRole(ROLE_POLICY.WRITE_INVENTORY));

  constructor() {
    addIcons({ add, layersOutline, chevronBack, chevronForward });
  }

  ngOnInit(): void {
    void this.loadCatalogs();
  }

  // El listado se refresca en cada entrada (Ionic no re-ejecuta ngOnInit al volver del formulario).
  ionViewWillEnter(): void {
    void this.load();
  }

  private async loadCatalogs(): Promise<void> {
    try {
      const [types, warehouses] = await Promise.all([
        firstValueFrom(this.typesService.list({ active: true, size: 100 })),
        firstValueFrom(this.warehousesService.list({ active: true, size: 100 })),
      ]);
      this.types.set(types.content);
      this.warehouses.set(warehouses.content);
    } catch {
      // Los selectores de filtro quedan vacíos; el listado sigue funcionando sin ellos.
    }
  }

  async load(): Promise<void> {
    this.isLoading.set(true);
    try {
      const result = await firstValueFrom(
        this.service.list({
          q: this.q().trim() || undefined,
          status: this.status() ?? undefined,
          equipmentTypeId: this.equipmentTypeId() ?? undefined,
          warehouseId: this.warehouseId() ?? undefined,
          page: this.page(),
          size: ForniturasPage.PAGE_SIZE,
        }),
      );
      this.items.set(result.content);
      this.totalPages.set(result.totalPages);
      this.totalElements.set(result.totalElements);
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

  onSearchChange(value: string | null | undefined): void {
    this.q.set(value ?? '');
    this.applyFilters();
  }

  onStatusChange(value: EquipmentStatus | null): void {
    this.status.set(value);
    this.applyFilters();
  }

  onTypeChange(value: number | null): void {
    this.equipmentTypeId.set(value);
    this.applyFilters();
  }

  onWarehouseChange(value: number | null): void {
    this.warehouseId.set(value);
    this.applyFilters();
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

  statusLabel(status: EquipmentStatus): string {
    return this.statuses.find((s) => s.value === status)?.label ?? status;
  }

  statusColor(status: EquipmentStatus): string {
    return this.statuses.find((s) => s.value === status)?.color ?? 'medium';
  }

  expiryLabel(vigencia: ExpiryStatus): string {
    return EXPIRY_STATUSES[vigencia].label;
  }

  expiryColor(vigencia: ExpiryStatus): string {
    return EXPIRY_STATUSES[vigencia].color;
  }

  goToNew(): void {
    void this.router.navigate(['/fornituras/nuevo']);
  }

  goToBatch(): void {
    void this.router.navigate(['/fornituras/lote']);
  }

  goToEdit(item: EquipmentSummary): void {
    void this.router.navigate(['/fornituras', item.id]);
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
