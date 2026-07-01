import { Component, computed, inject, signal } from '@angular/core';
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
  ],
})
export class IncidenciaFormPage {
  private readonly service = inject(IncidentsService);
  private readonly equipmentService = inject(EquipmentService);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);

  readonly codigo = signal('');
  readonly resolved = signal<EquipmentDetail | null>(null);
  readonly codigoError = signal<string | null>(null);
  readonly resolving = signal(false);

  readonly tipo = signal<IncidentType | null>(null);
  readonly descripcion = signal('');
  readonly submitting = signal(false);

  readonly typeOptions = Object.entries(INCIDENT_TYPES) as [IncidentType, { label: string }][];

  readonly canSubmit = computed(
    () => this.resolved() !== null && this.tipo() !== null && this.descripcion().trim().length > 0,
  );

  constructor() {
    addIcons({ searchOutline });
  }

  onCodigoInput(value: string): void {
    this.codigo.set(value);
    // Al editar el código, la fornitura previamente resuelta deja de ser válida.
    this.resolved.set(null);
    this.codigoError.set(null);
  }

  async resolveCodigo(): Promise<void> {
    const code = this.codigo().trim();
    if (!code) {
      this.codigoError.set('Ingresa un código de fornitura.');
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
    if (!this.canSubmit()) {
      return;
    }
    this.submitting.set(true);
    try {
      await firstValueFrom(
        this.service.report({
          equipmentId: this.resolved()!.id,
          tipo: this.tipo()!,
          descripcion: this.descripcion().trim(),
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
