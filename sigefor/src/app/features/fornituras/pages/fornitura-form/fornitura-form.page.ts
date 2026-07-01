import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput,
  IonItem,
  IonLabel,
  IonList,
  IonListHeader,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTextarea,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { QrScanComponent } from '../../../../core/qr-scan/qr-scan.component';
import { QrCaptureError } from '../../../../core/qr-scan/qr-scan.types';
import { EquipmentTypesService } from '../../../tipos/data/equipment-types.service';
import {
  EquipmentTypeSummary,
  SizeSummary,
} from '../../../tipos/data/equipment-type.model';
import { WarehousesService } from '../../../almacenes/data/warehouses.service';
import { WarehouseSummary } from '../../../almacenes/data/warehouse.model';
import { EquipmentService } from '../../data/equipment.service';
import {
  EQUIPMENT_STATUSES,
  EquipmentCreateRequest,
  EquipmentStatus,
} from '../../data/equipment.model';

@Component({
  selector: 'app-fornitura-form',
  templateUrl: './fornitura-form.page.html',
  styleUrls: ['./fornitura-form.page.scss'],
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
    IonInput,
    IonTextarea,
    IonSelect,
    IonSelectOption,
    IonButton,
    IonSpinner,
    QrScanComponent,
  ],
})
export class FornituraFormPage implements OnInit {
  private readonly service = inject(EquipmentService);
  private readonly typesService = inject(EquipmentTypesService);
  private readonly warehousesService = inject(WarehousesService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);

  readonly equipmentId = signal<number | null>(null);
  readonly isLoading = signal(false);
  readonly isSubmitting = signal(false);

  readonly types = signal<EquipmentTypeSummary[]>([]);
  readonly sizes = signal<SizeSummary[]>([]);
  readonly warehouses = signal<WarehouseSummary[]>([]);

  readonly statuses = EQUIPMENT_STATUSES;
  readonly currentStatus = signal<EquipmentStatus | null>(null);
  readonly statusToApply = signal<EquipmentStatus | null>(null);

  readonly form = this.formBuilder.nonNullable.group({
    codigoQr: ['', [Validators.required, Validators.maxLength(60)]],
    equipmentTypeId: ['', [Validators.required]],
    sizeId: [''],
    warehouseId: ['', [Validators.required]],
    descripcion: ['', [Validators.maxLength(255)]],
    marca: ['', [Validators.maxLength(120)]],
    modelo: ['', [Validators.maxLength(120)]],
    nivelBalistico: ['', [Validators.maxLength(60)]],
    numeroInventario: ['', [Validators.maxLength(60)]],
    fechaFabricacion: [''],
    fechaAdquisicion: [''],
    vidaUtilMeses: [''],
    fechaVencimiento: [''],
    observaciones: ['', [Validators.maxLength(500)]],
    fotoUrl: ['', [Validators.maxLength(500)]],
  });

  get isEditing(): boolean {
    return this.equipmentId() !== null;
  }

  ngOnInit(): void {
    void this.bootstrap();
  }

