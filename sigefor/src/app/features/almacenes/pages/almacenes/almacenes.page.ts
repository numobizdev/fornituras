import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  AlertController,
  IonBadge,
  IonButton,
  IonButtons,
  IonContent,
  IonFab,
  IonFabButton,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonMenuButton,
  IonNote,
  IonSegment,
  IonSegmentButton,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { add, createOutline, powerOutline } from 'ionicons/icons';
import { CATALOG_CODES, CatalogItemSummary } from '../../../../core/catalog/catalog.model';
import { CatalogService } from '../../../../core/catalog/catalog.service';
import { ROLE_POLICY } from '../../../../core/security/role-policy';
import { AuthService } from '../../../../core/services/auth.service';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { WarehousesService } from '../../data/warehouses.service';
import { WarehouseSummary } from '../../data/warehouse.model';

@Component({
  selector: 'app-almacenes',
  templateUrl: './almacenes.page.html',
  styleUrls: ['./almacenes.page.scss'],
  imports: [
    IonHeader,
    IonToolbar,
    IonButtons,
    IonMenuButton,
    IonTitle,
    IonContent,
    IonSegment,
    IonSegmentButton,
    IonSelect,
    IonSelectOption,
    IonLabel,
    IonNote,
    IonList,
    IonItem,
    IonBadge,
    IonButton,
    IonIcon,
    IonSpinner,
    IonFab,
    IonFabButton,
  ],
})
export class AlmacenesPage implements OnInit {
  private readonly service = inject(WarehousesService);
  private readonly catalog = inject(CatalogService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);
  private readonly alertController = inject(AlertController);

  readonly warehouses = signal<WarehouseSummary[]>([]);
  readonly isLoading = signal(false);
  readonly filter = signal<'active' | 'inactive'>('active');
  readonly tipoFilter = signal<number | null>(null);
  readonly isAdmin = computed(() => this.auth.hasAnyRole(ROLE_POLICY.MANAGE_CONFIG));
  readonly tipoOptions = signal<CatalogItemSummary[]>([]);

  constructor() {
    addIcons({ add, createOutline, powerOutline });
  }

  ngOnInit(): void {
    void this.loadTipos();
  }

  // Se recarga en cada entrada (incluye volver del formulario): Ionic cachea la página y NO
  // re-ejecuta ngOnInit al navegar hacia atrás, por eso el listado debe refrescarse aquí.
  ionViewWillEnter(): void {
    void this.load();
  }

  private async loadTipos(): Promise<void> {
    try {
      this.tipoOptions.set(
        await firstValueFrom(this.catalog.listActiveItems(CATALOG_CODES.TIPO_ALMACEN)),
      );
    } catch {
      this.tipoOptions.set([]);
    }
  }

  async load(): Promise<void> {
    this.isLoading.set(true);
    try {
      const page = await firstValueFrom(
        this.service.list({
          active: this.filter() === 'active',
          tipoItemId: this.tipoFilter() ?? undefined,
          size: 100,
        }),
      );
      this.warehouses.set(page.content);
    } catch (error) {
      await this.showError(extractApiErrorMessage(error));
    } finally {
      this.isLoading.set(false);
    }
  }

  onFilterChange(value: string | undefined): void {
    if (value === 'active' || value === 'inactive') {
      this.filter.set(value);
      void this.load();
    }
  }

  onTipoChange(value: number | null): void {
    this.tipoFilter.set(value ?? null);
    void this.load();
  }

  goToNew(): void {
    void this.router.navigate(['/almacenes/nuevo']);
  }

  goToEdit(warehouse: WarehouseSummary): void {
    void this.router.navigate(['/almacenes', warehouse.id]);
  }

  async confirmDeactivate(warehouse: WarehouseSummary): Promise<void> {
    const alert = await this.alertController.create({
      header: 'Desactivar almacén',
      message: `¿Desactivar "${warehouse.nombre}"? Dejará de ofrecerse como ubicación o destino de traslados, pero no se borra.`,
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        {
          text: 'Desactivar',
          role: 'destructive',
          handler: () => {
            void this.deactivate(warehouse);
          },
        },
      ],
    });
    await alert.present();
  }

  private async deactivate(warehouse: WarehouseSummary): Promise<void> {
    try {
      await firstValueFrom(this.service.deactivate(warehouse.id));
      await this.showToast('Almacén desactivado.', 'success');
      await this.load();
    } catch (error) {
      await this.showError(extractApiErrorMessage(error));
    }
  }

  private async showError(message: string): Promise<void> {
    await this.showToast(message, 'danger');
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
