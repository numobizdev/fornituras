import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  AlertController,
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
  IonSpinner,
  IonTextarea,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { addOutline, trashOutline } from 'ionicons/icons';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { PhotoPickerComponent } from '../../../../core/media/photo-picker/photo-picker.component';
import { EquipmentTypesService } from '../../data/equipment-types.service';
import { SizeSummary } from '../../data/equipment-type.model';

@Component({
  selector: 'app-tipo-form',
  templateUrl: './tipo-form.page.html',
  styleUrls: ['./tipo-form.page.scss'],
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
    IonButton,
    IonIcon,
    IonSpinner,
    PhotoPickerComponent,
  ],
})
export class TipoFormPage implements OnInit {
  private readonly service = inject(EquipmentTypesService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);
  private readonly alertController = inject(AlertController);

  readonly typeId = signal<number | null>(null);
  readonly sizes = signal<SizeSummary[]>([]);
  readonly isLoading = signal(false);
  readonly isSubmitting = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    descripcion: ['', [Validators.maxLength(500)]],
    fotoUrl: ['', [Validators.maxLength(500)]],
  });

  constructor() {
    addIcons({ addOutline, trashOutline });
  }

  get isEditing(): boolean {
    return this.typeId() !== null;
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.typeId.set(Number(idParam));
      void this.loadDetail(Number(idParam));
    }
  }

  private async loadDetail(id: number): Promise<void> {
    this.isLoading.set(true);
    try {
      const detail = await firstValueFrom(this.service.getById(id));
      this.form.patchValue({
        nombre: detail.nombre,
        descripcion: detail.descripcion ?? '',
        fotoUrl: detail.fotoUrl ?? '',
      });
      this.sizes.set(detail.sizes);
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
    const request = {
      nombre: value.nombre,
      descripcion: value.descripcion || null,
      fotoUrl: value.fotoUrl || null,
    };

    try {
      const id = this.typeId();
      if (id !== null) {
        await firstValueFrom(this.service.update(id, request));
        await this.showToast('Tipo de prenda actualizado.', 'success');
      } else {
        await firstValueFrom(this.service.create(request));
        await this.showToast('Tipo de prenda creado.', 'success');
      }
      await this.router.navigate(['/tipos']);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error, 'No se pudo guardar el tipo de prenda.'), 'danger');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  async addSize(): Promise<void> {
    const id = this.typeId();
    if (id === null) {
      return;
    }
    const alert = await this.alertController.create({
      header: 'Nueva talla',
      inputs: [{ name: 'etiqueta', type: 'text', placeholder: 'Ej. M, 42, Grande' }],
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        {
          text: 'Agregar',
          handler: (data) => {
            const etiqueta = (data?.etiqueta ?? '').trim();
            if (etiqueta) {
              void this.persistSize(id, etiqueta);
            }
          },
        },
      ],
    });
    await alert.present();
  }

  private async persistSize(equipmentTypeId: number, etiqueta: string): Promise<void> {
    try {
      const created = await firstValueFrom(this.service.createSize({ etiqueta, equipmentTypeId }));
      this.sizes.update((current) => [...current, created]);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    }
  }

  async removeSize(size: SizeSummary): Promise<void> {
    try {
      await firstValueFrom(this.service.deactivateSize(size.id));
      this.sizes.update((current) => current.filter((s) => s.id !== size.id));
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
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
