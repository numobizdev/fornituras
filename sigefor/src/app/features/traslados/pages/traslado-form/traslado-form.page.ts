import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonListHeader,
  IonNote,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTextarea,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { trashOutline } from 'ionicons/icons';
import { FieldErrorsComponent } from '../../../../core/forms/field-errors.component';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { QrScanComponent } from '../../../../core/qr-scan/qr-scan.component';
import { QrCaptureError } from '../../../../core/qr-scan/qr-scan.types';
import { WarehousesService } from '../../../almacenes/data/warehouses.service';
import { WarehouseSummary } from '../../../almacenes/data/warehouse.model';
import { EquipmentService } from '../../../fornituras/data/equipment.service';
import { EquipmentDetail } from '../../../fornituras/data/equipment.model';
import { TransfersService } from '../../data/transfers.service';
import { TransferCreateRequest } from '../../data/transfer.model';

@Component({
  selector: 'app-traslado-form',
  templateUrl: './traslado-form.page.html',
  styleUrls: ['./traslado-form.page.scss'],
  imports: [
    ReactiveFormsModule,
    IonHeader,
    IonToolbar,
    IonButtons,
    IonBackButton,
    IonTitle,
    IonContent,
    IonList,
    IonListHeader,
    IonItem,
    IonLabel,
    IonNote,
    IonSelect,
    IonSelectOption,
    IonTextarea,
    IonButton,
    IonIcon,
    IonSpinner,
    QrScanComponent,
    FieldErrorsComponent,
  ],
})
export class TrasladoFormPage implements OnInit {
  private readonly warehousesService = inject(WarehousesService);
  private readonly equipmentService = inject(EquipmentService);
  private readonly transfersService = inject(TransfersService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);

  readonly warehouses = signal<WarehouseSummary[]>([]);
  readonly items = signal<EquipmentDetail[]>([]);
  readonly isSubmitting = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    origenId: ['', [Validators.required]],
    destinoId: ['', [Validators.required]],
    observaciones: ['', [Validators.maxLength(500)]],
  });

  constructor() {
    addIcons({ trashOutline });
  }

  ngOnInit(): void {
    void this.loadWarehouses();
  }

  private async loadWarehouses(): Promise<void> {
    try {
      const page = await firstValueFrom(this.warehousesService.list({ active: true, size: 100 }));
      this.warehouses.set(page.content);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    }
  }

  private get origenId(): number | null {
    const raw = this.form.controls.origenId.value;
    return raw ? Number(raw) : null;
  }

  /** Código capturado (014): resuelve la fornitura server-side y valida que sea trasladable. */
  async onCodeCaptured(code: string): Promise<void> {
    const origenId = this.origenId;
    if (origenId === null) {
      await this.showToast('Seleccione primero el almacén origen.', 'warning');
      return;
    }
    let equipment: EquipmentDetail;
    try {
      equipment = await firstValueFrom(this.equipmentService.getByCodigo(code));
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error, 'No se encontró la fornitura.'), 'danger');
      return;
    }
    if (equipment.status !== 'DISPONIBLE') {
      await this.showToast(`La fornitura ${equipment.codigoQr} no está disponible.`, 'warning');
      return;
    }
    if (equipment.warehouseId !== origenId) {
      await this.showToast(`La fornitura ${equipment.codigoQr} no está en el almacén origen.`, 'warning');
      return;
    }
    if (this.items().some((e) => e.id === equipment.id)) {
      await this.showToast('Esa fornitura ya está en el traslado.', 'warning');
      return;
    }
    this.items.update((current) => [...current, equipment]);
  }

  async onCaptureError(error: QrCaptureError): Promise<void> {
    await this.showToast(error.message, 'warning');
  }

  removeItem(id: number): void {
    this.items.update((current) => current.filter((e) => e.id !== id));
  }

  async confirm(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    if (value.origenId === value.destinoId) {
      await this.showToast('El origen y el destino deben ser distintos.', 'warning');
      return;
    }
    if (this.items().length === 0) {
      await this.showToast('Agregue al menos una fornitura al traslado.', 'warning');
      return;
    }

    this.isSubmitting.set(true);
    const request: TransferCreateRequest = {
      origenId: Number(value.origenId),
      destinoId: Number(value.destinoId),
      equipmentIds: this.items().map((e) => e.id),
      observaciones: value.observaciones.trim() || null,
    };
    try {
      await firstValueFrom(this.transfersService.create(request));
      await this.showToast('Traslado creado.', 'success');
      await this.router.navigate(['/traslados']);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error, 'No se pudo crear el traslado.'), 'danger');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  cancel(): void {
    void this.router.navigate(['/traslados']);
  }

  private async showToast(message: string, color: string): Promise<void> {
    const toast = await this.toastController.create({
      message,
      duration: 3000,
      color,
      position: 'top',
    });
    await toast.present();
  }
}
