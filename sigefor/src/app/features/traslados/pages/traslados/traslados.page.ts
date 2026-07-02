import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom, Observable } from 'rxjs';
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
import { addOutline } from 'ionicons/icons';
import { ROLE_POLICY } from '../../../../core/security/role-policy';
import { AuthService } from '../../../../core/services/auth.service';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { WarehousesService } from '../../../almacenes/data/warehouses.service';
import { WarehouseSummary } from '../../../almacenes/data/warehouse.model';
import { TransfersService } from '../../data/transfers.service';
import { TRANSFER_STATUSES, TransferStatus, TransferSummary } from '../../data/transfer.model';

@Component({
  selector: 'app-traslados',
  templateUrl: './traslados.page.html',
  styleUrls: ['./traslados.page.scss'],
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
export class TrasladosPage implements OnInit {
  private readonly service = inject(TransfersService);
  private readonly warehousesService = inject(WarehousesService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);
  private readonly alertController = inject(AlertController);

  private static readonly PAGE_SIZE = 20;

  readonly transfers = signal<TransferSummary[]>([]);
  readonly warehouses = signal<WarehouseSummary[]>([]);
  readonly isLoading = signal(false);
  readonly page = signal(0);
  readonly totalPages = signal(0);

  readonly filterStatus = signal<TransferStatus | ''>('');
  readonly filterOrigen = signal<number | ''>('');

  readonly statuses = TRANSFER_STATUSES;
  readonly canWrite = computed(() => this.auth.hasAnyRole(ROLE_POLICY.WRITE_TRANSFERS));

  constructor() {
    addIcons({ addOutline });
  }

  ngOnInit(): void {
    void this.loadWarehouses();
  }

  // El listado se refresca en cada entrada (Ionic no re-ejecuta ngOnInit al volver del formulario).
  ionViewWillEnter(): void {
    void this.load();
  }

  private async loadWarehouses(): Promise<void> {
    try {
      const page = await firstValueFrom(this.warehousesService.list({ active: true, size: 100 }));
      this.warehouses.set(page.content);
    } catch {
      // El filtro de almacén es opcional; si falla la carga, la lista sigue operativa.
    }
  }

  async load(): Promise<void> {
    this.isLoading.set(true);
    try {
      const result = await firstValueFrom(
        this.service.list({
          page: this.page(),
          size: TrasladosPage.PAGE_SIZE,
          status: this.filterStatus() || undefined,
          origenId: this.filterOrigen() || undefined,
        }),
      );
      this.transfers.set(result.content);
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

  statusLabel(status: TransferStatus): string {
    return this.statuses[status].label;
  }

  statusColor(status: TransferStatus): string {
    return this.statuses[status].color;
  }

  newTransfer(): void {
    void this.router.navigate(['/traslados/nuevo']);
  }

  async receive(transfer: TransferSummary): Promise<void> {
    await this.confirmAction(
      'Recibir traslado',
      `¿Confirmar la recepción del traslado #${transfer.id}? Las ${transfer.itemCount} fornituras quedarán disponibles en ${transfer.destinoNombre}.`,
      'Recibir',
      () => this.service.receive(transfer.id),
      'Traslado recibido.',
    );
  }

  async cancel(transfer: TransferSummary): Promise<void> {
    await this.confirmAction(
      'Cancelar traslado',
      `¿Cancelar el traslado #${transfer.id}? Las fornituras volverán a estar disponibles en el origen.`,
      'Cancelar traslado',
      () => this.service.cancel(transfer.id),
      'Traslado cancelado.',
    );
  }

  private async confirmAction(
    header: string,
    message: string,
    confirmText: string,
    action: () => Observable<unknown>,
    successMessage: string,
  ): Promise<void> {
    const alert = await this.alertController.create({
      header,
      message,
      buttons: [
        { text: 'Volver', role: 'cancel' },
        {
          text: confirmText,
          handler: () => {
            void this.runAction(action, successMessage);
          },
        },
      ],
    });
    await alert.present();
  }

  private async runAction(
    action: () => Observable<unknown>,
    successMessage: string,
  ): Promise<void> {
    try {
      await firstValueFrom(action());
      await this.showToast(successMessage, 'success');
      await this.load();
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
