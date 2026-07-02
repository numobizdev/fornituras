import { Component, inject, signal } from '@angular/core';
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
import { searchOutline } from 'ionicons/icons';
import { FieldErrorsComponent } from '../../../../core/forms/field-errors.component';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { EquipmentService } from '../../../fornituras/data/equipment.service';
import { EquipmentDetail } from '../../../fornituras/data/equipment.model';
import { IncidentsService } from '../../data/incidents.service';
import { INCIDENT_TYPES, IncidentType } from '../../data/incident.model';

@Component({
  selector: 'app-incidencia-form',
  templateUrl: './incidencia-form.page.html',
  styleUrls: ['./incidencia-form.page.scss'],
  imports: [
    ReactiveFormsModule,
    IonHeader,
    IonToolbar,
    IonButtons,
    IonBackButton,
    IonTitle,
    IonContent,
    IonList,
    IonItem,
    IonInput,
    IonButton,
    IonIcon,
    IonSelect,
    IonSelectOption,
    IonTextarea,
    IonNote,
    IonLabel,
    IonSpinner,
    FieldErrorsComponent,
  ],
})
export class IncidenciaFormPage {
  private readonly service = inject(IncidentsService);
  private readonly equipmentService = inject(EquipmentService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);

  // Mismo esquema de validación que el resto de los formularios (021, FR-011).
  readonly form = this.formBuilder.nonNullable.group({
    codigo: ['', [Validators.required]],
    tipo: this.formBuilder.control<IncidentType | null>(null, [Validators.required]),
    descripcion: ['', [Validators.required, Validators.maxLength(500)]],
  });

  readonly resolved = signal<EquipmentDetail | null>(null);
  readonly codigoError = signal<string | null>(null);
  readonly resolving = signal(false);
  readonly submitting = signal(false);

  readonly typeOptions = Object.entries(INCIDENT_TYPES) as [IncidentType, { label: string }][];

  constructor() {
    addIcons({ searchOutline });
  }

  onCodigoInput(value: string): void {
    this.form.controls.codigo.setValue(value);
    // Al editar el código, la fornitura previamente resuelta deja de ser válida.
    this.resolved.set(null);
    this.codigoError.set(null);
  }

  async resolveCodigo(): Promise<void> {
    const codigoControl = this.form.controls.codigo;
    const code = codigoControl.value.trim();
    if (!code) {
      codigoControl.markAsTouched();
      return;
    }
    this.resolving.set(true);
    this.codigoError.set(null);
    try {
      this.resolved.set(await firstValueFrom(this.equipmentService.getByCodigo(code)));
    } catch {
      this.resolved.set(null);
      this.codigoError.set('No se encontró una fornitura con ese código.');
    } finally {
      this.resolving.set(false);
    }
  }

  async submit(): Promise<void> {
    if (this.form.invalid || this.resolved() === null) {
      this.form.markAllAsTouched();
      if (this.form.controls.codigo.valid && this.resolved() === null) {
        this.codigoError.set('Busque el código para identificar la fornitura antes de reportar.');
      }
      return;
    }
    this.submitting.set(true);
    try {
      const value = this.form.getRawValue();
      await firstValueFrom(
        this.service.report({
          equipmentId: this.resolved()!.id,
          tipo: value.tipo!,
          descripcion: value.descripcion.trim(),
        }),
      );
      await this.showToast('Incidencia reportada.', 'success');
      void this.router.navigate(['/incidencias']);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    } finally {
      this.submitting.set(false);
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