  private async bootstrap(): Promise<void> {
    await this.loadCatalogs();
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.equipmentId.set(Number(idParam));
      await this.loadDetail(Number(idParam));
    }
  }

  private async loadCatalogs(): Promise<void> {
    try {
      const [types, warehouses] = await Promise.all([
        firstValueFrom(this.typesService.list({ active: true, size: 100 })),
        firstValueFrom(this.warehousesService.list({ active: true, size: 100 })),
      ]);
      this.types.set(types.content);
      this.warehouses.set(warehouses.content);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    }
  }

  /** Código capturado por lector/cámara/manual (componente 014): llena el control del formulario. */
  onCodeCaptured(code: string): void {
    this.form.controls.codigoQr.setValue(code);
    this.form.controls.codigoQr.markAsDirty();
  }

  async onCaptureError(error: QrCaptureError): Promise<void> {
    await this.showToast(error.message, 'warning');
  }

  async onTypeSelected(value: number | string | null): Promise<void> {
    const typeId = value ? Number(value) : null;
    this.form.controls.sizeId.setValue('');
    if (typeId === null) {
      this.sizes.set([]);
      return;
    }
    try {
      this.sizes.set(await firstValueFrom(this.typesService.listSizes(typeId)));
    } catch {
      this.sizes.set([]);
    }
  }

  private async loadDetail(id: number): Promise<void> {
    this.isLoading.set(true);
    try {
      const detail = await firstValueFrom(this.service.getById(id));
      if (detail.equipmentTypeId) {
        this.sizes.set(await firstValueFrom(this.typesService.listSizes(detail.equipmentTypeId)));
      }
      this.currentStatus.set(detail.status);
      this.statusToApply.set(detail.status);
      this.form.patchValue({
        codigoQr: detail.codigoQr,
        equipmentTypeId: this.toText(detail.equipmentTypeId),
        sizeId: this.toText(detail.sizeId),
        warehouseId: this.toText(detail.warehouseId),
        descripcion: detail.descripcion ?? '',
        marca: detail.marca ?? '',
        modelo: detail.modelo ?? '',
        nivelBalistico: detail.nivelBalistico ?? '',
        numeroInventario: detail.numeroInventario ?? '',
        fechaFabricacion: detail.fechaFabricacion ?? '',
        fechaAdquisicion: detail.fechaAdquisicion ?? '',
        vidaUtilMeses: this.toText(detail.vidaUtilMeses),
        fechaVencimiento: detail.fechaVencimiento ?? '',
        observaciones: detail.observaciones ?? '',
        fotoUrl: detail.fotoUrl ?? '',
      });
      // El código es inmutable en edición: el backend lo rechaza, así que se bloquea en la UI.
      this.form.controls.codigoQr.disable();
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    } finally {
      this.isLoading.set(false);
    }
  }

  async onSubmit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const value = this.form.getRawValue();
    const request: EquipmentCreateRequest = {
      codigoQr: value.codigoQr.trim(),
      equipmentTypeId: Number(value.equipmentTypeId),
      sizeId: this.toNumber(value.sizeId),
      warehouseId: Number(value.warehouseId),
      descripcion: this.toNullable(value.descripcion),
      marca: this.toNullable(value.marca),
      modelo: this.toNullable(value.modelo),
      nivelBalistico: this.toNullable(value.nivelBalistico),
      numeroInventario: this.toNullable(value.numeroInventario),
      fechaFabricacion: this.toNullable(value.fechaFabricacion),
      fechaAdquisicion: this.toNullable(value.fechaAdquisicion),
      vidaUtilMeses: this.toNumber(value.vidaUtilMeses),
      fechaVencimiento: this.toNullable(value.fechaVencimiento),
      observaciones: this.toNullable(value.observaciones),
      fotoUrl: this.toNullable(value.fotoUrl),
    };

    try {
      const id = this.equipmentId();
      if (id !== null) {
        await firstValueFrom(this.service.update(id, request));
        await this.showToast('Fornitura actualizada.', 'success');
      } else {
        await firstValueFrom(this.service.create(request));
        await this.showToast('Fornitura registrada.', 'success');
      }
      await this.router.navigate(['/fornituras']);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error, 'No se pudo guardar la fornitura.'), 'danger');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  async applyStatus(): Promise<void> {
    const id = this.equipmentId();
    const next = this.statusToApply();
    if (id === null || next === null || next === this.currentStatus()) {
      return;
    }
    try {
      const updated = await firstValueFrom(this.service.changeStatus(id, next));
      this.currentStatus.set(updated.status);
      await this.showToast('Estado actualizado.', 'success');
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
      this.statusToApply.set(this.currentStatus());
    }
  }

  private toText(value: number | null): string {
    return value === null || value === undefined ? '' : String(value);
  }

  private toNullable(value: string): string | null {
    const trimmed = value.trim();
    return trimmed.length === 0 ? null : trimmed;
  }

  private toNumber(value: string): number | null {
    const trimmed = value.trim();
    if (trimmed.length === 0) {
      return null;
    }
    const parsed = Number(trimmed);
    return Number.isNaN(parsed) ? null : parsed;
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
