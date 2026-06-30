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
  IonInput,
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
import { addOutline, trashOutline } from 'ionicons/icons';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { EquipmentTypesService } from '../../../tipos/data/equipment-types.service';
import {
  EquipmentTypeSummary,
  SizeSummary,
} from '../../../tipos/data/equipment-type.model';
import { WarehousesService } from '../../../almacenes/data/warehouses.service';
import { WarehouseSummary } from '../../../almacenes/data/warehouse.model';
import { EquipmentService } from '../../data/equipment.service';
import { BatchCreateRequest } from '../../data/equipment.model';

@Component({
  selector: 'app-fornitura-lote',
  templateUrl: './fornitura-lote.page.html',
  styleUrls: ['./fornitura-lote.page.scss'],
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
    IonInput,
    IonTextarea,
    IonSelect,
    IonSelectOption,
    IonButton,
    IonIcon,
    IonSpinner,
  ],
})
export class FornituraLotePage implements OnInit {
  private readonly service = inject(EquipmentService);
  private readonly typesService = inject(EquipmentTypesService);
  private readonly warehousesService = inject(WarehousesService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);

  readonly types = signal<EquipmentTypeSummary[]>([]);
  readonly sizes = signal<SizeSummary[]>([]);
  readonly warehouses = signal<WarehouseSummary[]>([]);
  readonly codigos = signal<string[]>([]);
  readonly nextCode = signal('');
  readonly isSubmitting = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    equipmentTypeId: ['', [Validators.required]],
    sizeId: [''],
    warehouseId: ['', [Validators.required]],
    descripcion: ['', [Validators.maxLength(255)]],
    marca: ['', [Validators.maxLength(120)]],
    modelo: ['', [Validators.maxLength(120)]],
    nivelBalistico: ['', [Validators.maxLength(60)]],
    fechaFabricacion: [''],
    fechaAdquisicion: [''],
    vidaUtilMeses: [''],
    fechaVencimiento: [''],
    observaciones: ['', [Validators.maxLength(500)]],
  });

  constructor() {
    addIcons({ addOutline, trashOutline });
  }

  ngOnInit(): void {
    void this.loadCatalogs();
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

  addCode(): void {
    const raw = this.nextCode().trim();
    if (!raw) {
      return;
    }
    const normalized = this.normalize(raw);
    const exists = this.codigos().some((c) => this.normalize(c) === normalized);
    if (exists) {
      void this.showToast('Ese código ya está en el lote.', 'warning');
      return;
    }
    this.codigos.update((current) => [...current, raw.toUpperCase()]);
    this.nextCode.set('');
  }

  removeCode(index: number): void {
    this.codigos.update((current) => current.filter((_, i) => i !== index));
  }

  async createBatch(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.codigos().length === 0) {
      await this.showToast('Agregue al menos un código al lote.', 'warning');
      return;
    }

    this.isSubmitting.set(true);
    const value = this.form.getRawValue();
    const request: BatchCreateRequest = {
      equipmentTypeId: Number(value.equipmentTypeId),
      sizeId: this.toNumber(value.sizeId),
      warehouseId: Number(value.warehouseId),
      descripcion: this.toNullable(value.descripcion),
      marca: this.toNullable(value.marca),
      modelo: this.toNullable(value.modelo),
      nivelBalistico: this.toNullable(value.nivelBalistico),
      fechaFabricacion: this.toNullable(value.fechaFabricacion),
      fechaAdquisicion: this.toNullable(value.fechaAdquisicion),
      vidaUtilMeses: this.toNumber(value.vidaUtilMeses),
      fechaVencimiento: this.toNullable(value.fechaVencimiento),
      observaciones: this.toNullable(value.observaciones),
      codigos: this.codigos(),
    };

    try {
      const created = await firstValueFrom(this.service.createBatch(request));
      await this.showToast(`Lote creado: ${created.length} fornituras.`, 'success');
      await this.router.navigate(['/fornituras']);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error, 'No se pudo crear el lote.'), 'danger');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  cancel(): void {
    void this.router.navigate(['/fornituras']);
  }

  private normalize(raw: string): string {
    return raw.replace(/[\s-]+/g, '').toUpperCase();
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
      duration: 3000,
      color,
      position: 'top',
    });
    await toast.present();
  }
}
