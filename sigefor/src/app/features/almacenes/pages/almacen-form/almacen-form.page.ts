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
import { WarehousesService } from '../../data/warehouses.service';
import {
  WAREHOUSE_TYPES,
  WarehouseCreateRequest,
  WarehouseType,
} from '../../data/warehouse.model';

@Component({
  selector: 'app-almacen-form',
  templateUrl: './almacen-form.page.html',
  styleUrls: ['./almacen-form.page.scss'],
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
  ],
})
export class AlmacenFormPage implements OnInit {
  private readonly service = inject(WarehousesService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);

  readonly warehouseId = signal<number | null>(null);
  readonly isLoading = signal(false);
  readonly isSubmitting = signal(false);
  readonly types = WAREHOUSE_TYPES;

  readonly form = this.formBuilder.nonNullable.group({
    codigo: ['', [Validators.required, Validators.maxLength(40)]],
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    tipo: ['CENTRAL' as WarehouseType, [Validators.required]],
    municipioId: [''],
    direccion: ['', [Validators.maxLength(255)]],
    cp: ['', [Validators.maxLength(10)]],
    latitud: [''],
    longitud: [''],
    responsableId: [''],
    telefono: ['', [Validators.maxLength(30)]],
    emailContacto: ['', [Validators.email, Validators.maxLength(255)]],
    capacidad: [''],
    observaciones: ['', [Validators.maxLength(500)]],
  });

  get isEditing(): boolean {
    return this.warehouseId() !== null;
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.warehouseId.set(Number(idParam));
      void this.loadDetail(Number(idParam));
    }
  }

  private async loadDetail(id: number): Promise<void> {
    this.isLoading.set(true);
    try {
      const detail = await firstValueFrom(this.service.getById(id));
      this.form.patchValue({
        codigo: detail.codigo,
        nombre: detail.nombre,
        tipo: detail.tipo,
        municipioId: this.toText(detail.municipioId),
        direccion: detail.direccion ?? '',
        cp: detail.cp ?? '',
        latitud: this.toText(detail.latitud),
        longitud: this.toText(detail.longitud),
        responsableId: this.toText(detail.responsableId),
        telefono: detail.telefono ?? '',
        emailContacto: detail.emailContacto ?? '',
        capacidad: this.toText(detail.capacidad),
        observaciones: detail.observaciones ?? '',
      });
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
    const request: WarehouseCreateRequest = {
      codigo: value.codigo.trim(),
      nombre: value.nombre.trim(),
      tipo: value.tipo,
      municipioId: this.toNumber(value.municipioId),
      direccion: this.toNullable(value.direccion),
      cp: this.toNullable(value.cp),
      latitud: this.toNumber(value.latitud),
      longitud: this.toNumber(value.longitud),
      responsableId: this.toNumber(value.responsableId),
      telefono: this.toNullable(value.telefono),
      emailContacto: this.toNullable(value.emailContacto),
      capacidad: this.toNumber(value.capacidad),
      observaciones: this.toNullable(value.observaciones),
    };

    try {
      const id = this.warehouseId();
      if (id !== null) {
        await firstValueFrom(this.service.update(id, request));
        await this.showToast('Almacén actualizado.', 'success');
      } else {
        await firstValueFrom(this.service.create(request));
        await this.showToast('Almacén creado.', 'success');
      }
      await this.router.navigate(['/almacenes']);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error, 'No se pudo guardar el almacén.'), 'danger');
    } finally {
      this.isSubmitting.set(false);
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
