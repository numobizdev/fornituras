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
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { QrScanComponent } from '../../../../core/qr-scan/qr-scan.component';
import { QrCaptureError } from '../../../../core/qr-scan/qr-scan.types';
import { EquipmentService } from '../../../fornituras/data/equipment.service';
import { EquipmentDetail } from '../../../fornituras/data/equipment.model';
import { DecommissionsService } from '../../data/decommissions.service';
import { DecommissionReasonItem, DecommissionRequest } from '../../data/decommission.model';

@Component({
  selector: 'app-baja-form',
  templateUrl: './baja-form.page.html',
  styleUrls: ['./baja-form.page.scss'],
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
    IonSpinner,
    QrScanComponent,
  ],
})
export class BajaFormPage implements OnInit {
  private readonly equipmentService = inject(EquipmentService);
  private readonly decommissionsService = inject(DecommissionsService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);

  readonly reasons = signal<DecommissionReasonItem[]>([]);
  readonly equipment = signal<EquipmentDetail | null>(null);
  readonly isSubmitting = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    motivoId: ['', [Validators.required]],
    observaciones: ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    void this.loadReasons();
  }

  private async loadReasons(): Promise<void> {
    try {
      this.reasons.set(await firstValueFrom(this.decommissionsService.reasons()));
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error, 'No se pudieron cargar los motivos.'), 'danger');
    }
  }

  /** Código capturado (014): resuelve la fornitura server-side; la baja definitiva no admite reversión. */
  async onCodeCaptured(code: string): Promise<void> {
    let equipment: EquipmentDetail;
    try {
      equipment = await firstValueFrom(this.equipmentService.getByCodigo(code));
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error, 'No se encontró la fornitura.'), 'danger');
      return;
    }
    if (equipment.status === 'BAJA_DEFINITIVA') {
      await this.showToast(`La fornitura ${equipment.codigoQr} ya está dada de baja.`, 'warning');
      this.equipment.set(null);
      return;
    }
    this.equipment.set(equipment);
  }

  async onCaptureError(error: QrCaptureError): Promise<void> {
    await this.showToast(error.message, 'warning');
  }

  clearEquipment(): void {
    this.equipment.set(null);
  }

  async confirm(): Promise<void> {
    const equipment = this.equipment();
    if (equipment === null) {
      await this.showToast('Escanee o teclee el código de la fornitura a dar de baja.', 'warning');
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const value = this.form.getRawValue();
    const request: DecommissionRequest = {
      codigo: equipment.codigoQr,
      motivoId: Number(value.motivoId),
      observaciones: value.observaciones.trim() || null,
    };
    try {
      await firstValueFrom(this.decommissionsService.decommission(request));
      await this.showToast('Fornitura dada de baja.', 'success');
      await this.router.navigate(['/bajas']);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error, 'No se pudo dar de baja la fornitura.'), 'danger');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  cancel(): void {
    void this.router.navigate(['/bajas']);
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
